package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSquircleShape
import com.example.ui.theme.groupedListCardShape

/**
 * Material 3 Expressive Search & Personalized Suggestion Screen
 * - Features a full-width Pill Address/Search Bar at the top with quick Search Engine switching
 *   and personalized category filter chips.
 * - Dynamically surfaces Personalized Content Recommendations (`SuggestionType.PERSONALIZED` & `SWITCH_TO_TAB`)
 *   and Smart Search & Domain Autocomplete Suggestions as the user begins typing in the address bar.
 * - Renders Clipboard Copy/Paste and Real Search & Browsing History as a unified Material 3 Grouped List.
 */
private sealed interface SearchGroupEntry {
    data class ClipboardPasteEntry(val copiedText: String) : SearchGroupEntry
    data class RealHistoryEntry(val item: HistoryItem) : SearchGroupEntry
}

@Composable
fun SearchScreen(
    query: String,
    suggestions: List<SearchSuggestion>,
    personalizedRecommendations: List<SearchSuggestion> = emptyList(),
    suggestionsEnabled: Boolean = true,
    discoveryCategories: List<String> = emptyList(),
    selectedCategory: String? = null,
    currentSearchEngine: SearchEngine,
    history: List<HistoryItem>,
    dismissedClipboardTexts: Set<String> = emptySet(),
    onDismissClipboard: (String) -> Unit = {},
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit = {},
    onSearchEngineSelect: (SearchEngine) -> Unit = {},
    onExecuteSearch: (String) -> Unit,
    onSelectSuggestion: (SearchSuggestion) -> Unit,
    onClearQuery: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: ((String) -> Unit)? = null,
    onCopyToClipboard: ((String) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onNavigateBack != null) {
        BackHandler(onBack = onNavigateBack)
    }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    var clipboardLink by remember { mutableStateOf<String?>(null) }
    var isClipboardDismissed by remember { mutableStateOf(false) }

    fun checkClipboardForLink() {
        if (isClipboardDismissed) return
        try {
            val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val sysText = sysClipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            val composeText = clipboardManager.getText()?.text?.trim()
            val clipText = (!sysText.isNullOrBlank()).let { if (it) sysText else composeText }

            if (!clipText.isNullOrBlank() && !dismissedClipboardTexts.contains(clipText) && clipText != query) {
                val isLikelyUrl = clipText.startsWith("http://", ignoreCase = true) ||
                        clipText.startsWith("https://", ignoreCase = true) ||
                        clipText.startsWith("www.", ignoreCase = true) ||
                        clipText.startsWith("aura://", ignoreCase = true) ||
                        (clipText.contains(".") && !clipText.contains(" ") && clipText.length >= 4)
                if (isLikelyUrl || clipText.length in 2..200) {
                    clipboardLink = clipText
                } else {
                    clipboardLink = null
                }
            } else {
                clipboardLink = null
            }
        } catch (_: Exception) {
            clipboardLink = null
        }
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

    // Filter strictly REAL user search and browsing history (never dummy history)
    val realHistoryItems = remember(query, history) {
        val cleanHistory = history.filter { item ->
            item.url.isNotBlank() &&
                    !item.url.startsWith("aura://", ignoreCase = true) &&
                    item.url != "about:blank"
        }
        if (query.isBlank()) {
            cleanHistory
        } else {
            val trimmedQuery = query.trim()
            cleanHistory.filter { item ->
                item.title.contains(trimmedQuery, ignoreCase = true) ||
                        item.url.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val activeClipboardText = clipboardLink?.takeIf {
        it.isNotBlank() && !isClipboardDismissed && !dismissedClipboardTexts.contains(it)
    }

    // Build unified Material 3 Grouped List entries for Clipboard + Real History
    val groupedEntries = remember(activeClipboardText, realHistoryItems) {
        buildList {
            if (activeClipboardText != null) {
                add(SearchGroupEntry.ClipboardPasteEntry(activeClipboardText))
            }
            realHistoryItems.forEach { historyItem ->
                add(SearchGroupEntry.RealHistoryEntry(historyItem))
            }
        }
    }

    // Partition live suggestions as the user types in the address bar
    val typingPersonalizedItems = remember(query, suggestions, personalizedRecommendations, selectedCategory, suggestionsEnabled) {
        if (!suggestionsEnabled) {
            emptyList()
        } else if (query.isNotBlank()) {
            val fromSuggestions = suggestions.filter {
                it.type == SuggestionType.PERSONALIZED ||
                        it.type == SuggestionType.SWITCH_TO_TAB ||
                        it.type == SuggestionType.DISCOVERY
            }
            (fromSuggestions + personalizedRecommendations).distinctBy { "${it.type}_${it.title}_${it.targetUrl}" }.take(5)
        } else if (selectedCategory != null) {
            personalizedRecommendations.take(5)
        } else {
            personalizedRecommendations.take(4)
        }
    }

    val smartQueryAndDomainSuggestions = remember(query, suggestions, suggestionsEnabled) {
        if (!suggestionsEnabled || query.isBlank()) {
            emptyList()
        } else {
            suggestions.filter {
                it.type == SuggestionType.DOMAIN ||
                        it.type == SuggestionType.BOOKMARK ||
                        it.type == SuggestionType.SEARCH_AUTOCOMPLETE
            }.distinctBy { "${it.title}_${it.targetUrl ?: it.query}" }.take(6)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Top Pill Address Bar + Active Search Engine Selector & Personalized Category Filter Strip
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newQuery ->
                        onQueryChange(newQuery)
                        if (clipboardLink == newQuery) {
                            clipboardLink?.let { onDismissClipboard(it) }
                            clipboardLink = null
                            isClipboardDismissed = true
                        }
                    },
                    textStyle = TextStyle(
                        fontFamily = com.example.ui.theme.VarelaRoundFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = {
                        Text(
                            text = "Search ${currentSearchEngine.displayName} or type URL",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                IconButton(
                                    onClick = onClearQuery,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_clear_search_query")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear query",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (query.isNotBlank()) onExecuteSearch(query)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_submit_search")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Execute search",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
                    shape = ExpressivePillShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(min = 52.dp)
                        .focusRequester(focusRequester)
                        .testTag("search_input_field")
                )

                // Quick Search Engine Switcher & Personalized Topic Filter Row
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_engine_and_category_row")
                ) {
                    // Search Engine Selector Pills (Google, DuckDuckGo, Bing, Brave)
                    items(
                        items = SearchEngine.entries,
                        key = { "engine_${it.name}" }
                    ) { engine ->
                        val isSelected = engine == currentSearchEngine
                        Surface(
                            shape = ExpressivePillShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .clip(ExpressivePillShape)
                                .clickable { onSearchEngineSelect(engine) }
                                .testTag("chip_engine_${engine.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = engine.displayName,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    // Topic Category Filter Chips for Personalized Content Recommendations
                    if (suggestionsEnabled && discoveryCategories.isNotEmpty()) {
                        items(
                            items = discoveryCategories,
                            key = { "cat_$it" }
                        ) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelect(category) },
                                label = {
                                    Text(
                                        text = category,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                shape = ExpressivePillShape,
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("chip_category_${category.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        // Unified Scrollable Feed: Personalized Recommendations (as user types), Smart Search Suggestions, and Clipboard/History
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("suggestions_list")
        ) {
            // 1. When typing in the address bar (or filtering by category): Personalized Content Recommendations first!
            if (query.isNotBlank() && typingPersonalizedItems.isNotEmpty()) {
                item(key = "personalized_typing_recommendations_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personalized_recommendations_section")
                    ) {
                        ExpressiveGroupHeader(
                            title = if (selectedCategory != null) {
                                "Personalized For You • $selectedCategory"
                            } else {
                                "Personalized Content Recommendations"
                            },
                            icon = Icons.Default.AutoAwesome,
                            iconTint = Color(0xFF8B5CF6),
                            badgeText = "${typingPersonalizedItems.size} ${if (typingPersonalizedItems.size == 1) "match" else "matches"}",
                            testTagPrefix = "personalized"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_personalized_container")
                        ) {
                            typingPersonalizedItems.forEachIndexed { index, suggestion ->
                                val cardShape = groupedListCardShape(
                                    index = index,
                                    count = typingPersonalizedItems.size
                                )
                                PersonalizedSuggestionGroupedCardItem(
                                    suggestion = suggestion,
                                    shape = cardShape,
                                    onClick = { onSelectSuggestion(suggestion) },
                                    onFillQuery = {
                                        onQueryChange(suggestion.targetUrl ?: suggestion.query)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. When typing in the address bar: Smart Search & Domain Autocomplete Suggestions
            if (smartQueryAndDomainSuggestions.isNotEmpty()) {
                item(key = "smart_search_autocomplete_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smart_autocomplete_section")
                    ) {
                        ExpressiveGroupHeader(
                            title = "Search & Website Suggestions (${currentSearchEngine.displayName})",
                            icon = Icons.Default.Search,
                            iconTint = MaterialTheme.colorScheme.primary,
                            badgeText = "${smartQueryAndDomainSuggestions.size}",
                            testTagPrefix = "autocomplete"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_autocomplete_container")
                        ) {
                            smartQueryAndDomainSuggestions.forEachIndexed { index, suggestion ->
                                val cardShape = groupedListCardShape(
                                    index = index,
                                    count = smartQueryAndDomainSuggestions.size
                                )
                                PersonalizedSuggestionGroupedCardItem(
                                    suggestion = suggestion,
                                    shape = cardShape,
                                    onClick = { onSelectSuggestion(suggestion) },
                                    onFillQuery = {
                                        onQueryChange(suggestion.query)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Clipboard Copy/Paste (first) and Real Search/Browsing History
            if (groupedEntries.isNotEmpty()) {
                item(key = "clipboard_and_history_group_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ExpressiveGroupHeader(
                            title = if (activeClipboardText != null && realHistoryItems.isNotEmpty()) {
                                "Clipboard & Recent History"
                            } else if (activeClipboardText != null) {
                                "Clipboard Link"
                            } else {
                                "Recent Search & Browsing History"
                            },
                            icon = if (activeClipboardText != null && realHistoryItems.isEmpty()) {
                                Icons.Default.ContentPaste
                            } else {
                                Icons.Default.History
                            },
                            iconTint = MaterialTheme.colorScheme.primary,
                            badgeText = "${groupedEntries.size} ${if (groupedEntries.size == 1) "item" else "items"}",
                            onClear = if (realHistoryItems.isNotEmpty()) onClearHistory else null,
                            clearLabel = "Clear All",
                            testTagPrefix = "recent"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_recent_container")
                        ) {
                            groupedEntries.forEachIndexed { index, entry ->
                                val cardShape = groupedListCardShape(
                                    index = index,
                                    count = groupedEntries.size
                                )

                                when (entry) {
                                    is SearchGroupEntry.ClipboardPasteEntry -> {
                                        ClipboardGroupedCardItem(
                                            copiedText = entry.copiedText,
                                            shape = cardShape,
                                            onPasteAndGo = {
                                                val link = entry.copiedText
                                                onDismissClipboard(link)
                                                clipboardLink = null
                                                isClipboardDismissed = true
                                                onQueryChange(link)
                                                onExecuteSearch(link)
                                            },
                                            onInsertToBar = {
                                                onQueryChange(entry.copiedText)
                                            },
                                            onDismiss = {
                                                onDismissClipboard(entry.copiedText)
                                                clipboardLink = null
                                                isClipboardDismissed = true
                                            }
                                        )
                                    }

                                    is SearchGroupEntry.RealHistoryEntry -> {
                                        RealHistoryGroupedCardItem(
                                            item = entry.item,
                                            shape = cardShape,
                                            onClick = { onExecuteSearch(entry.item.url) },
                                            onInsert = {
                                                val textToInsert = entry.item.title
                                                    .takeIf { it.isNotBlank() && !it.startsWith("http") }
                                                    ?: entry.item.url
                                                onQueryChange(textToInsert)
                                            },
                                            onCopy = {
                                                val textToCopy = entry.item.url
                                                if (onCopyToClipboard != null) {
                                                    onCopyToClipboard(textToCopy)
                                                } else {
                                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                                }
                                                isClipboardDismissed = false
                                                clipboardLink = textToCopy
                                            },
                                            onDelete = onDeleteHistoryItem?.let { deleteFn ->
                                                { deleteFn(entry.item.id) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Zero-query Personalized Content Recommendations (when query is blank and recommendations are enabled)
            if (query.isBlank() && typingPersonalizedItems.isNotEmpty()) {
                item(key = "personalized_zero_query_recommendations_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personalized_recommendations_section")
                    ) {
                        ExpressiveGroupHeader(
                            title = if (selectedCategory != null) {
                                "Recommended For You • $selectedCategory"
                            } else {
                                "Recommended For You"
                            },
                            icon = Icons.Default.AutoAwesome,
                            iconTint = Color(0xFF8B5CF6),
                            badgeText = "Personalized",
                            testTagPrefix = "personalized"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_personalized_container")
                        ) {
                            typingPersonalizedItems.forEachIndexed { index, suggestion ->
                                val cardShape = groupedListCardShape(
                                    index = index,
                                    count = typingPersonalizedItems.size
                                )
                                PersonalizedSuggestionGroupedCardItem(
                                    suggestion = suggestion,
                                    shape = cardShape,
                                    onClick = { onSelectSuggestion(suggestion) },
                                    onFillQuery = {
                                        onQueryChange(suggestion.targetUrl ?: suggestion.query)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Fallback Empty State when suggestions are disabled and there is no history or clipboard
            if (groupedEntries.isEmpty() && typingPersonalizedItems.isEmpty() && smartQueryAndDomainSuggestions.isEmpty()) {
                item(key = "empty_search_history_state") {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp)
                        ) {
                            Surface(
                                shape = ExpressiveSquircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (query.isNotBlank()) {
                                    "Press search to look up \"$query\""
                                } else {
                                    "No recent search history"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (query.isNotBlank()) {
                                    "Tap the arrow button in the search bar or press Search on your keyboard to open results."
                                } else {
                                    "Copied links from your clipboard and your real searches or visited websites will appear here automatically."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Personalized Content Recommendation / Smart Autocomplete Card inside the Material 3 Grouped List.
 */
@Composable
private fun PersonalizedSuggestionGroupedCardItem(
    suggestion: SearchSuggestion,
    shape: Shape,
    onClick: () -> Unit,
    onFillQuery: () -> Unit
) {
    val isPersonalized = suggestion.type == SuggestionType.PERSONALIZED || suggestion.type == SuggestionType.DISCOVERY
    val isSwitchTab = suggestion.type == SuggestionType.SWITCH_TO_TAB
    val isBookmark = suggestion.type == SuggestionType.BOOKMARK
    val isDomain = suggestion.type == SuggestionType.DOMAIN

    val badgeAccent = when {
        isSwitchTab -> Color(0xFF10B981)
        isPersonalized -> Color(0xFF8B5CF6)
        isBookmark -> Color(0xFFF59E0B)
        isDomain -> Color(0xFF0284C7)
        else -> MaterialTheme.colorScheme.primary
    }

    val leadingIcon = when {
        isSwitchTab -> Icons.Default.Tab
        isPersonalized -> Icons.Default.AutoAwesome
        isBookmark -> Icons.Default.Bookmark
        isDomain -> Icons.Default.Language
        else -> Icons.Default.Search
    }

    val tagSlug = remember(suggestion.title) {
        suggestion.title.take(15).lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("suggestion_item_$tagSlug")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = ExpressiveSquircleShape,
                color = badgeAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = suggestion.type.name,
                        tint = badgeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val badgeLabel = suggestion.reasonBadge ?: suggestion.category
                    if (!badgeLabel.isNullOrBlank()) {
                        Surface(
                            shape = ExpressiveChipShape,
                            color = badgeAccent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = badgeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeAccent,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (!suggestion.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = suggestion.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick-fill query into address bar button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFillQuery)
                    .testTag("btn_fill_suggestion_$tagSlug")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NorthWest,
                        contentDescription = "Fill address bar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * First Card in the Material 3 Grouped List: Clipboard Copy/Paste Link Card.
 */
@Composable
private fun ClipboardGroupedCardItem(
    copiedText: String,
    shape: Shape,
    onPasteAndGo: () -> Unit,
    onInsertToBar: () -> Unit,
    onDismiss: () -> Unit
) {
    val isUrl = remember(copiedText) {
        copiedText.startsWith("http://", ignoreCase = true) ||
                copiedText.startsWith("https://", ignoreCase = true) ||
                copiedText.startsWith("www.", ignoreCase = true) ||
                (copiedText.contains(".") && !copiedText.contains(" "))
    }
    val cleanCopiedSubtitle = remember(copiedText) {
        copiedText.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onPasteAndGo)
            .testTag("paste_link_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = ExpressiveSquircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste Link",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isUrl) "Link you copied" else "Text you copied",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = ExpressiveChipShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Clipboard",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = cleanCopiedSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPasteAndGo)
                        .testTag("btn_paste_link")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .testTag("btn_dismiss_paste_link")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Dismiss copied link",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Real History Card Item inside the Material 3 Grouped List.
 */
@Composable
private fun RealHistoryGroupedCardItem(
    item: HistoryItem,
    shape: Shape,
    onClick: () -> Unit,
    onInsert: () -> Unit,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isSearchQueryUrl = remember(item.url) {
        item.url.contains("?q=", ignoreCase = true) || item.url.contains("&q=", ignoreCase = true)
    }
    val displayTitle = remember(item.title, item.url) {
        item.title.ifBlank { item.url }
    }
    val cleanSubtitle = remember(item.url) {
        item.url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("suggestion_item_${displayTitle.take(15).lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = ExpressiveSquircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearchQueryUrl) Icons.Default.History else Icons.Default.Language,
                        contentDescription = "History item",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = cleanSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCopy)
                        .testTag("btn_copy_search_history_${item.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (onDelete != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete)
                            .testTag("btn_delete_search_history_${item.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove from history",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Section Header for grouped sections.
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
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 2.dp)
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
                fontSize = 13.sp,
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = clearLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                )
            }
        }
    }
}
