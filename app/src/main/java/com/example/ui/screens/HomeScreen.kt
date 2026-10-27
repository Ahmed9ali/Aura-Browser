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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserSettings
import com.example.model.BrowserTab
import com.example.model.HistoryItem
import com.example.model.NavScreen
import com.example.model.SpeedDial
import com.example.model.TabGroup
import com.example.ui.components.SpeedDialEditDialog
import com.example.ui.components.getSpeedDialIcon

data class DiscoverStory(
    val title: String,
    val source: String,
    val timeAgo: String,
    val tag: String,
    val tagColor: Color,
    val targetUrl: String
)

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
        targetValue = if (hasActiveSections) 24.dp else 120.dp,
        label = "header_top_padding"
    )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 90.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        // Hero Header & Aura Brand
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(top = headerTopPadding, bottom = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Orbital Brand Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Aura Browser",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Aura Browser",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Fast, organized tab workflows & smart discovery",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Prominent Quick Search Bar trigger with clear contrast in light/dark themes
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        tonalElevation = 4.dp,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onNavigateToSearch)
                            .testTag("home_search_bar_trigger")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
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
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search or type web address...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Tab Groups Quick Strip (Requirement: Tab management with grouping capabilities)
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Tab Groups",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "(${tabGroups.size})",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Manage All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
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
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            val tabsInGroup = tabs.count { it.groupId == group.id }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = groupColor.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onNavigateToTabs)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(groupColor)
                                    )
                                    Column {
                                        Text(
                                            text = group.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$tabsInGroup active ${if (tabsInGroup == 1) "tab" else "tabs"}",
                                            fontSize = 11.sp,
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Speed Dials",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "(${speedDials.size})",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Add button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddSpeedDialDialog = true }
                                    .testTag("btn_home_add_speed_dial")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add shortcut",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Add",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Manage/Done button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isManageMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isManageMode = !isManageMode }
                                    .testTag("btn_toggle_manage_speed_dials")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isManageMode) Icons.Default.Check else Icons.Default.Edit,
                                        contentDescription = if (isManageMode) "Done managing" else "Manage speed dials",
                                        tint = if (isManageMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isManageMode) "Done" else "Manage",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Discover Feed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    discoverStories.forEach { story ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenUrl(story.targetUrl) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = story.tagColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = story.tag,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = story.tagColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = story.timeAgo,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = story.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = story.source,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Read",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
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
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val icon: ImageVector = getSpeedDialIcon(dial.iconCategory)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("speed_dial_${dial.title.lowercase()}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = dialColor.copy(alpha = 0.15f),
                border = if (isManageMode) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.size(52.dp)
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
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("btn_speed_dial_grid_add")
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add shortcut",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}
