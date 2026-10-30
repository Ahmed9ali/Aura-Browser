package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automation.AutomationEngine
import com.example.devtools.DevToolsEngine
import com.example.model.BrowserTab
import com.example.ui.components.AutomationHUD
import com.example.ui.components.BrowserDevToolsOverlay
import kotlinx.coroutines.flow.SharedFlow
import java.io.ByteArrayInputStream
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
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewGeneration by remember { mutableIntStateOf(0) }
    var lastLoadedUrl by remember(tab.id) { mutableStateOf<String?>(null) }
    var lastObservedTabPropUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var lastDesktopMode by remember { mutableStateOf(isDesktopMode) }
    var defaultMobileUserAgent by remember { mutableStateOf<String?>(null) }

    val desktopUserAgent =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    BackHandler(enabled = true) {
        val wv = webViewInstance
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
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
            onWebStateChanged(
                null,
                targetUrl,
                15,
                true,
                webViewInstance?.canGoBack(),
                webViewInstance?.canGoForward()
            )
            webViewInstance?.loadUrl(targetUrl)
        }
        val activeUrl = lastLoadedUrl ?: tab.url
        if (activeUrl.isNotBlank() && !activeUrl.startsWith("aura://")) {
            automationEngine?.syncCurrentPageUrl(activeUrl)
        }
    }

    LaunchedEffect(reloadTrigger) {
        reloadTrigger?.collect {
            val wv = webViewInstance
            if (wv != null) {
                if (wv.url.isNullOrBlank() || wv.url == "about:blank") {
                    val target = lastLoadedUrl ?: tab.url
                    if (target.isNotBlank() && !target.startsWith("aura://")) {
                        wv.loadUrl(target)
                    }
                } else {
                    wv.reload()
                }
            }
        }
    }

    LaunchedEffect(stopLoadingTrigger) {
        stopLoadingTrigger?.collect {
            webViewInstance?.stopLoading()
        }
    }

    LaunchedEffect(translateTrigger) {
        translateTrigger?.collect { targetLang ->
            val wv = webViewInstance ?: return@collect
            val currentUrl = wv.url ?: lastLoadedUrl ?: tab.url
            if (currentUrl.isNotBlank() && !currentUrl.startsWith("aura://") && currentUrl != "about:blank") {
                val encoded = try {
                    URLEncoder.encode(currentUrl, "UTF-8")
                } catch (_: Exception) {
                    currentUrl
                }
                val translateUrl = "https://translate.google.com/translate?sl=auto&tl=$targetLang&u=$encoded"
                lastLoadedUrl = translateUrl
                lastObservedTabPropUrl = translateUrl
                wv.loadUrl(translateUrl)
            }
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
                    settings.userAgentString = defaultMobileUserAgent
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
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
        key(tab.id, webViewGeneration) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        val cleanMobileUa = try {
                            WebSettings.getDefaultUserAgent(ctx).replace("; wv", "")
                        } catch (_: Exception) {
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        }
                        defaultMobileUserAgent = cleanMobileUa

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            allowContentAccess = true
                            allowFileAccess = false
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mediaPlaybackRequiresUserGesture = false
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(false)
                            userAgentString = if (isDesktopMode) desktopUserAgent else cleanMobileUa
                        }

                        automationEngine?.let { engine ->
                            addJavascriptInterface(engine.jsInterface, "AuraAutomation")
                        }
                        devToolsEngine?.let { devTools ->
                            addJavascriptInterface(devTools.jsInterface, "AuraDevTools")
                        }

                        val currentWebView = this
                        try {
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                setAcceptThirdPartyCookies(currentWebView, true)
                            }
                        } catch (_: Exception) {
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://")) {
                                    return false
                                }
                                if (reqUrl.startsWith("intent://")) {
                                    try {
                                        val intent = Intent.parseUri(reqUrl, Intent.URI_INTENT_SCHEME)
                                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                        if (!fallbackUrl.isNullOrBlank()) {
                                            view?.loadUrl(fallbackUrl)
                                            return true
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                                return true
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (request != null) {
                                    val reqUrl = request.url?.toString() ?: ""
                                    val isBlocked =
                                        adShieldEnabled && (devToolsEngine?.isTrackerUrl(reqUrl) == true)
                                    devToolsEngine?.recordInterceptedRequest(request, isBlocked)
                                    if (isBlocked) {
                                        return WebResourceResponse(
                                            "text/plain",
                                            "UTF-8",
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun doUpdateVisitedHistory(
                                view: WebView?,
                                url: String?,
                                isReload: Boolean
                            ) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                if (!url.isNullOrBlank() && !url.startsWith("aura://") && url != "about:blank") {
                                    lastLoadedUrl = url
                                    lastObservedTabPropUrl = url
                                    automationEngine?.syncCurrentPageUrl(url)
                                    onWebStateChanged(
                                        view?.title?.takeIf { it.isNotBlank() },
                                        url,
                                        null,
                                        null,
                                        view?.canGoBack(),
                                        view?.canGoForward()
                                    )
                                }
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                if (!url.isNullOrBlank() && !url.startsWith("aura://")) {
                                    lastLoadedUrl = url
                                    lastObservedTabPropUrl = url
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
                                onWebStateChanged(
                                    null,
                                    url,
                                    15,
                                    true,
                                    view?.canGoBack(),
                                    view?.canGoForward()
                                )
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!url.isNullOrBlank() && !url.startsWith("aura://")) {
                                    lastLoadedUrl = url
                                    lastObservedTabPropUrl = url
                                    automationEngine?.onPageLoaded(url)
                                    if (view != null) {
                                        devToolsEngine?.onPageFinished(view, url)
                                    }
                                }
                                onWebStateChanged(
                                    view?.title?.takeIf { it.isNotBlank() },
                                    url,
                                    100,
                                    false,
                                    view?.canGoBack(),
                                    view?.canGoForward()
                                )
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    onWebStateChanged(
                                        null,
                                        null,
                                        100,
                                        false,
                                        view?.canGoBack(),
                                        view?.canGoForward()
                                    )
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                try {
                                    (view?.parent as? ViewGroup)?.removeView(view)
                                    view?.destroy()
                                } catch (_: Exception) {
                                }
                                webViewInstance = null
                                webViewGeneration += 1
                                return true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                automationEngine?.onPageLoadProgress(newProgress)
                                onWebStateChanged(
                                    view?.title?.takeIf { it.isNotBlank() },
                                    null,
                                    newProgress,
                                    newProgress < 100,
                                    view?.canGoBack(),
                                    view?.canGoForward()
                                )
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) {
                                    onWebStateChanged(
                                        title,
                                        null,
                                        null,
                                        null,
                                        view?.canGoBack(),
                                        view?.canGoForward()
                                    )
                                }
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
                        if (tab.url.isNotBlank() &&
                            !tab.url.startsWith("aura://") &&
                            !areUrlsEquivalent(tab.url, lastLoadedUrl)
                        ) {
                            lastLoadedUrl = tab.url
                            webView.loadUrl(tab.url)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

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
                            onWebStateChanged(
                                null,
                                targetUrl,
                                15,
                                true,
                                webViewInstance?.canGoBack(),
                                webViewInstance?.canGoForward()
                            )
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
