package com.example.devtools

import android.os.Build
import android.text.Html
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray

data class LoadedWebPageResult(
    val finalUrl: String,
    val title: String,
    val rawHtml: String,
    val domNodes: List<LiveDomNode>,
    val mainNetworkEntry: NetworkLogEntry,
    val subresources: List<Pair<String, NetworkResourceType>>
)

object RealHtmlPageLoader {

    fun loadWebPage(
        targetUrl: String,
        isDesktopMode: Boolean
    ): Result<LoadedWebPageResult> {
        val startMs = System.currentTimeMillis()
        val ua = if (isDesktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        } else {
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        val requestHeaders = mapOf(
            "User-Agent" to ua,
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.9",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none"
        )

        return try {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 9000
                readTimeout = 9000
                instanceFollowRedirects = true
                requestHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            val statusCode = conn.responseCode
            val statusMessage = conn.responseMessage ?: "OK"
            val finalUrl = conn.url?.toString() ?: targetUrl
            val mimeType = conn.contentType ?: "text/html; charset=utf-8"

            val responseHeaders = mutableMapOf<String, String>()
            conn.headerFields?.forEach { (k, v) ->
                if (k != null && !v.isNullOrEmpty()) {
                    responseHeaders[k] = v.joinToString(", ")
                }
            }

            val stream = if (statusCode in 200..399) {
                conn.inputStream
            } else {
                conn.errorStream ?: conn.inputStream
            }

            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val sb = StringBuilder()
            var line: String?
            var charsRead = 0
            while (reader.readLine().also { line = it } != null && charsRead < 320_000) {
                sb.append(line).append('\n')
                charsRead += (line?.length ?: 0) + 1
            }
            reader.close()
            conn.disconnect()

            val rawHtml = sb.toString()
            val durationMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)

            val title = extractTitle(rawHtml, finalUrl)
            val subresources = extractSubresources(rawHtml, finalUrl)
            val domNodes = parseHtmlIntoDomNodes(rawHtml, finalUrl, title)

            val mainNetworkEntry = NetworkLogEntry(
                url = finalUrl,
                method = "GET",
                resourceType = NetworkResourceType.DOCUMENT,
                statusCode = statusCode,
                statusText = statusMessage,
                mimeType = mimeType,
                durationMs = durationMs,
                transferSizeBytes = rawHtml.toByteArray(Charsets.UTF_8).size.toLong(),
                requestHeaders = requestHeaders,
                responseHeaders = responseHeaders,
                responsePreview = rawHtml.take(5000),
                timestamp = System.currentTimeMillis(),
                isMainFrame = true,
                isBlockedByShield = false
            )

            Result.success(
                LoadedWebPageResult(
                    finalUrl = finalUrl,
                    title = title,
                    rawHtml = rawHtml,
                    domNodes = domNodes,
                    mainNetworkEntry = mainNetworkEntry,
                    subresources = subresources
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resolveRelativeUrl(baseUrl: String, href: String): String {
        val trimmed = href.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("javascript:") || trimmed.startsWith("#")) {
            return baseUrl
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("//")) {
            val scheme = runCatching { URI(baseUrl).scheme }.getOrDefault("https") ?: "https"
            return "$scheme:$trimmed"
        }
        return try {
            URI(baseUrl).resolve(trimmed).toString()
        } catch (_: Exception) {
            val origin = runCatching {
                val u = URI(baseUrl)
                "${u.scheme}://${u.host}"
            }.getOrDefault(baseUrl.trimEnd('/'))
            if (trimmed.startsWith("/")) "$origin$trimmed" else "$origin/$trimmed"
        }
    }

    fun translateNodesInPlace(
        nodes: List<LiveDomNode>,
        targetLang: String
    ): List<LiveDomNode> {
        return nodes.mapIndexed { idx, node ->
            if (idx < 35 && node.innerText.length in 4..400) {
                val translated = translateSingleSnippet(node.innerText, targetLang)
                if (translated.isNotBlank()) {
                    node.copy(innerText = translated)
                } else {
                    node
                }
            } else {
                node
            }
        }
    }

    private fun translateSingleSnippet(text: String, targetLang: String): String {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val apiUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val outer = JSONArray(body).optJSONArray(0) ?: return ""
            val sb = StringBuilder()
            for (i in 0 until outer.length()) {
                val seg = outer.optJSONArray(i)?.optString(0) ?: ""
                sb.append(seg)
            }
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractTitle(rawHtml: String, fallbackUrl: String): String {
        val titleMatch = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(rawHtml)
        val rawTitle = titleMatch?.groupValues?.getOrNull(1)?.let { decodeHtmlText(it) }?.trim().orEmpty()
        if (rawTitle.isNotBlank()) return rawTitle
        return runCatching {
            URI(fallbackUrl).host?.removePrefix("www.") ?: fallbackUrl
        }.getOrDefault(fallbackUrl)
    }

    private fun extractSubresources(
        rawHtml: String,
        baseUrl: String
    ): List<Pair<String, NetworkResourceType>> {
        val results = mutableListOf<Pair<String, NetworkResourceType>>()
        val seen = mutableSetOf<String>()

        // 1. Stylesheets <link ... href="...">
        val linkRegex = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
        linkRegex.findAll(rawHtml).take(20).forEach { m ->
            val tag = m.value
            val attrs = parseAttributes(tag)
            val href = attrs["href"].orEmpty()
            val rel = attrs["rel"].orEmpty().lowercase()
            if (href.isNotBlank() && (rel.contains("stylesheet") || href.endsWith(".css") || rel.contains("icon"))) {
                val resolved = resolveRelativeUrl(baseUrl, href)
                if (resolved.startsWith("http") && seen.add(resolved)) {
                    val type = if (rel.contains("icon")) NetworkResourceType.IMAGE else NetworkResourceType.STYLESHEET
                    results.add(resolved to type)
                }
            }
        }

        // 2. Scripts <script ... src="...">
        val scriptRegex = Regex("<script\\b[^>]*>", RegexOption.IGNORE_CASE)
        scriptRegex.findAll(rawHtml).take(20).forEach { m ->
            val attrs = parseAttributes(m.value)
            val src = attrs["src"].orEmpty()
            if (src.isNotBlank()) {
                val resolved = resolveRelativeUrl(baseUrl, src)
                if (resolved.startsWith("http") && seen.add(resolved)) {
                    results.add(resolved to NetworkResourceType.SCRIPT)
                }
            }
        }

        // 3. Images <img ... src="...">
        val imgRegex = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
        imgRegex.findAll(rawHtml).take(20).forEach { m ->
            val attrs = parseAttributes(m.value)
            val src = attrs["src"].orEmpty()
            if (src.isNotBlank() && !src.startsWith("data:")) {
                val resolved = resolveRelativeUrl(baseUrl, src)
                if (resolved.startsWith("http") && seen.add(resolved)) {
                    results.add(resolved to NetworkResourceType.IMAGE)
                }
            }
        }

        return results
    }

    private fun parseHtmlIntoDomNodes(
        rawHtml: String,
        baseUrl: String,
        pageTitle: String
    ): List<LiveDomNode> {
        val cleaned = rawHtml
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<script\\b[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style\\b[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<noscript\\b[^>]*>.*?</noscript>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<svg\\b[^>]*>.*?</svg>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

        val nodes = mutableListOf<LiveDomNode>()
        val tagCounts = mutableMapOf<String, Int>()

        fun nextNode(
            tag: String,
            attrs: Map<String, String>,
            innerText: String,
            outerHtml: String,
            depth: Int,
            href: String? = null,
            formAction: String? = null,
            inputName: String? = null,
            inputPlaceholder: String? = null,
            inputValue: String = ""
        ): LiveDomNode {
            val count = (tagCounts[tag] ?: 0) + 1
            tagCounts[tag] = count
            val id = attrs["id"].orEmpty()
            val cls = attrs["class"].orEmpty()
            val selector = when {
                id.isNotBlank() -> "$tag#$id"
                cls.isNotBlank() -> "$tag.${cls.trim().split(Regex("\\s+")).first()}:nth-of-type($count)"
                else -> "$tag:nth-of-type($count)"
            }
            val xpath = if (id.isNotBlank()) "//*[@id=\"$id\"]" else "//html/body//$tag[$count]"
            val inlineStyles = parseInlineStyleMap(attrs["style"].orEmpty())

            return LiveDomNode(
                index = nodes.size,
                tagName = tag,
                id = id,
                className = cls,
                cssSelector = selector,
                xpath = xpath,
                depth = depth,
                attributes = attrs,
                inlineStyles = inlineStyles,
                innerText = innerText,
                outerHtml = outerHtml.take(2500),
                href = href,
                formAction = formAction,
                inputName = inputName,
                inputPlaceholder = inputPlaceholder,
                inputValue = inputValue
            )
        }

        // Extract <meta name="description" content="..."> if present
        val metaRegex = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
        metaRegex.findAll(rawHtml).take(15).forEach { m ->
            val attrs = parseAttributes(m.value)
            val name = (attrs["name"] ?: attrs["property"]).orEmpty().lowercase()
            val content = attrs["content"].orEmpty().trim()
            if ((name == "description" || name == "og:description") && content.length > 10 && nodes.none { it.tagName == "meta" }) {
                nodes.add(
                    nextNode(
                        tag = "meta",
                        attrs = attrs,
                        innerText = decodeHtmlText(content),
                        outerHtml = m.value,
                        depth = 1
                    )
                )
            }
        }

        // Tokenize semantic blocks, headings, paragraphs, inputs, buttons, links, list items, code blocks
        val elementRegex = Regex(
            "<(h1|h2|h3|h4|p|a|button|input|textarea|li|pre|code|blockquote)\\b([^>]*)>(.*?)</\\1>|<input\\b([^>]*)/?>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        var currentFormAction = resolveDefaultSearchAction(baseUrl)
        val formActionMatch = Regex("<form\\b[^>]*action=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE).find(cleaned)
        if (formActionMatch != null) {
            currentFormAction = resolveRelativeUrl(baseUrl, formActionMatch.groupValues[1])
        }

        elementRegex.findAll(cleaned).forEach { match ->
            if (nodes.size >= 140) return@forEach

            val fullMatch = match.value
            val SelfClosingInputAttrs = match.groupValues[4]
            if (SelfClosingInputAttrs.isNotEmpty() || fullMatch.lowercase().startsWith("<input")) {
                val attrStr = if (SelfClosingInputAttrs.isNotEmpty()) SelfClosingInputAttrs else match.groupValues[2]
                val attrs = parseAttributes(attrStr)
                val inputType = attrs["type"]?.lowercase() ?: "text"
                if (inputType !in listOf("hidden", "checkbox", "radio", "file")) {
                    val name = attrs["name"] ?: "q"
                    val placeholder = attrs["placeholder"]
                        ?: attrs["aria-label"]
                        ?: attrs["title"]
                        ?: if (inputType == "submit") (attrs["value"] ?: "Submit") else "Enter $name..."
                    val value = attrs["value"].orEmpty()
                    if (inputType == "submit" || inputType == "button") {
                        nodes.add(
                            nextNode(
                                tag = "button",
                                attrs = attrs,
                                innerText = decodeHtmlText(placeholder.ifBlank { "Submit" }),
                                outerHtml = fullMatch,
                                depth = 2,
                                formAction = currentFormAction
                            )
                        )
                    } else {
                        nodes.add(
                            nextNode(
                                tag = "input",
                                attrs = attrs,
                                innerText = decodeHtmlText(placeholder),
                                outerHtml = fullMatch,
                                depth = 2,
                                formAction = currentFormAction,
                                inputName = name,
                                inputPlaceholder = decodeHtmlText(placeholder),
                                inputValue = value
                            )
                        )
                    }
                }
                return@forEach
            }

            val tag = match.groupValues[1].lowercase()
            val attrStr = match.groupValues[2]
            val innerRaw = match.groupValues[3]
            val attrs = parseAttributes(attrStr)
            val text = decodeHtmlText(innerRaw).replace(Regex("\\s+"), " ").trim()

            if (text.isBlank() && tag != "input") return@forEach
            if (text.length < 2 && tag in listOf("p", "li", "blockquote")) return@forEach

            val href = if (tag == "a") {
                attrs["href"]?.takeIf { it.isNotBlank() && !it.startsWith("javascript:") }?.let {
                    resolveRelativeUrl(baseUrl, it)
                }
            } else {
                null
            }

            // Also check if a heading or paragraph contains a nested <a href="...">
            val nestedHref = if (href == null) {
                Regex("<a\\b[^>]*href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                    .find(innerRaw)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.takeIf { it.isNotBlank() && !it.startsWith("javascript:") }
                    ?.let { resolveRelativeUrl(baseUrl, it) }
            } else {
                href
            }

            val depth = when (tag) {
                "h1" -> 1
                "h2", "h3", "h4" -> 2
                "p", "blockquote", "pre", "code" -> 2
                "li", "a", "button" -> 3
                else -> 2
            }

            // Avoid consecutive duplicate text snippets
            if (nodes.lastOrNull()?.innerText == text && nodes.lastOrNull()?.tagName == tag) {
                return@forEach
            }

            nodes.add(
                nextNode(
                    tag = tag,
                    attrs = attrs,
                    innerText = text,
                    outerHtml = fullMatch,
                    depth = depth,
                    href = nestedHref
                )
            )
        }

        // Ensure there's always an H1 and search input if the page is a minimal SPA or search home page
        if (nodes.none { it.tagName == "h1" }) {
            nodes.add(
                0,
                LiveDomNode(
                    index = 0,
                    tagName = "h1",
                    id = "page-title",
                    className = "page-heading",
                    cssSelector = "h1#page-title",
                    xpath = "//*[@id=\"page-title\"]",
                    depth = 1,
                    attributes = mapOf("id" to "page-title", "class" to "page-heading"),
                    innerText = pageTitle,
                    outerHtml = "<h1 id=\"page-title\" class=\"page-heading\">$pageTitle</h1>"
                )
            )
        }

        if (nodes.none { it.tagName == "input" } &&
            (baseUrl.contains("google.") || baseUrl.contains("duckduckgo.") || baseUrl.contains("bing.") || baseUrl.contains("brave.") || baseUrl.contains("wikipedia."))
        ) {
            nodes.add(
                1,
                LiveDomNode(
                    index = 1,
                    tagName = "input",
                    id = "search-input",
                    className = "search-box",
                    cssSelector = "input#search-input",
                    xpath = "//*[@id=\"search-input\"]",
                    depth = 2,
                    attributes = mapOf(
                        "id" to "search-input",
                        "name" to "q",
                        "type" to "search",
                        "placeholder" to "Search on $pageTitle..."
                    ),
                    innerText = "Search on $pageTitle...",
                    outerHtml = "<input id=\"search-input\" name=\"q\" type=\"search\" placeholder=\"Search on $pageTitle...\" />",
                    formAction = resolveDefaultSearchAction(baseUrl),
                    inputName = "q",
                    inputPlaceholder = "Search on $pageTitle..."
                )
            )
        }

        return nodes.mapIndexed { idx, n -> n.copy(index = idx) }
    }

    private fun resolveDefaultSearchAction(baseUrl: String): String {
        val lower = baseUrl.lowercase()
        return when {
            lower.contains("duckduckgo.com") -> "https://html.duckduckgo.com/html/?q="
            lower.contains("bing.com") -> "https://www.bing.com/search?q="
            lower.contains("brave.com") -> "https://search.brave.com/search?q="
            lower.contains("wikipedia.org") -> "https://en.wikipedia.org/w/index.php?search="
            lower.contains("github.com") -> "https://github.com/search?q="
            else -> "https://www.google.com/search?q="
        }
    }

    private fun parseAttributes(tagOrAttrStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val attrRegex = Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+))")
        attrRegex.findAll(tagOrAttrStr).forEach { m ->
            val key = m.groupValues[1].lowercase()
            val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            map[key] = decodeHtmlText(value)
        }
        return map
    }

    private fun parseInlineStyleMap(styleAttr: String): Map<String, String> {
        if (styleAttr.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        styleAttr.split(";").forEach { decl ->
            val parts = decl.split(":", limit = 2)
            if (parts.size == 2) {
                val prop = parts[0].trim().lowercase()
                val value = parts[1].trim()
                if (prop.isNotEmpty() && value.isNotEmpty()) {
                    map[prop] = value
                }
            }
        }
        return map
    }

    private fun decodeHtmlText(htmlSnippet: String): String {
        val stripped = htmlSnippet.replace(Regex("<[^>]+>"), " ")
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(stripped, Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(stripped).toString()
            }
        } catch (_: Exception) {
            stripped
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
        }
    }
}
