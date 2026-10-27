package com.example.data

import com.example.model.BookmarkItem
import com.example.model.HistoryItem
import com.example.model.SearchEngine
import com.example.model.SearchSuggestion
import com.example.model.SuggestionType
import java.util.Locale

class PersonalizedSuggestionsEngine {

    // Curated top domain suggestions for instant discovery & auto-complete
    private val popularWebsites = listOf(
        Pair("GitHub", "https://github.com"),
        Pair("Wikipedia", "https://www.wikipedia.org"),
        Pair("Reddit", "https://www.reddit.com"),
        Pair("Stack Overflow", "https://stackoverflow.com"),
        Pair("YouTube", "https://www.youtube.com"),
        Pair("Hacker News", "https://news.ycombinator.com"),
        Pair("Android Developers", "https://developer.android.com"),
        Pair("MDN Web Docs", "https://developer.mozilla.org"),
        Pair("ArXiv", "https://arxiv.org"),
        Pair("Product Hunt", "https://www.producthunt.com"),
        Pair("Dev.to", "https://dev.to"),
        Pair("TechCrunch", "https://techcrunch.com"),
        Pair("The Verge", "https://theverge.com"),
        Pair("Google News", "https://news.google.com"),
        Pair("Medium", "https://medium.com"),
        Pair("Dribbble", "https://dribbble.com"),
        Pair("Kaggle", "https://www.kaggle.com"),
        Pair("Wired", "https://www.wired.com"),
        Pair("Substack", "https://substack.com"),
        Pair("Figma", "https://www.figma.com")
    )

    // Curated trending & discovery topics for faster workflow discovery
    val discoveryCategories = listOf(
        "Technology",
        "AI & Dev",
        "Science",
        "Productivity",
        "Design",
        "Global News"
    )

    private val categorySuggestions = mapOf(
        "Technology" to listOf(
            "Next-gen AI assistant workflows",
            "Open source LLM benchmarks 2026",
            "Quantum computing practical milestones",
            "Rust vs Kotlin systems programming"
        ),
        "AI & Dev" to listOf(
            "Jetpack Compose Material 3 best practices",
            "Kotlin multiplatform architectural guides",
            "Local on-device machine learning in Android",
            "Modern reactive state management"
        ),
        "Science" to listOf(
            "James Webb Space Telescope latest images",
            "Fusion energy net output experiments",
            "CRISPR gene therapy advancements",
            "Clean renewable energy breakthroughs"
        ),
        "Productivity" to listOf(
            "Time blocking and deep work frameworks",
            "Minimalist workspace setup inspiration",
            "Second brain digital organization notes",
            "Smart browser tab management workflows"
        ),
        "Design" to listOf(
            "Spatial computing UI/UX design rules",
            "Accessible high contrast color palettes",
            "Micro-interactions and fluid motion design",
            "Modern typography pairing systems"
        ),
        "Global News" to listOf(
            "Global tech summit highlights",
            "World economic tech transition",
            "Digital privacy and web standards updates",
            "Autonomous transport progress"
        )
    )

    /**
     * Generate dynamic personalized search suggestions based on the user's input,
     * browsing history, bookmarks, domain registry, and discovery topics.
     */
    fun getSuggestions(
        query: String,
        history: List<HistoryItem>,
        bookmarks: List<BookmarkItem>,
        selectedCategory: String? = null
    ): List<SearchSuggestion> {
        val trimmed = query.trim()
        val results = mutableListOf<SearchSuggestion>()

        // 1. If query is empty, show personalized history + trending discovery suggestions
        if (trimmed.isEmpty()) {
            // If user clicked a discovery category pill, show suggestions for that category
            if (selectedCategory != null && categorySuggestions.containsKey(selectedCategory)) {
                categorySuggestions[selectedCategory]?.forEach { item ->
                    results.add(
                        SearchSuggestion(
                            title = item,
                            query = item,
                            type = SuggestionType.DISCOVERY,
                            subtitle = "Explore $selectedCategory topic",
                            category = selectedCategory
                        )
                    )
                }
                return results
            }

            // Top frequent history as personalized suggestions
            history.sortedByDescending { it.visitCount }
                .take(3)
                .forEach { h ->
                    results.add(
                        SearchSuggestion(
                            title = h.title.ifBlank { h.url },
                            query = h.url,
                            type = SuggestionType.PERSONALIZED,
                            subtitle = "Frequently visited • ${h.visitCount} visits",
                            targetUrl = h.url
                        )
                    )
                }

            // Recent bookmarks
            bookmarks.take(2).forEach { b ->
                results.add(
                    SearchSuggestion(
                        title = b.title,
                        query = b.url,
                        type = SuggestionType.PERSONALIZED,
                        subtitle = "Saved in ${b.folder}",
                        targetUrl = b.url
                    )
                )
            }

            // Trending web discovery items
            results.add(
                SearchSuggestion(
                    title = "Kotlin Multiplatform & Compose in 2026",
                    query = "Kotlin Multiplatform latest features",
                    type = SuggestionType.TRENDING,
                    subtitle = "Trending in Developer Community"
                )
            )
            results.add(
                SearchSuggestion(
                    title = "Modern Browser Tab Workflows & Productivity",
                    query = "browser tab grouping productivity tips",
                    type = SuggestionType.TRENDING,
                    subtitle = "Trending in Productivity"
                )
            )
            results.add(
                SearchSuggestion(
                    title = "Open Source AI & Local Inference Guides",
                    query = "local on device AI guide",
                    type = SuggestionType.TRENDING,
                    subtitle = "Trending in Tech"
                )
            )
            return results
        }

        val lowerQuery = trimmed.lowercase(Locale.ROOT)

        // 2. Direct URL Detection (e.g., github.com, http://..., sub.domain.org)
        val isExplicitUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
        val looksLikeDomain = !trimmed.contains(" ") && trimmed.contains(".") && trimmed.length > 3

        if (isExplicitUrl || looksLikeDomain) {
            val fullUrl = if (isExplicitUrl) trimmed else "https://$trimmed"
            results.add(
                SearchSuggestion(
                    title = "Go directly to $trimmed",
                    query = trimmed,
                    type = SuggestionType.DOMAIN,
                    subtitle = "Open website directly",
                    targetUrl = fullUrl
                )
            )
        }

        // 3. Match user's bookmarks
        bookmarks.filter {
            it.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    it.url.lowercase(Locale.ROOT).contains(lowerQuery)
        }.take(2).forEach { b ->
            results.add(
                SearchSuggestion(
                    title = b.title,
                    query = b.url,
                    type = SuggestionType.PERSONALIZED,
                    subtitle = "Bookmark • ${b.folder}",
                    targetUrl = b.url
                )
            )
        }

        // 4. Match user's history
        history.filter {
            it.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    it.url.lowercase(Locale.ROOT).contains(lowerQuery)
        }.take(3).forEach { h ->
            results.add(
                SearchSuggestion(
                    title = h.title.ifBlank { h.url },
                    query = h.url,
                    type = SuggestionType.HISTORY,
                    subtitle = "From your history",
                    targetUrl = h.url
                )
            )
        }

        // 5. Match popular websites (Domain auto-complete)
        popularWebsites.filter { (name, url) ->
            name.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    url.lowercase(Locale.ROOT).contains(lowerQuery)
        }.take(3).forEach { (name, url) ->
            if (results.none { it.targetUrl == url }) {
                results.add(
                    SearchSuggestion(
                        title = name,
                        query = url,
                        type = SuggestionType.DOMAIN,
                        subtitle = url.removePrefix("https://").removePrefix("www."),
                        targetUrl = url
                    )
                )
            }
        }

        // 6. Category discovery matches
        categorySuggestions.values.flatten().filter {
            it.lowercase(Locale.ROOT).contains(lowerQuery)
        }.take(2).forEach { suggestion ->
            if (results.none { it.query.equals(suggestion, ignoreCase = true) }) {
                results.add(
                    SearchSuggestion(
                        title = suggestion,
                        query = suggestion,
                        type = SuggestionType.DISCOVERY,
                        subtitle = "Personalized Discovery Topic"
                    )
                )
            }
        }

        // 7. General search suggestion
        results.add(
            SearchSuggestion(
                title = trimmed,
                query = trimmed,
                type = SuggestionType.TRENDING,
                subtitle = "Search web with chosen search engine"
            )
        )

        return results.distinctBy { it.title + it.targetUrl }
    }

    /**
     * Resolves the navigation destination URL based on the input text and chosen search engine.
     */
    fun resolveUrl(input: String, searchEngine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return searchEngine.homeUrl

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }

        // Domain heuristic: contains dot, no spaces, valid top-level domain or path
        if (!trimmed.contains(" ") && trimmed.contains(".") && trimmed.length > 3) {
            return "https://$trimmed"
        }

        // Otherwise execute search
        val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
        return "${searchEngine.searchUrlPrefix}$encodedQuery"
    }
}
