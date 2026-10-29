package com.example.model

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "about:blank",
    val groupId: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val previewSnippet: String = "Explore the web",
    val isDesktopMode: Boolean = false
)

data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String,
    val isCollapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val folder: String = "Favorites",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BookmarkSortOption(val label: String) {
    A_TO_Z("A–Z"),
    Z_TO_A("Z–A"),
    PINNED("Pinned First"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    GROUP("Group (A–Z)")
}

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)

enum class SuggestionType {
    PERSONALIZED,
    DOMAIN,
    HISTORY,
    TRENDING,
    DISCOVERY
}

data class SearchSuggestion(
    val title: String,
    val query: String,
    val type: SuggestionType,
    val subtitle: String? = null,
    val targetUrl: String? = null,
    val category: String? = null
)

enum class SearchEngine(
    val displayName: String,
    val searchUrlPrefix: String,
    val homeUrl: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=", "https://www.google.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "https://duckduckgo.com"),
    BING("Bing", "https://www.bing.com/search?q=", "https://www.bing.com"),
    BRAVE("Brave", "https://search.brave.com/search?q=", "https://search.brave.com")
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val suggestionsEnabled: Boolean = true,
    val adShieldEnabled: Boolean = true,
    val desktopModeDefault: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val javaScriptEnabled: Boolean = true,
    val doNotTrack: Boolean = true,
    val showTabGroups: Boolean = false,
    val showSpeedDials: Boolean = true,
    val showDiscoverFeed: Boolean = false
)

enum class NavScreen {
    HOME,
    BOOKMARK,
    SEARCH,
    TAB,
    SETTINGS,
    WEB_PAGE,
    HISTORY,
    AUTOMATION
}

data class SpeedDial(
    val title: String,
    val url: String,
    val iconCategory: String = "web", // "tech", "social", "wiki", "code", "news", "video"
    val colorHex: String = "#4285F4",
    val id: String = UUID.randomUUID().toString()
)
