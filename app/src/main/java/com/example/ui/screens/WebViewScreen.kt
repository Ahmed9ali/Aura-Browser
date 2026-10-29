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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharedFlow
import com.example.automation.AutomationEngine
import com.example.model.BrowserTab
import com.example.ui.components.AutomationHUD

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
    reloadTrigger: SharedFlow<Unit>? = null,
    stopLoadingTrigger: SharedFlow<Unit>? = null,
    translateTrigger: SharedFlow<String>? = null,
    onOpenScriptManager: () -> Unit = {},
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
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewRecreationKey by remember(tab.id) { mutableStateOf(0) }
    var hasAutoRetriedRenderer by remember(tab.id) { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var lastLoadedUrl by remember(tab.id) { mutableStateOf<String?>(null) }
    var lastObservedTabPropUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var lastDesktopMode by remember { mutableStateOf(isDesktopMode) }

    val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // Intercept back button if webview can navigate backwards
    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateHome()
        }
    }

    // Connect WebView provider and multi-page navigator with AutomationEngine
    LaunchedEffect(webViewInstance, automationEngine) {
        automationEngine?.webViewProvider = { webViewInstance }
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

    // Listen for explicit reload requests (e.g. from top bar reload button)
    LaunchedEffect(reloadTrigger) {
        reloadTrigger?.collect {
            webViewInstance?.reload()
        }
    }

    // Listen for explicit stop loading requests
    LaunchedEffect(stopLoadingTrigger) {
        stopLoadingTrigger?.collect {
            webViewInstance?.stopLoading()
        }
    }

    // Listen for in-place translation requests without bot detection
    LaunchedEffect(translateTrigger) {
        translateTrigger?.collect { targetLang ->
            webViewInstance?.let { wv ->
                executeInPageTranslation(wv, targetLang)
            }
        }
    }

    // Update User-Agent ONLY when desktop mode actually toggles, never on initial mount
    LaunchedEffect(isDesktopMode) {
        if (lastDesktopMode != isDesktopMode) {
            lastDesktopMode = isDesktopMode
            webViewInstance?.settings?.let { settings ->
                if (isDesktopMode) {
                    settings.userAgentString = desktopUserAgent
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                } else {
                    settings.userAgentString = null // default mobile UA
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
        androidx.compose.runtime.key(tab.id, webViewRecreationKey) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Use software layer on WebView to prevent DRM rendernode (/dev/dri/renderD128) and GPU crashes on cloud emulators
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                        }

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

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }

                            if (isDesktopMode) {
                                userAgentString = desktopUserAgent
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            } else {
                                userAgentString = null
                                useWideViewPort = false
                                loadWithOverviewMode = false
                            }
                        }

                        // Register JavaScript interface for Automation Engine
                        automationEngine?.let { engine ->
                            addJavascriptInterface(engine.jsInterface, "AuraAutomation")
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val nextUrl = request?.url?.toString() ?: return false
                                if (nextUrl.startsWith("http://") || nextUrl.startsWith("https://")) {
                                    return false
                                }
                                return try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)
                                    ctx.startActivity(intent)
                                    true
                                } catch (e: Exception) {
                                    true
                                }
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                hasError = false
                                if (!url.isNullOrBlank()) {
                                    lastLoadedUrl = url
                                    automationEngine?.syncCurrentPageUrl(url)
                                    automationEngine?.onPageLoadStarted(url)
                                }
                                onWebStateChanged(
                                    null,
                                    url,
                                    10,
                                    true,
                                    view?.canGoBack(),
                                    view?.canGoForward()
                                )
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                hasAutoRetriedRenderer = false
                                if (!url.isNullOrBlank()) {
                                    lastLoadedUrl = url
                                    automationEngine?.onPageLoaded(url)
                                } else {
                                    automationEngine?.onPageLoaded(lastLoadedUrl ?: "")
                                }
                                onWebStateChanged(
                                    view?.title,
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
                                    hasError = true
                                    errorMessage = error?.description?.toString() ?: "Failed to load page"
                                    automationEngine?.onPageLoaded(request.url?.toString() ?: lastLoadedUrl ?: "")
                                    onWebStateChanged(null, null, 100, false, null, null)
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: android.webkit.SslErrorHandler?,
                                error: android.net.http.SslError?
                            ) {
                                super.onReceivedSslError(view, handler, error)
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: android.webkit.RenderProcessGoneDetail?
                            ): Boolean {
                                // Cleanly detach and destroy the dead WebView instance per Android WebView requirements
                                try {
                                    view?.stopLoading()
                                    view?.removeJavascriptInterface("AuraAutomation")
                                    (view?.parent as? ViewGroup)?.removeView(view)
                                    view?.destroy()
                                } catch (_: Exception) {
                                }
                                if (webViewInstance === view) {
                                    webViewInstance = null
                                }
                                automationEngine?.onPageLoaded(lastLoadedUrl ?: "")
                                if (!hasAutoRetriedRenderer) {
                                    hasAutoRetriedRenderer = true
                                    hasError = false
                                    webViewRecreationKey++
                                } else {
                                    hasError = true
                                    errorMessage = "Page renderer restarted. Tap 'Try Again'."
                                }
                                return true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                automationEngine?.onPageLoadProgress(newProgress)
                                onWebStateChanged(
                                    null,
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
                                    onWebStateChanged(title, null, null, null, null, null)
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
                    // Only load when tab.url prop itself explicitly changed from the ViewModel
                    if (tab.url != lastObservedTabPropUrl) {
                        lastObservedTabPropUrl = tab.url
                        if (tab.url.isNotBlank() && !tab.url.startsWith("aura://") && !areUrlsEquivalent(tab.url, lastLoadedUrl)) {
                            lastLoadedUrl = tab.url
                            webView.loadUrl(tab.url)
                        }
                    }
                },
                onRelease = { webView ->
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_WAIVED, true)
                        }
                        webView.stopLoading()
                        webView.onPause()
                        webView.removeJavascriptInterface("AuraAutomation")
                        webView.webChromeClient = null
                        webView.destroy()
                    } catch (_: Exception) {
                    } finally {
                        if (webViewInstance === webView) {
                            webViewInstance = null
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Error Screen Overlay if network failed or domain cannot be reached
        if (hasError) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
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
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage.ifBlank { "Check your internet connection or the URL address." },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                hasError = false
                                hasAutoRetriedRenderer = false
                                if (webViewInstance == null) {
                                    webViewRecreationKey++
                                } else {
                                    webViewInstance?.reload()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Try Again")
                        }

                        androidx.compose.material3.OutlinedButton(onClick = onNavigateHome) {
                            Text("Go to Home")
                        }
                    }
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
                        currentUrl = lastLoadedUrl ?: tab.url,
                        scannedElements = scannedElements,
                        onSelectMode = { mode -> automationEngine.setHudMode(mode, lastLoadedUrl ?: tab.url) },
                        onSelectScript = { automationEngine.selectScript(it) },
                        onUpdateCurrentScript = { automationEngine.updateCurrentScript(it) },
                        onStartFreshScript = { automationEngine.startFreshRecordingScript(lastLoadedUrl ?: tab.url) },
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
    }
}

private fun executeInPageTranslation(webView: WebView, targetLang: String) {
    val jsCode = """
        (function() {
            try {
                // Ensure floating translation pill
                var banner = document.getElementById('__aura_tr_banner');
                if (!banner) {
                    banner = document.createElement('div');
                    banner.id = '__aura_tr_banner';
                    banner.style.cssText = 'position:fixed;top:12px;left:12px;right:12px;z-index:2147483647;background:#0f172a;color:#f8fafc;padding:10px 14px;border-radius:14px;box-shadow:0 6px 20px rgba(0,0,0,0.4);font-family:-apple-system,BlinkMacSystemFont,Roboto,sans-serif;font-size:13px;display:flex;align-items:center;justify-content:space-between;border:1px solid #334155;';
                    document.body.appendChild(banner);
                }
                banner.innerHTML = '<div style="display:flex;align-items:center;gap:8px;"><span>🌐</span><span style="font-weight:600;">Translating page to ' + '$targetLang' + '...</span></div><button id="__aura_tr_close" style="background:#334155;color:#fff;border:none;border-radius:8px;padding:4px 10px;font-size:12px;cursor:pointer;">Dismiss</button>';
                var closeBtn = document.getElementById('__aura_tr_close');
                if (closeBtn) closeBtn.onclick = function() { banner.remove(); };

                // First attempt: Google Translate element if allowed
                if (!window.__auraGtInit) {
                    window.__auraGtInit = true;
                    var s = document.createElement('script');
                    s.src = 'https://translate.google.com/translate_a/element.js?cb=__auraGtLoaded';
                    s.onerror = function() {
                        __auraTranslateDOM('$targetLang');
                    };
                    document.head.appendChild(s);

                    window.__auraGtLoaded = function() {
                        try {
                            new google.translate.TranslateElement({pageLanguage: 'auto', autoDisplay: false}, '__aura_gt_container');
                            setTimeout(function() {
                                var sel = document.querySelector('.goog-te-combo');
                                if (sel) {
                                    sel.value = '$targetLang';
                                    sel.dispatchEvent(new Event('change'));
                                    banner.innerHTML = '<div style="display:flex;align-items:center;gap:8px;color:#10b981;"><span>✓</span><span style="font-weight:600;">Page translated</span></div><button id="__aura_tr_done" style="background:#334155;color:#fff;border:none;border-radius:8px;padding:4px 10px;font-size:12px;cursor:pointer;">Done</button>';
                                    var doneBtn = document.getElementById('__aura_tr_done');
                                    if (doneBtn) doneBtn.onclick = function() { banner.remove(); };
                                    setTimeout(function() { if (banner) banner.remove(); }, 4000);
                                } else {
                                    __auraTranslateDOM('$targetLang');
                                }
                            }, 600);
                        } catch(e) {
                            __auraTranslateDOM('$targetLang');
                        }
                    };
                } else {
                    var sel = document.querySelector('.goog-te-combo');
                    if (sel) {
                        sel.value = '$targetLang';
                        sel.dispatchEvent(new Event('change'));
                        banner.innerHTML = '<div style="display:flex;align-items:center;gap:8px;color:#10b981;"><span>✓</span><span style="font-weight:600;">Page translated</span></div><button id="__aura_tr_done" style="background:#334155;color:#fff;border:none;border-radius:8px;padding:4px 10px;font-size:12px;cursor:pointer;">Done</button>';
                        var doneBtn = document.getElementById('__aura_tr_done');
                        if (doneBtn) doneBtn.onclick = function() { banner.remove(); };
                        setTimeout(function() { if (banner) banner.remove(); }, 3500);
                    } else {
                        __auraTranslateDOM('$targetLang');
                    }
                }

                function __auraTranslateDOM(lang) {
                    var nodes = [];
                    var walker = document.createTreeWalker(
                        document.body,
                        NodeFilter.SHOW_TEXT,
                        {
                            acceptNode: function(node) {
                                if (!node.nodeValue || !node.nodeValue.trim()) return NodeFilter.FILTER_REJECT;
                                var p = node.parentElement;
                                if (!p) return NodeFilter.FILTER_REJECT;
                                var tag = p.tagName.toLowerCase();
                                if (tag === 'script' || tag === 'style' || tag === 'textarea' || tag === 'input' || tag === 'code' || tag === 'pre' || tag === 'noscript') {
                                    return NodeFilter.FILTER_REJECT;
                                }
                                if (p.id && p.id.indexOf('__aura') === 0) return NodeFilter.FILTER_REJECT;
                                if (node.nodeValue.trim().length > 3) return NodeFilter.FILTER_ACCEPT;
                                return NodeFilter.FILTER_REJECT;
                            }
                        }
                    );
                    var curr;
                    while ((curr = walker.nextNode()) && nodes.length < 80) {
                        nodes.push(curr);
                    }

                    if (nodes.length === 0) {
                        banner.innerHTML = '<span style="font-weight:600;">Page content is already displayed</span>';
                        setTimeout(function() { if (banner) banner.remove(); }, 2500);
                        return;
                    }

                    var completed = 0;
                    nodes.forEach(function(node) {
                        var orig = node.nodeValue.trim();
                        var url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + lang + '&dt=t&q=' + encodeURIComponent(orig);
                        fetch(url)
                            .then(function(r) { return r.json(); })
                            .then(function(data) {
                                if (data && data[0]) {
                                    var tr = data[0].map(function(c) { return c[0]; }).join('');
                                    if (tr && tr.length > 0) {
                                        node.nodeValue = tr;
                                    }
                                }
                            })
                            .catch(function(e) {})
                            .finally(function() {
                                completed++;
                                if (completed >= nodes.length) {
                                    banner.innerHTML = '<div style="display:flex;align-items:center;gap:8px;color:#10b981;"><span>✓</span><span style="font-weight:600;">Translated successfully (' + completed + ' items)</span></div><button id="__aura_tr_ok" style="background:#334155;color:#fff;border:none;border-radius:8px;padding:4px 10px;font-size:12px;cursor:pointer;">Done</button>';
                                    var okBtn = document.getElementById('__aura_tr_ok');
                                    if (okBtn) okBtn.onclick = function() { banner.remove(); };
                                    setTimeout(function() { if (banner) banner.remove(); }, 4000);
                                }
                            });
                    });
                }
            } catch(err) {
                console.error("Aura translate error", err);
            }
        })();
    """.trimIndent()
    webView.evaluateJavascript(jsCode, null)
}
