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

    private val _tabGroups = MutableStateFlow<List<TabGroup>>(loadSavedTabGroups())
    val tabGroups: StateFlow<List<TabGroup>> = _tabGroups.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val _bookmarkFolders = MutableStateFlow<List<String>>(listOf("Favorites", "Work", "Reading List"))
    val bookmarkFolders: StateFlow<List<String>> = _bookmarkFolders.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(loadSavedHistory())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _settings = MutableStateFlow(loadSavedSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    // History Persistence (Real user searches and visited URLs only)
    private fun loadSavedHistory(): List<HistoryItem> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_HISTORY_RECORDS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            raw.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("\t")
                    if (parts.size >= 5) {
                        val id = parts[0]
                        val timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis()
                        val visitCount = parts[2].toIntOrNull() ?: 1
                        val url = parts[3]
                        val title = parts.subList(4, parts.size).joinToString("\t")
                        if (url.isNotBlank() && !url.startsWith("aura://")) {
                            HistoryItem(
                                id = id,
                                title = title.ifBlank { url },
                                url = url,
                                timestamp = timestamp,
                                visitCount = visitCount
                            )
                        } else null
                    } else null
                }
        }.getOrDefault(emptyList())
    }

    private fun persistHistory(items: List<HistoryItem>) {
        prefs?.edit()?.apply {
            val serialized = items.take(100).joinToString("\n") { item ->
                val cleanTitle = item.title.replace("\n", " ").replace("\r", " ").replace("\t", " ")
                val cleanUrl = item.url.replace("\n", "").replace("\r", "").replace("\t", "")
                "${item.id}\t${item.timestamp}\t${item.visitCount}\t$cleanUrl\t$cleanTitle"
            }
            putString(KEY_HISTORY_RECORDS, serialized)
            apply()
        }
    }

    // Tab Groups Persistence
    private fun loadSavedTabGroups(): List<TabGroup> {
        val sp = prefs ?: return defaultGroups
        val raw = sp.getString(KEY_TAB_GROUPS, null) ?: return defaultGroups
        if (raw.isBlank()) return defaultGroups
        return runCatching {
            raw.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("\t")
                    if (parts.size >= 4) {
                        val id = parts[0]
                        val colorHex = parts[1]
                        val isCollapsed = parts[2].toBooleanStrictOrNull() ?: false
                        val name = parts.subList(3, parts.size).joinToString("\t")
                        if (name.isNotBlank()) {
                            TabGroup(
                                id = id,
                                name = name,
                                colorHex = colorHex,
                                isCollapsed = isCollapsed
                            )
                        } else null
                    } else null
                }
        }.getOrDefault(defaultGroups)
    }

    private fun persistTabGroups(groups: List<TabGroup>) {
        prefs?.edit()?.apply {
            val serialized = groups.joinToString("\n") { group ->
                val cleanName = group.name.replace("\n", " ").replace("\r", " ").replace("\t", " ")
                "${group.id}\t${group.colorHex}\t${group.isCollapsed}\t$cleanName"
            }
            putString(KEY_TAB_GROUPS, serialized)
            apply()
        }
    }

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
            showTabGroups = sp.getBoolean(KEY_SHOW_TAB_GROUPS, false),
            showSpeedDials = sp.getBoolean(KEY_SHOW_SPEED_DIALS, true),
            showDiscoverFeed = sp.getBoolean(KEY_SHOW_DISCOVER_FEED, false)
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
        val cleanName = name.trim()
        val existing = _tabGroups.value.find { it.name.equals(cleanName, ignoreCase = true) }
        if (existing != null) {
            return existing.id
        }
        val newGroup = TabGroup(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            colorHex = colorHex
        )
        val updated = _tabGroups.value + newGroup
        _tabGroups.value = updated
        persistTabGroups(updated)
        return newGroup.id
    }

    fun createGroupAndAssignTab(tabId: String, name: String, colorHex: String): String {
        val groupId = createGroup(name, colorHex)
        assignTabToGroup(tabId, groupId)
        return groupId
    }

    fun updateGroup(groupId: String, name: String, colorHex: String) {
        val updated = _tabGroups.value.map {
            if (it.id == groupId) it.copy(name = name.trim(), colorHex = colorHex) else it
        }
        _tabGroups.value = updated
        persistTabGroups(updated)
    }

    fun deleteGroup(groupId: String, keepTabs: Boolean = true) {
        val updated = _tabGroups.value.filter { it.id != groupId }
        _tabGroups.value = updated
        persistTabGroups(updated)
        if (keepTabs) {
            _tabs.value = _tabs.value.map {
                if (it.groupId == groupId) it.copy(groupId = null) else it
            }
        } else {
            closeAllTabsInGroup(groupId)
        }
    }

    fun toggleGroupCollapse(groupId: String) {
        val updated = _tabGroups.value.map {
            if (it.id == groupId) it.copy(isCollapsed = !it.isCollapsed) else it
        }
        _tabGroups.value = updated
        persistTabGroups(updated)
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

    /**
     * Extracts flat JSON object blocks `{ ... }` safely without regex `{` / `}` character classes
     * (which throw PatternSyntaxException on Android's ICU regex engine).
     */
    private fun extractFlatJsonObjectBlocks(text: String): List<String> {
        val blocks = mutableListOf<String>()
        var inString = false
        var escaped = false
        var currentStart = -1

        for (i in text.indices) {
            val ch = text[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> currentStart = i
                    '}' -> {
                        if (currentStart >= 0 && i > currentStart) {
                            blocks.add(text.substring(currentStart, i + 1))
                            currentStart = -1
                        }
                    }
                }
            }
        }
        return blocks
    }

    /**
     * Deterministic, zero-regex string value extractor for a JSON key inside a flat `{ ... }` block.
     */
    private fun extractJsonStringField(block: String, key: String): String? {
        val targetKey = "\"$key\""
        var searchIdx = 0
        while (searchIdx < block.length) {
            val keyIdx = block.indexOf(targetKey, searchIdx, ignoreCase = true)
            if (keyIdx < 0) return null
            var cursor = keyIdx + targetKey.length
            while (cursor < block.length && block[cursor].isWhitespace()) cursor++
            if (cursor < block.length && block[cursor] == ':') {
                cursor++
                while (cursor < block.length && block[cursor].isWhitespace()) cursor++
                if (cursor < block.length && block[cursor] == '"') {
                    cursor++
                    val sb = StringBuilder()
                    var esc = false
                    while (cursor < block.length) {
                        val c = block[cursor]
                        if (esc) {
                            when (c) {
                                '"', '\\', '/' -> sb.append(c)
                                'n' -> sb.append('\n')
                                'r' -> sb.append('\r')
                                't' -> sb.append('\t')
                                else -> sb.append(c)
                            }
                            esc = false
                        } else if (c == '\\') {
                            esc = true
                        } else if (c == '"') {
                            return sb.toString()
                        } else {
                            sb.append(c)
                        }
                        cursor++
                    }
                }
            }
            searchIdx = keyIdx + targetKey.length
        }
        return null
    }

    private fun extractJsonBooleanField(block: String, key: String): Boolean {
        val targetKey = "\"$key\""
        val keyIdx = block.indexOf(targetKey, ignoreCase = true)
        if (keyIdx < 0) return false
        var cursor = keyIdx + targetKey.length
        while (cursor < block.length && block[cursor].isWhitespace()) cursor++
        if (cursor >= block.length || block[cursor] != ':') return false
        cursor++
        while (cursor < block.length && block[cursor].isWhitespace()) cursor++
        return block.regionMatches(cursor, "true", 0, 4, ignoreCase = true)
    }

    fun importBookmarksFromText(rawContent: String, defaultFolder: String = "Favorites"): Int {
        return runCatching {
            val trimmed = rawContent.trim()
            if (trimmed.isBlank()) return 0
            var importedCount = 0
            val seenUrlsInBatch = mutableSetOf<String>()
            val fallbackFolder = defaultFolder.trim().ifBlank { "Favorites" }

            // 1. Try JSON parsing (supports [...] arrays, nested {...} Chrome/Brave/Aura JSON exports, and embedded JSON blocks)
            if (trimmed.contains("{") && trimmed.contains("}")) {
                val blocks = extractFlatJsonObjectBlocks(trimmed)
                blocks.forEach { block ->
                    val rawUrl = (extractJsonStringField(block, "url")
                        ?: extractJsonStringField(block, "href")
                        ?: extractJsonStringField(block, "uri"))
                        ?.trim()
                        .orEmpty()
                    if (rawUrl.isNotBlank() && !rawUrl.startsWith("javascript:", ignoreCase = true)) {
                        val normalizedUrl = normalizeBookmarkUrl(rawUrl)
                        if (seenUrlsInBatch.add(normalizedUrl)) {
                            val title = (extractJsonStringField(block, "title")
                                ?: extractJsonStringField(block, "name"))
                                ?.trim()
                                .orEmpty()
                                .ifBlank { normalizedUrl }
                            val folder = (extractJsonStringField(block, "folder")
                                ?: extractJsonStringField(block, "group")
                                ?: extractJsonStringField(block, "category"))
                                ?.trim()
                                .orEmpty()
                                .ifBlank { fallbackFolder }
                            val isPinned = extractJsonBooleanField(block, "isPinned") ||
                                    extractJsonBooleanField(block, "pinned")
                            addBookmark(title = title, url = normalizedUrl, folder = folder, isPinned = isPinned)
                            importedCount++
                        }
                    }
                }
            }

            // 2. Try HTML (<A HREF="...">Title</A> and optional <H3>Folder</H3>) parsing without multi-line regex backtracking
            if (trimmed.contains("<a", ignoreCase = true) && trimmed.contains("href", ignoreCase = true)) {
                var currentHtmlFolder = fallbackFolder
                val h3Regex = Regex("<h3[^>]*>(.*?)</h3>", RegexOption.IGNORE_CASE)
                val hrefAttrRegex = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

                trimmed.lines().forEach { rawLine ->
                    val line = rawLine.trim()
                    val h3Match = h3Regex.find(line)
                    if (h3Match != null) {
                        val folderTitle = h3Match.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                        if (folderTitle.isNotBlank() && !folderTitle.equals("Bookmarks", ignoreCase = true)) {
                            currentHtmlFolder = folderTitle
                        }
                    }

                    var searchPos = 0
                    while (searchPos < line.length) {
                        val aStart = line.indexOf("<a", searchPos, ignoreCase = true)
                        if (aStart < 0) break
                        val tagClose = line.indexOf('>', aStart)
                        if (tagClose < 0) break
                        val aEnd = line.indexOf("</a>", tagClose, ignoreCase = true)
                        val openTag = line.substring(aStart, tagClose + 1)
                        val hrefMatch = hrefAttrRegex.find(openTag)
                        if (hrefMatch != null) {
                            val rawUrl = hrefMatch.groupValues[1].trim()
                            val rawTitle = if (aEnd > tagClose) {
                                line.substring(tagClose + 1, aEnd).replace(Regex("<[^>]*>"), "").trim()
                            } else {
                                line.substring(tagClose + 1).replace(Regex("<[^>]*>"), "").trim()
                            }
                            val isPinned = openTag.contains("PINNED=\"1\"", ignoreCase = true) ||
                                    openTag.contains("PINNED='1'", ignoreCase = true)
                            if (rawUrl.isNotBlank() &&
                                !rawUrl.startsWith("javascript:", ignoreCase = true) &&
                                !rawUrl.startsWith("place:", ignoreCase = true)
                            ) {
                                val normalizedUrl = normalizeBookmarkUrl(rawUrl)
                                if (seenUrlsInBatch.add(normalizedUrl)) {
                                    addBookmark(
                                        title = rawTitle.ifBlank { normalizedUrl },
                                        url = normalizedUrl,
                                        folder = currentHtmlFolder,
                                        isPinned = isPinned
                                    )
                                    importedCount++
                                }
                            }
                        }
                        searchPos = if (aEnd > tagClose) aEnd + 4 else tagClose + 1
                    }
                }
                if (importedCount > 0) return importedCount
            }

            // 3. Fallback: Line-by-line parsing ("Title | URL | Folder" or "Title - URL" or plain URL)
            val urlRegex = Regex("""(https?://[^\s,;"'\}\]]+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s,;"'\}\]]*)?)""")
            trimmed.lines().forEach { line ->
                val cleanLine = line.trim()
                if (cleanLine.isBlank() ||
                    cleanLine.startsWith("#") ||
                    cleanLine.startsWith("//") ||
                    cleanLine.contains("\"url\"", ignoreCase = true) ||
                    cleanLine.contains("\"title\"", ignoreCase = true) ||
                    cleanLine.contains("<a ", ignoreCase = true)
                ) return@forEach

                if (cleanLine.contains("|")) {
                    val parts = cleanLine.split("|").map { it.trim() }
                    if (parts.size >= 2) {
                        val title = parts[0]
                        val urlPart = parts[1]
                        val folderPart = parts.getOrNull(2)?.ifBlank { fallbackFolder } ?: fallbackFolder
                        val pinnedPart = parts.getOrNull(3)?.equals("pinned", ignoreCase = true) == true ||
                                parts.getOrNull(3)?.equals("true", ignoreCase = true) == true
                        if (urlPart.isNotBlank()) {
                            val normalizedUrl = normalizeBookmarkUrl(urlPart)
                            if (seenUrlsInBatch.add(normalizedUrl)) {
                                addBookmark(
                                    title = title.ifBlank { normalizedUrl },
                                    url = normalizedUrl,
                                    folder = folderPart,
                                    isPinned = pinnedPart
                                )
                                importedCount++
                            }
                        }
                    }
                } else {
                    val match = urlRegex.find(cleanLine)
                    if (match != null) {
                        val rawUrl = match.value
                        val normalizedUrl = normalizeBookmarkUrl(rawUrl)
                        if (seenUrlsInBatch.add(normalizedUrl)) {
                            val remainingTitle = cleanLine.replace(rawUrl, "")
                                .trim(' ', '-', ':', ',', '|', '•', '\t', '{', '}', '[', ']')
                                .ifBlank {
                                    normalizedUrl.removePrefix("https://")
                                        .removePrefix("http://")
                                        .removePrefix("www.")
                                        .substringBefore("/")
                                }
                            addBookmark(
                                title = remainingTitle,
                                url = normalizedUrl,
                                folder = fallbackFolder
                            )
                            importedCount++
                        }
                    }
                }
            }
            importedCount
        }.getOrDefault(0)
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

    private fun extractReadableTitle(rawTitle: String, url: String, existingTitle: String? = null): String {
        val cleaned = rawTitle.trim()
        val isPlaceholder = cleaned.isBlank() ||
                cleaned.equals("Aura Home", ignoreCase = true) ||
                cleaned.equals("New Tab", ignoreCase = true) ||
                cleaned.equals("about:blank", ignoreCase = true)
        if (!isPlaceholder && cleaned != url) {
            return cleaned
        }
        if (!existingTitle.isNullOrBlank() &&
            !existingTitle.equals("Aura Home", ignoreCase = true) &&
            !existingTitle.equals("New Tab", ignoreCase = true) &&
            existingTitle != url
        ) {
            return existingTitle
        }
        // Check if URL is a search engine query URL (?q=... or &q=...)
        val queryParamMatch = Regex("""[?&]q=([^&]+)""").find(url)
        if (queryParamMatch != null) {
            val decoded = runCatching {
                java.net.URLDecoder.decode(queryParamMatch.groupValues[1], "UTF-8").trim()
            }.getOrDefault("")
            if (decoded.isNotBlank()) return decoded
        }
        return if (!isPlaceholder) cleaned else url
    }

    private fun addToHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("aura://") || url == "about:blank") return
        val currentHistory = _history.value.toMutableList()
        val existingIndex = currentHistory.indexOfFirst { it.url == url }
        if (existingIndex >= 0) {
            val existing = currentHistory[existingIndex]
            currentHistory.removeAt(existingIndex)
            val resolvedTitle = extractReadableTitle(title, url, existing.title)
            currentHistory.add(
                0,
                existing.copy(
                    title = resolvedTitle,
                    timestamp = System.currentTimeMillis(),
                    visitCount = existing.visitCount + 1
                )
            )
        } else {
            val resolvedTitle = extractReadableTitle(title, url, null)
            currentHistory.add(
                0,
                HistoryItem(
                    title = resolvedTitle,
                    url = url,
                    timestamp = System.currentTimeMillis(),
                    visitCount = 1
                )
            )
        }
        val updated = currentHistory.take(100)
        _history.value = updated
        persistHistory(updated)
    }

    fun clearHistory() {
        _history.value = emptyList()
        persistHistory(emptyList())
    }

    fun clearTodayHistory() {
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val updated = _history.value.filter { it.timestamp < oneDayAgo }
        _history.value = updated
        persistHistory(updated)
    }

    fun removeHistoryItem(id: String) {
        val updated = _history.value.filter { it.id != id }
        _history.value = updated
        persistHistory(updated)
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
        private const val KEY_SHOW_TAB_GROUPS = "show_tab_groups_v2"
        private const val KEY_SHOW_SPEED_DIALS = "show_speed_dials_v3"
        private const val KEY_SHOW_DISCOVER_FEED = "show_discover_feed_v2"
        private const val KEY_HISTORY_RECORDS = "history_records_v1"
        private const val KEY_TAB_GROUPS = "tab_groups_v1"
    }
}
