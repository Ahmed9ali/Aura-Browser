package com.example.devtools

import java.util.UUID

enum class DevToolTab(val title: String) {
    SOURCE_VIEW("Source View"),
    INSPECT_DOM("Inspect DOM"),
    NETWORK_SNIFFER("Network Sniffer")
}

enum class NetworkResourceType(val label: String) {
    ALL("All"),
    DOCUMENT("Doc"),
    XHR("XHR/Fetch"),
    SCRIPT("JS"),
    STYLESHEET("CSS"),
    IMAGE("Img"),
    FONT("Font"),
    OTHER("Other"),
    BLOCKED("Blocked")
}

data class NetworkLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val method: String = "GET",
    val resourceType: NetworkResourceType = NetworkResourceType.OTHER,
    val statusCode: Int? = null,
    val statusText: String? = null,
    val mimeType: String? = null,
    val durationMs: Long? = null,
    val transferSizeBytes: Long? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val responsePreview: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isMainFrame: Boolean = false,
    val isBlockedByShield: Boolean = false
) {
    val host: String
        get() = runCatching {
            java.net.URI(url).host ?: url.substringAfter("://").substringBefore("/")
        }.getOrDefault(url.substringAfter("://").substringBefore("/"))

    val pathAndQuery: String
        get() = runCatching {
            val uri = java.net.URI(url)
            val path = uri.rawPath?.ifBlank { "/" } ?: "/"
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            "$path$query"
        }.getOrDefault(url)

    fun toCurlCommand(): String {
        val sb = StringBuilder("curl -X $method")
        requestHeaders.forEach { (k, v) ->
            val escaped = v.replace("\"", "\\\"")
            sb.append(" \\\n  -H \"$k: $escaped\"")
        }
        sb.append(" \\\n  \"$url\"")
        return sb.toString()
    }
}

data class InspectedDomElement(
    val tagName: String,
    val id: String = "",
    val className: String = "",
    val cssSelector: String = "",
    val xpath: String = "",
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val topPx: Int = 0,
    val leftPx: Int = 0,
    val attributes: Map<String, String> = emptyMap(),
    val computedStyles: Map<String, String> = emptyMap(),
    val outerHtml: String = "",
    val innerText: String = "",
    val childCount: Int = 0
)

data class DomTreeNodeSummary(
    val index: Int,
    val tagName: String,
    val id: String,
    val className: String,
    val cssSelector: String,
    val depth: Int,
    val textSnippet: String,
    val childCount: Int,
    val dimensionsLabel: String = ""
)

data class ConsoleLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val level: String, // "LOG", "INFO", "WARN", "ERROR", "RESULT", "INPUT"
    val message: String,
    val sourceId: String = "",
    val lineNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveDomNode(
    val index: Int,
    val tagName: String,
    val id: String = "",
    val className: String = "",
    val cssSelector: String,
    val xpath: String,
    val depth: Int = 1,
    val attributes: Map<String, String> = emptyMap(),
    val inlineStyles: Map<String, String> = emptyMap(),
    val innerText: String = "",
    val outerHtml: String = "",
    val href: String? = null,
    val formAction: String? = null,
    val inputName: String? = null,
    val inputPlaceholder: String? = null,
    val inputValue: String = ""
)
