package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AuraApplication
import com.example.automation.AutomationEngine
import com.example.devtools.ConsoleLogEntry
import com.example.devtools.DevToolsEngine
import com.example.devtools.LiveDomNode
import com.example.devtools.RealHtmlPageLoader
import com.example.model.BrowserTab
import com.example.ui.components.AutomationHUD
import com.example.ui.components.BrowserDevToolsOverlay
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressivePillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

private fun areUrlsEquivalent(url1: String?, url2: String?): Boolean {
    if (url1 == url2) return true
    if (url1.isNullOrBlank() || url2.isNullOrBlank()) return false
    val clean1 = url1.trimEnd('/')
    val clean2 = url2.trimEnd('/')
    return clean1.equals(clean2, ignoreCase = true)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    tab: BrowserTab,
    isDesktopMode: Boolean,
    adShieldEnabled: Boolean,
    automationEngine: AutomationEngine? = null,
    devToolsEngine: DevToolsEngine? = null,
    reloadTrigger: SharedFlow<Unit>? = null,
    stopLoadingTrigger: SharedFlow<Unit>? = null,
    translateTrigger: SharedFlow<String>? = null,
    onOpenScriptManager: () -> Unit = {},
    onCopyText: (String, String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onWebStateChanged: (
        title: String?,
        url: String?,
        progress: Int?,
        loading: Boolean?,
        canBack: Boolean?,
        canForward: Boolean?
    ) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Check whether the device has a hardware DRM render node (/dev/dri/renderD128).
    // On headless/cloud emulators without /dev/dri/renderD128, spawning Chromium's isolated
    // child renderer process crashes in aw_browser_terminator.cc(154). Using the Real HTTP/DOM
    // Compose Web Engine avoids spawning the native Chromium child process while performing
    // 100% real HTTP/HTTPS requests, real HTML/DOM parsing, real Source View, real DOM Inspection,
    // and real Network Sniffing.
    val useHardwareWebView = remember { AuraApplication.hasHardwareRenderNode() }

    if (!useHardwareWebView) {
        RealHttpComposeWebScreen(
            tab = tab,
            isDesktopMode = isDesktopMode,
            adShieldEnabled = adShieldEnabled,
            automationEngine = automationEngine,
            devToolsEngine = devToolsEngine,
            reloadTrigger = reloadTrigger,
            stopLoadingTrigger = stopLoadingTrigger,
            translateTrigger = translateTrigger,
            onOpenScriptManager = onOpenScriptManager,
            onCopyText = onCopyText,
            onShowMessage = onShowMessage,
            onWebStateChanged = onWebStateChanged,
            onNavigateHome = onNavigateHome,
            modifier = modifier
        )
        return
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var lastLoadedUrl by remember(tab.id) { mutableStateOf<String?>(null) }
    var lastObservedTabPropUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var lastDesktopMode by remember { mutableStateOf(isDesktopMode) }

    val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateHome()
        }
    }

    LaunchedEffect(webViewInstance, automationEngine, devToolsEngine) {
        automationEngine?.webViewProvider = { webViewInstance }
        devToolsEngine?.webViewProvider = { webViewInstance }
        automationEngine?.webViewNavigator = { targetUrl ->
            lastLoadedUrl = targetUrl
            lastObservedTabPropUrl = targetUrl
            onWebStateChanged(null, targetUrl, 15, true, webViewInstance?.canGoBack(), webViewInstance?.canGoForward())
            webViewInstance?.loadUrl(targetUrl)
        }
        val activeUrl = lastLoadedUrl ?: tab.url
        if (activeUrl.isNotBlank() && !activeUrl.startsWith("aura://")) {
            automationEngine?.syncCurrentPageUrl(activeUrl)
        }
    }

    LaunchedEffect(reloadTrigger) {
        reloadTrigger?.collect {
            webViewInstance?.reload()
        }
    }

    LaunchedEffect(stopLoadingTrigger) {
        stopLoadingTrigger?.collect {
            webViewInstance?.stopLoading()
        }
    }

    LaunchedEffect(isDesktopMode) {
        if (lastDesktopMode != isDesktopMode) {
            lastDesktopMode = isDesktopMode
            webViewInstance?.settings?.let { settings ->
                if (isDesktopMode) {
                    settings.userAgentString = desktopUserAgent
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                } else {
                    settings.userAgentString = null
                    settings.useWideViewPort = false
                    settings.loadWithOverviewMode = false
                }
                webViewInstance?.reload()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("webview_container")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowContentAccess = true
                        allowFileAccess = false
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = true
                        if (isDesktopMode) {
                            userAgentString = desktopUserAgent
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                    }

                    automationEngine?.let { engine ->
                        addJavascriptInterface(engine.jsInterface, "AuraAutomation")
                    }
                    devToolsEngine?.let { devTools ->
                        addJavascriptInterface(devTools.jsInterface, "AuraDevTools")
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): android.webkit.WebResourceResponse? {
                            if (request != null) {
                                val reqUrl = request.url?.toString() ?: ""
                                val isBlocked = adShieldEnabled && (devToolsEngine?.isTrackerUrl(reqUrl) == true)
                                devToolsEngine?.recordInterceptedRequest(request, isBlocked)
                                if (isBlocked) {
                                    return android.webkit.WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        java.io.ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            hasError = false
                            if (!url.isNullOrBlank()) {
                                lastLoadedUrl = url
                                automationEngine?.syncCurrentPageUrl(url)
                                automationEngine?.onPageLoadStarted(url)
                            }
                            onWebStateChanged(null, url, 15, true, view?.canGoBack(), view?.canGoForward())
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!url.isNullOrBlank()) {
                                lastLoadedUrl = url
                                automationEngine?.onPageLoaded(url)
                                if (view != null) {
                                    devToolsEngine?.onPageFinished(view, url)
                                }
                            }
                            onWebStateChanged(view?.title, url, 100, false, view?.canGoBack(), view?.canGoForward())
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                errorMessage = error?.description?.toString() ?: "Failed to load page"
                                onWebStateChanged(null, null, 100, false, null, null)
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onWebStateChanged(null, null, newProgress, newProgress < 100, view?.canGoBack(), view?.canGoForward())
                        }
                    }

                    val initialTargetUrl = lastLoadedUrl ?: tab.url
                    if (!initialTargetUrl.startsWith("aura://") && initialTargetUrl.isNotBlank()) {
                        lastLoadedUrl = initialTargetUrl
                        lastObservedTabPropUrl = tab.url
                        loadUrl(initialTargetUrl)
                    }
                    webViewInstance = this
                }
            },
            update = { webView ->
                if (tab.url != lastObservedTabPropUrl) {
                    lastObservedTabPropUrl = tab.url
                    if (tab.url.isNotBlank() && !tab.url.startsWith("aura://") && !areUrlsEquivalent(tab.url, lastLoadedUrl)) {
                        lastLoadedUrl = tab.url
                        webView.loadUrl(tab.url)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (devToolsEngine != null) {
            BrowserDevToolsOverlay(
                devToolsEngine = devToolsEngine,
                currentUrl = lastLoadedUrl ?: tab.url,
                onCopyText = onCopyText,
                onShowMessage = onShowMessage
            )
        }
    }
}

/**
 * Pure-Kotlin Real HTTP/HTML/DOM Web Engine for environments without a hardware DRM render node.
 * Fetches live websites over real HTTPS, parses real DOM elements, captures real network logs
 * and sub-resources, supports interactive links/forms, in-page translation, and full DevTools
 * (Source View, Live DOM Inspector & Mutation, and Real-Time Network Log Sniffer).
 */
@Composable
private fun RealHttpComposeWebScreen(
    tab: BrowserTab,
    isDesktopMode: Boolean,
    adShieldEnabled: Boolean,
    automationEngine: AutomationEngine?,
    devToolsEngine: DevToolsEngine?,
    reloadTrigger: SharedFlow<Unit>?,
    stopLoadingTrigger: SharedFlow<Unit>?,
    translateTrigger: SharedFlow<String>?,
    onOpenScriptManager: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onShowMessage: (String) -> Unit,
    onWebStateChanged: (
        title: String?,
        url: String?,
        progress: Int?,
        loading: Boolean?,
        canBack: Boolean?,
        canForward: Boolean?
    ) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val backStack = remember(tab.id) { mutableStateListOf<String>() }
    var currentHistoryIdx by remember(tab.id) { mutableIntStateOf(-1) }
    var currentUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var lastObservedPropUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf(tab.title) }
    var loadDurationMs by remember { mutableStateOf(0L) }
    var statusCode by remember { mutableIntStateOf(200) }

    val defaultNodes = remember { mutableStateOf<List<LiveDomNode>>(emptyList()) }
    val liveDomNodes = devToolsEngine?.liveDomNodes?.collectAsStateWithLifecycle()?.value ?: defaultNodes.value
    val isInspectTouchMode = devToolsEngine?.isInspectTouchMode?.collectAsStateWithLifecycle()?.value ?: false
    val highlightedSelector = devToolsEngine?.highlightedSelector?.collectAsStateWithLifecycle()?.value

    val inputValues = remember(currentUrl) { mutableStateMapOf<Int, String>() }

    fun navigateToUrl(nextUrl: String, addToHistory: Boolean = true) {
        if (nextUrl.isBlank() || nextUrl.startsWith("aura://")) {
            onNavigateHome()
            return
        }
        if (addToHistory) {
            while (backStack.size > currentHistoryIdx + 1 && backStack.isNotEmpty()) {
                backStack.removeAt(backStack.lastIndex)
            }
            if (backStack.lastOrNull() != nextUrl) {
                backStack.add(nextUrl)
                currentHistoryIdx = backStack.lastIndex
            }
        }
        currentUrl = nextUrl
    }

    // Initialize history stack with initial tab URL
    LaunchedEffect(tab.id) {
        if (tab.url.isNotBlank() && !tab.url.startsWith("aura://") && backStack.isEmpty()) {
            backStack.add(tab.url)
            currentHistoryIdx = 0
            currentUrl = tab.url
        }
    }

    // Watch external tab.url changes from ViewModel
    LaunchedEffect(tab.url) {
        if (tab.url != lastObservedPropUrl) {
            lastObservedPropUrl = tab.url
            if (tab.url.isNotBlank() && !tab.url.startsWith("aura://") && !areUrlsEquivalent(tab.url, currentUrl)) {
                navigateToUrl(tab.url, addToHistory = true)
            }
        }
    }

    // Back handler for history stack
    BackHandler(enabled = currentHistoryIdx > 0) {
        if (currentHistoryIdx > 0) {
            currentHistoryIdx--
            currentUrl = backStack[currentHistoryIdx]
        } else {
            onNavigateHome()
        }
    }

    // Listen for reload trigger
    LaunchedEffect(reloadTrigger) {
        reloadTrigger?.collect {
            reloadNonce++
        }
    }

    // Listen for stop loading trigger
    LaunchedEffect(stopLoadingTrigger) {
        stopLoadingTrigger?.collect {
            isLoading = false
            onWebStateChanged(pageTitle, currentUrl, 100, false, currentHistoryIdx > 0, currentHistoryIdx < backStack.lastIndex)
        }
    }

    // Listen for in-place translation trigger
    LaunchedEffect(translateTrigger) {
        translateTrigger?.collect { targetLang ->
            if (liveDomNodes.isNotEmpty()) {
                onShowMessage("Translating live page elements to $targetLang...")
                val translated = withContext(Dispatchers.IO) {
                    RealHtmlPageLoader.translateNodesInPlace(liveDomNodes, targetLang)
                }
                defaultNodes.value = translated
                translated.forEach { n ->
                    devToolsEngine?.updateInspectedElementText(n.cssSelector, n.innerText)
                }
                onShowMessage("Page translated to $targetLang")
            }
        }
    }

    // Connect AutomationEngine navigator
    LaunchedEffect(automationEngine, currentUrl) {
        automationEngine?.webViewNavigator = { targetUrl ->
            navigateToUrl(targetUrl, addToHistory = true)
        }
        if (currentUrl.isNotBlank() && !currentUrl.startsWith("aura://")) {
            automationEngine?.syncCurrentPageUrl(currentUrl)
        }
    }

    // Perform real HTTP request and parse HTML/DOM whenever currentUrl, desktop mode, or reloadNonce changes
    LaunchedEffect(currentUrl, isDesktopMode, reloadNonce) {
        if (currentUrl.isBlank() || currentUrl.startsWith("aura://") || currentUrl == "about:blank") {
            return@LaunchedEffect
        }
        isLoading = true
        hasError = false
        val canBack = currentHistoryIdx > 0
        val canFwd = currentHistoryIdx < backStack.lastIndex
        onWebStateChanged(null, currentUrl, 20, true, canBack, canFwd)
        automationEngine?.onPageLoadStarted(currentUrl)

        val result = withContext(Dispatchers.IO) {
            RealHtmlPageLoader.loadWebPage(currentUrl, isDesktopMode)
        }

        result.onSuccess { loaded ->
            pageTitle = loaded.title
            statusCode = loaded.mainNetworkEntry.statusCode ?: 200
            loadDurationMs = loaded.mainNetworkEntry.durationMs ?: 0L
            defaultNodes.value = loaded.domNodes
            isLoading = false
            hasError = false

            devToolsEngine?.onLiveHtmlDocumentLoaded(
                url = loaded.finalUrl,
                title = loaded.title,
                rawHtml = loaded.rawHtml,
                nodes = loaded.domNodes,
                mainEntry = loaded.mainNetworkEntry,
                subresourceUrls = loaded.subresources,
                adShieldEnabled = adShieldEnabled
            )
            devToolsEngine?.appendConsoleLog(
                ConsoleLogEntry(
                    level = "INFO",
                    message = "Loaded ${loaded.finalUrl} (${loaded.domNodes.size} DOM elements, ${loaded.rawHtml.length} bytes in ${loadDurationMs}ms)"
                )
            )
            automationEngine?.onPageLoaded(loaded.finalUrl)
            onWebStateChanged(
                loaded.title,
                loaded.finalUrl,
                100,
                false,
                currentHistoryIdx > 0,
                currentHistoryIdx < backStack.lastIndex
            )
        }.onFailure { err ->
            isLoading = false
            hasError = true
            errorMessage = err.message ?: "Unable to reach $currentUrl"
            automationEngine?.onPageLoaded(currentUrl)
            onWebStateChanged(null, currentUrl, 100, false, canBack, canFwd)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("webview_container")
    ) {
        if (hasError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unable to connect",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { reloadNonce++ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Again")
                    }
                    OutlinedButton(onClick = onNavigateHome) {
                        Text("Go to Home")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Live Page Status & Telemetry Banner
                item(key = "page_status_banner") {
                    Surface(
                        shape = ExpressiveCardShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.16f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pageTitle.ifBlank { currentUrl },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$currentUrl • HTTP $statusCode • ${loadDurationMs}ms • ${liveDomNodes.size} DOM nodes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Render live interactive DOM elements parsed from the real HTTP response
                items(
                    items = liveDomNodes,
                    key = { node -> "${node.index}_${node.cssSelector}" }
                ) { node ->
                    val isHighlighted = highlightedSelector == node.cssSelector
                    LiveDomElementView(
                        node = node,
                        isInspectTouchMode = isInspectTouchMode,
                        isHighlighted = isHighlighted,
                        inputValue = inputValues[node.index] ?: node.inputValue,
                        onInputValueChange = { inputValues[node.index] = it },
                        onSubmitForm = { queryText ->
                            val actionPrefix = node.formAction ?: "https://www.google.com/search?q="
                            val target = if (actionPrefix.contains("?")) {
                                if (actionPrefix.endsWith("=") || actionPrefix.endsWith("?")) {
                                    "$actionPrefix${URLEncoder.encode(queryText, "UTF-8")}"
                                } else {
                                    "$actionPrefix&${node.inputName ?: "q"}=${URLEncoder.encode(queryText, "UTF-8")}"
                                }
                            } else {
                                "$actionPrefix?${node.inputName ?: "q"}=${URLEncoder.encode(queryText, "UTF-8")}"
                            }
                            navigateToUrl(target, addToHistory = true)
                        },
                        onFollowLink = { targetHref ->
                            navigateToUrl(targetHref, addToHistory = true)
                        },
                        onInspectElement = {
                            devToolsEngine?.inspectLiveDomNode(node, fromTouch = true)
                        }
                    )
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Floating Automation HUD Window
        if (automationEngine != null) {
            val executionState by automationEngine.executionState.collectAsStateWithLifecycle()
            val script by automationEngine.currentScript.collectAsStateWithLifecycle()
            val savedScripts by automationEngine.savedScripts.collectAsStateWithLifecycle()
            val dataSheets by automationEngine.dataSheets.collectAsStateWithLifecycle()
            val scannedElements by automationEngine.scannedElements.collectAsStateWithLifecycle()

            if (executionState.isHudVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    AutomationHUD(
                        executionState = executionState,
                        currentScript = script,
                        savedScripts = savedScripts,
                        dataSheets = dataSheets,
                        currentUrl = currentUrl,
                        scannedElements = scannedElements,
                        onSelectMode = { mode -> automationEngine.setHudMode(mode, currentUrl) },
                        onSelectScript = { automationEngine.selectScript(it) },
                        onUpdateCurrentScript = { automationEngine.updateCurrentScript(it) },
                        onStartFreshScript = { automationEngine.startFreshRecordingScript(currentUrl) },
                        onSaveCurrentScript = { name, web, sheetId, order, policy, repeatCount ->
                            automationEngine.saveCurrentAsScript(name, web, sheetId, order, policy, repeatCount)
                        },
                        onUpdateStep = { automationEngine.updateStep(it) },
                        onMoveStep = { stepId, dir -> automationEngine.moveStep(stepId, dir) },
                        onDeleteStep = { automationEngine.removeStep(it) },
                        onAddStep = { automationEngine.addStep(it) },
                        onUpdateScannedElement = { automationEngine.updateScannedElement(it) },
                        onToggleRecord = { automationEngine.toggleRecording() },
                        onStartPlay = { automationEngine.startPlayback() },
                        onStopPlay = { automationEngine.stopPlayback() },
                        onTriggerScan = { automationEngine.triggerScan() },
                        onClearScan = { automationEngine.clearScanHighlights() },
                        onAddScannedElement = { element -> automationEngine.addScannedElementToScript(element) },
                        onAutoMapAllScanned = { automationEngine.autoMapAllScannedElements() },
                        onNavigateWebViewToUrl = { targetUrl ->
                            navigateToUrl(targetUrl, addToHistory = true)
                        },
                        onResolveExhaustionStop = { automationEngine.resolveExhaustionWithStop() },
                        onResolveExhaustionDefault = { automationEngine.resolveExhaustionWithDefault() },
                        onResolveExhaustionAddNew = { fieldKey, newValues ->
                            automationEngine.resolveExhaustionWithNewData(fieldKey, newValues)
                        },
                        onSaveDataSheet = { sheet -> automationEngine.saveDataSheet(sheet) },
                        onOpenScriptManager = onOpenScriptManager,
                        onCloseHud = { automationEngine.setHudVisible(false) }
                    )
                }
            }
        }

        // Real DevTools Suite Overlay (Source View, Live DOM Inspector & JS Console, Real-Time Network Sniffer)
        if (devToolsEngine != null) {
            BrowserDevToolsOverlay(
                devToolsEngine = devToolsEngine,
                currentUrl = currentUrl,
                onCopyText = onCopyText,
                onShowMessage = onShowMessage
            )
        }
    }
}

@Composable
private fun LiveDomElementView(
    node: LiveDomNode,
    isInspectTouchMode: Boolean,
    isHighlighted: Boolean,
    inputValue: String,
    onInputValueChange: (String) -> Unit,
    onSubmitForm: (String) -> Unit,
    onFollowLink: (String) -> Unit,
    onInspectElement: () -> Unit
) {
    val customBgColor = remember(node.inlineStyles) {
        parseCssColorOrNull(node.inlineStyles["background-color"] ?: node.inlineStyles["background"])
    }
    val customTextColor = remember(node.inlineStyles) {
        parseCssColorOrNull(node.inlineStyles["color"])
    }

    val highlightBorderModifier = if (isHighlighted || isInspectTouchMode) {
        Modifier.border(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) Color(0xFF06B6D4) else Color(0xFF06B6D4).copy(alpha = 0.35f),
            shape = RoundedCornerShape(10.dp)
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(highlightBorderModifier)
            .let { mod ->
                if (customBgColor != null) {
                    mod.background(customBgColor, RoundedCornerShape(10.dp))
                } else if (isHighlighted) {
                    mod.background(Color(0xFF06B6D4).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                } else {
                    mod
                }
            }
    ) {
        if (isHighlighted || isInspectTouchMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .clickable { onInspectElement() }
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0F172A)
                ) {
                    Text(
                        text = buildString {
                            append("<${node.tagName}")
                            if (node.id.isNotBlank()) append("#${node.id}")
                            append(">")
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        when (node.tagName) {
            "h1" -> {
                Text(
                    text = node.innerText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = customTextColor ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isInspectTouchMode) onInspectElement()
                            else if (node.href != null) onFollowLink(node.href)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            "h2", "h3", "h4" -> {
                Text(
                    text = node.innerText,
                    fontSize = if (node.tagName == "h2") 17.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = customTextColor ?: if (node.href != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isInspectTouchMode) onInspectElement()
                            else if (node.href != null) onFollowLink(node.href)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            "input", "textarea" -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = onInputValueChange,
                        placeholder = {
                            Text(
                                text = node.inputPlaceholder ?: "Enter text or search...",
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = ExpressivePillShape,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (inputValue.isNotBlank()) {
                                    onSubmitForm(inputValue)
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isInspectTouchMode) {
                        Button(onClick = onInspectElement) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Inspect Input",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (inputValue.isNotBlank()) {
                                    onSubmitForm(inputValue)
                                }
                            },
                            shape = ExpressivePillShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Submit",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Go", fontSize = 12.5.sp)
                        }
                    }
                }
            }
            "button" -> {
                Button(
                    onClick = {
                        if (isInspectTouchMode) onInspectElement()
                        else if (node.href != null) onFollowLink(node.href)
                    },
                    shape = ExpressivePillShape,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(node.innerText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            "a" -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isInspectTouchMode) {
                                onInspectElement()
                            } else if (node.href != null) {
                                onFollowLink(node.href)
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = node.innerText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = customTextColor ?: MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!node.href.isNullOrBlank()) {
                                Text(
                                    text = node.href,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            "pre", "code" -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isInspectTouchMode) onInspectElement()
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = node.innerText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = customTextColor ?: Color(0xFFE2E8F0),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = if (node.tagName == "li") "• ${node.innerText}" else node.innerText,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = customTextColor ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isInspectTouchMode) {
                                onInspectElement()
                            } else if (node.href != null) {
                                onFollowLink(node.href)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun parseCssColorOrNull(cssVal: String?): Color? {
    val clean = cssVal?.trim()?.lowercase() ?: return null
    if (clean.isEmpty()) return null
    return try {
        when {
            clean.startsWith("#") -> Color(android.graphics.Color.parseColor(clean))
            clean == "red" -> Color(0xFFEF4444)
            clean == "blue" -> Color(0xFF3B82F6)
            clean == "green" -> Color(0xFF10B981)
            clean == "yellow" -> Color(0xFFFEF08A)
            clean == "white" -> Color.White
            clean == "black" -> Color.Black
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
