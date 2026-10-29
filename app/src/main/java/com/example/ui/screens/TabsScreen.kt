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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Tab
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTabIds by remember { mutableStateOf(setOf<String>()) }
    var showBottomMoreMenu by remember { mutableStateOf(false) }
    var showGroupSelectedDialog by remember { mutableStateOf(false) }

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

            // Below the Top App Bar: 2-Column Grid View of Tab Cards or Group Tabs
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 14.dp,
                    bottom = 96.dp // Space for bottom-left FAB + 3 dots
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (topMode) {
                    TabsTopMode.TABS -> {
                        items(
                            items = tabs,
                            key = { tab -> "tab_grid_${tab.id}" }
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
                                onMove = { onOpenMoveTabDialog(tab) }
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
        }

        // Bottom Bar: Add Tab FAB at Center, 3-Dots Button at Right Side
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Add Tab FAB Icon at Center
            FloatingActionButton(
                onClick = { onOpenNewTab(null) },
                shape = ExpressiveSquircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .testTag("btn_new_tab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Tab",
                    modifier = Modifier.size(26.dp)
                )
            }

            // 3-Dots Menu Button at Right Side
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { showBottomMoreMenu = true }
                        .testTag("btn_tabs_more_menu")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More Tab Options",
                            tint = MaterialTheme.colorScheme.onSurface,
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

    Card(
        shape = morphShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.5.dp, groupColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header (Organized by Name)
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAutoNameGroup) {
                        Text(
                            text = "Organized by name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = ExpressivePillShape,
                    color = groupColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "${groupTabs.size} ${if (groupTabs.size == 1) "tab" else "tabs"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = groupColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onAddTabToGroup,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_add_tab_to_${group.name.lowercase().replace(" ", "_")}"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = groupColor.copy(alpha = 0.15f),
                        contentColor = groupColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add tab to ${group.name}",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteGroup,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete group ${group.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = if (group.isCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = if (group.isCollapsed) "Expand" else "Collapse",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Collapsible 2-Column Grid of Tabs inside Group (Sorted Alphabetically by Name)
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (groupTabs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tabs in this group yet. Tap + to open a tab here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        groupTabs.chunked(2).forEach { rowTabs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowTabs.forEach { tab ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        GridTabCard(
                                            tab = tab,
                                            isActive = tab.id == activeTabId,
                                            folderName = group.name,
                                            accentColor = groupColor,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = tab.id in selectedTabIds,
                                            onSelect = { onSelectTab(tab.id) },
                                            onClose = { onCloseTab(tab.id) },
                                            onMove = { onMoveTab(tab) }
                                        )
                                    }
                                }
                                if (rowTabs.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
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
private fun GridTabCard(
    tab: BrowserTab,
    isActive: Boolean,
    folderName: String?,
    accentColor: Color,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMove: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val domain = remember(tab.url) {
        if (tab.url.startsWith("aura://")) "Aura Home"
        else try {
            URI(tab.url).host?.removePrefix("www.") ?: tab.url
        } catch (_: Exception) {
            tab.url
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

    Surface(
        shape = ExpressiveSquircleShape,
        color = containerColor,
        border = borderStroke,
        tonalElevation = if (isActive) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .scale(scale)
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
                .padding(8.dp)
        ) {
            // Card Top Header: Favicon + Tab Title + Close Button (or Selection Checkbox)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(18.dp)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = if (isActive) accentColor else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(22.dp)
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
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = tab.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.labelLarge.copy(
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
                        .size(26.dp)
                        .testTag("btn_close_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close Tab",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                        .padding(10.dp)
                ) {
                    // Center Domain & Page Preview
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = ExpressiveChipShape,
                            color = accentColor.copy(alpha = 0.14f),
                            modifier = Modifier.size(36.dp)
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = domain,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = tab.previewSnippet.ifBlank { tab.url },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
