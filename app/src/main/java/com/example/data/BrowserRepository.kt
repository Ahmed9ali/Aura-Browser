package com.example.data

import com.example.model.BookmarkItem
import com.example.model.BrowserSettings
import com.example.model.BrowserTab
import com.example.model.HistoryItem
import com.example.model.SpeedDial
import com.example.model.TabGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BrowserRepository {

    // Default Tab Groups
    private val defaultGroups = listOf(
        TabGroup(
            id = "group_work",
            name = "Work & Projects",
            colorHex = "#6366F1", // Indigo
            isCollapsed = false
        ),
        TabGroup(
            id = "group_research",
            name = "Research & Docs",
            colorHex = "#06B6D4", // Cyan
            isCollapsed = false
        ),
        TabGroup(
            id = "group_personal",
            name = "Personal & Social",
            colorHex = "#10B981", // Emerald
            isCollapsed = false
        )
    )

    // Initial Tabs
    private val initialTabs = listOf(
        BrowserTab(
            id = "tab_1",
            title = "Aura Home",
            url = "aura://home",
            groupId = null,
            previewSnippet = "Fast discovery & smart speed dials"
        ),
        BrowserTab(
            id = "tab_2",
            title = "GitHub",
            url = "https://github.com",
            groupId = "group_work",
            previewSnippet = "GitHub: Where the world builds software"
        ),
        BrowserTab(
            id = "tab_3",
            title = "Android Developers",
            url = "https://developer.android.com",
            groupId = "group_research",
            previewSnippet = "Official modern Android developer guides and API reference"
        ),
        BrowserTab(
            id = "tab_4",
            title = "Wikipedia",
            url = "https://www.wikipedia.org",
            groupId = "group_research",
            previewSnippet = "The Free Encyclopedia"
        )
    )

    // Initial Bookmarks
    private val initialBookmarks = listOf(
        BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = "GitHub - AI & Dev",
            url = "https://github.com",
            folder = "Favorites"
        ),
        BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = "Wikipedia - The Free Encyclopedia",
            url = "https://www.wikipedia.org",
            folder = "Favorites"
        ),
        BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = "Android Developers Documentation",
            url = "https://developer.android.com",
            folder = "Work"
        ),
        BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = "MDN Web Docs - HTML, CSS & JS",
            url = "https://developer.mozilla.org",
            folder = "Work"
        ),
        BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = "Hacker News",
            url = "https://news.ycombinator.com",
            folder = "Reading List"
        ),
        BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = "Reddit - The front page of the internet",
            url = "https://www.reddit.com",
            folder = "Reading List"
        )
    )

    // Default Speed Dials for Home Screen
    val defaultSpeedDials = listOf(
        SpeedDial("Google", "https://www.google.com", "search", "#4285F4"),
        SpeedDial("GitHub", "https://github.com", "code", "#24292E"),
        SpeedDial("Wikipedia", "https://www.wikipedia.org", "wiki", "#333333"),
        SpeedDial("Reddit", "https://www.reddit.com", "social", "#FF4500"),
        SpeedDial("YouTube", "https://www.youtube.com", "video", "#FF0000"),
        SpeedDial("Hacker News", "https://news.ycombinator.com", "news", "#FF6600"),
        SpeedDial("Android Dev", "https://developer.android.com", "tech", "#3DDC84"),
        SpeedDial("DuckDuckGo", "https://duckduckgo.com", "privacy", "#DE5833")
    )

    private val _speedDials = MutableStateFlow<List<SpeedDial>>(defaultSpeedDials)
    val speedDials: StateFlow<List<SpeedDial>> = _speedDials.asStateFlow()

    // Observable states
    private val _tabs = MutableStateFlow<List<BrowserTab>>(initialTabs)
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("tab_1")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _tabGroups = MutableStateFlow<List<TabGroup>>(defaultGroups)
    val tabGroups: StateFlow<List<TabGroup>> = _tabGroups.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(initialBookmarks)
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(
        listOf(
            HistoryItem(title = "GitHub: Build software", url = "https://github.com", visitCount = 14),
            HistoryItem(title = "Wikipedia Encyclopedia", url = "https://www.wikipedia.org", visitCount = 8),
            HistoryItem(title = "Android Jetpack Docs", url = "https://developer.android.com", visitCount = 12),
            HistoryItem(title = "Stack Overflow Community", url = "https://stackoverflow.com", visitCount = 9),
            HistoryItem(title = "Reddit Discussions", url = "https://www.reddit.com", visitCount = 6)
        )
    )
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _settings = MutableStateFlow(BrowserSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    // Tab Management Operations
    fun openNewTab(url: String = "aura://home", title: String = "New Tab", groupId: String? = null): String {
        val newTab = BrowserTab(
            id = UUID.randomUUID().toString(),
            title = title,
            url = url,
            groupId = groupId,
            previewSnippet = if (url == "aura://home") "Explore the web" else url
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        return newTab.id
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            // Update last accessed
            _tabs.value = _tabs.value.map {
                if (it.id == tabId) it.copy(lastAccessedAt = System.currentTimeMillis()) else it
            }
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            // Keep at least one tab open
            val freshTab = BrowserTab(
                id = UUID.randomUUID().toString(),
                title = "Aura Home",
                url = "aura://home"
            )
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
            return
        }

        val closingIndex = currentTabs.indexOfFirst { it.id == tabId }
        val newTabs = currentTabs.filter { it.id != tabId }
        _tabs.value = newTabs

        // If the active tab was closed, switch to adjacent tab
        if (_activeTabId.value == tabId) {
            val nextIndex = (closingIndex - 1).coerceAtLeast(0)
            _activeTabId.value = newTabs[nextIndex].id
        }
    }

    fun closeAllTabsInGroup(groupId: String) {
        val remaining = _tabs.value.filter { it.groupId != groupId }
        if (remaining.isEmpty()) {
            openNewTab()
        } else {
            _tabs.value = remaining
            if (remaining.none { it.id == _activeTabId.value }) {
                _activeTabId.value = remaining.first().id
            }
        }
    }

    fun closeAllTabs() {
        val freshTab = BrowserTab(
            id = UUID.randomUUID().toString(),
            title = "Aura Home",
            url = "aura://home"
        )
        _tabs.value = listOf(freshTab)
        _activeTabId.value = freshTab.id
    }

    fun updateActiveTab(title: String? = null, url: String? = null, isDesktop: Boolean? = null) {
        val activeId = _activeTabId.value
        val currentTab = _tabs.value.find { it.id == activeId } ?: return
        val newTitle = title ?: currentTab.title
        val newUrl = url ?: currentTab.url
        val newDesktop = isDesktop ?: currentTab.isDesktopMode
        val newSnippet = url ?: currentTab.previewSnippet

        if (newTitle != currentTab.title || newUrl != currentTab.url || newDesktop != currentTab.isDesktopMode) {
            _tabs.value = _tabs.value.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(
                        title = newTitle,
                        url = newUrl,
                        isDesktopMode = newDesktop,
                        previewSnippet = newSnippet
                    )
                } else tab
            }
        }

        // Add to history if valid web url
        if (url != null && !url.startsWith("aura://") && url != "about:blank") {
            addToHistory(title ?: url, url)
        }
    }

    fun toggleDesktopMode(tabId: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isDesktopMode = !it.isDesktopMode) else it
        }
    }

    // Tab Grouping Operations
    fun createGroup(name: String, colorHex: String): String {
        val newGroup = TabGroup(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            colorHex = colorHex
        )
        _tabGroups.value = _tabGroups.value + newGroup
        return newGroup.id
    }

    fun updateGroup(groupId: String, name: String, colorHex: String) {
        _tabGroups.value = _tabGroups.value.map {
            if (it.id == groupId) it.copy(name = name, colorHex = colorHex) else it
        }
    }

    fun deleteGroup(groupId: String, keepTabs: Boolean = true) {
        _tabGroups.value = _tabGroups.value.filter { it.id != groupId }
        if (keepTabs) {
            // Ungroup tabs that were in this group
            _tabs.value = _tabs.value.map {
                if (it.groupId == groupId) it.copy(groupId = null) else it
            }
        } else {
            // Close tabs in this group
            closeAllTabsInGroup(groupId)
        }
    }

    fun toggleGroupCollapse(groupId: String) {
        _tabGroups.value = _tabGroups.value.map {
            if (it.id == groupId) it.copy(isCollapsed = !it.isCollapsed) else it
        }
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(groupId = groupId) else it
        }
    }

    // Bookmarks Operations
    fun addBookmark(title: String, url: String, folder: String = "Favorites") {
        if (_bookmarks.value.none { it.url == url }) {
            val newBookmark = BookmarkItem(
                title = title.ifBlank { url },
                url = url,
                folder = folder
            )
            _bookmarks.value = listOf(newBookmark) + _bookmarks.value
        }
    }

    fun removeBookmark(id: String) {
        _bookmarks.value = _bookmarks.value.filter { it.id != id }
    }

    fun removeBookmarkByUrl(url: String) {
        _bookmarks.value = _bookmarks.value.filter { it.url != url }
    }

    fun isBookmarked(url: String): Boolean {
        return _bookmarks.value.any { it.url == url }
    }

    // History Operations
    private fun addToHistory(title: String, url: String) {
        val currentHistory = _history.value.toMutableList()
        val existingIndex = currentHistory.indexOfFirst { it.url == url }
        if (existingIndex >= 0) {
            val existing = currentHistory[existingIndex]
            currentHistory.removeAt(existingIndex)
            currentHistory.add(
                0,
                existing.copy(
                    title = title.ifBlank { existing.title },
                    timestamp = System.currentTimeMillis(),
                    visitCount = existing.visitCount + 1
                )
            )
        } else {
            currentHistory.add(
                0,
                HistoryItem(
                    title = title.ifBlank { url },
                    url = url,
                    timestamp = System.currentTimeMillis(),
                    visitCount = 1
                )
            )
        }
        _history.value = currentHistory.take(100)
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun clearTodayHistory() {
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        _history.value = _history.value.filter { it.timestamp < oneDayAgo }
    }

    fun removeHistoryItem(id: String) {
        _history.value = _history.value.filter { it.id != id }
    }

    // Settings Operations
    fun updateSettings(transform: (BrowserSettings) -> BrowserSettings) {
        _settings.value = transform(_settings.value)
    }

    fun toggleShowTabGroups() {
        _settings.value = _settings.value.copy(showTabGroups = !_settings.value.showTabGroups)
    }

    fun toggleShowSpeedDials() {
        _settings.value = _settings.value.copy(showSpeedDials = !_settings.value.showSpeedDials)
    }

    fun toggleShowDiscoverFeed() {
        _settings.value = _settings.value.copy(showDiscoverFeed = !_settings.value.showDiscoverFeed)
    }

    // Speed Dial Operations
    fun addSpeedDial(title: String, url: String, iconCategory: String = "web", colorHex: String = "#4285F4") {
        val trimmedTitle = title.trim().ifBlank { "Shortcut" }
        val trimmedUrl = url.trim()
        val formattedUrl = if (!trimmedUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedUrl.startsWith("https://", ignoreCase = true) &&
            !trimmedUrl.startsWith("aura://", ignoreCase = true)
        ) {
            "https://$trimmedUrl"
        } else {
            trimmedUrl
        }
        val newDial = SpeedDial(
            title = trimmedTitle,
            url = formattedUrl,
            iconCategory = iconCategory,
            colorHex = colorHex
        )
        _speedDials.value = _speedDials.value + newDial
    }

    fun updateSpeedDial(id: String, title: String, url: String, iconCategory: String = "web", colorHex: String = "#4285F4") {
        val trimmedTitle = title.trim().ifBlank { "Shortcut" }
        val trimmedUrl = url.trim()
        val formattedUrl = if (!trimmedUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedUrl.startsWith("https://", ignoreCase = true) &&
            !trimmedUrl.startsWith("aura://", ignoreCase = true)
        ) {
            "https://$trimmedUrl"
        } else {
            trimmedUrl
        }
        _speedDials.value = _speedDials.value.map {
            if (it.id == id) {
                it.copy(
                    title = trimmedTitle,
                    url = formattedUrl,
                    iconCategory = iconCategory,
                    colorHex = colorHex
                )
            } else it
        }
    }

    fun deleteSpeedDial(id: String) {
        _speedDials.value = _speedDials.value.filter { it.id != id }
    }

    fun resetSpeedDials() {
        _speedDials.value = defaultSpeedDials
    }
}
