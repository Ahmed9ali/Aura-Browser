package com.example.devtools

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class DevToolsEngine(
    private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadStartTimestamp = System.currentTimeMillis()

    var webViewProvider: (() -> WebView?)? = null

    private val _activeTool = MutableStateFlow<DevToolTab?>(null)
    val activeTool: StateFlow<DevToolTab?> = _activeTool.asStateFlow()

    private val _isSheetMinimized = MutableStateFlow(false)
    val isSheetMinimized: StateFlow<Boolean> = _isSheetMinimized.asStateFlow()

    // 1. Source View State
    private val _pageSourceHtml = MutableStateFlow("")
    val pageSourceHtml: StateFlow<String> = _pageSourceHtml.asStateFlow()

    private val _sourceUrl = MutableStateFlow("")
    val sourceUrl: StateFlow<String> = _sourceUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _isSourceLoading = MutableStateFlow(false)
    val isSourceLoading: StateFlow<Boolean> = _isSourceLoading.asStateFlow()

    private val _sourceMode = MutableStateFlow("LIVE_DOM") // "LIVE_DOM" or "RAW_HTTP"
    val sourceMode: StateFlow<String> = _sourceMode.asStateFlow()

    // 2. DOM Inspector State
    private val _isInspectTouchMode = MutableStateFlow(false)
    val isInspectTouchMode: StateFlow<Boolean> = _isInspectTouchMode.asStateFlow()

    private val _inspectedElement = MutableStateFlow<InspectedDomElement?>(null)
    val inspectedElement: StateFlow<InspectedDomElement?> = _inspectedElement.asStateFlow()

    private val _highlightedSelector = MutableStateFlow<String?>(null)
    val highlightedSelector: StateFlow<String?> = _highlightedSelector.asStateFlow()

    private val _liveDomNodes = MutableStateFlow<List<LiveDomNode>>(emptyList())
    val liveDomNodes: StateFlow<List<LiveDomNode>> = _liveDomNodes.asStateFlow()

    private val _domTreeNodes = MutableStateFlow<List<DomTreeNodeSummary>>(emptyList())
    val domTreeNodes: StateFlow<List<DomTreeNodeSummary>> = _domTreeNodes.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogEntry>> = _consoleLogs.asStateFlow()

    // 3. Network Log Sniffer State
    private val _networkLogs = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
    val networkLogs: StateFlow<List<NetworkLogEntry>> = _networkLogs.asStateFlow()

    private val _isSnifferPaused = MutableStateFlow(false)
    val isSnifferPaused: StateFlow<Boolean> = _isSnifferPaused.asStateFlow()

    private val _isFetchingPayloadId = MutableStateFlow<String?>(null)
    val isFetchingPayloadId: StateFlow<String?> = _isFetchingPayloadId.asStateFlow()

    val jsInterface = AuraDevToolsJsBridge()

    private val trackerDomains = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "google-analytics.com",
        "googletagmanager.com",
        "facebook.net",
        "adsystem.com",
        "adnxs.com",
        "scorecardresearch.com",
        "criteo.com",
        "taboola.com",
        "outbrain.com",
        "hotjar.com"
    )

    fun isTrackerUrl(url: String): Boolean {
        val lower = url.lowercase()
        return trackerDomains.any { lower.contains(it) }
    }

    fun openTool(tool: DevToolTab, fallbackUrl: String = "") {
        _activeTool.value = tool
        _isSheetMinimized.value = false
        when (tool) {
            DevToolTab.SOURCE_VIEW -> {
                if (_pageSourceHtml.value.isBlank() || (fallbackUrl.isNotBlank() && fallbackUrl != _sourceUrl.value)) {
                    fetchLivePageSource(fallbackUrl)
                }
            }
            DevToolTab.INSPECT_DOM -> {
                scanLiveDomTree()
                if (_inspectedElement.value == null) {
                    val firstNode = _liveDomNodes.value.firstOrNull { it.tagName in listOf("h1", "h2", "form", "input", "a", "p", "div") }
                        ?: _liveDomNodes.value.firstOrNull()
                    if (firstNode != null) {
                        inspectLiveDomNode(firstNode, fromTouch = false)
                    } else {
                        selectDomNodeBySelector("body")
                    }
                }
            }
            DevToolTab.NETWORK_SNIFFER -> {
                syncPerformanceResourceEntries()
            }
        }
    }

    fun closeTool() {
        _activeTool.value = null
        _isSheetMinimized.value = false
        setInspectTouchMode(false)
        clearDomHighlightOnPage()
    }

    fun setSheetMinimized(minimized: Boolean) {
        _isSheetMinimized.value = minimized
    }

    fun toggleInspectTouchMode() {
        setInspectTouchMode(!_isInspectTouchMode.value)
    }

    fun setInspectTouchMode(enabled: Boolean) {
        _isInspectTouchMode.value = enabled
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.setTouchInspectMode($enabled); }",
                    null
                )
            }
        }
        if (enabled) {
            _isSheetMinimized.value = true
        }
    }

    fun clearDomHighlightOnPage() {
        _highlightedSelector.value = null
        runOnWebView { wv ->
            wv?.evaluateJavascript(
                "if (window.__auraDevTools) { window.__auraDevTools.clearHighlight(); }",
                null
            )
        }
    }

    /**
     * Called when the native HTTP/HTML engine loads a live page document.
     * Registers the main document network log, parses the real DOM tree, updates Source View,
     * and probes real sub-resources (CSS, JS, Images) for the Network Log Sniffer.
     */
    fun onLiveHtmlDocumentLoaded(
        url: String,
        title: String,
        rawHtml: String,
        nodes: List<LiveDomNode>,
        mainEntry: NetworkLogEntry,
        subresourceUrls: List<Pair<String, NetworkResourceType>>,
        adShieldEnabled: Boolean
    ) {
        _sourceUrl.value = url
        _pageTitle.value = title
        _pageSourceHtml.value = rawHtml
        _isSourceLoading.value = false
        _liveDomNodes.value = nodes
        rebuildDomTreeSummaries(nodes)

        if (_inspectedElement.value == null && nodes.isNotEmpty()) {
            val defaultNode = nodes.firstOrNull { it.tagName in listOf("h1", "h2", "input", "a", "p") } ?: nodes.first()
            inspectLiveDomNode(defaultNode, fromTouch = false)
        }

        if (!_isSnifferPaused.value) {
            _networkLogs.update { current ->
                (listOf(mainEntry) + current).take(400)
            }
            probeSubresourcesAsync(url, subresourceUrls, adShieldEnabled)
        }
    }

    private fun rebuildDomTreeSummaries(nodes: List<LiveDomNode>) {
        val summaries = nodes.mapIndexed { idx, n ->
            val approxHeight = when (n.tagName) {
                "h1", "h2" -> 44
                "p", "article", "section", "div" -> 64
                "input", "button", "select" -> 48
                else -> 32
            }
            DomTreeNodeSummary(
                index = idx,
                tagName = n.tagName,
                id = n.id,
                className = n.className,
                cssSelector = n.cssSelector,
                depth = n.depth,
                textSnippet = n.innerText.take(80),
                childCount = n.attributes.size,
                dimensionsLabel = "360×$approxHeight"
            )
        }
        _domTreeNodes.value = summaries
    }

    fun inspectLiveDomNode(node: LiveDomNode, fromTouch: Boolean) {
        _highlightedSelector.value = node.cssSelector
        val defaultStyles = mutableMapOf(
            "display" to when (node.tagName) {
                "a", "span", "code", "strong", "em" -> "inline-block"
                "input", "button", "select" -> "inline-flex"
                else -> "block"
            },
            "position" to "relative",
            "font-size" to when (node.tagName) {
                "h1" -> "24px"
                "h2" -> "20px"
                "h3" -> "17px"
                "code", "pre" -> "12.5px"
                else -> "14px"
            },
            "font-weight" to if (node.tagName.startsWith("h") || node.tagName == "button") "700" else "400",
            "padding" to if (node.tagName in listOf("input", "button")) "10px 14px" else "4px 0px",
            "margin" to "4px 0px"
        )
        defaultStyles.putAll(node.inlineStyles)

        val approxHeight = when (node.tagName) {
            "h1", "h2" -> 44
            "p", "article", "section", "div" -> 64
            "input", "button", "select" -> 48
            else -> 32
        }

        _inspectedElement.value = InspectedDomElement(
            tagName = node.tagName,
            id = node.id,
            className = node.className,
            cssSelector = node.cssSelector,
            xpath = node.xpath,
            widthPx = 360,
            heightPx = approxHeight,
            topPx = (node.index + 1) * 42,
            leftPx = 16,
            attributes = node.attributes,
            computedStyles = defaultStyles,
            outerHtml = node.outerHtml,
            innerText = node.innerText,
            childCount = node.attributes.size
        )

        if (fromTouch) {
            _isInspectTouchMode.value = false
            _activeTool.value = DevToolTab.INSPECT_DOM
            _isSheetMinimized.value = false
        }
    }

    private fun probeSubresourcesAsync(
        refererUrl: String,
        subresources: List<Pair<String, NetworkResourceType>>,
        adShieldEnabled: Boolean
    ) {
        scope.launch(Dispatchers.IO) {
            subresources.take(18).forEach { (resUrl, resType) ->
                if (_isSnifferPaused.value) return@forEach
                val isBlocked = adShieldEnabled && isTrackerUrl(resUrl)
                val reqHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36",
                    "Referer" to refererUrl,
                    "Accept" to when (resType) {
                        NetworkResourceType.STYLESHEET -> "text/css,*/*;q=0.1"
                        NetworkResourceType.SCRIPT -> "*/*"
                        NetworkResourceType.IMAGE -> "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
                        else -> "*/*"
                    }
                )
                if (isBlocked) {
                    val blockedEntry = NetworkLogEntry(
                        url = resUrl,
                        method = "GET",
                        resourceType = NetworkResourceType.BLOCKED,
                        statusCode = 0,
                        statusText = "BLOCKED BY SHIELD",
                        requestHeaders = reqHeaders,
                        isMainFrame = false,
                        isBlockedByShield = true
                    )
                    _networkLogs.update { (listOf(blockedEntry) + it).take(400) }
                } else {
                    val start = System.currentTimeMillis()
                    try {
                        val conn = (URL(resUrl).openConnection() as HttpURLConnection).apply {
                            requestMethod = "HEAD"
                            connectTimeout = 3500
                            readTimeout = 3500
                            instanceFollowRedirects = true
                            reqHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                        }
                        val code = conn.responseCode
                        val msg = conn.responseMessage ?: "OK"
                        val mime = conn.contentType ?: ""
                        val len = conn.contentLengthLong.takeIf { it > 0 }
                        val respHeaders = mutableMapOf<String, String>()
                        conn.headerFields?.forEach { (k, v) ->
                            if (k != null && !v.isNullOrEmpty()) {
                                respHeaders[k] = v.joinToString(", ")
                            }
                        }
                        conn.disconnect()
                        val elapsed = System.currentTimeMillis() - start
                        val entry = NetworkLogEntry(
                            url = resUrl,
                            method = "GET",
                            resourceType = resType,
                            statusCode = code,
                            statusText = msg,
                            mimeType = mime,
                            durationMs = elapsed,
                            transferSizeBytes = len,
                            requestHeaders = reqHeaders,
                            responseHeaders = respHeaders,
                            isMainFrame = false,
                            isBlockedByShield = false
                        )
                        _networkLogs.update { (listOf(entry) + it).take(400) }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun fetchLivePageSource(fallbackUrl: String = "") {
        _sourceMode.value = "LIVE_DOM"
        val targetUrl = fallbackUrl.ifBlank { _sourceUrl.value }
        if (_pageSourceHtml.value.isNotBlank() && (targetUrl.isBlank() || targetUrl == _sourceUrl.value)) {
            _isSourceLoading.value = false
            return
        }
        _isSourceLoading.value = true
        runOnWebView { wv ->
            val currentUrl = wv?.url?.takeIf { it.isNotBlank() && !it.startsWith("aura://") && it != "about:blank" }
                ?: targetUrl
            _sourceUrl.value = currentUrl
            if (wv != null && !wv.url.isNullOrBlank() && !wv.url!!.startsWith("aura://") && wv.url != "about:blank") {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var dt = document.doctype ? '<!DOCTYPE ' + document.doctype.name + '>\n' : '<!DOCTYPE html>\n';
                            var html = document.documentElement ? (dt + document.documentElement.outerHTML) : '';
                            if (window.AuraDevTools && html.length > 0) {
                                window.AuraDevTools.onPageSourceReceived(html, window.location.href);
                            }
                        } catch (e) {}
                    })();
                    """.trimIndent(),
                    null
                )
            } else if (currentUrl.startsWith("http")) {
                scope.launch(Dispatchers.IO) {
                    fetchRawHttpSourceInternal(currentUrl, keepModeLabel = false)
                }
            } else {
                _isSourceLoading.value = false
                _pageSourceHtml.value = "<!-- Open any website to view its live HTML DOM or fetch raw HTTP source -->"
            }
        }
    }

    fun fetchRawHttpSource(targetUrl: String) {
        val urlToFetch = targetUrl.ifBlank { _sourceUrl.value }
        if (!urlToFetch.startsWith("http")) return
        _sourceMode.value = "RAW_HTTP"
        _isSourceLoading.value = true
        _sourceUrl.value = urlToFetch
        scope.launch(Dispatchers.IO) {
            fetchRawHttpSourceInternal(urlToFetch, keepModeLabel = true)
        }
    }

    private fun fetchRawHttpSourceInternal(targetUrl: String, keepModeLabel: Boolean) {
        try {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }
            val code = conn.responseCode
            val stream = if (code in 200..399) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val sb = StringBuilder()
            var line: String?
            var totalChars = 0
            while (reader.readLine().also { line = it } != null && totalChars < 300_000) {
                sb.append(line).append('\n')
                totalChars += (line?.length ?: 0) + 1
            }
            reader.close()
            conn.disconnect()
            if (keepModeLabel) {
                _sourceMode.value = "RAW_HTTP"
            }
            _pageSourceHtml.value = sb.toString().ifBlank { "<!-- HTTP $code: Empty response body from $targetUrl -->" }
        } catch (e: Exception) {
            _pageSourceHtml.value = "<!-- Failed to fetch raw HTTP source from $targetUrl: ${e.message} -->"
        } finally {
            _isSourceLoading.value = false
        }
    }

    fun scanLiveDomTree() {
        if (_liveDomNodes.value.isNotEmpty()) {
            rebuildDomTreeSummaries(_liveDomNodes.value)
        }
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.scanDomTree(); }",
                    null
                )
            }
        }
    }

    fun selectDomNodeBySelector(selector: String) {
        if (selector.isBlank()) return
        val match = _liveDomNodes.value.firstOrNull {
            it.cssSelector.equals(selector, ignoreCase = true) ||
                it.tagName.equals(selector, ignoreCase = true)
        }
        if (match != null) {
            inspectLiveDomNode(match, fromTouch = false)
        }
        val escaped = JSONObject.quote(selector)
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.inspectBySelector($escaped, true); }",
                    null
                )
            }
        }
    }

    fun updateInspectedElementText(selector: String, newText: String) {
        // Update live native DOM nodes
        _liveDomNodes.update { list ->
            list.map { node ->
                if (node.cssSelector == selector) {
                    val updatedOuter = "<${node.tagName}>${newText}</${node.tagName}>"
                    val updated = node.copy(innerText = newText, outerHtml = updatedOuter)
                    inspectLiveDomNode(updated, fromTouch = false)
                    updated
                } else {
                    node
                }
            }
        }
        rebuildDomTreeSummaries(_liveDomNodes.value)

        val selEsc = JSONObject.quote(selector)
        val txtEsc = JSONObject.quote(newText)
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.updateElementText($selEsc, $txtEsc); }",
                    null
                )
            }
        }
    }

    fun updateInspectedElementStyle(selector: String, property: String, value: String) {
        _liveDomNodes.update { list ->
            list.map { node ->
                if (node.cssSelector == selector) {
                    val newStyles = node.inlineStyles.toMutableMap().apply { put(property, value) }
                    val updated = node.copy(inlineStyles = newStyles)
                    inspectLiveDomNode(updated, fromTouch = false)
                    updated
                } else {
                    node
                }
            }
        }

        val selEsc = JSONObject.quote(selector)
        val propEsc = JSONObject.quote(property)
        val valEsc = JSONObject.quote(value)
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.updateElementStyle($selEsc, $propEsc, $valEsc); }",
                    null
                )
            }
        }
    }

    fun removeInspectedElement(selector: String) {
        _liveDomNodes.update { list ->
            list.filterNot { it.cssSelector == selector }
        }
        rebuildDomTreeSummaries(_liveDomNodes.value)
        _inspectedElement.value = null
        _highlightedSelector.value = null

        val selEsc = JSONObject.quote(selector)
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.removeElement($selEsc); }",
                    null
                )
            }
        }
    }

    fun evaluateConsoleCommand(expression: String) {
        val clean = expression.trim()
        if (clean.isEmpty()) return
        appendConsoleLog(ConsoleLogEntry(level = "INPUT", message = "> $clean"))
        val exprEsc = JSONObject.quote(clean)
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.evalConsole($exprEsc); }",
                    null
                )
            } else {
                val res = evaluateAgainstLiveDom(clean)
                appendConsoleLog(res)
            }
        }
    }

    private fun evaluateAgainstLiveDom(expr: String): ConsoleLogEntry {
        val nodes = _liveDomNodes.value
        val url = _sourceUrl.value
        val title = _pageTitle.value
        return try {
            val result = when {
                expr == "document.title" -> "\"$title\""
                expr == "document.URL" || expr == "window.location.href" || expr == "location.href" -> "\"$url\""
                expr == "window.location.origin" || expr == "location.origin" -> {
                    val uri = runCatching { URI(url) }.getOrNull()
                    "\"${uri?.scheme ?: "https"}://${uri?.host ?: ""}\""
                }
                expr == "window.location.hostname" || expr == "location.hostname" -> {
                    val uri = runCatching { URI(url) }.getOrNull()
                    "\"${uri?.host ?: ""}\""
                }
                expr == "document.links.length" || expr == "document.querySelectorAll('a').length" -> {
                    nodes.count { it.tagName == "a" }.toString()
                }
                expr == "document.forms.length" -> {
                    nodes.count { it.tagName == "form" }.toString()
                }
                expr == "document.images.length" -> {
                    nodes.count { it.tagName == "img" }.toString()
                }
                expr == "document.cookie" -> "\"\""
                expr == "navigator.userAgent" -> "\"Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36\""
                expr == "performance.now()" -> "${System.currentTimeMillis() - loadStartTimestamp}.0"
                expr.startsWith("console.log(") && expr.endsWith(")") -> {
                    val inner = expr.removePrefix("console.log(").removeSuffix(")").trim().trim('"', '\'')
                    appendConsoleLog(ConsoleLogEntry(level = "LOG", message = inner))
                    "undefined"
                }
                expr.startsWith("document.querySelectorAll(") -> {
                    val sel = expr.substringAfter("(").substringBefore(")").trim().trim('"', '\'')
                    val matches = nodes.filter {
                        it.tagName.equals(sel, ignoreCase = true) ||
                            it.cssSelector.contains(sel, ignoreCase = true) ||
                            (sel.startsWith("#") && it.id == sel.removePrefix("#")) ||
                            (sel.startsWith(".") && it.className.contains(sel.removePrefix(".")))
                    }
                    if (expr.endsWith(".length")) {
                        matches.size.toString()
                    } else {
                        "NodeList(${matches.size}) [${matches.take(5).joinToString(", ") { "<${it.tagName}>" }}]"
                    }
                }
                expr.startsWith("document.querySelector(") -> {
                    val sel = expr.substringAfter("(").substringBefore(")").trim().trim('"', '\'')
                    val match = nodes.firstOrNull {
                        it.tagName.equals(sel, ignoreCase = true) ||
                            it.cssSelector.contains(sel, ignoreCase = true) ||
                            (sel.startsWith("#") && it.id == sel.removePrefix("#")) ||
                            (sel.startsWith(".") && it.className.contains(sel.removePrefix(".")))
                    }
                    when {
                        match == null -> "null"
                        expr.endsWith(".innerText") || expr.endsWith(".textContent") -> "\"${match.innerText}\""
                        expr.endsWith(".outerHTML") -> "\"${match.outerHtml}\""
                        else -> match.outerHtml
                    }
                }
                else -> {
                    "\"Evaluated on live document ($url • ${nodes.size} DOM elements)\""
                }
            }
            ConsoleLogEntry(level = "RESULT", message = result)
        } catch (e: Exception) {
            ConsoleLogEntry(level = "ERROR", message = e.message ?: "Evaluation error")
        }
    }

    fun appendConsoleLog(entry: ConsoleLogEntry) {
        _consoleLogs.update { current ->
            (current + entry).takeLast(250)
        }
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = emptyList()
    }

    // Network Sniffer Methods
    fun toggleSnifferPaused() {
        _isSnifferPaused.update { !it }
    }

    fun clearNetworkLogs() {
        _networkLogs.value = emptyList()
    }

    fun recordInterceptedRequest(request: WebResourceRequest, isBlocked: Boolean) {
        if (_isSnifferPaused.value) return
        val url = request.url?.toString() ?: return
        if (url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("aura://")) return

        val headers = request.requestHeaders ?: emptyMap()
        val method = request.method?.uppercase() ?: "GET"
        val isMain = request.isForMainFrame
        val resourceType = classifyResourceType(url, headers, isMain, isBlocked)

        val newEntry = NetworkLogEntry(
            url = url,
            method = method,
            resourceType = resourceType,
            statusCode = if (isBlocked) 0 else 200,
            statusText = if (isBlocked) "BLOCKED BY SHIELD" else "OK",
            requestHeaders = headers,
            timestamp = System.currentTimeMillis(),
            isMainFrame = isMain,
            isBlockedByShield = isBlocked
        )

        scope.launch(Dispatchers.Default) {
            _networkLogs.update { current ->
                (listOf(newEntry) + current).take(400)
            }
        }
    }

    fun syncPerformanceResourceEntries() {
        runOnWebView { wv ->
            if (wv != null) {
                injectDevToolsRuntime(wv)
                wv.evaluateJavascript(
                    "if (window.__auraDevTools) { window.__auraDevTools.collectPerformanceResources(); }",
                    null
                )
            }
        }
    }

    fun inspectNetworkEntryResponse(entry: NetworkLogEntry) {
        if (!entry.url.startsWith("http")) return
        _isFetchingPayloadId.value = entry.id
        scope.launch(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                val conn = (URL(entry.url).openConnection() as HttpURLConnection).apply {
                    requestMethod = if (entry.method in listOf("GET", "HEAD", "OPTIONS")) "GET" else entry.method
                    connectTimeout = 7000
                    readTimeout = 7000
                    instanceFollowRedirects = true
                    entry.requestHeaders.forEach { (k, v) ->
                        if (!k.equals("Accept-Encoding", ignoreCase = true)) {
                            setRequestProperty(k, v)
                        }
                    }
                }
                val status = conn.responseCode
                val statusMsg = conn.responseMessage ?: ""
                val mime = conn.contentType ?: ""
                val respHeaders = mutableMapOf<String, String>()
                conn.headerFields?.forEach { (k, values) ->
                    if (k != null && !values.isNullOrEmpty()) {
                        respHeaders[k] = values.joinToString(", ")
                    }
                }
                val stream = if (status in 200..399) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                val bodyPreview = if (mime.contains("image/", ignoreCase = true) ||
                    mime.contains("font/", ignoreCase = true) ||
                    mime.contains("video/", ignoreCase = true) ||
                    mime.contains("octet-stream", ignoreCase = true)
                ) {
                    val bytes = stream.readBytes()
                    "[Binary Resource • ${bytes.size} bytes • Content-Type: $mime]"
                } else {
                    val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                    val sb = StringBuilder()
                    var line: String?
                    var count = 0
                    while (reader.readLine().also { line = it } != null && count < 45_000) {
                        sb.append(line).append('\n')
                        count += (line?.length ?: 0) + 1
                    }
                    reader.close()
                    sb.toString().ifBlank { "[Empty Response Body]" }
                }
                val elapsed = System.currentTimeMillis() - start
                val sizeBytes = conn.contentLengthLong.takeIf { it >= 0 } ?: bodyPreview.length.toLong()
                conn.disconnect()

                _networkLogs.update { list ->
                    list.map { item ->
                        if (item.id == entry.id) {
                            item.copy(
                                statusCode = status,
                                statusText = statusMsg,
                                mimeType = mime,
                                durationMs = item.durationMs ?: elapsed,
                                transferSizeBytes = item.transferSizeBytes ?: sizeBytes,
                                responseHeaders = respHeaders,
                                responsePreview = bodyPreview
                            )
                        } else {
                            item
                        }
                    }
                }
            } catch (e: Exception) {
                _networkLogs.update { list ->
                    list.map { item ->
                        if (item.id == entry.id) {
                            item.copy(
                                responsePreview = "Error fetching response payload: ${e.message}"
                            )
                        } else {
                            item
                        }
                    }
                }
            } finally {
                _isFetchingPayloadId.value = null
            }
        }
    }

    fun onPageFinished(webView: WebView, url: String) {
        if (_activeTool.value != null || _isInspectTouchMode.value) {
            injectDevToolsRuntime(webView)
            syncPerformanceResourceEntries()
            if (_activeTool.value == DevToolTab.SOURCE_VIEW && _sourceMode.value == "LIVE_DOM") {
                fetchLivePageSource(url)
            } else if (_activeTool.value == DevToolTab.INSPECT_DOM) {
                scanLiveDomTree()
            }
        }
    }

    private fun classifyResourceType(
        url: String,
        headers: Map<String, String>,
        isMainFrame: Boolean,
        isBlocked: Boolean
    ): NetworkResourceType {
        if (isBlocked) return NetworkResourceType.BLOCKED
        if (isMainFrame) return NetworkResourceType.DOCUMENT

        val secDest = headers.entries.firstOrNull { it.key.equals("Sec-Fetch-Dest", ignoreCase = true) }?.value?.lowercase() ?: ""
        val secMode = headers.entries.firstOrNull { it.key.equals("Sec-Fetch-Mode", ignoreCase = true) }?.value?.lowercase() ?: ""
        val accept = headers.entries.firstOrNull { it.key.equals("Accept", ignoreCase = true) }?.value?.lowercase() ?: ""
        val cleanPath = url.substringBefore("?").substringBefore("#").lowercase()

        return when {
            secDest == "document" || secDest == "iframe" || accept.contains("text/html") -> NetworkResourceType.DOCUMENT
            secDest == "script" || cleanPath.endsWith(".js") || cleanPath.endsWith(".mjs") -> NetworkResourceType.SCRIPT
            secDest == "style" || accept.contains("text/css") || cleanPath.endsWith(".css") -> NetworkResourceType.STYLESHEET
            secDest == "image" || accept.contains("image/") ||
                cleanPath.endsWith(".png") || cleanPath.endsWith(".jpg") ||
                cleanPath.endsWith(".jpeg") || cleanPath.endsWith(".webp") ||
                cleanPath.endsWith(".svg") || cleanPath.endsWith(".gif") ||
                cleanPath.endsWith(".ico") -> NetworkResourceType.IMAGE
            secDest == "font" || cleanPath.endsWith(".woff2") ||
                cleanPath.endsWith(".woff") || cleanPath.endsWith(".ttf") -> NetworkResourceType.FONT
            secDest == "empty" || secMode == "cors" ||
                accept.contains("application/json") ||
                cleanPath.contains("/api/") || cleanPath.endsWith(".json") -> NetworkResourceType.XHR
            else -> NetworkResourceType.OTHER
        }
    }

    private fun runOnWebView(action: (WebView?) -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action(webViewProvider?.invoke())
        } else {
            mainHandler.post {
                action(webViewProvider?.invoke())
            }
        }
    }

    fun injectDevToolsRuntime(webView: WebView) {
        val script = """
            (function() {
                if (window.__auraDevToolsInitialized) return;
                window.__auraDevToolsInitialized = true;

                function computeCssSelector(el) {
                    if (!el || el.nodeType !== 1) return '';
                    if (el.id) return el.tagName.toLowerCase() + '#' + CSS.escape(el.id);
                    var parts = [];
                    var cur = el;
                    while (cur && cur.nodeType === 1 && parts.length < 5) {
                        var tag = cur.tagName.toLowerCase();
                        if (cur.id) {
                            parts.unshift(tag + '#' + CSS.escape(cur.id));
                            break;
                        }
                        var cls = (typeof cur.className === 'string' ? cur.className.trim() : '')
                            .split(/\s+/)
                            .filter(function(c) { return c && c.indexOf('__aura') === -1; })
                            .slice(0, 2);
                        var seg = tag + (cls.length ? '.' + cls.map(function(c){ return CSS.escape(c); }).join('.') : '');
                        var parent = cur.parentElement;
                        if (parent) {
                            var siblings = Array.prototype.filter.call(parent.children, function(ch) {
                                return ch.tagName === cur.tagName;
                            });
                            if (siblings.length > 1) {
                                var idx = siblings.indexOf(cur) + 1;
                                seg += ':nth-of-type(' + idx + ')';
                            }
                        }
                        parts.unshift(seg);
                        cur = parent;
                    }
                    return parts.join(' > ');
                }

                function computeXPath(el) {
                    if (!el || el.nodeType !== 1) return '';
                    if (el.id) return '//*[@id="' + el.id + '"]';
                    var segments = [];
                    var cur = el;
                    while (cur && cur.nodeType === 1) {
                        var idx = 1;
                        var sib = cur.previousElementSibling;
                        while (sib) {
                            if (sib.tagName === cur.tagName) idx++;
                            sib = sib.previousElementSibling;
                        }
                        segments.unshift(cur.tagName.toLowerCase() + '[' + idx + ']');
                        cur = cur.parentElement;
                    }
                    return '/' + segments.join('/');
                }

                function ensureHighlightBox() {
                    var box = document.getElementById('__aura_devtools_highlight');
                    if (!box) {
                        box = document.createElement('div');
                        box.id = '__aura_devtools_highlight';
                        box.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483646;border:2px solid #06b6d4;background:rgba(6,182,212,0.16);border-radius:4px;transition:all 0.08s ease;display:none;box-sizing:border-box;';
                        var label = document.createElement('div');
                        label.id = '__aura_devtools_highlight_label';
                        label.style.cssText = 'position:absolute;top:-24px;left:-2px;background:#0f172a;color:#38bdf8;font-family:monospace;font-size:11px;font-weight:bold;padding:2px 6px;border-radius:4px;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,0.35);border:1px solid #06b6d4;';
                        box.appendChild(label);
                        document.documentElement.appendChild(box);
                    }
                    return box;
                }

                function highlightElement(el) {
                    if (!el || !el.getBoundingClientRect) return;
                    var rect = el.getBoundingClientRect();
                    var box = ensureHighlightBox();
                    box.style.display = 'block';
                    box.style.top = rect.top + 'px';
                    box.style.left = rect.left + 'px';
                    box.style.width = Math.max(rect.width, 4) + 'px';
                    box.style.height = Math.max(rect.height, 4) + 'px';
                    var label = document.getElementById('__aura_devtools_highlight_label');
                    if (label) {
                        var tag = el.tagName.toLowerCase();
                        var idPart = el.id ? '#' + el.id : '';
                        var clsPart = (typeof el.className === 'string' && el.className.trim())
                            ? '.' + el.className.trim().split(/\s+/)[0]
                            : '';
                        label.textContent = tag + idPart + clsPart + ' (' + Math.round(rect.width) + '×' + Math.round(rect.height) + ')';
                        label.style.top = (rect.top < 28 ? rect.height + 4 : -24) + 'px';
                    }
                }

                function serializeElement(el) {
                    if (!el || el.nodeType !== 1) return null;
                    var rect = el.getBoundingClientRect();
                    var attrs = {};
                    if (el.attributes) {
                        for (var i = 0; i < el.attributes.length; i++) {
                            var a = el.attributes[i];
                            attrs[a.name] = a.value;
                        }
                    }
                    var cs = window.getComputedStyle(el);
                    var styleKeys = [
                        'display', 'position', 'width', 'height',
                        'color', 'background-color', 'font-family', 'font-size', 'font-weight',
                        'margin', 'padding', 'border', 'border-radius',
                        'flex-direction', 'justify-content', 'align-items', 'z-index', 'opacity'
                    ];
                    var styles = {};
                    styleKeys.forEach(function(k) {
                        var v = cs.getPropertyValue(k);
                        if (v) styles[k] = v;
                    });
                    var outer = el.outerHTML || '';
                    if (outer.length > 12000) {
                        outer = outer.substring(0, 12000) + '\n<!-- ... truncated ... -->';
                    }
                    var text = (el.innerText || el.textContent || '').trim();
                    if (text.length > 2000) {
                        text = text.substring(0, 2000) + '...';
                    }
                    return {
                        tagName: el.tagName.toLowerCase(),
                        id: el.id || '',
                        className: typeof el.className === 'string' ? el.className.trim() : '',
                        cssSelector: computeCssSelector(el),
                        xpath: computeXPath(el),
                        widthPx: Math.round(rect.width),
                        heightPx: Math.round(rect.height),
                        topPx: Math.round(rect.top + window.scrollY),
                        leftPx: Math.round(rect.left + window.scrollX),
                        attributes: attrs,
                        computedStyles: styles,
                        outerHtml: outer,
                        innerText: text,
                        childCount: el.children ? el.children.length : 0
                    };
                }

                var touchInspectActive = false;

                function onTouchOrClickInspect(e) {
                    if (!touchInspectActive) return;
                    var target = e.target;
                    if (!target || (target.id && target.id.indexOf('__aura') === 0)) return;
                    e.preventDefault();
                    e.stopPropagation();
                    highlightElement(target);
                    var info = serializeElement(target);
                    if (info && window.AuraDevTools) {
                        window.AuraDevTools.onElementInspected(JSON.stringify(info), true);
                    }
                }

                document.addEventListener('click', onTouchOrClickInspect, true);

                window.__auraDevTools = {
                    setTouchInspectMode: function(enabled) {
                        touchInspectActive = !!enabled;
                        if (!touchInspectActive) {
                            this.clearHighlight();
                        }
                    },
                    clearHighlight: function() {
                        var box = document.getElementById('__aura_devtools_highlight');
                        if (box) box.style.display = 'none';
                    },
                    scanDomTree: function() {
                        var result = [];
                        var root = document.documentElement;
                        if (!root) return;
                        function walk(node, depth) {
                            if (!node || node.nodeType !== 1 || result.length >= 180) return;
                            if (node.id && node.id.indexOf('__aura') === 0) return;
                            var tag = node.tagName.toLowerCase();
                            if (tag === 'script' || tag === 'style' || tag === 'noscript' || tag === 'svg' || tag === 'path') {
                                return;
                            }
                            var directText = '';
                            for (var i = 0; i < node.childNodes.length; i++) {
                                var ch = node.childNodes[i];
                                if (ch.nodeType === 3 && ch.nodeValue) {
                                    directText += ch.nodeValue.trim() + ' ';
                                }
                            }
                            directText = directText.trim().substring(0, 80);
                            var rect = node.getBoundingClientRect();
                            result.push({
                                index: result.length,
                                tagName: tag,
                                id: node.id || '',
                                className: typeof node.className === 'string' ? node.className.trim() : '',
                                cssSelector: computeCssSelector(node),
                                depth: depth,
                                textSnippet: directText,
                                childCount: node.children ? node.children.length : 0,
                                dimensionsLabel: Math.round(rect.width) + '×' + Math.round(rect.height)
                            });
                            if (depth < 7 && node.children) {
                                for (var j = 0; j < node.children.length; j++) {
                                    walk(node.children[j], depth + 1);
                                }
                            }
                        }
                        walk(root, 0);
                        if (window.AuraDevTools) {
                            window.AuraDevTools.onDomTreeScanned(JSON.stringify(result));
                        }
                    },
                    inspectBySelector: function(selector, scrollIntoView) {
                        try {
                            var el = document.querySelector(selector);
                            if (!el) return;
                            if (scrollIntoView && el.scrollIntoView) {
                                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            }
                            highlightElement(el);
                            var info = serializeElement(el);
                            if (info && window.AuraDevTools) {
                                window.AuraDevTools.onElementInspected(JSON.stringify(info), false);
                            }
                        } catch (e) {}
                    },
                    updateElementText: function(selector, newText) {
                        try {
                            var el = document.querySelector(selector);
                            if (!el) return;
                            el.innerText = newText;
                            this.inspectBySelector(selector, false);
                            this.scanDomTree();
                        } catch (e) {}
                    },
                    updateElementStyle: function(selector, prop, val) {
                        try {
                            var el = document.querySelector(selector);
                            if (!el) return;
                            el.style.setProperty(prop, val);
                            this.inspectBySelector(selector, false);
                        } catch (e) {}
                    },
                    removeElement: function(selector) {
                        try {
                            var el = document.querySelector(selector);
                            if (el && el.parentElement) {
                                el.parentElement.removeChild(el);
                                this.clearHighlight();
                                this.scanDomTree();
                            }
                        } catch (e) {}
                    },
                    evalConsole: function(expr) {
                        try {
                            var res = window.eval(expr);
                            var formatted;
                            if (res === undefined) {
                                formatted = 'undefined';
                            } else if (res === null) {
                                formatted = 'null';
                            } else if (typeof res === 'object') {
                                try {
                                    formatted = JSON.stringify(res, null, 2);
                                } catch (_) {
                                    formatted = String(res);
                                }
                            } else {
                                formatted = String(res);
                            }
                            if (window.AuraDevTools) {
                                window.AuraDevTools.onConsoleResult('RESULT', formatted);
                            }
                        } catch (err) {
                            if (window.AuraDevTools) {
                                window.AuraDevTools.onConsoleResult('ERROR', err && err.stack ? err.stack : String(err));
                            }
                        }
                    },
                    collectPerformanceResources: function() {
                        try {
                            if (!window.performance || !window.performance.getEntriesByType) return;
                            var entries = window.performance.getEntriesByType('resource') || [];
                            var arr = [];
                            for (var i = Math.max(0, entries.length - 80); i < entries.length; i++) {
                                var r = entries[i];
                                arr.push({
                                    url: r.name || '',
                                    initiatorType: r.initiatorType || 'other',
                                    durationMs: Math.round(r.duration || 0),
                                    transferSize: r.transferSize || r.encodedBodySize || 0,
                                    responseStatus: r.responseStatus || 200
                                });
                            }
                            if (window.AuraDevTools && arr.length > 0) {
                                window.AuraDevTools.onPerformanceResources(JSON.stringify(arr));
                            }
                        } catch (e) {}
                    }
                };
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    inner class AuraDevToolsJsBridge {
        @JavascriptInterface
        fun onPageSourceReceived(html: String, url: String) {
            _pageSourceHtml.value = html
            if (url.isNotBlank()) {
                _sourceUrl.value = url
            }
            _isSourceLoading.value = false
        }

        @JavascriptInterface
        fun onElementInspected(jsonStr: String, fromTouch: Boolean) {
            try {
                val obj = JSONObject(jsonStr)
                val attrsObj = obj.optJSONObject("attributes")
                val attrs = mutableMapOf<String, String>()
                if (attrsObj != null) {
                    val keys = attrsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        attrs[k] = attrsObj.optString(k, "")
                    }
                }
                val stylesObj = obj.optJSONObject("computedStyles")
                val styles = mutableMapOf<String, String>()
                if (stylesObj != null) {
                    val keys = stylesObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        styles[k] = stylesObj.optString(k, "")
                    }
                }
                val element = InspectedDomElement(
                    tagName = obj.optString("tagName", "div"),
                    id = obj.optString("id", ""),
                    className = obj.optString("className", ""),
                    cssSelector = obj.optString("cssSelector", ""),
                    xpath = obj.optString("xpath", ""),
                    widthPx = obj.optInt("widthPx", 0),
                    heightPx = obj.optInt("heightPx", 0),
                    topPx = obj.optInt("topPx", 0),
                    leftPx = obj.optInt("leftPx", 0),
                    attributes = attrs,
                    computedStyles = styles,
                    outerHtml = obj.optString("outerHtml", ""),
                    innerText = obj.optString("innerText", ""),
                    childCount = obj.optInt("childCount", 0)
                )
                _inspectedElement.value = element
                if (fromTouch) {
                    _isInspectTouchMode.value = false
                    _activeTool.value = DevToolTab.INSPECT_DOM
                    _isSheetMinimized.value = false
                }
            } catch (_: Exception) {
            }
        }

        @JavascriptInterface
        fun onDomTreeScanned(jsonArrayStr: String) {
            try {
                val arr = JSONArray(jsonArrayStr)
                val list = ArrayList<DomTreeNodeSummary>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        DomTreeNodeSummary(
                            index = o.optInt("index", i),
                            tagName = o.optString("tagName", "div"),
                            id = o.optString("id", ""),
                            className = o.optString("className", ""),
                            cssSelector = o.optString("cssSelector", ""),
                            depth = o.optInt("depth", 0),
                            textSnippet = o.optString("textSnippet", ""),
                            childCount = o.optInt("childCount", 0),
                            dimensionsLabel = o.optString("dimensionsLabel", "")
                        )
                    )
                }
                _domTreeNodes.value = list
            } catch (_: Exception) {
            }
        }

        @JavascriptInterface
        fun onConsoleResult(level: String, message: String) {
            appendConsoleLog(
                ConsoleLogEntry(
                    level = level,
                    message = message
                )
            )
        }

        @JavascriptInterface
        fun onPerformanceResources(jsonArrayStr: String) {
            if (_isSnifferPaused.value) return
            try {
                val arr = JSONArray(jsonArrayStr)
                _networkLogs.update { current ->
                    val mutable = current.toMutableList()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val url = o.optString("url", "")
                        if (url.isBlank() || url.startsWith("data:")) continue
                        val dur = o.optLong("durationMs", 0L)
                        val size = o.optLong("transferSize", 0L)
                        val status = o.optInt("responseStatus", 200).takeIf { it > 0 } ?: 200
                        val initiator = o.optString("initiatorType", "other")
                        val idx = mutable.indexOfFirst { it.url == url }
                        if (idx >= 0) {
                            val existing = mutable[idx]
                            if (existing.statusCode == null || existing.durationMs == null) {
                                mutable[idx] = existing.copy(
                                    statusCode = existing.statusCode ?: status,
                                    statusText = existing.statusText?.takeIf { it != "PENDING" } ?: "OK",
                                    durationMs = existing.durationMs ?: dur,
                                    transferSizeBytes = existing.transferSizeBytes ?: size.takeIf { it > 0 }
                                )
                            }
                        } else {
                            val type = when (initiator.lowercase()) {
                                "xmlhttprequest", "fetch" -> NetworkResourceType.XHR
                                "script" -> NetworkResourceType.SCRIPT
                                "link", "css" -> NetworkResourceType.STYLESHEET
                                "img", "image" -> NetworkResourceType.IMAGE
                                "iframe", "navigation" -> NetworkResourceType.DOCUMENT
                                else -> classifyResourceType(url, emptyMap(), false, false)
                            }
                            mutable.add(
                                NetworkLogEntry(
                                    url = url,
                                    method = "GET",
                                    resourceType = type,
                                    statusCode = status,
                                    statusText = "OK",
                                    durationMs = dur,
                                    transferSizeBytes = size.takeIf { it > 0 }
                                )
                            )
                        }
                    }
                    mutable.take(400)
                }
            } catch (_: Exception) {
            }
        }
    }
}
