package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSquircleShape
import com.example.ui.theme.groupedListCardShape

/**
 * Material 3 Expressive Search Screen
 * - Features a large, full-width Search Bar at the top (no back arrow button; search engine selection lives in Settings).
 * - Renders Clipboard Copy/Paste and Real Search & Browsing History as a unified Material 3 Grouped List
 *   (`groupedListCardShape(index, count)` with 3.dp inter-card spacing and 24.dp rounded group ends).
 * - Places the Clipboard Paste card as the very first item in the grouped list when a copied link/text is available.
 * - Uses strictly real user search and browsing history with zero dummy history items.
 */
private sealed interface SearchGroupEntry {
    data class ClipboardPasteEntry(val copiedText: String) : SearchGroupEntry
    data class RealHistoryEntry(val item: HistoryItem) : SearchGroupEntry
}

@Composable
fun SearchScreen(
    query: String,
    suggestions: List<SearchSuggestion>,
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

    // Build unified Material 3 Grouped List entries:
    // 1st item is ALWAYS the Clipboard Copy/Paste item (when available), followed by real History items
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Standard Chrome-Style Pill Search Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = {
                        Text(
                            text = "Search or type URL",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    modifier = Modifier.size(36.dp)
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
                        .heightIn(min = 52.dp)
                        .focusRequester(focusRequester)
                        .testTag("search_input_field")
                )
            }
        }

        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        // Material 3 Grouped List Cards for Clipboard Copy/Paste (first) and Real History
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("suggestions_list")
        ) {
            if (groupedEntries.isNotEmpty()) {
                item(key = "clipboard_and_history_group_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Section Header
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

                        // Material 3 Grouped List Cards with 3.dp spacing and rounded top/bottom group ends
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
            } else {
                // Clean Empty State when there is no clipboard link and no real search history yet
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
            // Compact Leading Squircle Icon matching Recent History cards
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

            // Compact 2-Line Title + Copied Link/Text matching Recent History cards
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

            // Compact Action Pills (Paste & Dismiss — redirect removed since tapping card opens link)
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

                // Dismiss Clipboard Item Button
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
 * Includes Copy to Clipboard action and Delete action (redirect removed as clicking card redirects).
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
            // Leading Squircle Icon
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

            // Separated Action Pill Buttons (Copy & Dismiss/Delete — redirect removed as clicking card redirects)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy to Clipboard Button
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

                // Dismiss / Delete History Item Button
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
