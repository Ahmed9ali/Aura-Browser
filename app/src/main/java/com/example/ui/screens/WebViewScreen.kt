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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.devtools.DevToolTab
import com.example.devtools.DevToolsEngine
import com.example.devtools.LiveDomNode
import com.example.devtools.RealHtmlPageLoader
import com.example.model.AutomationActionType
import com.example.model.AutomationStatus
import com.example.model.BrowserTab
import com.example.model.ElementSemanticType
import com.example.model.ScannedElement
import com.example.ui.components.AutomationHUD
import com.example.ui.components.BrowserDevToolsOverlay
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressivePillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
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
    val useNativeComposeEngine = remember { !AuraApplication.hasHardwareRenderNode() }
    var rendererCrashed by remember { mutableStateOf(false) }

    if (useNativeComposeEngine || rendererCrashed) {
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

    val currentActiveUrl = lastLoadedUrl ?: tab.url

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
                        databaseEnabled = true
                        allowContentAccess = true
                        allowFileAccess = false
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false)
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
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    if (!Element.prototype._auraAttachShadowPatched && Element.prototype.attachShadow) {
                                        var origAttach = Element.prototype.attachShadow;
                                        Element.prototype.attachShadow = function(init) {
                                            var sr = origAttach.call(this, init);
                                            this._auraShadowRoot = sr;
                                            return sr;
                                        };
                                        Element.prototype._auraAttachShadowPatched = true;
                                    }
                                })();
                                """.trimIndent(),
                                null
                            )
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

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            try {
                                (view?.parent as? ViewGroup)?.removeView(view)
                                view?.destroy()
                            } catch (_: Exception) {
                            }
                            webViewInstance = null
                            rendererCrashed = true
                            return true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            automationEngine?.onPageLoadProgress(newProgress)
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
                        currentUrl = currentActiveUrl,
                        scannedElements = scannedElements,
                        onSelectMode = { mode -> automationEngine.setHudMode(mode, currentActiveUrl) },
                        onSelectScript = { automationEngine.selectScript(it) },
                        onUpdateCurrentScript = { automationEngine.updateCurrentScript(it) },
                        onStartFreshScript = { automationEngine.startFreshRecordingScript(currentActiveUrl) },
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
                            lastLoadedUrl = targetUrl
                            lastObservedTabPropUrl = targetUrl
                            onWebStateChanged(null, targetUrl, 15, true, webViewInstance?.canGoBack(), webViewInstance?.canGoForward())
                            webViewInstance?.loadUrl(targetUrl)
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

        if (devToolsEngine != null) {
            BrowserDevToolsOverlay(
                devToolsEngine = devToolsEngine,
                currentUrl = currentActiveUrl,
                onCopyText = onCopyText,
                onShowMessage = onShowMessage
            )
        }
    }
}

/**
 * Pure-Kotlin Real HTTP/HTML/DOM Web Engine for environments without a hardware DRM render node.
 * Renders clean Search Results and interactive web pages (with Birthday & Dropdown Bottom Sheets
 * and full Automation Engine Scanner/Playback integration), while keeping Source View, Inspect,
 * and Network Sniffer strictly off until explicitly opened by the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val backStack = remember(tab.id) { mutableStateListOf<String>() }
    var currentHistoryIdx by remember(tab.id) { mutableIntStateOf(-1) }
    var currentUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var lastObservedPropUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf(tab.title) }

    val defaultNodes = remember { mutableStateOf<List<LiveDomNode>>(emptyList()) }
    val liveDomNodes = devToolsEngine?.liveDomNodes?.collectAsStateWithLifecycle()?.value ?: defaultNodes.value
    val activeDevTool = devToolsEngine?.activeTool?.collectAsStateWithLifecycle()?.value
    val isInspectTouchMode = devToolsEngine?.isInspectTouchMode?.collectAsStateWithLifecycle()?.value ?: false
    val highlightedSelector = devToolsEngine?.highlightedSelector?.collectAsStateWithLifecycle()?.value

    // Only show Inspect border/badges when the user explicitly opened Inspect DOM or Touch-Inspect mode
    val isInspectOverlayActive = (activeDevTool == DevToolTab.INSPECT_DOM) || isInspectTouchMode

    val inputValues = remember(currentUrl) { mutableStateMapOf<Int, String>() }
    val scannedSelectors = remember(currentUrl) { mutableStateListOf<String>() }
    var activeAutomationSelector by remember { mutableStateOf<String?>(null) }

    // Interactive Bottom Sheet states for Birthday Picker and Dropdown / Country Picker
    var activeDobNode by remember { mutableStateOf<LiveDomNode?>(null) }
    var activeSelectNode by remember { mutableStateOf<LiveDomNode?>(null) }

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

    LaunchedEffect(tab.id) {
        if (tab.url.isNotBlank() && !tab.url.startsWith("aura://") && backStack.isEmpty()) {
            backStack.add(tab.url)
            currentHistoryIdx = 0
            currentUrl = tab.url
        }
    }

    LaunchedEffect(tab.url) {
        if (tab.url != lastObservedPropUrl) {
            lastObservedPropUrl = tab.url
            if (tab.url.isNotBlank() && !tab.url.startsWith("aura://") && !areUrlsEquivalent(tab.url, currentUrl)) {
                navigateToUrl(tab.url, addToHistory = true)
            }
        }
    }

    BackHandler(enabled = currentHistoryIdx > 0) {
        if (currentHistoryIdx > 0) {
            currentHistoryIdx--
            currentUrl = backStack[currentHistoryIdx]
        } else {
            onNavigateHome()
        }
    }

    LaunchedEffect(reloadTrigger) {
        reloadTrigger?.collect {
            reloadNonce++
        }
    }

    LaunchedEffect(stopLoadingTrigger) {
        stopLoadingTrigger?.collect {
            isLoading = false
            onWebStateChanged(pageTitle, currentUrl, 100, false, currentHistoryIdx > 0, currentHistoryIdx < backStack.lastIndex)
        }
    }

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

    // Connect AutomationEngine fallback scanner & step executor so Grab Elements and Playback work 100%
    LaunchedEffect(automationEngine, currentUrl, liveDomNodes) {
        automationEngine?.webViewProvider = { null }
        automationEngine?.webViewNavigator = { targetUrl ->
            navigateToUrl(targetUrl, addToHistory = true)
        }
        automationEngine?.fallbackClearHighlights = {
            scannedSelectors.clear()
            activeAutomationSelector = null
        }
        automationEngine?.fallbackDomScanner = {
            scannedSelectors.clear()
            val results = mutableListOf<ScannedElement>()
            liveDomNodes.forEach { node ->
                val semStr = node.attributes["data-semantic"]
                val isSearchInput = node.id == "search-input" || node.attributes["type"] == "search"
                when (node.tagName) {
                    "input", "textarea" -> {
                        val sem = when {
                            semStr != null -> runCatching { ElementSemanticType.valueOf(semStr) }.getOrDefault(ElementSemanticType.GENERIC_INPUT)
                            node.id.contains("account_name", ignoreCase = true) || node.inputName?.contains("email", ignoreCase = true) == true || node.attributes["type"] == "email" -> ElementSemanticType.EMAIL
                            node.id.contains("password", ignoreCase = true) || node.attributes["type"] == "password" -> ElementSemanticType.PASSWORD
                            node.inputName?.contains("first", ignoreCase = true) == true -> ElementSemanticType.FIRST_NAME
                            node.inputName?.contains("last", ignoreCase = true) == true -> ElementSemanticType.LAST_NAME
                            node.inputName?.contains("phone", ignoreCase = true) == true || node.attributes["type"] == "tel" -> ElementSemanticType.PHONE
                            isSearchInput -> ElementSemanticType.GENERIC_INPUT
                            else -> ElementSemanticType.USERNAME
                        }
                        scannedSelectors.add(node.cssSelector)
                        results.add(
                            ScannedElement(
                                tagName = node.tagName,
                                type = node.attributes["type"] ?: "text",
                                nameAttr = node.inputName ?: node.id,
                                idAttr = node.id,
                                placeholder = node.inputPlaceholder ?: node.innerText,
                                text = node.innerText,
                                selector = node.cssSelector,
                                pageUrl = currentUrl,
                                isInput = true,
                                isButton = false,
                                semanticType = sem
                            )
                        )
                    }
                    "dob" -> {
                        scannedSelectors.add(node.cssSelector)
                        results.add(
                            ScannedElement(
                                tagName = "input",
                                type = "date",
                                nameAttr = node.inputName ?: "dob",
                                idAttr = node.id,
                                placeholder = node.inputPlaceholder ?: "Birthday (MM/DD/YYYY)",
                                text = node.innerText,
                                selector = node.cssSelector,
                                pageUrl = currentUrl,
                                isInput = true,
                                isButton = false,
                                semanticType = ElementSemanticType.DOB
                            )
                        )
                    }
                    "select" -> {
                        val opts = node.attributes["data-options"]?.split("|")?.filter { it.isNotBlank() }.orEmpty()
                        val sem = when {
                            semStr == "COUNTRY_CODE" || node.id.contains("country", ignoreCase = true) -> ElementSemanticType.COUNTRY_CODE
                            semStr == "GENDER" || node.id.contains("gender", ignoreCase = true) -> ElementSemanticType.GENDER
                            else -> ElementSemanticType.DROPDOWN
                        }
                        scannedSelectors.add(node.cssSelector)
                        results.add(
                            ScannedElement(
                                tagName = "select",
                                type = "select-one",
                                nameAttr = node.inputName ?: node.id,
                                idAttr = node.id,
                                placeholder = node.inputPlaceholder ?: node.innerText,
                                text = node.innerText,
                                selector = node.cssSelector,
                                pageUrl = currentUrl,
                                isInput = false,
                                isButton = false,
                                semanticType = sem,
                                detectedOptions = opts
                            )
                        )
                    }
                    "checkbox" -> {
                        val sem = if (semStr == "CHECKBOX_TERMS" || node.innerText.contains("terms", ignoreCase = true)) {
                            ElementSemanticType.CHECKBOX_TERMS
                        } else {
                            ElementSemanticType.CHECKBOX
                        }
                        scannedSelectors.add(node.cssSelector)
                        results.add(
                            ScannedElement(
                                tagName = "input",
                                type = "checkbox",
                                nameAttr = node.inputName ?: node.id,
                                idAttr = node.id,
                                placeholder = node.innerText,
                                text = node.innerText,
                                selector = node.cssSelector,
                                pageUrl = currentUrl,
                                isInput = false,
                                isButton = false,
                                semanticType = sem
                            )
                        )
                    }
                    "button" -> {
                        val sem = if (node.attributes["type"] == "submit" || semStr == "SUBMIT_BUTTON" ||
                            Regex("sign in|submit|create|continue|register", RegexOption.IGNORE_CASE).containsMatchIn(node.innerText)
                        ) {
                            ElementSemanticType.SUBMIT_BUTTON
                        } else {
                            ElementSemanticType.NEXT_BUTTON
                        }
                        scannedSelectors.add(node.cssSelector)
                        results.add(
                            ScannedElement(
                                tagName = "button",
                                type = node.attributes["type"] ?: "button",
                                nameAttr = node.id,
                                idAttr = node.id,
                                placeholder = "",
                                text = node.innerText,
                                selector = node.cssSelector,
                                pageUrl = currentUrl,
                                isInput = false,
                                isButton = true,
                                semanticType = sem
                            )
                        )
                    }
                }
            }
            results
        }
        automationEngine?.fallbackStepExecutor = { step, resolvedVal ->
            val targetNode = liveDomNodes.firstOrNull {
                it.cssSelector.equals(step.selector, ignoreCase = true) ||
                        (it.id.isNotBlank() && step.selector.contains(it.id, ignoreCase = true)) ||
                        (it.inputName != null && step.selector.contains(it.inputName, ignoreCase = true))
            } ?: liveDomNodes.firstOrNull {
                when (step.actionType) {
                    AutomationActionType.SELECT_DATE -> it.tagName == "dob"
                    AutomationActionType.SELECT_COUNTRY_CODE -> it.tagName == "select" && it.id.contains("country", ignoreCase = true)
                    AutomationActionType.SELECT_DROPDOWN -> it.tagName == "select"
                    AutomationActionType.CHECKBOX, AutomationActionType.RADIO -> it.tagName == "checkbox"
                    AutomationActionType.SUBMIT, AutomationActionType.CLICK -> it.tagName == "button"
                    else -> false
                }
            }

            if (targetNode != null) {
                activeAutomationSelector = targetNode.cssSelector
                when (step.actionType) {
                    AutomationActionType.INPUT,
                    AutomationActionType.SELECT_DATE,
                    AutomationActionType.SELECT_DROPDOWN,
                    AutomationActionType.SELECT_COUNTRY_CODE -> {
                        val finalVal = if (targetNode.tagName == "select") {
                            val opts = targetNode.attributes["data-options"]?.split("|").orEmpty()
                            opts.firstOrNull { it.contains(resolvedVal, ignoreCase = true) } ?: resolvedVal
                        } else {
                            resolvedVal
                        }
                        inputValues[targetNode.index] = finalVal
                    }
                    AutomationActionType.CHECKBOX, AutomationActionType.RADIO -> {
                        inputValues[targetNode.index] = "true"
                    }
                    AutomationActionType.CLEAR -> {
                        inputValues[targetNode.index] = ""
                    }
                    AutomationActionType.SUBMIT, AutomationActionType.CLICK -> {
                        delay(250L)
                        onShowMessage("Submitted: ${targetNode.innerText}")
                    }
                    else -> {}
                }
                true
            } else {
                false
            }
        }
        if (currentUrl.isNotBlank() && !currentUrl.startsWith("aura://")) {
            automationEngine?.syncCurrentPageUrl(currentUrl)
        }
    }

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
                    message = "Loaded ${loaded.finalUrl} (${loaded.domNodes.size} DOM elements)"
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

    val isRecordingAutomation = automationEngine?.executionState?.collectAsStateWithLifecycle()?.value?.status == AutomationStatus.RECORDING

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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = liveDomNodes,
                    key = { node -> "${node.index}_${node.cssSelector}" }
                ) { node ->
                    val isDevToolsHighlighted = isInspectOverlayActive && (highlightedSelector == node.cssSelector)
                    val isAutomationHighlighted = scannedSelectors.contains(node.cssSelector) || activeAutomationSelector == node.cssSelector

                    LiveDomElementView(
                        node = node,
                        isInspectTouchMode = isInspectTouchMode,
                        isDevToolsHighlighted = isDevToolsHighlighted,
                        isAutomationHighlighted = isAutomationHighlighted,
                        inputValue = inputValues[node.index] ?: node.inputValue,
                        onInputValueChange = { inputValues[node.index] = it },
                        onOpenDobSheet = { activeDobNode = node },
                        onOpenSelectSheet = { activeSelectNode = node },
                        onRecordElementIfRecording = {
                            if (isRecordingAutomation) {
                                val actionTypeStr = when (node.tagName) {
                                    "input", "textarea" -> "INPUT"
                                    "dob" -> "SELECT_DATE"
                                    "select" -> if (node.id.contains("country", ignoreCase = true)) "SELECT_COUNTRY_CODE" else "SELECT_DROPDOWN"
                                    "checkbox" -> "CHECKBOX"
                                    "button" -> if (node.attributes["type"] == "submit") "SUBMIT" else "CLICK"
                                    else -> "CLICK"
                                }
                                automationEngine?.syncCurrentPageUrl(currentUrl)
                                automationEngine?.jsInterface?.onUserAction(
                                    actionTypeStr,
                                    node.cssSelector,
                                    node.inputPlaceholder ?: node.innerText.take(40),
                                    node.attributes["data-field-key"] ?: ""
                                )
                            }
                        },
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

        // Interactive Birthday / Date of Birth Bottom Sheet Picker
        val currentDobNode = activeDobNode
        if (currentDobNode != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val months = remember {
                listOf("01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr", "05 - May", "06 - Jun", "07 - Jul", "08 - Aug", "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec")
            }
            var selectedMonth by remember { mutableStateOf("05") }
            var selectedDay by remember { mutableStateOf("14") }
            var selectedYear by remember { mutableStateOf("1998") }

            ModalBottomSheet(
                onDismissRequest = { activeDobNode = null },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Select Birthday",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose Month, Day, and Year",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Month", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        months.take(6).forEach { m ->
                            val code = m.substringBefore(" ")
                            FilterChip(
                                selected = selectedMonth == code,
                                onClick = { selectedMonth = code },
                                label = { Text(m.substringAfter("- "), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        months.drop(6).forEach { m ->
                            val code = m.substringBefore(" ")
                            FilterChip(
                                selected = selectedMonth == code,
                                onClick = { selectedMonth = code },
                                label = { Text(m.substringAfter("- "), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedDay,
                            onValueChange = { if (it.length <= 2) selectedDay = it },
                            label = { Text("Day (DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = selectedYear,
                            onValueChange = { if (it.length <= 4) selectedYear = it },
                            label = { Text("Year (YYYY)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            val formatted = "${selectedYear.ifBlank { "1998" }}-${selectedMonth.padStart(2, '0')}-${selectedDay.ifBlank { "14" }.padStart(2, '0')}"
                            inputValues[currentDobNode.index] = formatted
                            if (isRecordingAutomation) {
                                automationEngine?.syncCurrentPageUrl(currentUrl)
                                automationEngine?.jsInterface?.onUserAction(
                                    "SELECT_DATE",
                                    currentDobNode.cssSelector,
                                    currentDobNode.innerText,
                                    "dob"
                                )
                            }
                            activeDobNode = null
                        },
                        shape = ExpressivePillShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Birthday")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Interactive Dropdown / Country Code Bottom Sheet Picker
        val currentSelectNode = activeSelectNode
        if (currentSelectNode != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val options = remember(currentSelectNode) {
                currentSelectNode.attributes["data-options"]
                    ?.split("|")
                    ?.filter { it.isNotBlank() }
                    ?.ifEmpty { listOf("Option 1", "Option 2") }
                    ?: listOf("Option 1", "Option 2")
            }
            ModalBottomSheet(
                onDismissRequest = { activeSelectNode = null },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = currentSelectNode.innerText.ifBlank { "Select Option" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    options.forEach { opt ->
                        val isSelected = (inputValues[currentSelectNode.index] ?: currentSelectNode.inputValue) == opt
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    inputValues[currentSelectNode.index] = opt
                                    if (isRecordingAutomation) {
                                        val isCountry = currentSelectNode.id.contains("country", ignoreCase = true)
                                        val act = if (isCountry) "SELECT_COUNTRY_CODE" else "SELECT_DROPDOWN"
                                        automationEngine?.syncCurrentPageUrl(currentUrl)
                                        automationEngine?.jsInterface?.onUserAction(
                                            act,
                                            currentSelectNode.cssSelector,
                                            currentSelectNode.innerText,
                                            if (isCountry) "country_code" else "gender"
                                        )
                                    }
                                    activeSelectNode = null
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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

        // Real DevTools Suite Overlay (Strictly shown ONLY when user explicitly opens Source View, Inspect, or Network Sniffer)
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
    isDevToolsHighlighted: Boolean,
    isAutomationHighlighted: Boolean,
    inputValue: String,
    onInputValueChange: (String) -> Unit,
    onOpenDobSheet: () -> Unit,
    onOpenSelectSheet: () -> Unit,
    onRecordElementIfRecording: () -> Unit,
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

    val highlightBorderModifier = when {
        isDevToolsHighlighted || isInspectTouchMode -> {
            Modifier.border(
                width = if (isDevToolsHighlighted) 2.dp else 1.dp,
                color = if (isDevToolsHighlighted) Color(0xFF06B6D4) else Color(0xFF06B6D4).copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
        }
        isAutomationHighlighted -> {
            Modifier.border(
                width = 1.5.dp,
                color = Color(0xFF10B981).copy(alpha = 0.75f),
                shape = RoundedCornerShape(14.dp)
            )
        }
        else -> Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(highlightBorderModifier)
            .let { mod ->
                if (customBgColor != null) {
                    mod.background(customBgColor, RoundedCornerShape(12.dp))
                } else if (isDevToolsHighlighted) {
                    mod.background(Color(0xFF06B6D4).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                } else {
                    mod
                }
            }
    ) {
        // Strictly show DevTools <tag#id> badge ONLY when DevTools Inspect or Touch-Inspect mode is active
        if (isDevToolsHighlighted || isInspectTouchMode) {
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
                        fontFamily = com.example.ui.theme.VarelaRoundFontFamily,
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
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = customTextColor ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isInspectTouchMode) onInspectElement()
                            else if (node.href != null) onFollowLink(node.href)
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
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
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
            "input", "textarea" -> {
                val isSearchBox = node.id == "search-input" || node.attributes["type"] == "search"
                val isPassword = node.attributes["type"] == "password"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = {
                            onRecordElementIfRecording()
                            onInputValueChange(it)
                        },
                        label = if (!isSearchBox) {
                            { Text(node.inputPlaceholder ?: node.innerText, fontSize = 12.sp) }
                        } else null,
                        placeholder = {
                            Text(
                                text = node.inputPlaceholder ?: "Enter text or search...",
                                fontSize = 13.5.sp
                            )
                        },
                        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                        singleLine = true,
                        shape = if (isSearchBox) ExpressivePillShape else RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (isSearchBox) ImeAction.Search else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (inputValue.isNotBlank()) {
                                    onSubmitForm(inputValue)
                                }
                            },
                            onDone = {
                                onRecordElementIfRecording()
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isInspectTouchMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onInspectElement) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Inspect Input",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (isSearchBox) {
                        Spacer(modifier = Modifier.width(8.dp))
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
                                contentDescription = "Search",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search", fontSize = 12.5.sp)
                        }
                    }
                }
            }
            "dob" -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (isInspectTouchMode) {
                                onInspectElement()
                            } else {
                                onRecordElementIfRecording()
                                onOpenDobSheet()
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = node.innerText.ifBlank { "Birthday / Date of Birth" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = inputValue.ifBlank { node.inputPlaceholder ?: "MM/DD/YYYY (Tap to select)" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (inputValue.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            "select" -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (isInspectTouchMode) {
                                onInspectElement()
                            } else {
                                onRecordElementIfRecording()
                                onOpenSelectSheet()
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = node.innerText.ifBlank { "Select Option" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = inputValue.ifBlank { node.inputPlaceholder ?: "Select..." },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            "checkbox" -> {
                val isChecked = inputValue.equals("true", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isInspectTouchMode) {
                                onInspectElement()
                            } else {
                                onRecordElementIfRecording()
                                onInputValueChange(if (isChecked) "false" else "true")
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                onRecordElementIfRecording()
                                onInputValueChange(if (checked) "true" else "false")
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = node.innerText,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            "button" -> {
                Button(
                    onClick = {
                        if (isInspectTouchMode) {
                            onInspectElement()
                        } else {
                            onRecordElementIfRecording()
                            if (node.href != null) {
                                onFollowLink(node.href)
                            }
                        }
                    },
                    shape = ExpressivePillShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(48.dp)
                ) {
                    Text(node.innerText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            "a" -> {
                val snippet = node.attributes["data-snippet"]
                val domain = node.attributes["data-domain"]
                if (node.className == "search-result-card" || !snippet.isNullOrBlank()) {
                    // Clean, modern Search Result Card
                    Surface(
                        shape = ExpressiveCardShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveCardShape)
                            .clickable {
                                if (isInspectTouchMode) {
                                    onInspectElement()
                                } else if (node.href != null) {
                                    onFollowLink(node.href)
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = domain ?: (node.href ?: ""),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = node.innerText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!node.href.isNullOrBlank()) {
                                Text(
                                    text = node.href,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!snippet.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = snippet,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = node.innerText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = customTextColor ?: MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isInspectTouchMode) {
                                    onInspectElement()
                                } else if (node.href != null) {
                                    onFollowLink(node.href)
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )
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
                        fontFamily = com.example.ui.theme.VarelaRoundFontFamily,
                        color = customTextColor ?: Color(0xFFE2E8F0),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = if (node.tagName == "li") "• ${node.innerText}" else node.innerText,
                    fontSize = 14.sp,
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
                        .padding(horizontal = 4.dp, vertical = 4.dp)
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
