package com.example.data

import com.example.model.BookmarkItem
import com.example.model.BrowserTab
import com.example.model.HistoryItem
import com.example.model.SearchEngine
import com.example.model.SearchSuggestion
import com.example.model.SuggestionType
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

data class WebsiteDirectoryEntry(
    val title: String,
    val url: String,
    val description: String,
    val aliases: List<String> = emptyList()
)

class PersonalizedSuggestionsEngine {

    // Comprehensive website directory for fast, intelligent domain & site suggestions
    private val popularWebsites = listOf(
        WebsiteDirectoryEntry("GitHub", "https://github.com", "Build, host, and review open-source code", listOf("git", "gh", "repo", "repository", "gist")),
        WebsiteDirectoryEntry("Google", "https://www.google.com", "Web search, news, and cloud services", listOf("goog", "search")),
        WebsiteDirectoryEntry("YouTube", "https://www.youtube.com", "Watch videos, tutorials, and live streams", listOf("yt", "video", "videos", "stream")),
        WebsiteDirectoryEntry("Wikipedia", "https://www.wikipedia.org", "The Free Encyclopedia", listOf("wiki", "encyclopedia")),
        WebsiteDirectoryEntry("Reddit", "https://www.reddit.com", "Communities, discussions, and megathreads", listOf("red", "subreddit", "forum")),
        WebsiteDirectoryEntry("Stack Overflow", "https://stackoverflow.com", "Programming Q&A and debugging solutions", listOf("so", "stack", "overflow", "debug")),
        WebsiteDirectoryEntry("Hacker News", "https://news.ycombinator.com", "Startup, engineering, and deep tech news", listOf("hn", "yc", "ycombinator")),
        WebsiteDirectoryEntry("Android Developers", "https://developer.android.com", "Official Android SDK, Jetpack Compose, and docs", listOf("android", "adb", "jetpack", "compose")),
        WebsiteDirectoryEntry("Kotlin", "https://kotlinlang.org", "Official Kotlin programming language docs", listOf("kt", "kotlin", "coroutines", "kmp")),
        WebsiteDirectoryEntry("MDN Web Docs", "https://developer.mozilla.org", "HTML, CSS, JavaScript, and Web APIs reference", listOf("mdn", "mozilla", "html", "css", "js", "dom")),
        WebsiteDirectoryEntry("DuckDuckGo", "https://duckduckgo.com", "Privacy-first web search engine", listOf("ddg", "duck")),
        WebsiteDirectoryEntry("Bing", "https://www.bing.com", "Microsoft web and visual search engine", listOf("bing")),
        WebsiteDirectoryEntry("Brave Search", "https://search.brave.com", "Independent privacy search engine", listOf("brave")),
        WebsiteDirectoryEntry("ArXiv", "https://arxiv.org", "Open-access scientific preprints and AI papers", listOf("arxiv", "papers", "preprint")),
        WebsiteDirectoryEntry("ChatGPT", "https://chatgpt.com", "Conversational AI assistant", listOf("gpt", "openai", "chat")),
        WebsiteDirectoryEntry("Google Gemini", "https://gemini.google.com", "Google AI assistant and reasoning workspace", listOf("gemini", "bard", "ai")),
        WebsiteDirectoryEntry("Claude", "https://claude.ai", "Anthropic AI assistant", listOf("claude", "anthropic")),
        WebsiteDirectoryEntry("Hugging Face", "https://huggingface.co", "Open-source ML models, datasets, and spaces", listOf("hf", "huggingface", "models")),
        WebsiteDirectoryEntry("Kaggle", "https://www.kaggle.com", "Data science competitions, datasets, and notebooks", listOf("kaggle", "datasets", "notebooks")),
        WebsiteDirectoryEntry("Product Hunt", "https://www.producthunt.com", "Discover the best new tech products daily", listOf("ph", "producthunt", "launch")),
        WebsiteDirectoryEntry("Dev.to", "https://dev.to", "Software engineering community and tutorials", listOf("dev", "devto")),
        WebsiteDirectoryEntry("Medium", "https://medium.com", "Long-form articles on tech, design, and business", listOf("med", "blog")),
        WebsiteDirectoryEntry("Substack", "https://substack.com", "Independent newsletters and analysis", listOf("substack", "newsletter")),
        WebsiteDirectoryEntry("TechCrunch", "https://techcrunch.com", "Startup funding, venture capital, and tech news", listOf("tc", "techcrunch")),
        WebsiteDirectoryEntry("The Verge", "https://www.theverge.com", "Technology news, gadget reviews, and culture", listOf("verge", "reviews")),
        WebsiteDirectoryEntry("Wired", "https://www.wired.com", "Science, cybersecurity, and future computing", listOf("wired")),
        WebsiteDirectoryEntry("Google News", "https://news.google.com", "Global headlines and breaking news", listOf("gnews", "headlines", "news")),
        WebsiteDirectoryEntry("BBC News", "https://www.bbc.com/news", "International breaking news and world reports", listOf("bbc", "world news")),
        WebsiteDirectoryEntry("Figma", "https://www.figma.com", "Collaborative interface design and prototyping", listOf("fig", "figma", "ui", "ux")),
        WebsiteDirectoryEntry("Dribbble", "https://dribbble.com", "Design portfolios and UI inspiration", listOf("dribbble", "design")),
        WebsiteDirectoryEntry("Notion", "https://www.notion.so", "Connected workspace for docs, wikis, and projects", listOf("notion", "notes", "workspace")),
        WebsiteDirectoryEntry("Vercel", "https://vercel.com", "Frontend cloud and Next.js deployment platform", listOf("vercel", "nextjs", "deploy")),
        WebsiteDirectoryEntry("npm", "https://www.npmjs.com", "JavaScript and TypeScript package registry", listOf("npm", "npmjs", "packages")),
        WebsiteDirectoryEntry("Google Scholar", "https://scholar.google.com", "Search academic literature and citations", listOf("scholar", "academic", "citations")),
        WebsiteDirectoryEntry("Google Translate", "https://translate.google.com", "Instant text and website translation", listOf("translate", "translation")),
        WebsiteDirectoryEntry("Google Maps", "https://maps.google.com", "Directions, live traffic, and local places", listOf("maps", "directions", "navigation")),
        WebsiteDirectoryEntry("Apple Account", "https://account.apple.com/sign-in", "Manage Apple ID, security, and devices", listOf("apple", "icloud", "appleid"))
    )

    // Rich search autocomplete corpus for fast prefix and token matching
    private val smartAutocompleteCorpus = listOf(
        "android jetpack compose material 3 guide",
        "android studio keyboard shortcuts and tips",
        "android webview performance optimization",
        "android lifecycle viewmodel stateflow best practices",
        "apple account sign in",
        "apple create new account birthday",
        "ai agent browser automation workflows",
        "arxiv machine learning papers",
        "best open source android apps",
        "best privacy search engines compared",
        "bing search engine",
        "brave privacy search",
        "css grid and flexbox layout cheat sheet",
        "chrome devtools network and console debugging",
        "clean architecture mvvm android kotlin",
        "curl command examples and headers",
        "developer.android.com documentation",
        "docker compose tutorial for beginners",
        "duckduckgo privacy browser",
        "duckduckgo bang commands list",
        "figma ui design system templates",
        "firebase authentication and firestore rules",
        "git rebase vs merge workflow",
        "github trending repositories today",
        "github sign in login",
        "github actions ci cd pipeline",
        "google account sign in",
        "google scholar research papers",
        "google translate english to spanish",
        "gradle version catalog libs.versions.toml",
        "hacker news top stories",
        "how to build a custom android browser",
        "how to center a div in css",
        "how to use kotlin coroutines and flow",
        "http status codes reference guide",
        "javascript dom manipulation guide",
        "javascript async await error handling",
        "json formatter and validator online",
        "jetpack compose lazyverticalgrid example",
        "jetpack compose animation and transitions",
        "kotlin coroutines and stateflow tutorial",
        "kotlin multiplatform mobile guide",
        "kotlin sealed class vs enum",
        "linux command line cheat sheet",
        "linux kernel & systems programming",
        "local weather forecast today",
        "mdn web docs html css reference",
        "medium software architecture articles",
        "nextjs & react 19 server components",
        "npm package search and documentation",
        "openai & gemini api documentation",
        "product hunt top launches today",
        "python data science & machine learning",
        "python virtual environment venv setup",
        "react hooks useeffect and usestate guide",
        "reddit programming communities",
        "regex tester and regular expressions guide",
        "rest api design best practices",
        "rust programming language book",
        "sql join types inner left right full",
        "stackoverflow debugging answers",
        "tailwind css responsive design",
        "techcrunch latest startup news",
        "the verge technology reviews",
        "typescript strict mode best practices",
        "unit testing with junit and robolectric",
        "what is webassembly and how it works",
        "what is oauth 2.0 and oidc",
        "wikipedia free encyclopedia search",
        "world news headlines today",
        "youtube developer conference talks"
    )

    private val commonTlds = setOf(
        "com", "org", "net", "io", "dev", "ai", "app", "co", "edu", "gov",
        "me", "so", "gg", "tv", "xyz", "info", "tech", "cloud", "uk", "de",
        "fr", "jp", "in", "ca", "au", "br", "eu", "us", "nl", "ch", "se"
    )

    val discoveryCategories: List<String> = emptyList()

    /**
     * Kept as an empty stub so callers that reference getPersonalizedRecommendations
     * receive no personalized items.
     */
    fun getPersonalizedRecommendations(
        query: String,
        history: List<HistoryItem>,
        bookmarks: List<BookmarkItem>,
        tabs: List<BrowserTab> = emptyList(),
        selectedCategory: String? = null,
        limit: Int = 6
    ): List<SearchSuggestion> = emptyList()

    /**
     * Generates fast, accurate, non-personalized search suggestions as the user types:
     * 1. Direct URL / Domain navigation if input looks like a URL or domain
     * 2. Exact query search on the active SearchEngine
     * 3. Matching open tabs (`SuggestionType.SWITCH_TO_TAB`)
     * 4. Matching saved bookmarks (`SuggestionType.BOOKMARK`)
     * 5. Matching browsing history (`SuggestionType.HISTORY`)
     * 6. Smart website/domain completions from popular sites (`SuggestionType.DOMAIN`)
     * 7. Ranked search autocomplete phrases (`SuggestionType.SEARCH_AUTOCOMPLETE`)
     */
    fun getSuggestions(
        query: String,
        history: List<HistoryItem>,
        bookmarks: List<BookmarkItem>,
        selectedCategory: String? = null,
        tabs: List<BrowserTab> = emptyList(),
        searchEngine: SearchEngine = SearchEngine.GOOGLE
    ): List<SearchSuggestion> {
        val trimmed = query.trim()
        val results = mutableListOf<SearchSuggestion>()

        // Zero-query state: return user's frequent history and saved bookmarks only (no trending or personalized clutter)
        if (trimmed.isEmpty()) {
            history
                .filter { it.url.isNotBlank() && !it.url.startsWith("aura://") && it.url != "about:blank" }
                .sortedWith(compareByDescending<HistoryItem> { it.visitCount }.thenByDescending { it.timestamp })
                .take(5)
                .forEach { h ->
                    results.add(
                        SearchSuggestion(
                            title = h.title.ifBlank { h.url },
                            query = h.url,
                            type = SuggestionType.HISTORY,
                            subtitle = "${extractCleanHost(h.url)} • ${h.visitCount} ${if (h.visitCount == 1) "visit" else "visits"}",
                            targetUrl = h.url,
                            relevanceScore = 100 + h.visitCount.coerceAtMost(20),
                            reasonBadge = "Visited ${h.visitCount}x"
                        )
                    )
                }

            bookmarks
                .sortedByDescending { it.isPinned }
                .take(3)
                .forEach { b ->
                    results.add(
                        SearchSuggestion(
                            title = b.title,
                            query = b.url,
                            type = SuggestionType.BOOKMARK,
                            subtitle = "${extractCleanHost(b.url)} • ${b.folder}",
                            targetUrl = b.url,
                            relevanceScore = if (b.isPinned) 95 else 85,
                            reasonBadge = "Bookmark"
                        )
                    )
                }

            return results.distinctBy { it.targetUrl ?: it.query }
        }

        val lowerQuery = trimmed.lowercase(Locale.ROOT)
        val queryTokens = lowerQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        // 1. Direct URL / Domain Detection
        val isExplicitUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
        val looksLikeDomain = isLikelyDomainInput(trimmed)

        if (isExplicitUrl || looksLikeDomain) {
            val fullUrl = if (isExplicitUrl) trimmed else "https://$trimmed"
            results.add(
                SearchSuggestion(
                    title = trimmed,
                    query = trimmed,
                    type = SuggestionType.DOMAIN,
                    subtitle = "Open $fullUrl directly",
                    targetUrl = fullUrl,
                    relevanceScore = 300,
                    reasonBadge = "Direct URL"
                )
            )
        }

        // 2. Primary Search Action on Active Search Engine
        results.add(
            SearchSuggestion(
                title = trimmed,
                query = trimmed,
                type = SuggestionType.SEARCH_AUTOCOMPLETE,
                subtitle = "Search ${searchEngine.displayName} for \"$trimmed\"",
                relevanceScore = if (isExplicitUrl || looksLikeDomain) 240 else 280,
                reasonBadge = searchEngine.displayName
            )
        )

        // 3. Switch to Open Tab matches
        tabs.asSequence()
            .filter { tab ->
                !tab.url.startsWith("aura://") &&
                        tab.url != "about:blank" &&
                        matchesAllTokens(tab.title, tab.url, lowerQuery, queryTokens)
            }
            .map { tab ->
                val host = extractCleanHost(tab.url)
                val titleLower = tab.title.lowercase(Locale.ROOT)
                val prefixBonus = if (titleLower.startsWith(lowerQuery) || host.startsWith(lowerQuery)) 30 else 0
                SearchSuggestion(
                    title = tab.title.ifBlank { host.ifBlank { tab.url } },
                    query = tab.url,
                    type = SuggestionType.SWITCH_TO_TAB,
                    subtitle = "Switch to open tab • ${host.ifBlank { tab.url }}",
                    targetUrl = tab.url,
                    relevanceScore = 230 + prefixBonus,
                    reasonBadge = "Open Tab",
                    tabId = tab.id
                )
            }
            .sortedByDescending { it.relevanceScore }
            .take(2)
            .forEach { results.add(it) }

        // 4. Match User's Saved Bookmarks
        bookmarks.asSequence()
            .filter { b -> matchesAllTokens(b.title, b.url, lowerQuery, queryTokens) }
            .map { b ->
                val host = extractCleanHost(b.url)
                val titleLower = b.title.lowercase(Locale.ROOT)
                var score = 195
                if (titleLower.startsWith(lowerQuery) || host.startsWith(lowerQuery)) score += 25
                if (b.isPinned) score += 10
                SearchSuggestion(
                    title = b.title,
                    query = b.url,
                    type = SuggestionType.BOOKMARK,
                    subtitle = "${host.ifBlank { b.url }} • Saved in ${b.folder}",
                    targetUrl = b.url,
                    relevanceScore = score,
                    reasonBadge = "Bookmark"
                )
            }
            .sortedByDescending { it.relevanceScore }
            .take(3)
            .forEach { results.add(it) }

        // 5. Match User's Browsing History (ranked by prefix match, visit frequency & recency)
        history.asSequence()
            .filter { h ->
                h.url.isNotBlank() &&
                        !h.url.startsWith("aura://") &&
                        h.url != "about:blank" &&
                        matchesAllTokens(h.title, h.url, lowerQuery, queryTokens)
            }
            .sortedWith(compareByDescending<HistoryItem> { it.visitCount }.thenByDescending { it.timestamp })
            .take(4)
            .forEach { h ->
                val host = extractCleanHost(h.url)
                val titleLower = h.title.lowercase(Locale.ROOT)
                var score = 175 + h.visitCount.coerceAtMost(15)
                if (titleLower.startsWith(lowerQuery) || host.startsWith(lowerQuery)) score += 20
                results.add(
                    SearchSuggestion(
                        title = h.title.ifBlank { h.url },
                        query = h.url,
                        type = SuggestionType.HISTORY,
                        subtitle = "${host.ifBlank { h.url }} • Visited ${h.visitCount}x",
                        targetUrl = h.url,
                        relevanceScore = score,
                        reasonBadge = "History"
                    )
                )
            }

        // 6. Smart Website & Domain Completions (prefix, host, and alias matching)
        popularWebsites.asSequence()
            .mapNotNull { site ->
                val titleLower = site.title.lowercase(Locale.ROOT)
                val hostLower = extractCleanHost(site.url)
                val descLower = site.description.lowercase(Locale.ROOT)

                val exactAlias = site.aliases.any { it.equals(lowerQuery, ignoreCase = true) }
                val prefixTitleOrHost = titleLower.startsWith(lowerQuery) || hostLower.startsWith(lowerQuery)
                val prefixAlias = site.aliases.any { it.startsWith(lowerQuery) && lowerQuery.length >= 2 }
                val containsTitleOrHost = lowerQuery.length >= 2 && (titleLower.contains(lowerQuery) || hostLower.contains(lowerQuery))
                val multiTokenMatch = queryTokens.size > 1 && queryTokens.all { tok ->
                    titleLower.contains(tok) || hostLower.contains(tok) || descLower.contains(tok) || site.aliases.any { it.contains(tok) }
                }

                val score = when {
                    exactAlias || titleLower == lowerQuery || hostLower == lowerQuery -> 215
                    prefixTitleOrHost -> 205
                    prefixAlias -> 190
                    containsTitleOrHost -> 165
                    multiTokenMatch -> 155
                    else -> 0
                }

                if (score > 0 && results.none { it.targetUrl == site.url && it.type == SuggestionType.DOMAIN }) {
                    SearchSuggestion(
                        title = site.title,
                        query = site.url,
                        type = SuggestionType.DOMAIN,
                        subtitle = "$hostLower • ${site.description}",
                        targetUrl = site.url,
                        relevanceScore = score,
                        reasonBadge = hostLower
                    )
                } else {
                    null
                }
            }
            .sortedByDescending { it.relevanceScore }
            .take(3)
            .forEach { results.add(it) }

        // 7. Ranked Search Autocomplete Phrases
        val autocompleteCandidates = mutableListOf<Pair<String, Int>>()

        smartAutocompleteCorpus.forEach { candidate ->
            val candLower = candidate.lowercase(Locale.ROOT)
            if (candLower != lowerQuery) {
                val candWords = candLower.split(' ')
                val score = when {
                    candLower.startsWith(lowerQuery) -> 150
                    candWords.any { it.startsWith(lowerQuery) } -> 135
                    queryTokens.size > 1 && queryTokens.all { tok -> candLower.contains(tok) } -> 125
                    candLower.contains(lowerQuery) && lowerQuery.length >= 2 -> 115
                    else -> 0
                }
                if (score > 0) {
                    autocompleteCandidates.add(candidate to score)
                }
            }
        }

        // Dynamic contextual query expansions so any typed query gets helpful search completions
        if (!isExplicitUrl && !looksLikeDomain) {
            val dynamicCompletions = buildContextualSearchCompletions(trimmed, lowerQuery, queryTokens)
            dynamicCompletions.forEachIndexed { idx, completion ->
                if (autocompleteCandidates.none { it.first.equals(completion, ignoreCase = true) }) {
                    autocompleteCandidates.add(completion to (110 - idx * 3))
                }
            }
        }

        autocompleteCandidates
            .sortedByDescending { it.second }
            .distinctBy { it.first.lowercase(Locale.ROOT) }
            .take(5)
            .forEach { (completion, score) ->
                results.add(
                    SearchSuggestion(
                        title = completion,
                        query = completion,
                        type = SuggestionType.SEARCH_AUTOCOMPLETE,
                        subtitle = "Search ${searchEngine.displayName}",
                        relevanceScore = score,
                        reasonBadge = searchEngine.displayName
                    )
                )
            }

        return results
            .sortedByDescending { it.relevanceScore }
            .distinctBy { "${it.type}_${it.title.lowercase(Locale.ROOT)}_${it.targetUrl ?: it.query.lowercase(Locale.ROOT)}" }
    }

    private fun matchesAllTokens(
        title: String,
        url: String,
        lowerQuery: String,
        queryTokens: List<String>
    ): Boolean {
        val titleLower = title.lowercase(Locale.ROOT)
        val urlLower = url.lowercase(Locale.ROOT)
        if (titleLower.contains(lowerQuery) || urlLower.contains(lowerQuery)) return true
        if (queryTokens.size > 1) {
            val combined = "$titleLower $urlLower"
            return queryTokens.all { tok -> combined.contains(tok) }
        }
        return false
    }

    private fun isLikelyDomainInput(input: String): Boolean {
        if (input.contains(" ") || !input.contains(".")) return false
        val clean = input.lowercase(Locale.ROOT)
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
        val hostPart = clean.substringBefore("/").substringBefore("?").substringBefore(":")
        if (hostPart.length < 3 || !hostPart.contains(".")) return false
        val tld = hostPart.substringAfterLast(".")
        return tld.length in 2..12 && (tld in commonTlds || tld.all { it.isLetter() })
    }

    private fun buildContextualSearchCompletions(
        trimmed: String,
        lowerQuery: String,
        queryTokens: List<String>
    ): List<String> {
        val firstWord = queryTokens.firstOrNull() ?: return emptyList()
        return when {
            firstWord in setOf("how", "what", "why", "when", "where", "who", "can", "should") -> listOf(
                "$trimmed work",
                "$trimmed step by step",
                "$trimmed examples",
                "$trimmed explained"
            )
            firstWord in setOf("best", "top", "compare") -> listOf(
                "$trimmed 2026",
                "$trimmed review and comparison",
                "$trimmed alternatives",
                "$trimmed reddit"
            )
            queryTokens.size == 1 && lowerQuery.length >= 2 -> listOf(
                "$trimmed documentation",
                "$trimmed tutorial",
                "$trimmed github",
                "$trimmed latest news",
                "$trimmed alternatives"
            )
            else -> listOf(
                "$trimmed guide",
                "$trimmed tutorial & examples",
                "$trimmed latest updates",
                "$trimmed discussion"
            )
        }
    }

    /**
     * Resolves the navigation destination URL based on the input text and chosen search engine.
     */
    fun resolveUrl(input: String, searchEngine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return searchEngine.homeUrl

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("aura://", ignoreCase = true)
        ) {
            return trimmed
        }

        // Domain heuristic: contains dot, no spaces, valid top-level domain or path
        if (!trimmed.contains(" ") && trimmed.contains(".") && trimmed.length > 3) {
            return "https://$trimmed"
        }

        // Otherwise execute search on the active search engine
        val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
        return "${searchEngine.searchUrlPrefix}$encodedQuery"
    }

    private fun extractCleanHost(url: String): String {
        return try {
            URI(url).host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            url.lowercase(Locale.ROOT)
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore("/")
                .substringBefore("?")
        }
    }
}
