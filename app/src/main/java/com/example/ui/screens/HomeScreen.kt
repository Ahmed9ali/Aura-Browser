package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserSettings
import com.example.model.BrowserTab
import com.example.model.HistoryItem
import com.example.model.SpeedDial
import com.example.model.TabGroup
import com.example.ui.components.SpeedDialEditDialog
import com.example.ui.components.getSpeedDialIcon
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSquircleShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DiscoverStory(
    val title: String,
    val source: String,
    val timeAgo: String,
    val tag: String,
    val tagColor: Color,
    val targetUrl: String
)

/**
 * Material 3 Expressive Home Screen
 * Features bold brand hierarchy, pill search trigger, squircle speed dials,
 * Material 3 PullToRefreshBox with shape-morphing LoadingIndicator,
 * and high-contrast tonal container sections.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: BrowserSettings = BrowserSettings(),
    speedDials: List<SpeedDial>,
    tabGroups: List<TabGroup>,
    tabs: List<BrowserTab>,
    history: List<HistoryItem>,
    onOpenUrl: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onAddSpeedDial: (title: String, url: String, iconCategory: String, colorHex: String) -> Unit = { _, _, _, _ -> },
    onUpdateSpeedDial: (id: String, title: String, url: String, iconCategory: String, colorHex: String) -> Unit = { _, _, _, _, _ -> },
    onDeleteSpeedDial: (id: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isManageMode by remember { mutableStateOf(false) }
    var speedDialToEdit by remember { mutableStateOf<SpeedDial?>(null) }
    var showAddSpeedDialDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    val discoverStories = listOf(
        DiscoverStory(
            title = "Android 16 & Modern Jetpack Architecture Patterns",
            source = "Android Developers Blog",
            timeAgo = "2h ago",
            tag = "Tech",
            tagColor = Color(0xFF0284C7),
            targetUrl = "https://developer.android.com"
        ),
        DiscoverStory(
            title = "Next-Generation Generative AI Models and Local Edge Inference",
            source = "ArXiv Research",
            timeAgo = "4h ago",
            tag = "AI",
            tagColor = Color(0xFF6366F1),
            targetUrl = "https://arxiv.org"
        ),
        DiscoverStory(
            title = "Open Source Developer Ecosystem Trends in 2026",
            source = "GitHub Universe",
            timeAgo = "6h ago",
            tag = "Open Source",
            tagColor = Color(0xFF10B981),
            targetUrl = "https://github.com"
        ),
        DiscoverStory(
            title = "Web Standards: Enhanced Privacy and Fast Responsive Layouts",
            source = "MDN Web Insights",
            timeAgo = "8h ago",
            tag = "Web",
            tagColor = Color(0xFFF59E0B),
            targetUrl = "https://developer.mozilla.org"
        )
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                delay(850)
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
                    .testTag("home_pull_to_refresh_indicator")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("home_pull_to_refresh_box")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen")
        ) {
        // Recents Section directly below the Top Search Bar (Horizontal sliding only, shown only when visited >= 2 times)
        if (settings.showSpeedDials && speedDials.isNotEmpty()) {
            item {
                Surface(
                    shape = ExpressiveCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("recents_container")
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recents_horizontal_row")
                    ) {
                        items(
                            items = speedDials,
                            key = { dial -> dial.id }
                        ) { dial ->
                            SpeedDialItem(
                                dial = dial,
                                isManageMode = false,
                                onClick = { onOpenUrl(dial.url) },
                                onEdit = { speedDialToEdit = dial },
                                onDelete = { onDeleteSpeedDial(dial.id) },
                                modifier = Modifier.width(72.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Tab Groups Quick Strip (disabled by default, shown below Speed Dials if enabled in Settings)
        if (settings.showTabGroups) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = ExpressiveChipShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Tab Groups",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = ExpressiveChipShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "${tabGroups.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Manage All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(onClick = onNavigateToTabs)
                                .testTag("btn_manage_tab_groups")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(tabGroups) { group ->
                            val groupColor = try {
                                Color(android.graphics.Color.parseColor(group.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            val tabsInGroup = tabs.count { it.groupId == group.id }

                            Surface(
                                shape = ExpressiveCardShape,
                                color = groupColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, groupColor.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clip(ExpressiveCardShape)
                                    .clickable(onClick = onNavigateToTabs)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(groupColor)
                                    )
                                    Column {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$tabsInGroup ${if (tabsInGroup == 1) "tab" else "tabs"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Curated Discover Feed for faster content discovery
        if (settings.showDiscoverFeed) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Surface(
                            shape = ExpressiveChipShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Discover Feed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    discoverStories.forEach { story ->
                        Card(
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(ExpressiveCardShape)
                                .clickable { onOpenUrl(story.targetUrl) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = ExpressiveChipShape,
                                        color = story.tagColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = story.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = story.tagColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = story.timeAgo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = story.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = story.source,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Read",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Add New Speed Dial Dialog
    if (showAddSpeedDialDialog) {
        SpeedDialEditDialog(
            initialDial = null,
            onDismiss = { showAddSpeedDialDialog = false },
            onSave = { title, url, cat, col ->
                onAddSpeedDial(title, url, cat, col)
            }
        )
    }

    // Edit Existing Speed Dial Dialog
    speedDialToEdit?.let { dial ->
        SpeedDialEditDialog(
            initialDial = dial,
            onDismiss = { speedDialToEdit = null },
            onSave = { title, url, cat, col ->
                onUpdateSpeedDial(dial.id, title, url, cat, col)
            },
            onDelete = { id ->
                onDeleteSpeedDial(id)
            }
        )
    }
}

@Composable
fun SpeedDialItem(
    dial: SpeedDial,
    isManageMode: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dialColor = try {
        Color(android.graphics.Color.parseColor(dial.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val icon: ImageVector = getSpeedDialIcon(dial.iconCategory)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("speed_dial_${dial.title.lowercase()}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = ExpressiveSquircleShape,
                color = dialColor.copy(alpha = 0.16f),
                border = if (isManageMode) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else BorderStroke(1.dp, dialColor.copy(alpha = 0.25f)),
                shadowElevation = 2.dp,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = dial.title,
                        tint = dialColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (isManageMode) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove ${dial.title}",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${dial.title}",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = dial.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AddSpeedDialGridItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("btn_speed_dial_grid_add")
    ) {
        Surface(
            shape = ExpressiveSquircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add shortcut",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}
