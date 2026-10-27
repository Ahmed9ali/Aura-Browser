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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * and high-contrast tonal container sections.
 */
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

    val hasActiveSections = settings.showTabGroups || settings.showSpeedDials || settings.showDiscoverFeed
    val headerTopPadding by animateDpAsState(
        targetValue = if (hasActiveSections) 20.dp else 110.dp,
        label = "header_top_padding"
    )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        // Expressive Hero Header & Aura Brand
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(top = headerTopPadding, bottom = 18.dp, start = 20.dp, end = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Expressive Squircle Brand Emblem
                    Surface(
                        shape = ExpressiveSquircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Aura Browser",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Aura Browser",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = ExpressiveChipShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Fast · Private · Automated",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Expressive Omnibox Search Bar Trigger
                    Surface(
                        shape = ExpressivePillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(ExpressivePillShape)
                            .clickable(onClick = onNavigateToSearch)
                            .testTag("home_search_bar_trigger")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search or type web address...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Tab Groups Quick Strip
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

        // Speed Dials Section (Customizable and manageable shortcuts)
        if (settings.showSpeedDials) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
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
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Speed Dials",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = ExpressiveChipShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "${speedDials.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Add button
                            Surface(
                                shape = ExpressivePillShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clip(ExpressivePillShape)
                                    .clickable { showAddSpeedDialDialog = true }
                                    .testTag("btn_home_add_speed_dial")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add shortcut",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Manage/Done button
                            Surface(
                                shape = ExpressivePillShape,
                                color = if (isManageMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clip(ExpressivePillShape)
                                    .clickable { isManageMode = !isManageMode }
                                    .testTag("btn_toggle_manage_speed_dials")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isManageMode) Icons.Default.Check else Icons.Default.Edit,
                                        contentDescription = if (isManageMode) "Done managing" else "Manage speed dials",
                                        tint = if (isManageMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isManageMode) "Done" else "Manage",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isManageMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Grid of Speed dials + Add Shortcut tile
                    val chunkedDials = speedDials.chunked(4)
                    chunkedDials.forEachIndexed { rowIndex, rowItems ->
                        val isLastRow = rowIndex == chunkedDials.size - 1
                        val canAppendAddTile = isLastRow && rowItems.size < 4

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            rowItems.forEach { dial ->
                                SpeedDialItem(
                                    dial = dial,
                                    isManageMode = isManageMode,
                                    onClick = {
                                        if (isManageMode) {
                                            speedDialToEdit = dial
                                        } else {
                                            onOpenUrl(dial.url)
                                        }
                                    },
                                    onEdit = { speedDialToEdit = dial },
                                    onDelete = { onDeleteSpeedDial(dial.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (canAppendAddTile) {
                                AddSpeedDialGridItem(
                                    onClick = { showAddSpeedDialDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                val remainingSpace = 4 - (rowItems.size + 1)
                                repeat(remainingSpace) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            } else {
                                val fillCount = 4 - rowItems.size
                                repeat(fillCount) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // If speedDials is empty or perfectly filled with 4-item multiple, show Add tile row
                    if (speedDials.isEmpty() || speedDials.size % 4 == 0) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            AddSpeedDialGridItem(
                                onClick = { showAddSpeedDialDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                            repeat(3) {
                                Spacer(modifier = Modifier.weight(1f))
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
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
