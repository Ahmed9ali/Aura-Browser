package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.AuraApplication
import com.example.model.BookmarkItem
import com.example.model.BrowserSettings
import com.example.model.BrowserTab
import com.example.model.HistoryItem
import com.example.model.SearchEngine
import com.example.model.SpeedDial
import com.example.model.TabGroup
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BrowserRepository(
    context: Context? = null
) {
    private val prefs: SharedPreferences? = runCatching {
        val ctx = context ?: AuraApplication.instance
        ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.getOrNull()

    // Default clean initial tabs (starts with 1 clean home tab)
    private val initialTabs = listOf(
        BrowserTab(
            id = "tab_1",
            title = "Aura Home",
            url = "aura://home",
            groupId = null,
            previewSnippet = "Fast discovery & smart speed dials"
        )
    )

    // Clean initial groups (empty for fresh app)
    private val defaultGroups = emptyList<TabGroup>()

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

    // Observable states - Fresh and clean initial state
    private val _tabs = MutableStateFlow<List<BrowserTab>>(initialTabs)
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("tab_1")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _tabGroups = MutableStateFlow<List<TabGroup>>(defaultGroups)
    val tabGroups: StateFlow<List<TabGroup>> = _tabGroups.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val _bookmarkFolders = MutableStateFlow<List<String>>(listOf("Favorites", "Work", "Reading List"))
    val bookmarkFolders: StateFlow<List<String>> = _bookmarkFolders.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _settings = MutableStateFlow(loadSavedSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    // Settings Persistence
    private fun loadSavedSettings(): BrowserSettings {
        val sp = prefs ?: return BrowserSettings()
        val engineStr = sp.getString(KEY_SEARCH_ENGINE, SearchEngine.GOOGLE.name)
        val engine = runCatching { SearchEngine.valueOf(engineStr ?: SearchEngine.GOOGLE.name) }
            .getOrDefault(SearchEngine.GOOGLE)
        val themeStr = sp.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val theme = runCatching { ThemeMode.valueOf(themeStr ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)

        return BrowserSettings(
            searchEngine = engine,
            suggestionsEnabled = sp.getBoolean(KEY_SUGGESTIONS_ENABLED, true),
            adShieldEnabled = sp.getBoolean(KEY_AD_SHIELD_ENABLED, true),
            desktopModeDefault = sp.getBoolean(KEY_DESKTOP_MODE_DEFAULT, false),
            themeMode = theme,
            javaScriptEnabled = sp.getBoolean(KEY_JAVASCRIPT_ENABLED, true),
            doNotTrack = sp.getBoolean(KEY_DO_NOT_TRACK, true),
            showTabGroups = sp.getBoolean(KEY_SHOW_TAB_GROUPS, true),
            showSpeedDials = sp.getBoolean(KEY_SHOW_SPEED_DIALS, true),
            showDiscoverFeed = sp.getBoolean(KEY_SHOW_DISCOVER_FEED, true)
        )
    }

    private fun persistSettings(newSettings: BrowserSettings) {
        prefs?.edit()?.apply {
            putString(KEY_SEARCH_ENGINE, newSettings.searchEngine.name)
            putString(KEY_THEME_MODE, newSettings.themeMode.name)
            putBoolean(KEY_SUGGESTIONS_ENABLED, newSettings.suggestionsEnabled)
            putBoolean(KEY_AD_SHIELD_ENABLED, newSettings.adShieldEnabled)
            putBoolean(KEY_DESKTOP_MODE_DEFAULT, newSettings.desktopModeDefault)
            putBoolean(KEY_JAVASCRIPT_ENABLED, newSettings.javaScriptEnabled)
            putBoolean(KEY_DO_NOT_TRACK, newSettings.doNotTrack)
            putBoolean(KEY_SHOW_TAB_GROUPS, newSettings.showTabGroups)
            putBoolean(KEY_SHOW_SPEED_DIALS, newSettings.showSpeedDials)
            putBoolean(KEY_SHOW_DISCOVER_FEED, newSettings.showDiscoverFeed)
            apply()
        }
    }

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
            _tabs.value = _tabs.value.map {
                if (it.id == tabId) it.copy(lastAccessedAt = System.currentTimeMillis()) else it
            }
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
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
            _tabs.value = _tabs.value.map {
                if (it.groupId == groupId) it.copy(groupId = null) else it
            }
        } else {
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
    fun addBookmark(
        title: String,
        url: String,
        folder: String = "Favorites",
        isPinned: Boolean = false
    ) {
        val cleanFolder = folder.trim().ifBlank { "Favorites" }
        if (cleanFolder != "All" && _bookmarkFolders.value.none { it.equals(cleanFolder, ignoreCase = true) }) {
            _bookmarkFolders.value = _bookmarkFolders.value + cleanFolder
        }
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        if (_bookmarks.value.none { it.url == cleanUrl }) {
            val newBookmark = BookmarkItem(
                title = title.trim().ifBlank { cleanUrl },
                url = cleanUrl,
                folder = cleanFolder,
                isPinned = isPinned
            )
            _bookmarks.value = listOf(newBookmark) + _bookmarks.value
        } else {
            _bookmarks.value = _bookmarks.value.map { existing ->
                if (existing.url == cleanUrl) {
                    existing.copy(
                        title = title.trim().ifBlank { existing.title },
                        folder = cleanFolder,
                        isPinned = isPinned || existing.isPinned
                    )
                } else existing
            }
        }
    }

    fun toggleBookmarkPin(id: String) {
        _bookmarks.value = _bookmarks.value.map { item ->
            if (item.id == id) item.copy(isPinned = !item.isPinned) else item
        }
    }

    fun addBookmarkFolder(folderName: String): Boolean {
        val cleaned = folderName.trim()
        if (cleaned.isBlank() || cleaned.equals("All", ignoreCase = true)) return false
        if (_bookmarkFolders.value.any { it.equals(cleaned, ignoreCase = true) }) return false
        _bookmarkFolders.value = _bookmarkFolders.value + cleaned
        return true
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

    private fun escapeJsonString(input: String): String {
        return buildString {
            for (ch in input) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }

    private fun unescapeJsonString(input: String): String {
        return input
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    fun exportBookmarksJson(): String {
        val items = _bookmarks.value
        if (items.isEmpty()) return "[]"
        val sb = StringBuilder("[\n")
        items.forEachIndexed { index, b ->
            sb.append("  {\n")
            sb.append("    \"id\": \"${escapeJsonString(b.id)}\",\n")
            sb.append("    \"title\": \"${escapeJsonString(b.title)}\",\n")
            sb.append("    \"url\": \"${escapeJsonString(b.url)}\",\n")
            sb.append("    \"folder\": \"${escapeJsonString(b.folder)}\",\n")
            sb.append("    \"isPinned\": ${b.isPinned},\n")
            sb.append("    \"createdAt\": ${b.createdAt}\n")
            sb.append("  }")
            if (index < items.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun exportBookmarksHtml(): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        sb.appendLine("<TITLE>Bookmarks</TITLE>")
        sb.appendLine("<H1>Bookmarks</H1>")
        sb.appendLine("<DL><p>")
        val grouped = _bookmarks.value.groupBy { it.folder }
        grouped.forEach { (folder, items) ->
            sb.appendLine("    <DT><H3>$folder</H3>")
            sb.appendLine("    <DL><p>")
            items.forEach { item ->
                val pinAttr = if (item.isPinned) " PINNED=\"1\"" else ""
                sb.appendLine("        <DT><A HREF=\"${item.url}\" ADD_DATE=\"${item.createdAt / 1000}\"$pinAttr>${item.title}</A>")
            }
            sb.appendLine("    </DL><p>")
        }
        sb.appendLine("</DL><p>")
        return sb.toString()
    }

    fun importBookmarksFromText(rawContent: String, defaultFolder: String = "Favorites"): Int {
        val trimmed = rawContent.trim()
        if (trimmed.isBlank()) return 0
        var importedCount = 0

        // 1. Try JSON array parsing (pure Kotlin, works in Android runtime and JVM unit tests)
        if (trimmed.startsWith("[")) {
            val objectBlockRegex = Regex("""\{[^{}]*\}""", RegexOption.DOT_MATCHES_ALL)
            val stringFieldRegex = { key: String ->
                Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""", RegexOption.IGNORE_CASE)
            }
            val boolFieldRegex = { key: String ->
                Regex(""""$key"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
            }

            objectBlockRegex.findAll(trimmed).forEach { blockMatch ->
                val block = blockMatch.value
                val rawUrl = stringFieldRegex("url").find(block)?.groupValues?.get(1)?.let { unescapeJsonString(it).trim() }.orEmpty()
                if (rawUrl.isNotBlank()) {
                    val normalizedUrl = normalizeBookmarkUrl(rawUrl)
                    val title = stringFieldRegex("title").find(block)?.groupValues?.get(1)
                        ?.let { unescapeJsonString(it).trim() }
                        .orEmpty()
                        .ifBlank { normalizedUrl }
                    val folder = stringFieldRegex("folder").find(block)?.groupValues?.get(1)
                        ?.let { unescapeJsonString(it).trim() }
                        .orEmpty()
                        .ifBlank { defaultFolder }
                    val isPinned = boolFieldRegex("isPinned").find(block)?.groupValues?.get(1)
                        ?.equals("true", ignoreCase = true) == true
                    addBookmark(title = title, url = normalizedUrl, folder = folder, isPinned = isPinned)
                    importedCount++
                }
            }
            if (importedCount > 0) return importedCount
        }

        // 2. Try HTML (<A HREF="...">Title</A>) parsing
        val htmlAnchorRegex = Regex("""<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val htmlMatches = htmlAnchorRegex.findAll(trimmed).toList()
        if (htmlMatches.isNotEmpty()) {
            htmlMatches.forEach { match ->
                val rawUrl = match.groupValues[1].trim()
                val rawTitle = match.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
                val isPinned = match.value.contains("PINNED=\"1\"", ignoreCase = true)
                if (rawUrl.isNotBlank() && !rawUrl.startsWith("javascript:", ignoreCase = true)) {
                    val normalizedUrl = normalizeBookmarkUrl(rawUrl)
                    addBookmark(
                        title = rawTitle.ifBlank { normalizedUrl },
                        url = normalizedUrl,
                        folder = defaultFolder,
                        isPinned = isPinned
                    )
                    importedCount++
                }
            }
            if (importedCount > 0) return importedCount
        }

        // 3. Fallback: Line-by-line parsing ("Title | URL | Folder" or "Title - URL" or plain URL)
        trimmed.lines().forEach { line ->
            val cleanLine = line.trim()
            if (cleanLine.isBlank() || cleanLine.startsWith("#") || cleanLine.startsWith("//")) return@forEach

            if (cleanLine.contains("|")) {
                val parts = cleanLine.split("|").map { it.trim() }
                if (parts.size >= 2) {
                    val title = parts[0]
                    val urlPart = parts[1]
                    val folderPart = parts.getOrNull(2)?.ifBlank { defaultFolder } ?: defaultFolder
                    val pinnedPart = parts.getOrNull(3)?.equals("pinned", ignoreCase = true) == true ||
                            parts.getOrNull(3)?.equals("true", ignoreCase = true) == true
                    if (urlPart.isNotBlank()) {
                        val normalizedUrl = normalizeBookmarkUrl(urlPart)
                        addBookmark(
                            title = title.ifBlank { normalizedUrl },
                            url = normalizedUrl,
                            folder = folderPart,
                            isPinned = pinnedPart
                        )
                        importedCount++
                    }
                }
            } else {
                val urlRegex = Regex("""(https?://[^\s,;"']+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s,;"']*)?)""")
                val match = urlRegex.find(cleanLine)
                if (match != null) {
                    val rawUrl = match.value
                    val normalizedUrl = normalizeBookmarkUrl(rawUrl)
                    val remainingTitle = cleanLine.replace(rawUrl, "")
                        .trim(' ', '-', ':', ',', '|', '•', '\t')
                        .ifBlank {
                            normalizedUrl.removePrefix("https://")
                                .removePrefix("http://")
                                .removePrefix("www.")
                                .substringBefore("/")
                        }
                    addBookmark(
                        title = remainingTitle,
                        url = normalizedUrl,
                        folder = defaultFolder
                    )
                    importedCount++
                }
            }
        }

        return importedCount
    }

    private fun normalizeBookmarkUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("aura://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    // History Operations
    fun recordVisit(url: String, title: String = "") {
        addToHistory(title.ifBlank { url }, url)
    }

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

    // Settings Operations - Persisted across app restarts
    fun updateSettings(transform: (BrowserSettings) -> BrowserSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        persistSettings(updated)
    }

    fun toggleShowTabGroups() {
        updateSettings { it.copy(showTabGroups = !it.showTabGroups) }
    }

    fun toggleShowSpeedDials() {
        updateSettings { it.copy(showSpeedDials = !it.showSpeedDials) }
    }

    fun toggleShowDiscoverFeed() {
        updateSettings { it.copy(showDiscoverFeed = !it.showDiscoverFeed) }
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

    companion object {
        private const val PREFS_NAME = "aura_browser_settings"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SUGGESTIONS_ENABLED = "suggestions_enabled"
        private const val KEY_AD_SHIELD_ENABLED = "ad_shield_enabled"
        private const val KEY_DESKTOP_MODE_DEFAULT = "desktop_mode_default"
        private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
        private const val KEY_DO_NOT_TRACK = "do_not_track"
        private const val KEY_SHOW_TAB_GROUPS = "show_tab_groups"
        private const val KEY_SHOW_SPEED_DIALS = "show_speed_dials"
        private const val KEY_SHOW_DISCOVER_FEED = "show_discover_feed"
    }
}
