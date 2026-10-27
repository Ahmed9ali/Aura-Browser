package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AuraToastBanner
import com.example.ui.components.AutomationHUD
import com.example.model.AutomationActionType
import com.example.model.AutomationStatus
import com.example.model.AutomationStep
import com.example.model.NavScreen
import com.example.model.ThemeMode
import com.example.ui.components.BrowserBottomBar
import com.example.ui.components.BrowserMoreMenuSheet
import com.example.ui.components.BrowserTopBar
import com.example.ui.components.ProxyDialog
import com.example.ui.screens.AutomationManagerScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TabsScreen
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.AuraBrowserTheme
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val webViewDir = java.io.File(applicationContext.dataDir, "app_webview")
            if (webViewDir.exists()) {
                webViewDir.listFiles()?.forEach { f ->
                    if (f.name.contains("variations", ignoreCase = true) || f.name.contains("seed", ignoreCase = true)) {
                        f.delete()
                    }
                }
            }
        } catch (_: Exception) {}
        enableEdgeToEdge()
        setContent {
            val viewModel: BrowserViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val isDarkTheme = when (uiState.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            AuraBrowserTheme(darkTheme = isDarkTheme) {
                BrowserApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BrowserApp(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeTab = uiState.activeTab
    val activeGroup = uiState.tabGroups.find { it.id == activeTab?.groupId }
    val isBookmarked = activeTab?.url?.let {
        !it.startsWith("aura://") && uiState.bookmarks.any { b -> b.url == it }
    } ?: false

    val automationExecutionState by viewModel.automationEngine.executionState.collectAsStateWithLifecycle()
    val isAutomationActive = automationExecutionState.isHudVisible ||
            automationExecutionState.status == AutomationStatus.RECORDING ||
            automationExecutionState.status == AutomationStatus.PLAYING

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    // Back button behavior for sub-screens
    BackHandler(enabled = uiState.currentScreen != NavScreen.HOME && uiState.currentScreen != NavScreen.WEB_PAGE) {
        if (activeTab != null && !activeTab.url.startsWith("aura://") && activeTab.url != "about:blank") {
            viewModel.navigateTo(NavScreen.WEB_PAGE)
        } else {
            viewModel.navigateTo(NavScreen.HOME)
        }
    }

    Scaffold(
        topBar = {
            if (uiState.currentScreen == NavScreen.WEB_PAGE || uiState.currentScreen == NavScreen.HOME) {
                BrowserTopBar(
                    activeTab = activeTab,
                    activeGroup = activeGroup,
                    isLoading = uiState.isLoading,
                    loadProgress = uiState.loadProgress,
                    isAutomationActive = isAutomationActive,
                    onSearchClick = { viewModel.navigateTo(NavScreen.SEARCH) },
                    onReload = {
                        viewModel.reloadActiveTab()
                    },
                    onStopLoading = {
                        viewModel.stopLoadingActiveTab()
                    },
                    onToggleAutomation = {
                        viewModel.toggleAutomationHud()
                    },
                    onCopyUrl = {
                        activeTab?.let { tab ->
                            if (!tab.url.startsWith("aura://")) {
                                viewModel.copyToClipboard(context, tab.url)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            BrowserBottomBar(
                currentScreen = uiState.currentScreen,
                tabCount = uiState.tabs.size,
                isCurrentPageBookmarked = isBookmarked,
                onNavigate = { screen ->
                    if (screen == NavScreen.HOME && activeTab != null && !activeTab.url.startsWith("aura://")) {
                        viewModel.loadUrl("aura://home")
                    } else {
                        viewModel.navigateTo(screen)
                    }
                },
                onBookmarkPress = {
                    viewModel.toggleBookmarkCurrentTab()
                },
                onBookmarkHold = {
                    viewModel.navigateTo(NavScreen.BOOKMARK)
                },
                onMoreClick = {
                    viewModel.setShowMoreSheet(true)
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("aura_snackbar_host")
            ) { snackbarData ->
                AuraToastBanner(message = snackbarData.visuals.message)
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { targetScreen ->
                when (targetScreen) {
                    NavScreen.HOME -> {
                        HomeScreen(
                            settings = uiState.settings,
                            speedDials = uiState.speedDials,
                            tabGroups = uiState.tabGroups,
                            tabs = uiState.tabs,
                            history = uiState.history,
                            onOpenUrl = { url -> viewModel.loadUrl(url) },
                            onNavigateToSearch = { viewModel.navigateTo(NavScreen.SEARCH) },
                            onNavigateToTabs = { viewModel.navigateTo(NavScreen.TAB) },
                            onAddSpeedDial = { title, url, cat, col ->
                                viewModel.addSpeedDial(title, url, cat, col)
                            },
                            onUpdateSpeedDial = { id, title, url, cat, col ->
                                viewModel.updateSpeedDial(id, title, url, cat, col)
                            },
                            onDeleteSpeedDial = { id ->
                                viewModel.deleteSpeedDial(id)
                            }
                        )
                    }

                    NavScreen.SEARCH -> {
                        SearchScreen(
                            query = uiState.searchQuery,
                            suggestions = uiState.suggestions,
                            discoveryCategories = viewModel.discoveryCategories,
                            selectedCategory = uiState.selectedCategory,
                            currentSearchEngine = uiState.settings.searchEngine,
                            history = uiState.history,
                            dismissedClipboardTexts = uiState.dismissedClipboardTexts,
                            onDismissClipboard = { text -> viewModel.dismissClipboardText(text) },
                            onQueryChange = { q -> viewModel.updateSearchQuery(q) },
                            onCategorySelect = { cat -> viewModel.selectCategory(cat) },
                            onSearchEngineSelect = { engine -> viewModel.setSearchEngine(engine) },
                            onExecuteSearch = { query -> viewModel.executeSearch(query) },
                            onSelectSuggestion = { suggestion -> viewModel.selectSuggestion(suggestion) },
                            onClearQuery = { viewModel.clearSearchQuery() },
                            onClearHistory = { viewModel.clearHistory() },
                            onDeleteHistoryItem = { id -> viewModel.removeHistoryItem(id) }
                        )
                    }

                    NavScreen.TAB -> {
                        TabsScreen(
                            tabs = uiState.tabs,
                            activeTab = activeTab,
                            tabGroups = uiState.tabGroups,
                            showCreateGroupDialog = uiState.showCreateGroupDialog,
                            tabToMove = uiState.tabToMove,
                            onSelectTab = { id -> viewModel.selectTab(id) },
                            onCloseTab = { id -> viewModel.closeTab(id) },
                            onOpenNewTab = { groupId -> viewModel.openNewTab(groupId = groupId) },
                            onCloseAllTabs = { viewModel.closeAllTabs() },
                            onCloseAllInGroup = { groupId -> viewModel.closeAllTabsInGroup(groupId) },
                            onCreateGroup = { name, color -> viewModel.createGroup(name, color) },
                            onDeleteGroup = { groupId -> viewModel.deleteGroup(groupId) },
                            onToggleGroupCollapse = { groupId -> viewModel.toggleGroupCollapse(groupId) },
                            onOpenCreateGroupDialog = { viewModel.setShowCreateGroupDialog(true) },
                            onCloseCreateGroupDialog = { viewModel.setShowCreateGroupDialog(false) },
                            onOpenMoveTabDialog = { tab -> viewModel.openMoveTabDialog(tab) },
                            onCloseMoveTabDialog = { viewModel.closeMoveTabDialog() },
                            onAssignTabToGroup = { tabId, groupId -> viewModel.assignTabToGroup(tabId, groupId) }
                        )
                    }

                    NavScreen.BOOKMARK -> {
                        BookmarksScreen(
                            bookmarks = uiState.bookmarks,
                            selectedFolder = uiState.selectedBookmarkFolder,
                            onSelectFolder = { f -> viewModel.setSelectedBookmarkFolder(f) },
                            onOpenBookmark = { url -> viewModel.loadUrl(url) },
                            onOpenInNewTab = { url, title -> viewModel.openNewTab(url = url, title = title) },
                            onDeleteBookmark = { id -> viewModel.removeBookmark(id) },
                            onAddBookmark = { title, url, folder -> viewModel.addBookmark(title, url, folder) },
                            onCopyUrl = { url -> viewModel.copyToClipboard(context, url) }
                        )
                    }

                    NavScreen.HISTORY -> {
                        HistoryScreen(
                            history = uiState.history,
                            onOpenUrl = { url -> viewModel.loadUrl(url) },
                            onOpenInNewTab = { url, title -> viewModel.openNewTab(url = url, title = title) },
                            onDeleteHistoryItem = { id -> viewModel.removeHistoryItem(id) },
                            onClearAllHistory = { viewModel.clearHistory() },
                            onClearTodayHistory = { viewModel.clearTodayHistory() },
                            onCopyUrl = { url -> viewModel.copyToClipboard(context, url) },
                            onNavigateBack = {
                                if (activeTab != null && !activeTab.url.startsWith("aura://") && activeTab.url != "about:blank") {
                                    viewModel.navigateTo(NavScreen.WEB_PAGE)
                                } else {
                                    viewModel.navigateTo(NavScreen.HOME)
                                }
                            }
                        )
                    }

                    NavScreen.AUTOMATION -> {
                        val currentScript by viewModel.automationEngine.currentScript.collectAsStateWithLifecycle()
                        val savedScripts by viewModel.automationEngine.savedScripts.collectAsStateWithLifecycle()
                        val dataSheets by viewModel.automationEngine.dataSheets.collectAsStateWithLifecycle()

                        AutomationManagerScreen(
                            currentScript = currentScript,
                            savedScripts = savedScripts,
                            dataSheets = dataSheets,
                            executionState = automationExecutionState,
                            onSelectScript = { viewModel.automationEngine.selectScript(it) },
                            onSaveScript = { viewModel.automationEngine.updateCurrentScript(it) },
                            onDeleteScript = { viewModel.automationEngine.deleteScript(it) },
                            onRunScript = {
                                viewModel.automationEngine.setHudVisible(true)
                                if (activeTab != null && !activeTab.url.startsWith("aura://")) {
                                    viewModel.navigateTo(NavScreen.WEB_PAGE)
                                } else {
                                    val target = currentScript.targetUrl.ifBlank {
                                        if (currentScript.targetWebsite != "*") "https://${currentScript.targetWebsite}"
                                        else "https://developer.android.com"
                                    }
                                    viewModel.loadUrl(target)
                                }
                                viewModel.automationEngine.startPlayback()
                            },
                            onSaveDataSheet = { viewModel.automationEngine.saveDataSheet(it) },
                            onDeleteDataSheet = { viewModel.automationEngine.deleteDataSheet(it) },
                            onDeleteStep = { stepId -> viewModel.automationEngine.removeStep(stepId) },
                            onMoveStep = { stepId, dir -> viewModel.automationEngine.moveStep(stepId, dir) },
                            onUpdateStep = { step -> viewModel.automationEngine.updateStep(step) },
                            onAddStep = { step -> viewModel.automationEngine.addStep(step) },
                            onClearAllSteps = { viewModel.automationEngine.clearAllSteps() },
                            onNavigateBack = {
                                if (activeTab != null && !activeTab.url.startsWith("aura://")) {
                                    viewModel.navigateTo(NavScreen.WEB_PAGE)
                                } else {
                                    viewModel.navigateTo(NavScreen.HOME)
                                }
                            }
                        )
                    }

                    NavScreen.SETTINGS -> {
                        SettingsScreen(
                            settings = uiState.settings,
                            speedDials = uiState.speedDials,
                            onSetSearchEngine = { engine -> viewModel.setSearchEngine(engine) },
                            onToggleSuggestions = { viewModel.toggleSuggestions() },
                            onToggleAdShield = { viewModel.toggleAdShield() },
                            onToggleDesktopDefault = { viewModel.toggleDesktopModeDefault() },
                            onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                            onClearBrowsingData = { viewModel.clearHistory() },
                            onToggleTabGroups = { viewModel.toggleShowTabGroups() },
                            onToggleSpeedDials = { viewModel.toggleShowSpeedDials() },
                            onToggleDiscoverFeed = { viewModel.toggleShowDiscoverFeed() },
                            onAddSpeedDial = { title, url, cat, col ->
                                viewModel.addSpeedDial(title, url, cat, col)
                            },
                            onUpdateSpeedDial = { id, title, url, cat, col ->
                                viewModel.updateSpeedDial(id, title, url, cat, col)
                            },
                            onDeleteSpeedDial = { id ->
                                viewModel.deleteSpeedDial(id)
                            },
                            onResetSpeedDials = { viewModel.resetSpeedDials() }
                        )
                    }

                    NavScreen.WEB_PAGE -> {
                        if (activeTab != null) {
                            WebViewScreen(
                                tab = activeTab,
                                isDesktopMode = activeTab.isDesktopMode || uiState.settings.desktopModeDefault,
                                adShieldEnabled = uiState.settings.adShieldEnabled,
                                automationEngine = viewModel.automationEngine,
                                reloadTrigger = viewModel.reloadTrigger,
                                stopLoadingTrigger = viewModel.stopLoadingTrigger,
                                translateTrigger = viewModel.translateTrigger,
                                onOpenScriptManager = { viewModel.openAutomationManager() },
                                onWebStateChanged = { title, url, progress, loading, canBack, canForward ->
                                    viewModel.updateWebState(
                                        title = title,
                                        url = url,
                                        progress = progress,
                                        loading = loading,
                                        canBack = canBack,
                                        canForward = canForward
                                    )
                                },
                                onNavigateHome = { viewModel.loadUrl("aura://home") }
                            )
                        } else {
                            viewModel.navigateTo(NavScreen.HOME)
                        }
                    }
                }
            }

            // Show Floating Automation HUD on the Main Screen (HOME) when user clicks the Robot icon
            if (uiState.currentScreen == NavScreen.HOME && automationExecutionState.isHudVisible) {
                val script by viewModel.automationEngine.currentScript.collectAsStateWithLifecycle()
                val savedScripts by viewModel.automationEngine.savedScripts.collectAsStateWithLifecycle()
                val dataSheets by viewModel.automationEngine.dataSheets.collectAsStateWithLifecycle()
                val scannedElements by viewModel.automationEngine.scannedElements.collectAsStateWithLifecycle()
                val homeUrl = activeTab?.url ?: "aura://home"

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    AutomationHUD(
                        executionState = automationExecutionState,
                        currentScript = script,
                        savedScripts = savedScripts,
                        dataSheets = dataSheets,
                        currentUrl = homeUrl,
                        scannedElements = scannedElements,
                        onSelectMode = { mode -> viewModel.automationEngine.setHudMode(mode, homeUrl) },
                        onSelectScript = { viewModel.automationEngine.selectScript(it) },
                        onUpdateCurrentScript = { viewModel.automationEngine.updateCurrentScript(it) },
                        onStartFreshScript = { viewModel.automationEngine.startFreshRecordingScript(homeUrl) },
                        onSaveCurrentScript = { name, web, sheetId, order, policy, repeatCount ->
                            viewModel.automationEngine.saveCurrentAsScript(name, web, sheetId, order, policy, repeatCount)
                        },
                        onUpdateStep = { viewModel.automationEngine.updateStep(it) },
                        onMoveStep = { stepId, dir -> viewModel.automationEngine.moveStep(stepId, dir) },
                        onDeleteStep = { viewModel.automationEngine.removeStep(it) },
                        onAddStep = { viewModel.automationEngine.addStep(it) },
                        onUpdateScannedElement = { viewModel.automationEngine.updateScannedElement(it) },
                        onToggleRecord = { viewModel.automationEngine.toggleRecording() },
                        onStartPlay = {
                            val targetUrl = script.targetUrl.ifBlank {
                                if (script.targetWebsite != "*" && script.targetWebsite.isNotBlank()) {
                                    "https://${script.targetWebsite}"
                                } else ""
                            }
                            if (targetUrl.isNotBlank() && !targetUrl.startsWith("aura://")) {
                                viewModel.loadUrl(targetUrl)
                            }
                            viewModel.automationEngine.startPlayback()
                        },
                        onStopPlay = { viewModel.automationEngine.stopPlayback() },
                        onTriggerScan = {
                            if (homeUrl.startsWith("aura://")) {
                                viewModel.showMessage("Open a website first to grab its page elements")
                            } else {
                                viewModel.automationEngine.triggerScan()
                            }
                        },
                        onClearScan = { viewModel.automationEngine.clearScanHighlights() },
                        onAddScannedElement = { element -> viewModel.automationEngine.addScannedElementToScript(element) },
                        onAutoMapAllScanned = { viewModel.automationEngine.autoMapAllScannedElements() },
                        onNavigateWebViewToUrl = { targetUrl ->
                            viewModel.executeSearch(targetUrl)
                        },
                        onResolveExhaustionStop = { viewModel.automationEngine.resolveExhaustionWithStop() },
                        onResolveExhaustionDefault = { viewModel.automationEngine.resolveExhaustionWithDefault() },
                        onResolveExhaustionAddNew = { fieldKey, newValues ->
                            viewModel.automationEngine.resolveExhaustionWithNewData(fieldKey, newValues)
                        },
                        onSaveDataSheet = { sheet -> viewModel.automationEngine.saveDataSheet(sheet) },
                        onOpenScriptManager = { viewModel.openAutomationManager() },
                        onCloseHud = { viewModel.automationEngine.setHudVisible(false) }
                    )
                }
            }
        }
    }

    // 3-Dots More Menu Bottom Sheet
    if (uiState.showMoreSheet) {
        BrowserMoreMenuSheet(
            activeTab = activeTab,
            isDesktopMode = activeTab?.isDesktopMode == true || uiState.settings.desktopModeDefault,
            isProxyEnabled = uiState.isProxyEnabled,
            proxyNode = uiState.proxyNode,
            adShieldEnabled = uiState.settings.adShieldEnabled,
            onDismiss = { viewModel.setShowMoreSheet(false) },
            onOpenBookmarks = { viewModel.navigateTo(NavScreen.BOOKMARK) },
            onOpenHistory = { viewModel.navigateTo(NavScreen.HISTORY) },
            onTranslatePage = { viewModel.translateCurrentPage() },
            onToggleProxy = { viewModel.toggleProxy() },
            onOpenProxySettings = {
                viewModel.setShowMoreSheet(false)
                viewModel.setShowProxyDialog(true)
            },
            onToggleDesktopMode = {
                activeTab?.let { viewModel.toggleDesktopMode(it.id) }
            },
            onToggleAdShield = { viewModel.toggleAdShield() },
            onOpenAutomation = { viewModel.openAutomationManager() },
            onOpenSettings = { viewModel.navigateTo(NavScreen.SETTINGS) },
            onShowMessage = { msg -> viewModel.showMessage(msg) }
        )
    }

    // Proxy Configuration Dialog
    if (uiState.showProxyDialog) {
        ProxyDialog(
            isEnabled = uiState.isProxyEnabled,
            currentNode = uiState.proxyNode,
            onToggleEnabled = { viewModel.toggleProxy() },
            onSelectNode = { node -> viewModel.setProxyNode(node) },
            onDismiss = { viewModel.setShowProxyDialog(false) }
        )
    }
}
