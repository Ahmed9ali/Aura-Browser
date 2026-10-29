package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.model.BrowserSettings
import com.example.model.SearchEngine
import com.example.model.SpeedDial
import com.example.model.ThemeMode
import com.example.ui.components.ManageSpeedDialsDialog
import com.example.ui.components.SpeedDialEditDialog
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressiveDialogShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSquircleShape
import com.example.ui.theme.groupedListCardShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive Settings Screen
 * Features Material 3 Grouped List Cards with inter-item spacing,
 * rounded outer group ends (top/bottom), squircle icon badges,
 * and Material 3 PullToRefreshBox with shape-morphing LoadingIndicator.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    speedDials: List<SpeedDial> = emptyList(),
    onSetSearchEngine: (SearchEngine) -> Unit,
    onToggleSuggestions: () -> Unit,
    onToggleAdShield: () -> Unit,
    onToggleDesktopDefault: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onClearBrowsingData: () -> Unit,
    onToggleTabGroups: () -> Unit = {},
    onToggleSpeedDials: () -> Unit = {},
    onToggleDiscoverFeed: () -> Unit = {},
    onAddSpeedDial: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onUpdateSpeedDial: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteSpeedDial: (String) -> Unit = {},
    onResetSpeedDials: () -> Unit = {},
    onRefreshSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEngineDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showManageSpeedDials by remember { mutableStateOf(false) }
    var speedDialToEdit by remember { mutableStateOf<SpeedDial?>(null) }
    var showAddSpeedDialDialog by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Professional Settings App Bar
        com.example.ui.components.AuraSubScreenTopBar(
            title = "Browser Customization",
            subtitle = "Engine: ${settings.searchEngine.displayName} • Theme: ${settings.themeMode.name}",
            icon = Icons.Default.Settings,
            accentColor = MaterialTheme.colorScheme.primary
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    onRefreshSettings()
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
                        .testTag("settings_pull_to_refresh_indicator")
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_pull_to_refresh_box")
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Group 1: Search & Suggestions Engine (2 items: Top & Bottom rounded ends)
                item {
                    SettingsGroupSection(title = "Search & Suggestions Engine") {
                        SettingsGroupedClickableCard(
                            shape = groupedListCardShape(index = 0, count = 2),
                            icon = Icons.Default.Search,
                            badgeColor = Color(0xFF0284C7),
                            title = "Default Search Engine",
                            subtitle = settings.searchEngine.displayName,
                            onClick = { showEngineDialog = true },
                            testTag = "setting_search_engine"
                        )

                        SettingsGroupedSwitchCard(
                            shape = groupedListCardShape(index = 1, count = 2),
                            icon = Icons.Default.Psychology,
                            badgeColor = Color(0xFF8B5CF6),
                            title = "Personalized Suggestions",
                            subtitle = "Real-time query discovery and smart domain completion",
                            checked = settings.suggestionsEnabled,
                            onCheckedChange = { onToggleSuggestions() },
                            testTag = "setting_toggle_suggestions"
                        )
                    }
                }

                // Group 2: Privacy & Security (2 items: Top & Bottom rounded ends)
                item {
                    SettingsGroupSection(title = "Privacy & Security") {
                        SettingsGroupedSwitchCard(
                            shape = groupedListCardShape(index = 0, count = 2),
                            icon = Icons.Default.Security,
                            badgeColor = Color(0xFF6366F1),
                            title = "Ad & Tracker Shield",
                            subtitle = "Block third-party tracking scripts and intrusive ads",
                            checked = settings.adShieldEnabled,
                            onCheckedChange = { onToggleAdShield() },
                            testTag = "setting_toggle_shield"
                        )

                        SettingsGroupedClickableCard(
                            shape = groupedListCardShape(index = 1, count = 2),
                            icon = Icons.Default.DeleteSweep,
                            badgeColor = Color(0xFFF43F5E),
                            title = "Clear Browsing Data",
                            subtitle = "Clear browsing history and cached data",
                            onClick = { showClearConfirmDialog = true },
                            testTag = "setting_clear_data"
                        )
                    }
                }

                // Group 3: Browsing Experience (2 items: Top & Bottom rounded ends)
                item {
                    SettingsGroupSection(title = "Browsing Experience") {
                        SettingsGroupedClickableCard(
                            shape = groupedListCardShape(index = 0, count = 2),
                            icon = Icons.Default.DarkMode,
                            badgeColor = Color(0xFFF59E0B),
                            title = "Theme Appearance",
                            subtitle = when (settings.themeMode) {
                                ThemeMode.SYSTEM -> "System Default"
                                ThemeMode.DARK -> "Dark Mode"
                                ThemeMode.LIGHT -> "Light Mode"
                            },
                            onClick = { showThemeDialog = true },
                            testTag = "setting_theme_mode"
                        )

                        SettingsGroupedSwitchCard(
                            shape = groupedListCardShape(index = 1, count = 2),
                            icon = Icons.Default.Computer,
                            badgeColor = Color(0xFF0D9488),
                            title = "Desktop Site by Default",
                            subtitle = "Request desktop version of web pages automatically",
                            checked = settings.desktopModeDefault,
                            onCheckedChange = { onToggleDesktopDefault() },
                            testTag = "setting_toggle_desktop"
                        )
                    }
                }

                // Group 4: Home Screen Customization (4 items: Top, 2 Middle, Bottom rounded ends)
                item {
                    SettingsGroupSection(title = "Home Screen Customization") {
                        SettingsGroupedSwitchCard(
                            shape = groupedListCardShape(index = 0, count = 4),
                            icon = Icons.Default.Layers,
                            badgeColor = Color(0xFF06B6D4),
                            title = "Tab Groups",
                            subtitle = "Show active tab groups and quick switch row on home",
                            checked = settings.showTabGroups,
                            onCheckedChange = { onToggleTabGroups() },
                            testTag = "setting_toggle_tab_groups"
                        )

                        SettingsGroupedSwitchCard(
                            shape = groupedListCardShape(index = 1, count = 4),
                            icon = Icons.Default.GridView,
                            badgeColor = Color(0xFF10B981),
                            title = "Speed Dials",
                            subtitle = "Display website shortcuts grid on home screen",
                            checked = settings.showSpeedDials,
                            onCheckedChange = { onToggleSpeedDials() },
                            testTag = "setting_toggle_speed_dials"
                        )

                        SettingsGroupedSwitchCard(
                            shape = groupedListCardShape(index = 2, count = 4),
                            icon = Icons.Default.RssFeed,
                            badgeColor = Color(0xFFF97316),
                            title = "Discover Feed",
                            subtitle = "Show curated tech and web discovery stories",
                            checked = settings.showDiscoverFeed,
                            onCheckedChange = { onToggleDiscoverFeed() },
                            testTag = "setting_toggle_discover_feed"
                        )

                        SettingsGroupedClickableCard(
                            shape = groupedListCardShape(index = 3, count = 4),
                            icon = Icons.Default.Edit,
                            badgeColor = MaterialTheme.colorScheme.primary,
                            title = "Manage Speed Dials",
                            subtitle = "${speedDials.size} shortcuts configured • Add, edit, or remove",
                            onClick = { showManageSpeedDials = true },
                            testTag = "setting_manage_speed_dials"
                        )
                    }
                }

                // Group 5: About Section (2 grouped cards with top & bottom rounded ends)
                item {
                    SettingsGroupSection(title = "About") {
                        SettingsGroupedInfoCard(
                            shape = groupedListCardShape(index = 0, count = 2),
                            icon = Icons.Default.Info,
                            badgeColor = MaterialTheme.colorScheme.primary,
                            title = "About Aura Browser",
                            subtitle = "Version 1.0 • Built with Jetpack Compose & Material 3 Expressive"
                        )

                        SettingsGroupedInfoCard(
                            shape = groupedListCardShape(index = 1, count = 2),
                            icon = Icons.Default.AutoAwesome,
                            badgeColor = MaterialTheme.colorScheme.tertiary,
                            title = "Capabilities & Architecture",
                            subtitle = "Features tab grouping, fast speed dials, customized suggestions engine, autonomous web automation, and hardened browsing."
                        )
                    }
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }

    // Search Engine Selection Dialog (using M3 grouped list cards)
    if (showEngineDialog) {
        val engines = SearchEngine.values()
        AlertDialog(
            onDismissRequest = { showEngineDialog = false },
            shape = ExpressiveDialogShape,
            title = { Text("Choose Search Engine", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    engines.forEachIndexed { index, engine ->
                        val itemShape = groupedListCardShape(index = index, count = engines.size)
                        val isSelected = settings.searchEngine == engine
                        Surface(
                            shape = itemShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .clickable {
                                    onSetSearchEngine(engine)
                                    showEngineDialog = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSetSearchEngine(engine)
                                        showEngineDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = engine.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = engine.homeUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showEngineDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Theme Mode Selection Dialog (using M3 grouped list cards)
    if (showThemeDialog) {
        val modes = ThemeMode.values()
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = ExpressiveDialogShape,
            title = { Text("Theme Appearance", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    modes.forEachIndexed { index, mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "System Default (Auto)"
                            ThemeMode.LIGHT -> "Light Mode"
                            ThemeMode.DARK -> "Dark Mode"
                        }
                        val itemShape = groupedListCardShape(index = index, count = modes.size)
                        val isSelected = settings.themeMode == mode
                        Surface(
                            shape = itemShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .clickable {
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSetThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showThemeDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Browsing Data Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            shape = ExpressiveDialogShape,
            title = { Text("Clear Browsing Data", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "This will remove your browsing history and clean cached site storage. Your speed dials and bookmarks will remain intact.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearBrowsingData()
                        showClearConfirmDialog = false
                    },
                    shape = ExpressivePillShape
                ) {
                    Text("Clear Now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manage Speed Dials Dialog
    if (showManageSpeedDials) {
        ManageSpeedDialsDialog(
            speedDials = speedDials,
            onDismiss = { showManageSpeedDials = false },
            onAddNew = {
                showManageSpeedDials = false
                showAddSpeedDialDialog = true
            },
            onEdit = { dial ->
                showManageSpeedDials = false
                speedDialToEdit = dial
            },
            onDelete = { id ->
                onDeleteSpeedDial(id)
            },
            onReset = {
                onResetSpeedDials()
            }
        )
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

/**
 * Material 3 Grouped List Section container.
 * Places a 3.dp vertical gap between individual item cards in the group,
 * while the first and last cards have large outer rounded corners (24.dp)
 * and inner edges have small 4.dp corners.
 */
@Composable
fun SettingsGroupSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun SettingsGroupedClickableCard(
    shape: Shape,
    icon: ImageVector,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            Surface(
                shape = ExpressiveSquircleShape,
                color = badgeColor.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = ExpressiveChipShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGroupedSwitchCard(
    shape: Shape,
    icon: ImageVector,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Surface(
                shape = ExpressiveSquircleShape,
                color = badgeColor.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsGroupedInfoCard(
    shape: Shape,
    icon: ImageVector,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            Surface(
                shape = ExpressiveChipShape,
                color = badgeColor.copy(alpha = 0.16f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
