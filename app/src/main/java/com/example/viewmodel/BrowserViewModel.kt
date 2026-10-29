package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BrowserRepository
import com.example.data.PersonalizedSuggestionsEngine
import com.example.model.BookmarkItem
import com.example.model.BrowserSettings
import com.example.model.BrowserTab
import com.example.model.HistoryItem
import com.example.model.NavScreen
import com.example.model.SearchEngine
import com.example.model.SearchSuggestion
import com.example.model.SpeedDial
import com.example.model.TabGroup
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.net.URLEncoder
import com.example.automation.AutomationEngine

import com.example.model.BookmarkSortOption

data class BrowserUiState(
    val currentScreen: NavScreen = NavScreen.HOME,
    val tabs: List<BrowserTab> = emptyList(),
    val activeTab: BrowserTab? = null,
    val tabGroups: List<TabGroup> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val bookmarkFolders: List<String> = listOf("Favorites", "Work", "Reading List"),
    val bookmarkSortOption: BookmarkSortOption = BookmarkSortOption.PINNED,
    val history: List<HistoryItem> = emptyList(),
    val settings: BrowserSettings = BrowserSettings(),
    val speedDials: List<SpeedDial> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val suggestions: List<SearchSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val loadProgress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val selectedBookmarkFolder: String = "All",
    val showCreateGroupDialog: Boolean = false,
    val tabToMove: BrowserTab? = null,
    val showMoreSheet: Boolean = false,
    val isProxyEnabled: Boolean = false,
    val proxyNode: String = "US Fast Gateway",
    val showProxyDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val dismissedClipboardTexts: Set<String> = emptySet()
)

class BrowserViewModel(
    private val repository: BrowserRepository = BrowserRepository(),
    private val suggestionsEngine: PersonalizedSuggestionsEngine = PersonalizedSuggestionsEngine()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(NavScreen.HOME)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _loadProgress = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(false)
    private val _canGoBack = MutableStateFlow(false)
    private val _canGoForward = MutableStateFlow(false)
    private val _selectedBookmarkFolder = MutableStateFlow("All")
    private val _bookmarkSortOption = MutableStateFlow(BookmarkSortOption.PINNED)
    private val _showCreateGroupDialog = MutableStateFlow(false)
    private val _tabToMove = MutableStateFlow<BrowserTab?>(null)
    private val _showMoreSheet = MutableStateFlow(false)
    private val _isProxyEnabled = MutableStateFlow(false)
    private val _proxyNode = MutableStateFlow("US Fast Gateway")
    private val _showProxyDialog = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _dismissedClipboardTexts = MutableStateFlow<Set<String>>(emptySet())

    private val _reloadTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reloadTrigger: SharedFlow<Unit> = _reloadTrigger.asSharedFlow()

    private val _stopLoadingTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stopLoadingTrigger: SharedFlow<Unit> = _stopLoadingTrigger.asSharedFlow()

    private val _translateTrigger = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val translateTrigger: SharedFlow<String> = _translateTrigger.asSharedFlow()

    // Flow for suggestions computed reactively
    val speedDials: StateFlow<List<SpeedDial>> = repository.speedDials
    val discoveryCategories: List<String> = suggestionsEngine.discoveryCategories

    val automationEngine = AutomationEngine(viewModelScope).apply {
        onRotateProxy = {
            val nodes = listOf(
                "US Fast Gateway",
                "EU Privacy Guard",
                "Asia-Pacific Edge",
                "UK High-Speed Node",
                "Decentralized Onion Relay"
            )
            val currentIndex = nodes.indexOf(_proxyNode.value)
            val nextIndex = (currentIndex + 1) % nodes.size
            _proxyNode.value = nodes[nextIndex]
            _isProxyEnabled.value = true
        }
    }

    val devToolsEngine = com.example.devtools.DevToolsEngine(viewModelScope)

    val uiState: StateFlow<BrowserUiState> = combine(
        listOf(
            _currentScreen,
            repository.tabs,
            repository.activeTabId,
            repository.tabGroups,
            repository.bookmarks,
            repository.history,
            repository.settings,
            repository.speedDials,
            _searchQuery,
            _selectedCategory,
            _loadProgress,
            _isLoading,
            _canGoBack,
            _canGoForward,
            _selectedBookmarkFolder,
            _showCreateGroupDialog,
            _tabToMove,
            _showMoreSheet,
            _isProxyEnabled,
            _proxyNode,
            _showProxyDialog,
            _snackbarMessage,
            _dismissedClipboardTexts,
            repository.bookmarkFolders,
            _bookmarkSortOption
        )
    ) { params ->
        val screen = params[0] as NavScreen
        @Suppress("UNCHECKED_CAST")
        val tabs = params[1] as List<BrowserTab>
        val activeTabId = params[2] as String
        @Suppress("UNCHECKED_CAST")
        val tabGroups = params[3] as List<TabGroup>
        @Suppress("UNCHECKED_CAST")
        val bookmarks = params[4] as List<BookmarkItem>
        @Suppress("UNCHECKED_CAST")
        val history = params[5] as List<HistoryItem>
        val settings = params[6] as BrowserSettings
        @Suppress("UNCHECKED_CAST")
        val speedDialsList = params[7] as List<SpeedDial>
        val searchQuery = params[8] as String
        val selectedCategory = params[9] as String?
        val loadProgress = params[10] as Int
        val isLoading = params[11] as Boolean
        val canGoBack = params[12] as Boolean
        val canGoForward = params[13] as Boolean
        val selectedBookmarkFolder = params[14] as String
        val showCreateGroup = params[15] as Boolean
        val tabToMove = params[16] as BrowserTab?
        val showMoreSheet = params[17] as Boolean
        val isProxyEnabled = params[18] as Boolean
        val proxyNode = params[19] as String
        val showProxyDialog = params[20] as Boolean
        val snackbarMessage = params[21] as String?
        @Suppress("UNCHECKED_CAST")
        val dismissedClipboard = params[22] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val bookmarkFolders = params[23] as List<String>
        val bookmarkSortOption = params[24] as BookmarkSortOption

        val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

        val suggestions = suggestionsEngine.getSuggestions(
            query = searchQuery,
            history = history,
            bookmarks = bookmarks,
            selectedCategory = selectedCategory
        )

        BrowserUiState(
            currentScreen = screen,
            tabs = tabs,
            activeTab = activeTab,
            tabGroups = tabGroups,
            bookmarks = bookmarks,
            bookmarkFolders = bookmarkFolders,
            bookmarkSortOption = bookmarkSortOption,
            history = history,
            settings = settings,
            speedDials = speedDialsList,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            suggestions = suggestions,
            isLoading = isLoading,
            loadProgress = loadProgress,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            selectedBookmarkFolder = selectedBookmarkFolder,
            showCreateGroupDialog = showCreateGroup,
            tabToMove = tabToMove,
            showMoreSheet = showMoreSheet,
            isProxyEnabled = isProxyEnabled,
            proxyNode = proxyNode,
            showProxyDialog = showProxyDialog,
            snackbarMessage = snackbarMessage,
            dismissedClipboardTexts = dismissedClipboard
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BrowserUiState()
    )

    // Navigation Bottom Bar Handlers
    fun navigateTo(screen: NavScreen) {
        _currentScreen.value = screen
    }

    fun toggleAutomationHud() {
        val activeTab = uiState.value.activeTab
        if (_currentScreen.value != NavScreen.WEB_PAGE && _currentScreen.value != NavScreen.HOME) {
            if (activeTab != null && !activeTab.url.startsWith("aura://")) {
                _currentScreen.value = NavScreen.WEB_PAGE
            } else {
                _currentScreen.value = NavScreen.HOME
            }
        }
        val currentUrl = uiState.value.activeTab?.url ?: "aura://home"
        automationEngine.toggleHud(currentUrl)
    }

    fun openAutomationManager() {
        _currentScreen.value = NavScreen.AUTOMATION
    }

    fun openDevTool(tool: com.example.devtools.DevToolTab) {
        val activeTab = uiState.value.activeTab
        val currentUrl = activeTab?.url ?: ""
        val targetUrl = if (currentUrl.isBlank() || currentUrl.startsWith("aura://") || currentUrl == "about:blank") {
            val engineHome = repository.settings.value.searchEngine.homeUrl
            loadUrl(engineHome, initialTitle = repository.settings.value.searchEngine.displayName)
            engineHome
        } else {
            _currentScreen.value = NavScreen.WEB_PAGE
            currentUrl
        }
        devToolsEngine.openTool(tool, targetUrl)
    }

    fun setShowMoreSheet(show: Boolean) {
        _showMoreSheet.value = show
    }

    fun setShowProxyDialog(show: Boolean) {
        _showProxyDialog.value = show
    }

    fun toggleProxy() {
        val newState = !_isProxyEnabled.value
        _isProxyEnabled.value = newState
        _snackbarMessage.value = if (newState) "Proxy enabled: ${_proxyNode.value}" else "Proxy disabled (Direct connection)"
    }

    fun setProxyNode(node: String) {
        _proxyNode.value = node
        _snackbarMessage.value = "Proxy server switched to $node"
    }

    fun translateCurrentPage(targetLang: String = "en") {
        val activeTab = uiState.value.activeTab
        if (activeTab != null && !activeTab.url.startsWith("aura://") && activeTab.url != "about:blank") {
            _translateTrigger.tryEmit(targetLang)
            _snackbarMessage.value = "Translating webpage to English in-place..."
        } else {
            _snackbarMessage.value = "Open a webpage first to translate it"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun dismissClipboardText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            _dismissedClipboardTexts.value = _dismissedClipboardTexts.value + trimmed
        }
    }

    fun copyToClipboard(context: android.content.Context, text: String, label: String = "Link") {
        try {
            val trimmed = text.trim()
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, trimmed)
            clipboard.setPrimaryClip(clip)
            _dismissedClipboardTexts.value = _dismissedClipboardTexts.value - trimmed
            _snackbarMessage.value = "Link copied to clipboard: $trimmed"
        } catch (_: Exception) {}
    }

    // Tab Management
    fun openNewTab(url: String = "aura://home", title: String = "New Tab", groupId: String? = null) {
        repository.openNewTab(url = url, title = title, groupId = groupId)
        if (url == "aura://home") {
            _currentScreen.value = NavScreen.HOME
        } else {
            _currentScreen.value = NavScreen.WEB_PAGE
        }
    }

    fun selectTab(tabId: String) {
        repository.selectTab(tabId)
        val tab = repository.tabs.value.find { it.id == tabId }
        if (tab?.url == "aura://home" || tab?.url == "about:blank") {
            _currentScreen.value = NavScreen.HOME
        } else {
            _currentScreen.value = NavScreen.WEB_PAGE
        }
    }

    fun closeTab(tabId: String) {
        repository.closeTab(tabId)
    }

    fun closeAllTabsInGroup(groupId: String) {
        repository.closeAllTabsInGroup(groupId)
    }

    fun closeAllTabs() {
        repository.closeAllTabs()
        _snackbarMessage.value = "All tabs closed"
    }

    fun closeTabs(tabIds: Set<String>) {
        if (tabIds.isEmpty()) return
        tabIds.forEach { id -> repository.closeTab(id) }
        _snackbarMessage.value = "Closed ${tabIds.size} ${if (tabIds.size == 1) "tab" else "tabs"}"
    }

    fun clearBrowsingData() {
        repository.clearHistory()
        repository.closeAllTabs()
        _dismissedClipboardTexts.value = emptySet()
        _searchQuery.value = ""
        _snackbarMessage.value = "Browsing history and tab data cleared"
    }

    fun toggleDesktopMode(tabId: String) {
        repository.toggleDesktopMode(tabId)
        val tab = repository.tabs.value.find { it.id == tabId }
        val isDesktop = tab?.isDesktopMode == true
        _snackbarMessage.value = if (isDesktop) "Desktop view enabled" else "Mobile view enabled"
    }

    // Tab Grouping Handlers
    fun createGroup(name: String, colorHex: String) {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) {
            repository.createGroup(cleanName, colorHex)
            _snackbarMessage.value = "Group \"$cleanName\" created"
        }
        _showCreateGroupDialog.value = false
    }

    fun createGroupWithTabs(name: String, colorHex: String, tabIds: Set<String>) {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) {
            val groupId = repository.createGroup(cleanName, colorHex)
            tabIds.forEach { tabId ->
                repository.assignTabToGroup(tabId, groupId)
            }
            _snackbarMessage.value = "Grouped ${tabIds.size} ${if (tabIds.size == 1) "tab" else "tabs"} into \"$cleanName\""
        }
        _showCreateGroupDialog.value = false
    }

    fun createGroupAndAssignTab(tabId: String, name: String, colorHex: String) {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) {
            repository.createGroupAndAssignTab(tabId, cleanName, colorHex)
            _snackbarMessage.value = "Categorized tab into group \"$cleanName\""
        }
        _tabToMove.value = null
        _showCreateGroupDialog.value = false
    }

    fun deleteGroup(groupId: String, keepTabs: Boolean = true) {
        val folderName = repository.tabGroups.value.find { it.id == groupId }?.name
        repository.deleteGroup(groupId, keepTabs)
        if (folderName != null) {
            _snackbarMessage.value = "Folder \"$folderName\" removed"
        }
    }

    fun toggleGroupCollapse(groupId: String) {
        repository.toggleGroupCollapse(groupId)
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        repository.assignTabToGroup(tabId, groupId)
        val folderName = groupId?.let { id -> repository.tabGroups.value.find { it.id == id }?.name }
        _snackbarMessage.value = if (folderName != null) {
            "Categorized tab into \"$folderName\""
        } else {
            "Removed tab from folder"
        }
        _tabToMove.value = null
    }

    fun openMoveTabDialog(tab: BrowserTab) {
        _tabToMove.value = tab
    }

    fun closeMoveTabDialog() {
        _tabToMove.value = null
    }

    fun setShowCreateGroupDialog(show: Boolean) {
        _showCreateGroupDialog.value = show
    }

    // Search and Suggestion Engine Handlers
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            _selectedCategory.value = null
        }
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value == category) {
            _selectedCategory.value = null
        } else {
            _selectedCategory.value = category
            _searchQuery.value = ""
        }
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        _selectedCategory.value = null
    }

    fun executeSearch(input: String) {
        val cleanInput = input.trim()
        val destinationUrl = suggestionsEngine.resolveUrl(
            input = cleanInput,
            searchEngine = repository.settings.value.searchEngine
        )
        loadUrl(destinationUrl, initialTitle = cleanInput.ifBlank { null })
    }

    fun selectSuggestion(suggestion: SearchSuggestion) {
        if (suggestion.targetUrl != null) {
            loadUrl(suggestion.targetUrl, initialTitle = suggestion.title.ifBlank { null })
        } else {
            executeSearch(suggestion.query)
        }
    }

    // Web Loading & Lifecycle Handlers
    fun loadUrl(url: String, initialTitle: String? = null) {
        val target = if (url.isBlank()) "aura://home" else url
        if (target == "aura://home") {
            repository.updateActiveTab(title = "Aura Home", url = "aura://home")
            _currentScreen.value = NavScreen.HOME
            return
        }

        repository.updateActiveTab(title = initialTitle, url = target)
        _searchQuery.value = ""
        _currentScreen.value = NavScreen.WEB_PAGE
    }

    fun reloadActiveTab() {
        _reloadTrigger.tryEmit(Unit)
    }

    fun stopLoadingActiveTab() {
        _stopLoadingTrigger.tryEmit(Unit)
        _isLoading.value = false
    }

    fun updateWebState(
        title: String? = null,
        url: String? = null,
        progress: Int? = null,
        loading: Boolean? = null,
        canBack: Boolean? = null,
        canForward: Boolean? = null
    ) {
        if (title != null || url != null) {
            repository.updateActiveTab(title = title, url = url)
        }
        if (progress != null) _loadProgress.value = progress
        if (loading != null) _isLoading.value = loading
        if (canBack != null) _canGoBack.value = canBack
        if (canForward != null) _canGoForward.value = canForward
    }

    // Bookmarks Management
    fun toggleBookmarkCurrentTab() {
        val activeTab = uiState.value.activeTab ?: return
        val url = activeTab.url

        if (url.startsWith("aura://") || url == "about:blank") {
            _snackbarMessage.value = "Cannot bookmark internal dashboard"
            return
        }

        if (repository.isBookmarked(url)) {
            repository.removeBookmarkByUrl(url)
            _snackbarMessage.value = "Bookmark removed"
        } else {
            val title = activeTab.title.ifBlank { url }
            repository.addBookmark(
                title = title,
                url = url,
                folder = "Favorites"
            )
            _snackbarMessage.value = "Page added to bookmarks"
        }
    }

    fun addBookmark(title: String, url: String, folder: String, isPinned: Boolean = false) {
        repository.addBookmark(title, url, folder, isPinned)
        _snackbarMessage.value = "Bookmark saved to $folder"
    }

    fun toggleBookmarkPin(id: String) {
        repository.toggleBookmarkPin(id)
        val item = repository.bookmarks.value.find { it.id == id }
        if (item != null) {
            _snackbarMessage.value = if (item.isPinned) "Pinned \"${item.title}\"" else "Unpinned \"${item.title}\""
        }
    }

    fun addBookmarkFolder(folderName: String) {
        val added = repository.addBookmarkFolder(folderName)
        if (added) {
            val cleaned = folderName.trim()
            _selectedBookmarkFolder.value = cleaned
            _snackbarMessage.value = "Bookmark group \"$cleaned\" created"
        } else {
            _snackbarMessage.value = "Group \"$folderName\" already exists"
        }
    }

    fun setBookmarkSortOption(option: BookmarkSortOption) {
        _bookmarkSortOption.value = option
        _snackbarMessage.value = "Sorted bookmarks by ${option.label}"
    }

    fun importBookmarks(rawContent: String, defaultFolder: String = "Favorites"): Int {
        val count = repository.importBookmarksFromText(rawContent, defaultFolder)
        _snackbarMessage.value = if (count > 0) {
            "Imported $count bookmark${if (count == 1) "" else "s"}"
        } else {
            "No valid bookmarks found to import"
        }
        return count
    }

    fun exportBookmarksJson(): String = repository.exportBookmarksJson()

    fun exportBookmarksHtml(): String = repository.exportBookmarksHtml()

    fun removeBookmark(id: String) {
        repository.removeBookmark(id)
    }

    fun setSelectedBookmarkFolder(folder: String) {
        _selectedBookmarkFolder.value = folder
    }

    // Settings Updates
    fun setSearchEngine(engine: SearchEngine) {
        repository.updateSettings { it.copy(searchEngine = engine) }
    }

    fun toggleSuggestions() {
        repository.updateSettings { it.copy(suggestionsEnabled = !it.suggestionsEnabled) }
    }

    fun toggleAdShield() {
        repository.updateSettings { it.copy(adShieldEnabled = !it.adShieldEnabled) }
        val isShieldOn = repository.settings.value.adShieldEnabled
        _snackbarMessage.value = if (isShieldOn) "Ad & Tracker Shield enabled" else "Ad & Tracker Shield disabled"
    }

    fun toggleDesktopModeDefault() {
        repository.updateSettings { it.copy(desktopModeDefault = !it.desktopModeDefault) }
    }

    fun setThemeMode(mode: com.example.model.ThemeMode) {
        repository.updateSettings { it.copy(themeMode = mode) }
    }

    fun clearHistory() {
        repository.clearHistory()
        _snackbarMessage.value = "Browsing history cleared"
    }

    fun clearTodayHistory() {
        repository.clearTodayHistory()
        _snackbarMessage.value = "Today's browsing history cleared"
    }

    fun removeHistoryItem(id: String) {
        repository.removeHistoryItem(id)
    }

    // Home Screen Sections Customization
    fun toggleShowTabGroups() {
        repository.toggleShowTabGroups()
    }

    fun toggleShowSpeedDials() {
        repository.toggleShowSpeedDials()
    }

    fun toggleShowDiscoverFeed() {
        repository.toggleShowDiscoverFeed()
    }

    // Speed Dial Management
    fun addSpeedDial(title: String, url: String, iconCategory: String = "web", colorHex: String = "#4285F4") {
        repository.addSpeedDial(title, url, iconCategory, colorHex)
        _snackbarMessage.value = "Shortcut added to Speed Dials"
    }

    fun updateSpeedDial(id: String, title: String, url: String, iconCategory: String = "web", colorHex: String = "#4285F4") {
        repository.updateSpeedDial(id, title, url, iconCategory, colorHex)
        _snackbarMessage.value = "Speed Dial updated"
    }

    fun deleteSpeedDial(id: String) {
        repository.deleteSpeedDial(id)
        _snackbarMessage.value = "Speed Dial removed"
    }

    fun resetSpeedDials() {
        repository.resetSpeedDials()
        _snackbarMessage.value = "Speed Dials reset to default"
    }
}
