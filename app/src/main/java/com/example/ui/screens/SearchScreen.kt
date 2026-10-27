package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.HistoryItem
import com.example.model.SearchEngine
import com.example.model.SearchSuggestion
import com.example.model.SuggestionType

@Composable
fun SearchScreen(
    query: String,
    suggestions: List<SearchSuggestion>,
    discoveryCategories: List<String>,
    selectedCategory: String?,
    currentSearchEngine: SearchEngine,
    history: List<HistoryItem>,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onSearchEngineSelect: (SearchEngine) -> Unit,
    onExecuteSearch: (String) -> Unit,
    onSelectSuggestion: (SearchSuggestion) -> Unit,
    onClearQuery: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    var clipboardLink by remember { mutableStateOf<String?>(null) }
    var isClipboardDismissed by remember { mutableStateOf(false) }
    var usedClipboardText by remember { mutableStateOf<String?>(null) }

    fun checkClipboardForLink() {
        if (isClipboardDismissed) return
        try {
            val clipText = clipboardManager.getText()?.text?.trim()
            if (!clipText.isNullOrBlank() && clipText != usedClipboardText) {
                val isLikelyUrl = clipText.startsWith("http://", ignoreCase = true) ||
                        clipText.startsWith("https://", ignoreCase = true) ||
                        clipText.startsWith("www.", ignoreCase = true) ||
                        clipText.startsWith("aura://", ignoreCase = true) ||
                        (clipText.contains(".") && !clipText.contains(" ") && clipText.length >= 4)
                if (isLikelyUrl && clipText != query) {
                    clipboardLink = clipText
                } else if (!isLikelyUrl && clipText.length in 3..120 && clipText != query && query.isEmpty()) {
                    // Also accept copied query snippet
                    clipboardLink = clipText
                }
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
        checkClipboardForLink()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkClipboardForLink()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Partition suggestions into Material 3 Expressive Groups
    val historySuggestions = remember(suggestions) {
        suggestions.filter { it.type == SuggestionType.HISTORY }
    }

    // When query is empty and no history suggestions yet, fall back to recent browsing history items
    val recentItems = remember(query, historySuggestions, history) {
        if (historySuggestions.isNotEmpty()) {
            historySuggestions
        } else if (query.isEmpty() && history.isNotEmpty()) {
            history.take(6).map { item ->
                SearchSuggestion(
                    title = item.title.ifBlank { item.url },
                    query = item.url,
                    type = SuggestionType.HISTORY,
                    subtitle = item.url,
                    targetUrl = item.url
                )
            }
        } else {
            emptyList()
        }
    }

    val domainItems = remember(suggestions) {
        suggestions.filter { it.type == SuggestionType.DOMAIN }
    }

    val personalizedItems = remember(suggestions) {
        suggestions.filter { it.type == SuggestionType.PERSONALIZED }
    }

    val trendingAndDiscoveryItems = remember(suggestions) {
        suggestions.filter { it.type == SuggestionType.TRENDING || it.type == SuggestionType.DISCOVERY }
    }

    val otherItems = remember(suggestions, historySuggestions, domainItems, personalizedItems, trendingAndDiscoveryItems) {
        val groupedSet = (historySuggestions + domainItems + personalizedItems + trendingAndDiscoveryItems).toSet()
        suggestions.filterNot { it in groupedSet }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Search Input Bar with high contrast and distinct border
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newQuery ->
                        onQueryChange(newQuery)
                        if (clipboardLink == newQuery) {
                            isClipboardDismissed = true
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search or enter website address...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear query",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) onExecuteSearch(query)
                        }
                    ),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("search_input_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Engine Switcher Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Engine:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    SearchEngine.values().forEach { engine ->
                        val isSelected = engine == currentSearchEngine
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSearchEngineSelect(engine) }
                                .testTag("search_engine_${engine.name.lowercase()}")
                        ) {
                            Text(
                                text = engine.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dedicated "Paste link" quick action banner below search bar when a link is copied
        if (!clipboardLink.isNullOrBlank() && !isClipboardDismissed && clipboardLink != query) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        clipboardLink?.let { link ->
                            usedClipboardText = link
                            clipboardLink = null
                            isClipboardDismissed = true
                            onQueryChange(link)
                            onExecuteSearch(link)
                        }
                    }
                    .testTag("paste_link_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste Link",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Copied link detected",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Clipboard",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = clipboardLink ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Explicit "Paste link" button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardLink?.let { link ->
                                    usedClipboardText = link
                                    clipboardLink = null
                                    isClipboardDismissed = true
                                    onQueryChange(link)
                                    onExecuteSearch(link)
                                }
                            }
                            .testTag("btn_paste_link")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Paste link",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            usedClipboardText = clipboardLink
                            clipboardLink = null
                            isClipboardDismissed = true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_dismiss_paste_link")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Dismiss copied link banner",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Discovery Category Pills (Integrated suggestions engine discovery)
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(discoveryCategories) { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onCategorySelect(category) }
                            .testTag("category_pill_${category.lowercase().replace(" ", "_")}")
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Material 3 Expressive Grouped List for Recent Activity & Suggestions
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("suggestions_list")
        ) {
            // Group 1: Recent Searches & Browsing Activity
            if (recentItems.isNotEmpty()) {
                item(key = "group_recent_header") {
                    ExpressiveGroupHeader(
                        title = "Recent Activity & Searches",
                        icon = Icons.Default.History,
                        iconTint = Color(0xFF64748B),
                        badgeText = "${recentItems.size} items",
                        onClear = onClearHistory,
                        clearLabel = "Clear All",
                        testTagPrefix = "recent"
                    )
                }

                item(key = "group_recent_container") {
                    ExpressiveGroupContainer(
                        items = recentItems,
                        onSelectSuggestion = onSelectSuggestion,
                        onInsertQuery = onQueryChange,
                        onDeleteItem = { suggestion ->
                            val matchId = history.firstOrNull { it.url == suggestion.query || it.title == suggestion.title }?.id
                            if (matchId != null) {
                                onDeleteHistoryItem?.invoke(matchId)
                            }
                        },
                        testTagPrefix = "group_recent"
                    )
                }
            }

            // Group 2: Direct Websites & Domains
            if (domainItems.isNotEmpty()) {
                item(key = "group_domain_header") {
                    ExpressiveGroupHeader(
                        title = "Direct Websites",
                        icon = Icons.Default.Language,
                        iconTint = Color(0xFF0284C7),
                        badgeText = "${domainItems.size} sites",
                        testTagPrefix = "domain"
                    )
                }

                item(key = "group_domain_container") {
                    ExpressiveGroupContainer(
                        items = domainItems,
                        onSelectSuggestion = onSelectSuggestion,
                        onInsertQuery = onQueryChange,
                        testTagPrefix = "group_websites"
                    )
                }
            }

            // Group 3: Smart Recommendations
            if (personalizedItems.isNotEmpty()) {
                item(key = "group_personalized_header") {
                    ExpressiveGroupHeader(
                        title = "Smart Recommendations",
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFF6366F1),
                        badgeText = "Recommended",
                        testTagPrefix = "personalized"
                    )
                }

                item(key = "group_personalized_container") {
                    ExpressiveGroupContainer(
                        items = personalizedItems,
                        onSelectSuggestion = onSelectSuggestion,
                        onInsertQuery = onQueryChange,
                        testTagPrefix = "group_suggestions"
                    )
                }
            }

            // Group 4: Trending & Discovery Topics
            if (trendingAndDiscoveryItems.isNotEmpty()) {
                item(key = "group_trending_header") {
                    ExpressiveGroupHeader(
                        title = "Trending & Topics",
                        icon = Icons.Default.TrendingUp,
                        iconTint = Color(0xFFF59E0B),
                        badgeText = "${trendingAndDiscoveryItems.size} topics",
                        testTagPrefix = "trending"
                    )
                }

                item(key = "group_trending_container") {
                    ExpressiveGroupContainer(
                        items = trendingAndDiscoveryItems,
                        onSelectSuggestion = onSelectSuggestion,
                        onInsertQuery = onQueryChange,
                        testTagPrefix = "group_trending"
                    )
                }
            }

            // Group 5: Other Search Queries
            if (otherItems.isNotEmpty()) {
                item(key = "group_other_header") {
                    ExpressiveGroupHeader(
                        title = "Search Suggestions",
                        icon = Icons.Default.Search,
                        iconTint = Color(0xFF10B981),
                        badgeText = "${otherItems.size} queries",
                        testTagPrefix = "other"
                    )
                }

                item(key = "group_other_container") {
                    ExpressiveGroupContainer(
                        items = otherItems,
                        onSelectSuggestion = onSelectSuggestion,
                        onInsertQuery = onQueryChange,
                        testTagPrefix = "group_other"
                    )
                }
            }

            // Empty state when no suggestions and no recent history
            if (recentItems.isEmpty() && domainItems.isEmpty() && personalizedItems.isEmpty() && trendingAndDiscoveryItems.isEmpty() && otherItems.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching suggestions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type a search term or enter a web address to start exploring",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Section Header for grouped suggestion sections.
 */
@Composable
fun ExpressiveGroupHeader(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    onClear: (() -> Unit)? = null,
    clearLabel: String = "Clear",
    testTagPrefix: String = "group"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                    )
                }
            }
        }

        if (onClear != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("btn_clear_history")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = clearLabel,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = clearLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Material 3 Expressive Group Container wrapping items with curved shapes, subtle borders,
 * and high-contrast divided list items.
 */
@Composable
fun ExpressiveGroupContainer(
    items: List<SearchSuggestion>,
    onSelectSuggestion: (SearchSuggestion) -> Unit,
    onInsertQuery: (String) -> Unit,
    onDeleteItem: ((SearchSuggestion) -> Unit)? = null,
    testTagPrefix: String = "group"
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("${testTagPrefix}_container")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, suggestion ->
                ExpressiveSuggestionRowItem(
                    suggestion = suggestion,
                    onClick = { onSelectSuggestion(suggestion) },
                    onInsert = { onInsertQuery(suggestion.query.ifBlank { suggestion.title }) },
                    onDelete = if (onDeleteItem != null) { { onDeleteItem(suggestion) } } else null
                )

                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 54.dp, end = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.8.dp
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Suggestion Row Item with leading squircle icon, badges,
 * rich typography, and trailing quick-insert and navigate actions.
 */
@Composable
fun ExpressiveSuggestionRowItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit,
    onInsert: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val (icon: ImageVector, badgeColor: Color, badgeLabel: String) = when (suggestion.type) {
        SuggestionType.PERSONALIZED -> Triple(Icons.Default.Star, Color(0xFF6366F1), "Smart")
        SuggestionType.DOMAIN -> Triple(Icons.Default.Language, Color(0xFF0284C7), "Website")
        SuggestionType.HISTORY -> Triple(Icons.Default.History, Color(0xFF64748B), "Recent")
        SuggestionType.TRENDING -> Triple(Icons.Default.TrendingUp, Color(0xFFF59E0B), "Trending")
        SuggestionType.DISCOVERY -> Triple(Icons.Default.Explore, Color(0xFF10B981), "Discover")
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("suggestion_item_${suggestion.title.take(15).lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            // Squircle leading container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = badgeLabel,
                        tint = badgeColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = suggestion.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!suggestion.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = suggestion.subtitle,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Trailing quick insert arrow: populates the search bar so user can refine
            IconButton(
                onClick = onInsert,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_insert_query")
            ) {
                Icon(
                    imageVector = Icons.Default.NorthWest,
                    contentDescription = "Insert into search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }

            // Trailing navigate action
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = "Navigate directly",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp)
            )

            // Optional individual delete for history items
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Remove item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
