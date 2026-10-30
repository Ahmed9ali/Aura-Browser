package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.example.model.BrowserTab
import com.example.model.TabGroup
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSquircleShape
import com.example.ui.theme.ExpressiveTileShape
import com.example.ui.theme.groupedListCardShape
import java.net.URI

private enum class TabsTopMode {
    TABS,
    GROUP_TABS
}

private enum class TabGridDensity {
    AUTO,
    STANDARD_2_COL,
    COMPACT_3_COL
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    activeTab: BrowserTab?,
    tabGroups: List<TabGroup>,
    showCreateGroupDialog: Boolean,
    tabToMove: BrowserTab?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCloseSelectedTabs: (Set<String>) -> Unit = { ids -> ids.forEach(onCloseTab) },
    onOpenNewTab: (String?) -> Unit,
    onCloseAllTabs: () -> Unit,
    onClearBrowsingData: () -> Unit = onCloseAllTabs,
    onCloseAllInGroup: (String) -> Unit,
    onCreateGroup: (String, String) -> Unit,
    onCreateGroupWithTabs: (String, String, Set<String>) -> Unit = { name, color, _ ->
        onCreateGroup(name, color)
    },
    onCreateGroupAndAssignTab: (String, String, String) -> Unit = { _, name, color ->
        onCreateGroup(name, color)
    },
    onDeleteGroup: (String) -> Unit,
    onToggleGroupCollapse: (String) -> Unit,
    onOpenCreateGroupDialog: () -> Unit,
    onCloseCreateGroupDialog: () -> Unit,
    onOpenMoveTabDialog: (BrowserTab) -> Unit,
    onCloseMoveTabDialog: () -> Unit,
    onAssignTabToGroup: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var topMode by rememberSaveable { mutableStateOf(TabsTopMode.TABS) }
    var gridDensity by rememberSaveable { mutableStateOf(TabGridDensity.AUTO) }
    var tabSearchQuery by rememberSaveable { mutableStateOf("") }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTabIds by remember { mutableStateOf(setOf<String>()) }
    var showBottomMoreMenu by remember { mutableStateOf(false) }
    var showGroupSelectedDialog by remember { mutableStateOf(false) }
    val mainGridState = rememberLazyGridState()

    val isCompactGrid = when (gridDensity) {
        TabGridDensity.AUTO -> tabs.size >= 8
        TabGridDensity.COMPACT_3_COL -> true
        TabGridDensity.STANDARD_2_COL -> false
    }

    val gridColumns = remember(gridDensity, isCompactGrid) {
        when (gridDensity) {
            TabGridDensity.STANDARD_2_COL -> GridCells.Fixed(2)
            TabGridDensity.COMPACT_3_COL -> GridCells.Fixed(3)
            TabGridDensity.AUTO -> GridCells.Adaptive(minSize = if (isCompactGrid) 114.dp else 156.dp)
        }
    }

    val filteredTabs = remember(tabs, tabSearchQuery) {
        if (tabSearchQuery.isBlank()) {
            tabs
        } else {
            val q = tabSearchQuery.trim().lowercase()
            tabs.filter { tab ->
                tab.title.lowercase().contains(q) || tab.url.lowercase().contains(q)
            }
        }
    }

    // Keep selectedTabIds valid when tabs close
    val validSelectedTabIds = remember(selectedTabIds, tabs) {
        val currentIds = tabs.map { it.id }.toSet()
        selectedTabIds.intersect(currentIds)
    }

    // Named groups sorted alphabetically by name
    val sortedNamedGroups = remember(tabGroups) {
        tabGroups.sortedBy { it.name.lowercase() }
    }

    // Tabs organized by name for the Group Tab view
    val ungroupedTabsSortedByName = remember(tabs, tabGroups) {
        val validGroupIds = tabGroups.map { it.id }.toSet()
        tabs
            .filter { it.groupId == null || it.groupId !in validGroupIds }
            .sortedBy { extractTabDisplayName(it).lowercase() }
    }

    // Automatically group ungrouped tabs by their Name / Site Title in Group Tab view
    val autoNameGroupedTabs = remember(ungroupedTabsSortedByName) {
        ungroupedTabsSortedByName.groupBy { tab ->
            extractTabGroupNameByTitle(tab)
        }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    var collapsedAutoGroups by remember { mutableStateOf(setOf<String>()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tabs_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar: 2 Icons (Tab and Group Tab) instead of text
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Left-side Grid Density Toggle + Tab Counter Pill for efficient multi-tab viewing
                    Surface(
                        shape = ExpressivePillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clip(ExpressivePillShape)
                            .clickable {
                                gridDensity = when (gridDensity) {
                                    TabGridDensity.AUTO -> TabGridDensity.COMPACT_3_COL
                                    TabGridDensity.COMPACT_3_COL -> TabGridDensity.STANDARD_2_COL
                                    TabGridDensity.STANDARD_2_COL -> TabGridDensity.AUTO
                                }
                            }
                            .testTag("btn_toggle_grid_density")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompactGrid) Icons.Rounded.ViewModule else Icons.Rounded.GridView,
                                contentDescription = "Toggle Grid Density",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (gridDensity) {
                                    TabGridDensity.AUTO -> "${tabs.size}"
                                    TabGridDensity.COMPACT_3_COL -> "3×"
                                    TabGridDensity.STANDARD_2_COL -> "2×"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Centered 2-Icon Switcher: Tab icon and Group Tab icon
                    Surface(
                        shape = ExpressivePillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Tab Icon
                            val isTabsSelected = topMode == TabsTopMode.TABS
                            val tabIconBg by animateColorAsState(
                                targetValue = if (isTabsSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "tab_mode_bg"
                            )
                            val tabIconTint by animateColorAsState(
                                targetValue = if (isTabsSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "tab_mode_tint"
                            )

                            Surface(
                                shape = ExpressivePillShape,
                                color = tabIconBg,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 40.dp)
                                    .clip(ExpressivePillShape)
                                    .clickable { topMode = TabsTopMode.TABS }
                                    .testTag("btn_mode_tabs")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isTabsSelected) Icons.Rounded.Tab else Icons.Outlined.Tab,
                                        contentDescription = "Tabs Grid View",
                                        tint = tabIconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // 2. Group Tab Icon
                            val isGroupsSelected = topMode == TabsTopMode.GROUP_TABS
                            val groupIconBg by animateColorAsState(
                                targetValue = if (isGroupsSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "group_mode_bg"
                            )
                            val groupIconTint by animateColorAsState(
                                targetValue = if (isGroupsSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "group_mode_tint"
                            )

                            Surface(
                                shape = ExpressivePillShape,
                                color = groupIconBg,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 40.dp)
                                    .clip(ExpressivePillShape)
                                    .clickable { topMode = TabsTopMode.GROUP_TABS }
                                    .testTag("btn_mode_group_tabs")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isGroupsSelected) Icons.Rounded.Folder else Icons.Outlined.Folder,
                                        contentDescription = "Group Tabs",
                                        tint = groupIconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right-side New Group icon button
                    FilledTonalIconButton(
                        onClick = onOpenCreateGroupDialog,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                            .testTag("btn_new_group")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = "New Group",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Multi-Select Action Bar (visible when "Select tab" is chosen from the 3-dots menu)
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedTabIds = emptySet()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Exit selection",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${validSelectedTabIds.size} selected",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    selectedTabIds = if (validSelectedTabIds.size == tabs.size) {
                                        emptySet()
                                    } else {
                                        tabs.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (validSelectedTabIds.size == tabs.size) "None" else "All",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = { showGroupSelectedDialog = true },
                                enabled = validSelectedTabIds.isNotEmpty(),
                                shape = ExpressivePillShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderSpecial,
                                    contentDescription = "Group selected tabs",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Group", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledIconButton(
                                onClick = {
                                    onCloseSelectedTabs(validSelectedTabIds)
                                    selectedTabIds = emptySet()
                                    isSelectionMode = false
                                },
                                enabled = validSelectedTabIds.isNotEmpty(),
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Close selected tabs",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Filter Bar when many tabs are open in Tabs mode
            AnimatedVisibility(
                visible = topMode == TabsTopMode.TABS && (tabs.size >= 6 || tabSearchQuery.isNotBlank()),
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = ExpressivePillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Filter open tabs",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (tabSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Filter ${tabs.size} open tabs...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = tabSearchQuery,
                                        onValueChange = { tabSearchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_filter_tabs")
                                    )
                                }
                                if (tabSearchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear tab filter",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { tabSearchQuery = "" }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Below the Top App Bar: Space-Efficient LazyVerticalGrid of Tab Preview Cards or Group Tabs
            LazyVerticalGrid(
                state = mainGridState,
                columns = gridColumns,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tabs_lazy_vertical_grid"),
                contentPadding = PaddingValues(
                    start = if (isCompactGrid) 10.dp else 14.dp,
                    end = if (isCompactGrid) 10.dp else 14.dp,
                    top = 12.dp,
                    bottom = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactGrid) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompactGrid) 8.dp else 12.dp)
            ) {
                when (topMode) {
                    TabsTopMode.TABS -> {
                        items(
                            items = filteredTabs,
                            key = { tab -> "tab_grid_${tab.id}" },
                            contentType = { "tab_preview_card" }
                        ) { tab ->
                            val assignedGroup = tabGroups.find { it.id == tab.groupId }
                            val groupColor = assignedGroup?.let { parseHexColor(it.colorHex) }
                                ?: MaterialTheme.colorScheme.primary
                            val isSelectedForBatch = tab.id in validSelectedTabIds

                            GridTabCard(
                                tab = tab,
                                isActive = tab.id == activeTab?.id,
                                folderName = assignedGroup?.name,
                                accentColor = groupColor,
                                isCompact = isCompactGrid,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelectedForBatch,
                                onSelect = {
                                    if (isSelectionMode) {
                                        selectedTabIds = if (isSelectedForBatch) {
                                            validSelectedTabIds - tab.id
                                        } else {
                                            validSelectedTabIds + tab.id
                                        }
                                    } else {
                                        onSelectTab(tab.id)
                                    }
                                },
                                onClose = { onCloseTab(tab.id) },
                                onMove = { onOpenMoveTabDialog(tab) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    TabsTopMode.GROUP_TABS -> {
                        // Named Groups Sorted Alphabetically by Name
                        if (sortedNamedGroups.isNotEmpty()) {
                            items(
                                items = sortedNamedGroups,
                                key = { "named_group_${it.id}" },
                                span = { GridItemSpan(maxLineSpan) }
                            ) { group ->
                                val groupTabs = tabs
                                    .filter { it.groupId == group.id }
                                    .sortedBy { extractTabDisplayName(it).lowercase() }

                                TabGroupCard(
                                    group = group,
                                    groupTabs = groupTabs,
                                    activeTabId = activeTab?.id,
                                    isSelectionMode = isSelectionMode,
                                    selectedTabIds = validSelectedTabIds,
                                    onToggleCollapse = { onToggleGroupCollapse(group.id) },
                                    onAddTabToGroup = { onOpenNewTab(group.id) },
                                    onDeleteGroup = { onDeleteGroup(group.id) },
                                    onSelectTab = { tabId ->
                                        if (isSelectionMode) {
                                            selectedTabIds = if (tabId in validSelectedTabIds) {
                                                validSelectedTabIds - tabId
                                            } else {
                                                validSelectedTabIds + tabId
                                            }
                                        } else {
                                            onSelectTab(tabId)
                                        }
                                    },
                                    onCloseTab = onCloseTab,
                                    onMoveTab = onOpenMoveTabDialog
                                )
                            }
                        }

                        // Automatically organize remaining tabs by Name
                        autoNameGroupedTabs.entries.forEachIndexed { index, (groupName, groupTabs) ->
                            item(
                                key = "auto_name_group_$groupName",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                val palette = listOf(
                                    "#4285F4",
                                    "#34A853",
                                    "#A142F4",
                                    "#FBBC04",
                                    "#00ACC1",
                                    "#EA4335"
                                )
                                val colorHex = palette[index % palette.size]
                                val isCollapsed = groupName in collapsedAutoGroups
                                val virtualGroup = TabGroup(
                                    id = "auto_$groupName",
                                    name = groupName,
                                    colorHex = colorHex,
                                    isCollapsed = isCollapsed
                                )

                                TabGroupCard(
                                    group = virtualGroup,
                                    groupTabs = groupTabs,
                                    activeTabId = activeTab?.id,
                                    isSelectionMode = isSelectionMode,
                                    selectedTabIds = validSelectedTabIds,
                                    isAutoNameGroup = true,
                                    onToggleCollapse = {
                                        collapsedAutoGroups = if (isCollapsed) {
                                            collapsedAutoGroups - groupName
                                        } else {
                                            collapsedAutoGroups + groupName
                                        }
                                    },
                                    onAddTabToGroup = { onOpenNewTab(null) },
                                    onDeleteGroup = {
                                        groupTabs.forEach { t -> onCloseTab(t.id) }
                                    },
                                    onSelectTab = { tabId ->
                                        if (isSelectionMode) {
                                            selectedTabIds = if (tabId in validSelectedTabIds) {
                                                validSelectedTabIds - tabId
                                            } else {
                                                validSelectedTabIds + tabId
                                            }
                                        } else {
                                            onSelectTab(tabId)
                                        }
                                    },
                                    onCloseTab = onCloseTab,
                                    onMoveTab = onOpenMoveTabDialog
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Bar for Tab Screen: ONLY Add (+) in Center and 3-Dots on Right Side
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_bar")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        // Center: Add (+) Icon in Bottom Navigation
                        Surface(
                            shape = ExpressivePillShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = 68.dp, height = 46.dp)
                                .clip(ExpressivePillShape)
                                .clickable { onOpenNewTab(null) }
                                .testTag("btn_new_tab")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add Tab",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Right Side: 3-Dots Icon in Bottom Navigation
                        Box(
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Surface(
                                shape = ExpressivePillShape,
                                color = if (showBottomMoreMenu) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .size(width = 52.dp, height = 46.dp)
                                    .clip(ExpressivePillShape)
                                    .clickable { showBottomMoreMenu = true }
                                    .testTag("btn_tabs_more_menu")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "More Tab Options",
                                        tint = if (showBottomMoreMenu) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showBottomMoreMenu,
                                onDismissRequest = { showBottomMoreMenu = false },
                                offset = DpOffset(x = 0.dp, y = (-8).dp),
                                shape = ExpressiveSquircleShape,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Close all tabs",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteSweep,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showBottomMoreMenu = false
                                        isSelectionMode = false
                                        selectedTabIds = emptySet()
                                        onCloseAllTabs()
                                    },
                                    modifier = Modifier.testTag("menu_close_all_tabs")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isSelectionMode) "Exit select tab" else "Select tab",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Checklist,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showBottomMoreMenu = false
                                        isSelectionMode = !isSelectionMode
                                        if (!isSelectionMode) {
                                            selectedTabIds = emptySet()
                                        }
                                    },
                                    modifier = Modifier.testTag("menu_select_tab")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Clear data",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.CleaningServices,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        showBottomMoreMenu = false
                                        isSelectionMode = false
                                        selectedTabIds = emptySet()
                                        onClearBrowsingData()
                                    },
                                    modifier = Modifier.testTag("menu_clear_data")
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Group tabs by name",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.SortByAlpha,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showBottomMoreMenu = false
                                        topMode = TabsTopMode.GROUP_TABS
                                    },
                                    modifier = Modifier.testTag("menu_group_by_name")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "New tab group",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.CreateNewFolder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showBottomMoreMenu = false
                                        onOpenCreateGroupDialog()
                                    },
                                    modifier = Modifier.testTag("menu_new_group")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = onCloseCreateGroupDialog,
            onConfirm = { name, color ->
                onCreateGroup(name, color)
                topMode = TabsTopMode.GROUP_TABS
            }
        )
    }

    if (showGroupSelectedDialog) {
        CreateGroupDialog(
            onDismiss = { showGroupSelectedDialog = false },
            onConfirm = { name, color ->
                onCreateGroupWithTabs(name, color, validSelectedTabIds)
                showGroupSelectedDialog = false
                isSelectionMode = false
                selectedTabIds = emptySet()
                topMode = TabsTopMode.GROUP_TABS
            }
        )
    }

    if (tabToMove != null) {
        MoveTabDialog(
            tab = tabToMove,
            groups = sortedNamedGroups,
            tabs = tabs,
            onDismiss = onCloseMoveTabDialog,
            onSelectGroup = { groupId ->
                onAssignTabToGroup(tabToMove.id, groupId)
            },
            onCreateFolderAndMove = { folderName, colorHex ->
                onCreateGroupAndAssignTab(tabToMove.id, folderName, colorHex)
                topMode = TabsTopMode.GROUP_TABS
            }
        )
    }
}

private fun extractTabDisplayName(tab: BrowserTab): String {
    val cleanTitle = tab.title.trim()
    if (cleanTitle.isNotEmpty() && !cleanTitle.equals("New Tab", ignoreCase = true)) {
        return cleanTitle
    }
    if (tab.url.startsWith("aura://")) return "Aura Home"
    return runCatching {
        URI(tab.url).host?.removePrefix("www.") ?: tab.url
    }.getOrDefault(tab.url)
}

private fun extractTabGroupNameByTitle(tab: BrowserTab): String {
    val display = extractTabDisplayName(tab)
    if (tab.url.startsWith("aura://")) return "Aura Home"
    val host = runCatching {
        URI(tab.url).host?.removePrefix("www.")?.substringBefore(".")?.replaceFirstChar { it.uppercase() }
    }.getOrNull()
    if (!host.isNullOrBlank()) {
        return host
    }
    return display.split(" ", "-", "|").firstOrNull()?.trim()?.ifBlank { "General" } ?: "General"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabGroupCard(
    group: TabGroup,
    groupTabs: List<BrowserTab>,
    activeTabId: String?,
    isSelectionMode: Boolean = false,
    selectedTabIds: Set<String> = emptySet(),
    isAutoNameGroup: Boolean = false,
    onToggleCollapse: () -> Unit,
    onAddTabToGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onMoveTab: (BrowserTab) -> Unit
) {
    val groupColor = parseHexColor(group.colorHex)
    val morphShape = if (group.isCollapsed) ExpressiveSquircleShape else ExpressiveCardShape
    var groupFilterQuery by rememberSaveable(group.id) { mutableStateOf("") }
    var isDenseThreeColumn by rememberSaveable(group.id) { mutableStateOf(groupTabs.size >= 12) }

    val filteredGroupTabs = remember(groupTabs, groupFilterQuery) {
        val q = groupFilterQuery.trim().lowercase()
        if (q.isEmpty()) {
            groupTabs
        } else {
            groupTabs.filter { tab ->
                tab.title.lowercase().contains(q) || tab.url.lowercase().contains(q)
            }
        }
    }

    val containsActiveTab = remember(groupTabs, activeTabId) {
        activeTabId != null && groupTabs.any { it.id == activeTabId }
    }

    Card(
        shape = morphShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = if (containsActiveTab) 2.dp else 1.5.dp,
            color = groupColor.copy(alpha = if (containsActiveTab) 0.65f else 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header (Formatted & Compact)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                groupColor.copy(alpha = 0.16f),
                                groupColor.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .clickable(onClick = onToggleCollapse)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = groupColor.copy(alpha = 0.22f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (group.isCollapsed) Icons.Rounded.Folder else Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = groupColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (containsActiveTab) {
                            Surface(
                                shape = CircleShape,
                                color = groupColor,
                                modifier = Modifier.size(7.dp)
                            ) {}
                        }
                    }
                    Text(
                        text = if (isAutoNameGroup) {
                            "Organized by name • Compact group view"
                        } else {
                            "Bounded group • ${groupTabs.size} ${if (groupTabs.size == 1) "tab" else "tabs"}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = ExpressivePillShape,
                    color = groupColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "${groupTabs.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = groupColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onAddTabToGroup,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_add_tab_to_${group.name.lowercase().replace(" ", "_")}"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = groupColor.copy(alpha = 0.15f),
                        contentColor = groupColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add tab to ${group.name}",
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteGroup,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete group ${group.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Icon(
                    imageVector = if (group.isCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = if (group.isCollapsed) "Expand" else "Collapse",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Collapsed Summary Strip: shows a compact 1-line preview of tabs without increasing length
            if (group.isCollapsed && groupTabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleCollapse)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewSlice = groupTabs.take(3)
                    previewSlice.forEach { tab ->
                        val isTabActive = tab.id == activeTabId
                        Surface(
                            shape = ExpressivePillShape,
                            color = if (isTabActive) {
                                groupColor.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                0.5.dp,
                                if (isTabActive) groupColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (tab.url.startsWith("aura://")) Icons.Rounded.AutoAwesome else Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = groupColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = extractTabDisplayName(tab),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (groupTabs.size > 3) {
                        Surface(
                            shape = ExpressivePillShape,
                            color = groupColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "+${groupTabs.size - 3} more",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = groupColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Expanded Bounded-Height Formatted Grid for 100s of Tabs
            AnimatedVisibility(
                visible = !group.isCollapsed,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (groupTabs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tabs in this group yet. Tap + to open a tab here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Compact Group Toolbar (Filter + 2x/3x Density Switcher + Fixed-Height Indicator)
                        if (groupTabs.size > 4 || groupFilterQuery.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        0.8.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = groupColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (groupFilterQuery.isEmpty()) {
                                                Text(
                                                    text = "Filter ${groupTabs.size} tabs in ${group.name}...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            BasicTextField(
                                                value = groupFilterQuery,
                                                onValueChange = { groupFilterQuery = it },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.labelMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        if (groupFilterQuery.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Clear group filter",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clickable { groupFilterQuery = "" }
                                            )
                                        }
                                    }
                                }

                                // Density Toggle (2-column compact vs 3-column ultra-dense for 100s of tabs)
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = groupColor.copy(alpha = 0.14f),
                                    border = BorderStroke(0.8.dp, groupColor.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(ExpressivePillShape)
                                        .clickable { isDenseThreeColumn = !isDenseThreeColumn }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDenseThreeColumn) Icons.Rounded.ViewModule else Icons.Rounded.GridView,
                                            contentDescription = "Toggle group grid density",
                                            tint = groupColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isDenseThreeColumn) "3×" else "2×",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = groupColor
                                        )
                                    }
                                }
                            }
                        }

                        val columnsCount = if (isDenseThreeColumn) 3 else 2
                        val itemHeight = if (isDenseThreeColumn) 48.dp else 56.dp
                        val rowSpacing = 6.dp
                        val rowCount = (filteredGroupTabs.size + columnsCount - 1) / columnsCount
                        val naturalHeight = if (rowCount == 0) {
                            44.dp
                        } else {
                            (itemHeight * rowCount) + (rowSpacing * (rowCount - 1))
                        }
                        // Strict maximum height cap (228.dp) so even 100s of tabs NEVER increase the group card length
                        val boundedGridHeight = naturalHeight.coerceAtMost(228.dp)

                        if (filteredGroupTabs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching tabs in ${group.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnsCount),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(boundedGridHeight),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(rowSpacing)
                            ) {
                                items(
                                    items = filteredGroupTabs,
                                    key = { tab -> "group_${group.id}_tab_${tab.id}" }
                                ) { tab ->
                                    GroupFormattedTabTile(
                                        tab = tab,
                                        isActive = tab.id == activeTabId,
                                        accentColor = groupColor,
                                        isDense = isDenseThreeColumn,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = tab.id in selectedTabIds,
                                        onSelect = { onSelectTab(tab.id) },
                                        onClose = { onCloseTab(tab.id) },
                                        onMove = { onMoveTab(tab) }
                                    )
                                }
                            }
                        }

                        // Compact footer indicator when group has more rows than visible height
                        if (filteredGroupTabs.size > columnsCount * 4) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredGroupTabs.size} tabs formatted in fixed-height container",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Scroll inside group ↕",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = groupColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun GroupFormattedTabTile(
    tab: BrowserTab,
    isActive: Boolean,
    accentColor: Color,
    isDense: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMove: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val domain = remember(tab.url) {
        if (tab.url.startsWith("aura://")) "Aura Home"
        else runCatching {
            URI(tab.url).host?.removePrefix("www.") ?: tab.url
        }.getOrDefault(tab.url)
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            isActive -> accentColor.copy(alpha = 0.16f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "group_tile_bg"
    )

    val borderStroke = when {
        isSelectionMode && isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        isActive -> BorderStroke(1.5.dp, accentColor)
        else -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Surface(
        shape = ExpressiveChipShape,
        color = containerColor,
        border = borderStroke,
        tonalElevation = if (isActive) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isDense) 48.dp else 56.dp)
            .clip(ExpressiveChipShape)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMove()
                }
            )
            .testTag("tab_card_${tab.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isDense) 6.dp else 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            } else {
                Surface(
                    shape = CircleShape,
                    color = if (isActive) accentColor else accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(if (isDense) 20.dp else 24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (tab.url.startsWith("aura://")) {
                                Icons.Rounded.AutoAwesome
                            } else {
                                Icons.Rounded.Language
                            },
                            contentDescription = null,
                            tint = if (isActive) Color.White else accentColor,
                            modifier = Modifier.size(if (isDense) 11.dp else 13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tab.title.ifBlank { domain },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = if (isDense) 11.sp else 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isDense) {
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isDense) {
                IconButton(
                    onClick = onMove,
                    modifier = Modifier
                        .size(22.dp)
                        .testTag("btn_move_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                        contentDescription = "Move Tab to Group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(22.dp)
                    .testTag("btn_close_tab_${tab.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close Tab",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GridTabCard(
    tab: BrowserTab,
    isActive: Boolean,
    folderName: String?,
    accentColor: Color,
    isCompact: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val cardHeight = if (isCompact) 138.dp else 174.dp
    val domain = remember(tab.url) {
        if (tab.url.startsWith("aura://")) "Aura Home"
        else try {
            URI(tab.url).host?.removePrefix("www.") ?: tab.url
        } catch (_: Exception) {
            tab.url
        }
    }

    val dismissState = key(tab.id) {
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (!isSelectionMode && (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart)) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClose()
                    true
                } else {
                    false
                }
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.36f }
        )
    }

    var hasTriggeredThresholdHaptic by remember(tab.id) { mutableStateOf(false) }
    LaunchedEffect(dismissState.targetValue, isSelectionMode) {
        if (isSelectionMode) {
            hasTriggeredThresholdHaptic = false
            return@LaunchedEffect
        }
        val crossedThreshold = dismissState.targetValue != SwipeToDismissBoxValue.Settled
        if (crossedThreshold && !hasTriggeredThresholdHaptic) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            hasTriggeredThresholdHaptic = true
        } else if (!crossedThreshold && hasTriggeredThresholdHaptic) {
            hasTriggeredThresholdHaptic = false
        }
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            isActive -> accentColor.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "grid_tab_bg"
    )

    val borderStroke = when {
        isSelectionMode && isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isActive -> BorderStroke(2.dp, accentColor)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    }

    val scale by animateFloatAsState(
        targetValue = if (isActive || isSelected) 1.02f else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "grid_tab_scale"
    )

    val isPastThreshold = dismissState.targetValue != SwipeToDismissBoxValue.Settled
    val dismissBgColor by animateColorAsState(
        targetValue = when {
            dismissState.dismissDirection == SwipeToDismissBoxValue.Settled -> Color.Transparent
            isPastThreshold -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "swipe_dismiss_bg"
    )
    val dismissContentColor by animateColorAsState(
        targetValue = if (isPastThreshold) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "swipe_dismiss_content"
    )
    val dismissIconScale by animateFloatAsState(
        targetValue = if (isPastThreshold) 1.16f else 0.9f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "swipe_dismiss_icon_scale"
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        modifier = modifier
            .fillMaxWidth()
            .testTag("swipe_tab_card_${tab.id}"),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(cardHeight)
                        .clip(ExpressiveSquircleShape)
                        .background(dismissBgColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = if (isPastThreshold) 0.9f else 0.35f),
                            shape = ExpressiveSquircleShape
                        )
                        .padding(horizontal = if (isCompact) 10.dp else 16.dp),
                    contentAlignment = alignment
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.scale(dismissIconScale)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = dismissContentColor.copy(alpha = 0.16f),
                            modifier = Modifier.size(if (isCompact) 30.dp else 38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Swipe to close tab",
                                    tint = dismissContentColor,
                                    modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = dismissContentColor
                        )
                    }
                }
            }
        }
    ) {
        Surface(
            shape = ExpressiveSquircleShape,
            color = containerColor,
            border = borderStroke,
            tonalElevation = if (isActive) 4.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .graphicsLayer {
                    val rawOffset = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
                    val dragFraction = (abs(rawOffset) / 260f).coerceIn(0f, 1f)
                    val dynamicScale = scale * (1f - dragFraction * 0.05f)
                    scaleX = dynamicScale
                    scaleY = dynamicScale
                    rotationZ = (rawOffset / 48f).coerceIn(-6.5f, 6.5f)
                    alpha = 1f - (dragFraction * 0.25f)
                }
                .clip(ExpressiveSquircleShape)
                .combinedClickable(
                    onClick = onSelect,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMove()
                    }
                )
                .testTag("tab_card_${tab.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isCompact) 6.dp else 8.dp)
            ) {
                // Card Top Header: Favicon + Tab Title + Close Button (or Selection Checkbox)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(if (isCompact) 16.dp else 18.dp)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) accentColor else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (tab.url.startsWith("aura://")) {
                                        Icons.Rounded.AutoAwesome
                                    } else {
                                        Icons.Rounded.Language
                                    },
                                    contentDescription = null,
                                    tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
                    }

                    Text(
                        text = tab.title.ifBlank { "Untitled" },
                        style = (if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge).copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(if (isCompact) 22.dp else 26.dp)
                            .testTag("btn_close_tab_${tab.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close Tab",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isCompact) 13.dp else 15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

                // Webpage Preview Area inside Grid Card
                Surface(
                    shape = ExpressiveTileShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = if (isActive) 0.12f else 0.05f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(if (isCompact) 6.dp else 10.dp)
                    ) {
                        // Center Domain & Page Preview
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = ExpressiveChipShape,
                                color = accentColor.copy(alpha = 0.14f),
                                modifier = Modifier.size(if (isCompact) 26.dp else 34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (tab.url.startsWith("aura://")) {
                                            Icons.Rounded.AutoAwesome
                                        } else {
                                            Icons.Rounded.Language
                                        },
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(if (isCompact) 14.dp else 18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 5.dp))

                            Text(
                                text = domain,
                                style = (if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium).copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!isCompact) {
                                Text(
                                    text = tab.previewSnippet.ifBlank { tab.url },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                    // Bottom bar inside preview: Active / Group badge + Move button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isActive) {
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = accentColor
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (folderName != null) {
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = accentColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = folderName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (tab.isDesktopMode) {
                                Icon(
                                    imageVector = Icons.Rounded.DesktopWindows,
                                    contentDescription = "Desktop Mode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onMove,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("btn_move_tab_${tab.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                                contentDescription = "Move Tab to Group",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val colorOptions = listOf(
        "#4285F4" to "Blue",
        "#EA4335" to "Coral",
        "#34A853" to "Emerald",
        "#FBBC04" to "Amber",
        "#A142F4" to "Purple",
        "#00ACC1" to "Cyan"
    )
    var selectedColor by remember { mutableStateOf(colorOptions.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveSquircleShape,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CreateNewFolder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "New Tab Group",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name (e.g. Work, Shopping, Research)") },
                    singleLine = true,
                    shape = ExpressiveChipShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name")
                )

                Text(
                    text = "Group Accent Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { (hex, _) ->
                        val color = parseHexColor(hex)
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onConfirm(groupName, selectedColor)
                    }
                },
                enabled = groupName.isNotBlank(),
                shape = ExpressivePillShape,
                modifier = Modifier.testTag("btn_confirm_create_group")
            ) {
                Text("Create Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MoveTabDialog(
    tab: BrowserTab,
    groups: List<TabGroup>,
    tabs: List<BrowserTab>,
    onDismiss: () -> Unit,
    onSelectGroup: (String?) -> Unit,
    onCreateFolderAndMove: (String, String) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    val colorOptions = listOf(
        "#4285F4",
        "#EA4335",
        "#34A853",
        "#FBBC04",
        "#A142F4",
        "#00ACC1"
    )
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }
    val currentGroup = groups.find { it.id == tab.groupId }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveSquircleShape,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Organize Tab by Group Name",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = ExpressiveChipShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title.ifBlank { "Untitled Tab" },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (currentGroup != null) {
                                    "Current group: ${currentGroup.name}"
                                } else {
                                    "Currently ungrouped"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = ExpressiveSquircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NEW GROUP NAME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text("Enter group name & move...") },
                            singleLine = true,
                            shape = ExpressiveChipShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_quick_folder_name")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                colorOptions.forEach { hex ->
                                    val dotColor = parseHexColor(hex)
                                    val isSelected = selectedColor == hex
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (newFolderName.isNotBlank()) {
                                        onCreateFolderAndMove(newFolderName.trim(), selectedColor)
                                    }
                                },
                                enabled = newFolderName.isNotBlank(),
                                shape = ExpressivePillShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_create_and_move_tab")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Create & Move",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "GROUPS (SORTED BY NAME)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                val totalOptions = groups.size + 1
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    groups.forEachIndexed { index, group ->
                        val groupColor = parseHexColor(group.colorHex)
                        val isCurrent = tab.groupId == group.id
                        val countInGroup = tabs.count { it.groupId == group.id }
                        val itemShape = groupedListCardShape(index, totalOptions)

                        Surface(
                            shape = itemShape,
                            color = if (isCurrent) {
                                groupColor.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .clickable { onSelectGroup(group.id) }
                                .testTag("move_option_${group.name.lowercase().replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = groupColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$countInGroup ${if (countInGroup == 1) "tab" else "tabs"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = groupColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Option: None (Ungrouped Tab)
                    val isUngrouped = tab.groupId == null
                    val ungroupShape = groupedListCardShape(groups.size, totalOptions)
                    Surface(
                        shape = ungroupShape,
                        color = if (isUngrouped) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ungroupShape)
                            .clickable { onSelectGroup(null) }
                            .testTag("move_option_ungroup")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "None (Ungrouped Tab)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isUngrouped) FontWeight.Bold else FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isUngrouped) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF4285F4)
    }
}
