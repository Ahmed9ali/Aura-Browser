package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    onOpenUrl: (String) -> Unit,
    onOpenInNewTab: (url: String, title: String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onClearTodayHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf("All", "Today", "Yesterday", "Older")

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    val filteredHistory = history.filter { item ->
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // History Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_history_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = "Browsing History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${history.size} past sessions",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (history.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Search in History input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search browsing history...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_history_input")
        )

        // Timeframe Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(filterOptions) { filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedFilter = filter }
                        .testTag("filter_history_${filter.lowercase()}")
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // History Sessions List
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredHistory.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching browsing history" else "No browsing history",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different keyword or domain" else "Websites you visit will appear here for easy recall",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                items(filteredHistory, key = { it.id }) { item ->
                    HistoryCardItem(
                        item = item,
                        onClick = { onOpenUrl(item.url) },
                        onOpenInNewTab = { onOpenInNewTab(item.url, item.title) },
                        onDelete = { onDeleteHistoryItem(item.id) }
                    )
                }
            }
        }
    }

    // Clear History Dialog with timeframe options
    if (showClearDialog) {
        var clearScope by remember { mutableStateOf("ALL") }

        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Browsing Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose the timeframe of history to remove:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { clearScope = "TODAY" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = clearScope == "TODAY",
                            onClick = { clearScope = "TODAY" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Today's history only", fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { clearScope = "ALL" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = clearScope == "ALL",
                            onClick = { clearScope = "ALL" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "All time history", fontSize = 14.sp)
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
                    modifier = Modifier.testTag("btn_confirm_clear_history")
                ) {
                    Text("Clear Data", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryCardItem(
    item: HistoryItem,
    onClick: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedTime = remember(item.timestamp) { timeFormatter.format(Date(item.timestamp)) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { item.url },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.url.removePrefix("https://").removePrefix("http://"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (item.visitCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${item.visitCount} visits",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenInNewTab,
                    modifier = Modifier.size(32.dp).testTag("btn_open_history_new_tab_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in New Tab",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("btn_delete_history_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
