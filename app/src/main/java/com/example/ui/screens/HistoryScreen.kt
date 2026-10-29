package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistoryItem
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressiveDialogShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSquircleShape
import com.example.ui.theme.groupedListCardShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Material 3 Expressive History Screen
 * Features a professional browsing analytics overview, search & timeframe filter bar with count badges,
 * chronological date sections rendered as Material 3 Grouped List Cards (with 3.dp inter-card spacing
 * and 24.dp rounded group ends), domain monogram squircle avatars, and Material 3 PullToRefreshBox
 * with shape-morphing LoadingIndicator.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    onOpenUrl: (String) -> Unit,
    onOpenInNewTab: (url: String, title: String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onClearTodayHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    onCopyUrl: ((String) -> Unit)? = null,
    onRefreshHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showClearDialog by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    val filterOptions = listOf("All", "Today", "Yesterday", "Older")

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    // Compute counts for filter chips and insights
    val todayCount = remember(history, now) {
        history.count { (now - it.timestamp) < oneDayMs }
    }
    val yesterdayCount = remember(history, now) {
        history.count { (now - it.timestamp) in oneDayMs until (2 * oneDayMs) }
    }
    val olderCount = remember(history, now) {
        history.count { (now - it.timestamp) >= (2 * oneDayMs) }
    }
    val totalVisitsCount = remember(history) {
        history.sumOf { it.visitCount.coerceAtLeast(1) }
    }
    val uniqueDomainsCount = remember(history) {
        history.map { extractDomainHost(it.url) }.filter { it.isNotBlank() }.distinct().size
    }

    val filteredHistory = remember(history, searchQuery, selectedFilter, now) {
        history.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.url.contains(searchQuery, ignoreCase = true)

            val timeDiff = now - item.timestamp
            val matchesTime = when (selectedFilter) {
                "Today" -> timeDiff < oneDayMs
                "Yesterday" -> timeDiff in oneDayMs until (2 * oneDayMs)
                "Older" -> timeDiff >= (2 * oneDayMs)
                else -> true
            }

            matchesQuery && matchesTime
        }
    }

    // Group filtered history into chronological date buckets while preserving sort order
    val groupedHistory = remember(filteredHistory, now) {
        filteredHistory.groupBy { item ->
            formatHistorySectionDate(item.timestamp, now)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // Professional History Top App Bar
        com.example.ui.components.AuraSubScreenTopBar(
            title = "Browsing History",
            subtitle = "${history.size} recorded sessions • Filter: $selectedFilter",
            icon = Icons.Default.History,
            accentColor = Color(0xFF8B5CF6),
            onNavigateBack = onNavigateBack,
            backButtonTestTag = "btn_history_back",
            actions = {
                if (history.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        shape = ExpressivePillShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_clear_history_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )

        // Elevated Search & Filter Surface Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                // Search in History input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search page titles, URLs, or domains...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${filteredHistory.size} found",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear text",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = ExpressivePillShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_history_input")
                )

                // Timeframe Filter Chips with count badges
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = filter == selectedFilter
                        val filterCount = when (filter) {
                            "All" -> history.size
                            "Today" -> todayCount
                            "Yesterday" -> yesterdayCount
                            "Older" -> olderCount
                            else -> 0
                        }

                        Surface(
                            shape = ExpressivePillShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(ExpressivePillShape)
                                .clickable { selectedFilter = filter }
                                .testTag("filter_history_${filter.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                ) {
                                    Text(
                                        text = filterCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pull-to-Refresh Box with Material 3 Expressive Morphing LoadingIndicator
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    onRefreshHistory()
                    delay(800)
                    isRefreshing = false
                }
            },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .testTag("history_pull_to_refresh_indicator")
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("history_pull_to_refresh_box")
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Browsing Activity Summary Card when history has records
                if (history.isNotEmpty() && searchQuery.isBlank() && selectedFilter == "All") {
                    item(key = "history_insights_banner") {
                        HistoryInsightsOverviewCard(
                            totalVisits = totalVisitsCount,
                            uniqueDomains = uniqueDomainsCount,
                            todaySessions = todayCount
                        )
                    }
                }

                if (filteredHistory.isEmpty()) {
                    item(key = "history_empty_state") {
                        HistoryEmptyStateCard(
                            hasActiveFilters = searchQuery.isNotBlank() || selectedFilter != "All",
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            onResetFilters = {
                                searchQuery = ""
                                selectedFilter = "All"
                            }
                        )
                    }
                } else {
                    groupedHistory.entries.forEach { (dateLabel, sectionItems) ->
                        item(key = "section_$dateLabel") {
                            HistoryDateGroupSection(
                                dateLabel = dateLabel,
                                items = sectionItems,
                                onOpenUrl = onOpenUrl,
                                onOpenInNewTab = onOpenInNewTab,
                                onDeleteHistoryItem = onDeleteHistoryItem,
                                onCopyUrl = onCopyUrl
                            )
                        }
                    }
                }
            }
        }
    }

    // Clear History Dialog with Material 3 Grouped List Radio Cards
    if (showClearDialog) {
        var clearScope by remember { mutableStateOf("ALL") }

        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = ExpressiveDialogShape,
            icon = {
                Surface(
                    shape = ExpressiveSquircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Clear Browsing Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select the timeframe of browsing history to permanently remove from this device:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        // Option 0: Today's history only
                        val isTodaySelected = clearScope == "TODAY"
                        val todayShape = groupedListCardShape(index = 0, count = 2)
                        Surface(
                            shape = todayShape,
                            color = if (isTodaySelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isTodaySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(todayShape)
                                .clickable { clearScope = "TODAY" }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isTodaySelected,
                                    onClick = { clearScope = "TODAY" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Today's history only",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Removes $todayCount recorded ${if (todayCount == 1) "session" else "sessions"} from the last 24 hours",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Option 1: All time history
                        val isAllSelected = clearScope == "ALL"
                        val allShape = groupedListCardShape(index = 1, count = 2)
                        Surface(
                            shape = allShape,
                            color = if (isAllSelected) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isAllSelected) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(allShape)
                                .clickable { clearScope = "ALL" }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isAllSelected,
                                    onClick = { clearScope = "ALL" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "All time history",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Removes all ${history.size} recorded sessions across $uniqueDomainsCount domains",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (clearScope == "TODAY") {
                            onClearTodayHistory()
                        } else {
                            onClearAllHistory()
                        }
                        showClearDialog = false
                    },
                    shape = ExpressivePillShape,
                    modifier = Modifier.testTag("btn_confirm_clear_history")
                ) {
                    Text(
                        text = "Clear Data",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Browsing Activity Insights Banner Card
 * Displays key telemetry metrics in a cohesive Material 3 Expressive surface container.
 */
@Composable
private fun HistoryInsightsOverviewCard(
    totalVisits: Int,
    uniqueDomains: Int,
    todaySessions: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = ExpressiveCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = ExpressiveSquircleShape,
                    color = Color(0xFF8B5CF6).copy(alpha = 0.16f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "ACTIVITY OVERVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryMetricTile(
                    label = "Total Visits",
                    value = totalVisits.toString(),
                    icon = Icons.Default.History,
                    accentColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                HistoryMetricTile(
                    label = "Domains",
                    value = uniqueDomains.toString(),
                    icon = Icons.Default.Language,
                    accentColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                HistoryMetricTile(
                    label = "Today",
                    value = todaySessions.toString(),
                    icon = Icons.Default.AccessTime,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HistoryMetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ExpressiveSquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Chronological Date Group Section
 * Renders a date header and a Material 3 Grouped List of HistoryCardItems
 * where the first card has 24.dp top corners, the last card has 24.dp bottom corners,
 * and inner cards have 4.dp corners with 3.dp spacing between cards.
 */
@Composable
private fun HistoryDateGroupSection(
    dateLabel: String,
    items: List<HistoryItem>,
    onOpenUrl: (String) -> Unit,
    onOpenInNewTab: (url: String, title: String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onCopyUrl: ((String) -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = dateLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.6.sp
                )
            }

            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Material 3 Grouped List Cards with 3.dp inter-item spacing
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, item ->
                HistoryCardItem(
                    item = item,
                    shape = groupedListCardShape(index = index, count = items.size),
                    onClick = { onOpenUrl(item.url) },
                    onOpenInNewTab = { onOpenInNewTab(item.url, item.title) },
                    onDelete = { onDeleteHistoryItem(item.id) },
                    onCopy = onCopyUrl?.let { copyFn -> { copyFn(item.url) } }
                )
            }
        }
    }
}

/**
 * Polished Empty State Card for Browsing History
 */
@Composable
private fun HistoryEmptyStateCard(
    hasActiveFilters: Boolean,
    searchQuery: String,
    selectedFilter: String,
    onResetFilters: () -> Unit
) {
    Card(
        shape = ExpressiveCardShape,
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
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (hasActiveFilters) Icons.Default.FilterListOff else Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (searchQuery.isNotBlank()) {
                    "No matching browsing history"
                } else if (selectedFilter != "All") {
                    "No sessions recorded for $selectedFilter"
                } else {
                    "Your browsing timeline is clean"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (searchQuery.isNotBlank()) {
                    "No visited pages or domains matched \"$searchQuery\". Try another keyword or clear your filter."
                } else if (selectedFilter != "All") {
                    "Switch back to 'All' to view sessions from other dates."
                } else {
                    "Pages you visit and AI automation sessions will appear here organized chronologically for instant recall."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(18.dp))
                FilledTonalButton(
                    onClick = onResetFilters,
                    shape = ExpressivePillShape
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Search & Filters",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive History Item Card
 * Supports both standalone `ExpressiveCardShape` and Material 3 Grouped List `groupedListCardShape`.
 */
@Composable
fun HistoryCardItem(
    item: HistoryItem,
    onClick: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (() -> Unit)? = null,
    shape: Shape = ExpressiveCardShape,
    modifier: Modifier = Modifier
) {
    val relativeTime = remember(item.timestamp) { formatRelativeTime(item.timestamp) }
    val domainHost = remember(item.url) { extractDomainHost(item.url) }
    val domainInitial = remember(domainHost, item.title) {
        domainHost.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
            ?: item.title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
            ?: ""
    }
    val domainAccent = remember(domainHost) { domainAccentColor(domainHost) }
    val cleanDisplayUrl = remember(item.url) {
        item.url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("history_item_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Compact Domain Monogram Squircle Badge
            Surface(
                shape = ExpressiveSquircleShape,
                color = domainAccent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, domainAccent.copy(alpha = 0.3f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (domainInitial.isNotEmpty()) {
                        Text(
                            text = domainInitial,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = domainAccent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = domainAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Compact 2-Line Title + URL & Relative Time
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title.ifBlank { domainHost.ifBlank { item.url } },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = relativeTime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = cleanDisplayUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Compact Action Pills (Copy & Delete — redirect removed since tapping card opens URL)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onCopy != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCopy)
                            .testTag("btn_copy_history_${item.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .testTag("btn_delete_history_${item.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
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
 * Extracts a clean domain host (e.g., "github.com") from a URL string.
 */
private fun extractDomainHost(url: String): String {
    return try {
        val normalized = if (url.contains("://")) url else "https://$url"
        val host = Uri.parse(normalized).host ?: url.substringBefore("/")
        host.removePrefix("www.").lowercase(Locale.getDefault())
    } catch (_: Exception) {
        url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
    }
}

/**
 * Computes a deterministic, harmonious accent color for a domain name.
 */
private fun domainAccentColor(domain: String): Color {
    val palette = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF0284C7), // Sky Blue
        Color(0xFF8B5CF6), // Violet
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6), // Teal
        Color(0xFF3B82F6)  // Blue
    )
    if (domain.isBlank()) return palette[0]
    val index = abs(domain.hashCode()) % palette.size
    return palette[index]
}

/**
 * Formats a timestamp into a human-friendly section header ("Today", "Yesterday", or "EEEE, MMM d").
 */
private fun formatHistorySectionDate(timestamp: Long, now: Long): String {
    val calItem = Calendar.getInstance().apply { timeInMillis = timestamp }
    val calNow = Calendar.getInstance().apply { timeInMillis = now }

    val sameYear = calItem.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
    val dayDiff = calNow.get(Calendar.DAY_OF_YEAR) - calItem.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayDiff == 0 -> "Today — ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))}"
        sameYear && dayDiff == 1 -> "Yesterday — ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))}"
        (now - timestamp) < 24 * 60 * 60 * 1000L -> "Today — ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))}"
        (now - timestamp) < 48 * 60 * 60 * 1000L -> "Yesterday — ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))}"
        else -> SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Formats a timestamp into a concise relative string ("Just now", "15m ago", "3h ago", "4d ago").
 */
private fun formatRelativeTime(timestamp: Long): String {
    val diffMs = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diffMs / (60 * 1000L)
    val hours = diffMs / (60 * 60 * 1000L)
    val days = diffMs / (24 * 60 * 60 * 1000L)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
