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

data class PersonalizedContentItem(
    val title: String,
    val subtitle: String,
    val targetUrl: String,
    val category: String,
    val keywords: List<String>
)

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
        Pair("Apple Account", "https://account.apple.com/sign-in"),
        Pair("Google", "https://www.google.com"),
        Pair("DuckDuckGo", "https://duckduckgo.com"),
        Pair("Bing", "https://www.bing.com"),
        Pair("Brave Search", "https://search.brave.com"),
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
        Pair("Figma", "https://www.figma.com"),
        Pair("Kotlin", "https://kotlinlang.org")
    )

    // Curated discovery categories for personalized content filtering
    val discoveryCategories = listOf(
        "Technology",
        "AI & Dev",
        "Science",
        "Productivity",
        "Design",
        "Global News"
    )

    // Rich catalog of personalized content recommendations surfaced as the user types
    private val contentCatalog = listOf(
        // AI & Dev
        PersonalizedContentItem(
            title = "Jetpack Compose Material 3 Expressive UI Guide",
            subtitle = "developer.android.com • Official Android architecture & motion patterns",
            targetUrl = "https://developer.android.com/jetpack/compose",
            category = "AI & Dev",
            keywords = listOf("android", "jetpack", "compose", "material", "ui", "kotlin", "dev", "google", "app")
        ),
        PersonalizedContentItem(
            title = "Kotlin Coroutines, StateFlow & Multiplatform Architecture",
            subtitle = "kotlinlang.org • Asynchronous reactive programming & KMP guides",
            targetUrl = "https://kotlinlang.org/docs/coroutines-overview.html",
            category = "AI & Dev",
            keywords = listOf("kotlin", "coroutines", "flow", "stateflow", "multiplatform", "kmp", "code", "dev", "java")
        ),
        PersonalizedContentItem(
            title = "GitHub Trending Open-Source AI Agents & Repositories",
            subtitle = "github.com • Explore top developer tools, frameworks, and LLM projects",
            targetUrl = "https://github.com/trending",
            category = "AI & Dev",
            keywords = listOf("git", "github", "open source", "repo", "code", "ai", "agent", "developer", "project")
        ),
        PersonalizedContentItem(
            title = "Stack Overflow Top Engineering Solutions & Architecture Q&A",
            subtitle = "stackoverflow.com • Community debugging & system design answers",
            targetUrl = "https://stackoverflow.com/questions",
            category = "AI & Dev",
            keywords = listOf("stack", "stackoverflow", "debug", "error", "code", "programming", "python", "javascript", "rust")
        ),
        PersonalizedContentItem(
            title = "MDN Web Docs: Modern HTML5, CSS Grid & Web APIs Reference",
            subtitle = "developer.mozilla.org • Complete browser DOM & JavaScript specification",
            targetUrl = "https://developer.mozilla.org/en-US/docs/Web",
            category = "AI & Dev",
            keywords = listOf("mdn", "web", "html", "css", "javascript", "dom", "api", "browser", "frontend")
        ),
        PersonalizedContentItem(
            title = "Dev.to Community: Full-Stack Engineering & Cloud Tutorials",
            subtitle = "dev.to • Practical software engineering articles and deep dives",
            targetUrl = "https://dev.to",
            category = "AI & Dev",
            keywords = listOf("dev", "tutorial", "backend", "cloud", "docker", "react", "typescript", "coding")
        ),

        // Technology
        PersonalizedContentItem(
            title = "Hacker News: Top Startup, Systems & Deep Tech Discussions",
            subtitle = "news.ycombinator.com • Real-time engineering & founder insights",
            targetUrl = "https://news.ycombinator.com",
            category = "Technology",
            keywords = listOf("hacker", "hn", "yc", "startup", "tech", "news", "systems", "rust", "linux")
        ),
        PersonalizedContentItem(
            title = "Apple Account & iCloud Security Portal (Apple ID Sign-In)",
            subtitle = "account.apple.com • Manage Apple ID, passkeys, devices, and iCloud",
            targetUrl = "https://account.apple.com/sign-in",
            category = "Technology",
            keywords = listOf("apple", "icloud", "iphone", "mac", "ios", "sign in", "login", "account", "appleid")
        ),
        PersonalizedContentItem(
            title = "The Verge: Consumer Tech, AI Hardware & Flagship Reviews",
            subtitle = "theverge.com • In-depth reviews, mobile ecosystems, and future tech",
            targetUrl = "https://www.theverge.com",
            category = "Technology",
            keywords = listOf("verge", "tech", "review", "phone", "laptop", "hardware", "gadget", "mobile")
        ),
        PersonalizedContentItem(
            title = "TechCrunch: Venture Capital, AI Startups & Product Launches",
            subtitle = "techcrunch.com • Breaking startup funding and technology industry news",
            targetUrl = "https://techcrunch.com",
            category = "Technology",
            keywords = listOf("techcrunch", "startup", "ai", "funding", "founder", "product", "saas", "business")
        ),
        PersonalizedContentItem(
            title = "Product Hunt: Best New Apps, AI Workflows & Developer Tools",
            subtitle = "producthunt.com • Daily leaderboard of newly launched digital products",
            targetUrl = "https://www.producthunt.com",
            category = "Technology",
            keywords = listOf("product", "hunt", "launch", "tool", "app", "startup", "workflow", "new")
        ),
        PersonalizedContentItem(
            title = "Wired: Future Computing, Cybersecurity & Digital Culture",
            subtitle = "wired.com • Long-form journalism on science, security, and society",
            targetUrl = "https://www.wired.com",
            category = "Technology",
            keywords = listOf("wired", "security", "cyber", "privacy", "quantum", "future", "culture")
        ),

        // Science
        PersonalizedContentItem(
            title = "ArXiv Preprints: Machine Learning, Physics & Quantitative Research",
            subtitle = "arxiv.org • Open-access scientific papers and latest AI breakthroughs",
            targetUrl = "https://arxiv.org",
            category = "Science",
            keywords = listOf("arxiv", "paper", "research", "science", "physics", "math", "llm", "machine learning", "ai")
        ),
        PersonalizedContentItem(
            title = "Wikipedia Free Encyclopedia: Featured Knowledge & Deep Dives",
            subtitle = "en.wikipedia.org • Comprehensive articles, history, and scientific references",
            targetUrl = "https://en.wikipedia.org/wiki/Main_Page",
            category = "Science",
            keywords = listOf("wiki", "wikipedia", "encyclopedia", "history", "science", "space", "biology", "earth", "learn")
        ),
        PersonalizedContentItem(
            title = "Kaggle Data Science Competitions, Datasets & ML Notebooks",
            subtitle = "kaggle.com • Real-world machine learning benchmarks and open datasets",
            targetUrl = "https://www.kaggle.com",
            category = "Science",
            keywords = listOf("kaggle", "data", "dataset", "notebook", "python", "ml", "model", "analytics", "science")
        ),

        // Productivity
        PersonalizedContentItem(
            title = "Medium: Deep Work, Engineering Leadership & Personal Growth",
            subtitle = "medium.com • Curated essays on productivity, software, and strategy",
            targetUrl = "https://medium.com",
            category = "Productivity",
            keywords = listOf("medium", "article", "blog", "productivity", "writing", "career", "management", "focus")
        ),
        PersonalizedContentItem(
            title = "Substack: Independent Newsletters on Tech, Finance & Culture",
            subtitle = "substack.com • Direct long-form analysis from domain experts",
            targetUrl = "https://substack.com",
            category = "Productivity",
            keywords = listOf("substack", "newsletter", "reading", "finance", "economics", "essay", "notes")
        ),
        PersonalizedContentItem(
            title = "YouTube Learning: Tech Talks, Keynotes & Coding Walkthroughs",
            subtitle = "youtube.com • Video tutorials, conference talks, and creator channels",
            targetUrl = "https://www.youtube.com",
            category = "Productivity",
            keywords = listOf("youtube", "video", "watch", "tutorial", "course", "lecture", "music", "stream")
        ),

        // Design
        PersonalizedContentItem(
            title = "Figma Design Systems, Auto-Layout & Interactive Prototyping",
            subtitle = "figma.com • Collaborative interface design and component libraries",
            targetUrl = "https://www.figma.com",
            category = "Design",
            keywords = listOf("figma", "design", "ui", "ux", "prototype", "wireframe", "icon", "layout", "typography")
        ),
        PersonalizedContentItem(
            title = "Dribbble: Mobile UI Inspiration, Motion Graphics & Brand Craft",
            subtitle = "dribbble.com • Showcase of visual design, app interfaces, and 3D art",
            targetUrl = "https://dribbble.com",
            category = "Design",
            keywords = listOf("dribbble", "inspiration", "visual", "color", "graphic", "mobile", "portfolio", "art")
        ),

        // Global News
        PersonalizedContentItem(
            title = "Google News: Personalized World, Technology & Science Briefing",
            subtitle = "news.google.com • Multi-source global headlines and topic tracking",
            targetUrl = "https://news.google.com",
            category = "Global News",
            keywords = listOf("news", "world", "global", "breaking", "headline", "today", "market", "economy")
        ),
        PersonalizedContentItem(
            title = "Reddit Communities: Live Discussions, Tech & Topic Megathreads",
            subtitle = "reddit.com • Front page of community forums, AMAs, and real-time threads",
            targetUrl = "https://www.reddit.com",
            category = "Global News",
            keywords = listOf("reddit", "forum", "community", "discussion", "thread", "ask", "review")
        )
    )

    // Smart query completions indexed by common typing prefixes
    private val smartAutocompleteCorpus = listOf(
        "android jetpack compose material 3",
        "android studio emulator tips",
        "apple account sign in",
        "apple create new account birthday",
        "ai agent browser automation workflows",
        "arxiv machine learning papers",
        "bing search engine",
        "brave privacy search",
        "chatgpt & gemini ai comparison",
        "css grid and flexbox layout guide",
        "developer.android.com documentation",
        "duckduckgo privacy browser",
        "figma ui design system templates",
        "github trending repositories",
        "github sign in login",
        "google account sign in",
        "google scholar research papers",
        "hacker news top stories",
        "javascript dom manipulation guide",
        "kotlin coroutines and stateflow tutorial",
        "kotlin multiplatform mobile guide",
        "linux kernel & systems programming",
        "mdn web docs html css reference",
        "medium software architecture articles",
        "nextjs & react 19 server components",
        "openai & gemini api documentation",
        "product hunt top launches today",
        "python data science & machine learning",
        "reddit programming communities",
        "rust programming language book",
        "stackoverflow debugging answers",
        "tailwind css responsive design",
        "techcrunch latest startup news",
        "the verge technology reviews",
        "typescript strict mode best practices",
        "wikipedia free encyclopedia search",
        "youtube developer conference talks"
    )

    /**
     * Infers the user's preferred categories and domains based on their real browsing history,
     * saved bookmarks, and currently open tabs.
     */
    fun inferCategoryAffinities(
        history: List<HistoryItem>,
        bookmarks: List<BookmarkItem>,
        tabs: List<BrowserTab> = emptyList()
    ): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        discoveryCategories.forEach { scores[it] = 1 }

        fun scoreTextAndUrl(title: String, url: String, weight: Int) {
            val combined = "${title.lowercase(Locale.ROOT)} ${url.lowercase(Locale.ROOT)}"
            contentCatalog.forEach { item ->
                val domain = extractCleanHost(item.targetUrl)
                val matchesDomain = domain.isNotBlank() && combined.contains(domain)
                val keywordHits = item.keywords.count { kw -> combined.contains(kw) }
                if (matchesDomain) {
                    scores[item.category] = (scores[item.category] ?: 1) + (weight * 4)
                } else if (keywordHits > 0) {
                    scores[item.category] = (scores[item.category] ?: 1) + (weight * keywordHits)
                }
            }
        }

        history.forEach { h ->
            val visitWeight = h.visitCount.coerceIn(1, 10)
            scoreTextAndUrl(h.title, h.url, visitWeight * 2)
        }
        bookmarks.forEach { b ->
            val pinWeight = if (b.isPinned) 5 else 3
            scoreTextAndUrl(b.title, b.url, pinWeight)
        }
        tabs.forEach { t ->
            if (!t.url.startsWith("aura://") && t.url != "about:blank") {
                scoreTextAndUrl(t.title, t.url, 2)
            }
        }

        return scores
    }

    /**
     * Returns personalized content recommendations for the zero-query state or when a category
     * chip is selected in the address bar.
     */
    fun getPersonalizedRecommendations(
        query: String,
        history: List<HistoryItem>,
        bookmarks: List<BookmarkItem>,
        tabs: List<BrowserTab> = emptyList(),
        selectedCategory: String? = null,
        limit: Int = 6
    ): List<SearchSuggestion> {
        val trimmed = query.trim()
        val lowerQuery = trimmed.lowercase(Locale.ROOT)
        val affinities = inferCategoryAffinities(history, bookmarks, tabs)
        val topCategory = affinities.maxByOrNull { it.value }?.key ?: "AI & Dev"
        val hasUserSignals = history.isNotEmpty() || bookmarks.isNotEmpty()

        val visitedHosts = history.map { extractCleanHost(it.url) }.filter { it.isNotBlank() }.toSet()
        val bookmarkedHosts = bookmarks.map { extractCleanHost(it.url) }.filter { it.isNotBlank() }.toSet()

        val scoredItems = contentCatalog.mapNotNull { item ->
            if (selectedCategory != null && !item.category.equals(selectedCategory, ignoreCase = true)) {
                return@mapNotNull null
            }

            val itemHost = extractCleanHost(item.targetUrl)
            val titleLower = item.title.lowercase(Locale.ROOT)
            val subLower = item.subtitle.lowercase(Locale.ROOT)
            val catLower = item.category.lowercase(Locale.ROOT)

            var score = (affinities[item.category] ?: 1) * 3

            if (lowerQuery.isNotEmpty()) {
                val queryTokens = lowerQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
                var matchedAny = false

                if (titleLower.startsWith(lowerQuery) || itemHost.startsWith(lowerQuery)) {
                    score += 120
                    matchedAny = true
                } else if (titleLower.contains(lowerQuery) || itemHost.contains(lowerQuery)) {
                    score += 85
                    matchedAny = true
                }

                if (subLower.contains(lowerQuery) || catLower.contains(lowerQuery)) {
                    score += 45
                    matchedAny = true
                }

                val kwExactMatches = item.keywords.count { kw ->
                    kw.startsWith(lowerQuery) || lowerQuery.startsWith(kw) || kw.contains(lowerQuery)
                }
                if (kwExactMatches > 0) {
                    score += kwExactMatches * 40
                    matchedAny = true
                }

                val tokenMatches = queryTokens.count { tok ->
                    tok.length >= 2 && (titleLower.contains(tok) || subLower.contains(tok) || item.keywords.any { it.contains(tok) })
                }
                if (tokenMatches > 0) {
                    score += tokenMatches * 25
                    matchedAny = true
                }

                if (!matchedAny) return@mapNotNull null
            }

            val isVisitedDomain = visitedHosts.any { it.contains(itemHost) || itemHost.contains(it) }
            val isBookmarkedDomain = bookmarkedHosts.any { it.contains(itemHost) || itemHost.contains(it) }

            if (isVisitedDomain) score += 35
            if (isBookmarkedDomain) score += 30

            val badge = when {
                isBookmarkedDomain -> "From Bookmarked Site • ${item.category}"
                isVisitedDomain -> "Based on Your Visits • ${item.category}"
                hasUserSignals && item.category == topCategory -> "For You • ${item.category}"
                else -> "Recommended • ${item.category}"
            }

            SearchSuggestion(
                title = item.title,
                query = item.title,
                type = SuggestionType.PERSONALIZED,
                subtitle = item.subtitle,
                targetUrl = item.targetUrl,
                category = item.category,
                relevanceScore = score,
                reasonBadge = badge
            )
        }.sortedByDescending { it.relevanceScore }
            .take(limit)
            .toMutableList()

        // If the user typed a specific topic that has fewer than 2 static catalog hits,
        // dynamically synthesize personalized content recommendations tailored to their query & profile!
        if (lowerQuery.length >= 2 && scoredItems.size < 2 && !lowerQuery.startsWith("http")) {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val activeCategory = selectedCategory ?: topCategory
            val wikiSlug = URLEncoder.encode(trimmed.replaceFirstChar { it.uppercase() }.replace(' ', '_'), "UTF-8")

            scoredItems.add(
                SearchSuggestion(
                    title = "${trimmed.replaceFirstChar { it.uppercase() }} — Deep Dive & Reference Guide",
                    query = trimmed,
                    type = SuggestionType.PERSONALIZED,
                    subtitle = "en.wikipedia.org • Comprehensive overview & curated references",
                    targetUrl = "https://en.wikipedia.org/wiki/$wikiSlug",
                    category = activeCategory,
                    relevanceScore = 70,
                    reasonBadge = "For You • $activeCategory"
                )
            )
            scoredItems.add(
                SearchSuggestion(
                    title = "$trimmed — Open-Source Projects & Code Examples",
                    query = "$trimmed github",
                    type = SuggestionType.PERSONALIZED,
                    subtitle = "github.com • Explore repositories, guides, and discussions",
                    targetUrl = "https://github.com/search?q=$encoded",
                    category = "AI & Dev",
                    relevanceScore = 65,
                    reasonBadge = "Recommended • AI & Dev"
                )
            )
        }

        return scoredItems.take(limit)
    }

    /**
     * Generate search suggestions and personalized content recommendations as the user
     * begins typing in the address bar.
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

        // 1. If query is empty and no category filter is selected, return user's frequent history & bookmarks
        // (and when a category chip IS selected, return personalized recommendations for that category)
        if (trimmed.isEmpty()) {
            if (selectedCategory != null) {
                results.addAll(
                    getPersonalizedRecommendations(
                        query = "",
                        history = history,
                        bookmarks = bookmarks,
                        tabs = tabs,
                        selectedCategory = selectedCategory,
                        limit = 6
                    )
                )
            }

            // Frequent history items
            history.sortedByDescending { it.visitCount }
                .take(5)
                .forEach { h ->
                    results.add(
                        SearchSuggestion(
                            title = h.title.ifBlank { h.url },
                            query = h.url,
                            type = SuggestionType.HISTORY,
                            subtitle = "History • ${h.visitCount} ${if (h.visitCount == 1) "visit" else "visits"}",
                            targetUrl = h.url,
                            relevanceScore = 90 + h.visitCount,
                            reasonBadge = "Visited ${h.visitCount}x"
                        )
                    )
                }

            // Saved bookmarks
            bookmarks.take(3).forEach { b ->
                results.add(
                    SearchSuggestion(
                        title = b.title,
                        query = b.url,
                        type = SuggestionType.HISTORY,
                        subtitle = "Bookmark • ${b.folder}",
                        targetUrl = b.url,
                        relevanceScore = if (b.isPinned) 95 else 85,
                        reasonBadge = "Bookmark • ${b.folder}"
                    )
                )
            }
            return results.distinctBy { it.targetUrl ?: it.query }
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
                    subtitle = "Open website directly ($fullUrl)",
                    targetUrl = fullUrl,
                    relevanceScore = 200,
                    reasonBadge = "Direct URL"
                )
            )
        }

        // 3. Switch to Open Tab matches (if an already-open tab matches the typed query)
        tabs.filter { tab ->
            !tab.url.startsWith("aura://") &&
                    tab.url != "about:blank" &&
                    (tab.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                            tab.url.lowercase(Locale.ROOT).contains(lowerQuery))
        }.take(2).forEach { tab ->
            results.add(
                SearchSuggestion(
                    title = tab.title.ifBlank { tab.url },
                    query = tab.url,
                    type = SuggestionType.SWITCH_TO_TAB,
                    subtitle = "Switch to open tab • ${extractCleanHost(tab.url)}",
                    targetUrl = tab.url,
                    relevanceScore = 180,
                    reasonBadge = "Open Tab",
                    tabId = tab.id
                )
            )
        }

        // 4. Match user's bookmarks
        bookmarks.filter {
            it.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    it.url.lowercase(Locale.ROOT).contains(lowerQuery)
        }.take(3).forEach { b ->
            results.add(
                SearchSuggestion(
                    title = b.title,
                    query = b.url,
                    type = SuggestionType.BOOKMARK,
                    subtitle = "Bookmark • ${b.folder} • ${extractCleanHost(b.url)}",
                    targetUrl = b.url,
                    relevanceScore = 160,
                    reasonBadge = "Saved in ${b.folder}"
                )
            )
        }

        // 5. Match user's browsing history (ranked by visit count & recency)
        history.filter {
            it.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    it.url.lowercase(Locale.ROOT).contains(lowerQuery)
        }.sortedWith(compareByDescending<HistoryItem> { it.visitCount }.thenByDescending { it.timestamp })
            .take(4)
            .forEach { h ->
                results.add(
                    SearchSuggestion(
                        title = h.title.ifBlank { h.url },
                        query = h.url,
                        type = SuggestionType.HISTORY,
                        subtitle = "From your history • ${h.visitCount} ${if (h.visitCount == 1) "visit" else "visits"}",
                        targetUrl = h.url,
                        relevanceScore = 140 + h.visitCount.coerceAtMost(15),
                        reasonBadge = "Visited ${h.visitCount}x"
                    )
                )
            }

        // 6. Personalized Content Recommendations as the user types in the address bar
        val personalizedMatches = getPersonalizedRecommendations(
            query = trimmed,
            history = history,
            bookmarks = bookmarks,
            tabs = tabs,
            selectedCategory = selectedCategory,
            limit = 4
        )
        personalizedMatches.forEach { rec ->
            if (results.none { it.targetUrl == rec.targetUrl && it.title == rec.title }) {
                results.add(rec)
            }
        }

        // 7. Match popular websites (Smart Domain Auto-Complete)
        popularWebsites.filter { (name, url) ->
            name.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    url.lowercase(Locale.ROOT).contains(lowerQuery)
        }.take(3).forEach { (name, url) ->
            if (results.none { it.targetUrl == url && it.type == SuggestionType.DOMAIN }) {
                results.add(
                    SearchSuggestion(
                        title = name,
                        query = url,
                        type = SuggestionType.DOMAIN,
                        subtitle = "${url.removePrefix("https://").removePrefix("www.")} • Official Site",
                        targetUrl = url,
                        relevanceScore = 110,
                        reasonBadge = "Website"
                    )
                )
            }
        }

        // 8. Direct search query on active search engine
        results.add(
            SearchSuggestion(
                title = trimmed,
                query = trimmed,
                type = SuggestionType.SEARCH_AUTOCOMPLETE,
                subtitle = "Search ${searchEngine.displayName} for \"$trimmed\"",
                relevanceScore = 100,
                reasonBadge = searchEngine.displayName
            )
        )

        // 9. Live Search Autocomplete Query Completions
        val autocompleteMatches = smartAutocompleteCorpus
            .filter { candidate ->
                candidate.contains(lowerQuery) && !candidate.equals(lowerQuery, ignoreCase = true)
            }
            .sortedByDescending { it.startsWith(lowerQuery) }
            .take(3)
            .toMutableList()

        if (autocompleteMatches.size < 2 && !isExplicitUrl && !looksLikeDomain) {
            autocompleteMatches.add("$trimmed tutorial & guide")
            autocompleteMatches.add("$trimmed latest news & updates")
        }

        autocompleteMatches.distinct().take(3).forEach { completion ->
            results.add(
                SearchSuggestion(
                    title = completion,
                    query = completion,
                    type = SuggestionType.SEARCH_AUTOCOMPLETE,
                    subtitle = "Search ${searchEngine.displayName}",
                    relevanceScore = 80,
                    reasonBadge = "Autocomplete"
                )
            )
        }

        return results.distinctBy { "${it.type}_${it.title}_${it.targetUrl ?: it.query}" }
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
