package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.BrowserTab
import com.example.model.TabGroup
import com.example.ui.theme.TabGroupColors

@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    activeTab: BrowserTab?,
    tabGroups: List<TabGroup>,
    showCreateGroupDialog: Boolean,
    tabToMove: BrowserTab?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenNewTab: (groupId: String?) -> Unit,
    onCloseAllTabs: () -> Unit,
    onCloseAllInGroup: (String) -> Unit,
    onCreateGroup: (name: String, colorHex: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onToggleGroupCollapse: (String) -> Unit,
    onOpenCreateGroupDialog: () -> Unit,
    onCloseCreateGroupDialog: () -> Unit,
    onOpenMoveTabDialog: (BrowserTab) -> Unit,
    onCloseMoveTabDialog: () -> Unit,
    onAssignTabToGroup: (tabId: String, groupId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tabs_screen")
    ) {
        // Tabs Header & Actions Bar
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Tab Manager",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${tabs.size} tabs • ${tabGroups.size} groups",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // New Group Button
                    OutlinedButton(
                        onClick = onOpenCreateGroupDialog,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_new_group")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "New Group",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "New Group", fontSize = 12.sp)
                    }

                    // New Tab Button
                    Button(
                        onClick = { onOpenNewTab(null) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_new_tab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "New Tab", fontSize = 12.sp)
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Close All Tabs", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    onCloseAllTabs()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Tab Groups & Tabs Content
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Grouped Tabs Sections
            items(tabGroups) { group ->
                val groupTabs = tabs.filter { it.groupId == group.id }
                val groupColor = try {
                    Color(android.graphics.Color.parseColor(group.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                TabGroupSection(
                    group = group,
                    groupColor = groupColor,
                    tabs = groupTabs,
                    activeTabId = activeTab?.id,
                    onSelectTab = onSelectTab,
                    onCloseTab = onCloseTab,
                    onToggleCollapse = { onToggleGroupCollapse(group.id) },
                    onAddTabToGroup = { onOpenNewTab(group.id) },
                    onDeleteGroup = { onDeleteGroup(group.id) },
                    onCloseAllInGroup = { onCloseAllInGroup(group.id) },
                    onMoveTab = onOpenMoveTabDialog
                )
            }

            // 2. Ungrouped Tabs Section
            val ungroupedTabs = tabs.filter { it.groupId == null }
            if (ungroupedTabs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "Ungrouped Tabs",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${ungroupedTabs.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Grid of ungrouped tab cards (2 per row)
                        val pairs = ungroupedTabs.chunked(2)
                        pairs.forEach { rowTabs ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                rowTabs.forEach { tab ->
                                    TabCard(
                                        tab = tab,
                                        isActive = tab.id == activeTab?.id,
                                        tagColor = MaterialTheme.colorScheme.primary,
                                        onSelect = { onSelectTab(tab.id) },
                                        onClose = { onCloseTab(tab.id) },
                                        onMove = { onOpenMoveTabDialog(tab) },
                                        modifier = Modifier.weight(1f)
                                    )
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

    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = onCloseCreateGroupDialog,
            onConfirm = onCreateGroup
        )
    }

    // Move Tab Dialog
    if (tabToMove != null) {
        MoveTabDialog(
            tab = tabToMove,
            groups = tabGroups,
            onDismiss = onCloseMoveTabDialog,
            onAssign = { groupId ->
                onAssignTabToGroup(tabToMove.id, groupId)
            }
        )
    }
}

@Composable
fun TabGroupSection(
    group: TabGroup,
    groupColor: Color,
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleCollapse: () -> Unit,
    onAddTabToGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onCloseAllInGroup: () -> Unit,
    onMoveTab: (BrowserTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = groupColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, groupColor.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Group Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleCollapse)
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(groupColor)
                    )

                    Text(
                        text = group.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = groupColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${tabs.size} tabs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = groupColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAddTabToGroup,
                        modifier = Modifier.size(32.dp).testTag("btn_add_tab_to_${group.name.lowercase().replace(" ", "_")}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add tab to group",
                            tint = groupColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Group menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Close All in Group") },
                                onClick = {
                                    menuExpanded = false
                                    onCloseAllInGroup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Group", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteGroup()
                                }
                            )
                        }
                    }

                    Icon(
                        imageVector = if (group.isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (group.isCollapsed) "Expand" else "Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expanded Grid of Tab Cards
            AnimatedVisibility(visible = !group.isCollapsed) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (tabs.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "No tabs in this group yet. Tap + to add one.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        val pairs = tabs.chunked(2)
                        pairs.forEach { rowTabs ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                rowTabs.forEach { tab ->
                                    TabCard(
                                        tab = tab,
                                        isActive = tab.id == activeTabId,
                                        tagColor = groupColor,
                                        onSelect = { onSelectTab(tab.id) },
                                        onClose = { onCloseTab(tab.id) },
                                        onMove = { onMoveTab(tab) },
                                        modifier = Modifier.weight(1f)
                                    )
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

@Composable
fun TabCard(
    tab: BrowserTab,
    isActive: Boolean,
    tagColor: Color,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isActive) BorderStroke(2.dp, tagColor)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .testTag("tab_card_${tab.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Card Top Row: Favicon/Icon + Close Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = tagColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tab.isDesktopMode) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = "Desktop mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Move to another group icon
                    IconButton(
                        onClick = onMove,
                        modifier = Modifier.size(24.dp).testTag("btn_move_tab_${tab.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "Move to Group",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Close tab
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp).testTag("btn_close_tab_${tab.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close tab",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Title
            Text(
                text = tab.title.ifBlank { "Untitled" },
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Preview snippet or clean domain
            val preview = when {
                tab.url.startsWith("aura://") -> "Aura Home Dashboard"
                else -> tab.url.removePrefix("https://").removePrefix("http://")
            }

            Text(
                text = preview,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(TabGroupColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Tab Group",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Travel, Finance, Docs") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_group_name")
                )

                Text(
                    text = "Select Color Badge",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TabGroupColors) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        val hex = String.format("#%06X", (0xFFFFFF and selectedColor.value.toInt()))
                        onConfirm(groupName, hex)
                    }
                },
                enabled = groupName.isNotBlank(),
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

@Composable
fun MoveTabDialog(
    tab: BrowserTab,
    groups: List<TabGroup>,
    onDismiss: () -> Unit,
    onAssign: (groupId: String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Move Tab to Group",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Move \"${tab.title}\" into:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option: Ungroup (No Group)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (tab.groupId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAssign(null) }
                        .padding(vertical = 2.dp)
                        .testTag("move_option_ungroup")
                ) {
                    Text(
                        text = "None (Ungrouped Tab)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Options: Existing Groups
                groups.forEach { group ->
                    val isCurrent = tab.groupId == group.id
                    val color = try {
                        Color(android.graphics.Color.parseColor(group.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isCurrent) BorderStroke(1.dp, color) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAssign(group.id) }
                            .padding(vertical = 2.dp)
                            .testTag("move_option_${group.name.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = group.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
