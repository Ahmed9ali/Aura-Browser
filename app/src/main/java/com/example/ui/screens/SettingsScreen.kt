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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserSettings
import com.example.model.SearchEngine
import com.example.model.SpeedDial
import com.example.model.ThemeMode
import com.example.ui.components.ManageSpeedDialsDialog
import com.example.ui.components.SpeedDialEditDialog

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
    modifier: Modifier = Modifier
) {
    var showEngineDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showManageSpeedDials by remember { mutableStateOf(false) }
    var speedDialToEdit by remember { mutableStateOf<SpeedDial?>(null) }
    var showAddSpeedDialDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Settings Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Browser Customization",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Search & Discovery Section
            item {
                SettingsSectionCard(title = "Search & Suggestions Engine") {
                    // Search Engine Picker
                    SettingsClickableRow(
                        icon = Icons.Default.Search,
                        title = "Default Search Engine",
                        subtitle = settings.searchEngine.displayName,
                        onClick = { showEngineDialog = true },
                        testTag = "setting_search_engine"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Suggestions Engine Toggle
                    SettingsSwitchRow(
                        icon = Icons.Default.Psychology,
                        title = "Personalized Suggestions",
                        subtitle = "Real-time query discovery and smart domain completion",
                        checked = settings.suggestionsEnabled,
                        onCheckedChange = { onToggleSuggestions() },
                        testTag = "setting_toggle_suggestions"
                    )
                }
            }

            // Privacy & Protection Section
            item {
                SettingsSectionCard(title = "Privacy & Security") {
                    SettingsSwitchRow(
                        icon = Icons.Default.Security,
                        title = "Ad & Tracker Shield",
                        subtitle = "Block third-party tracking scripts and intrusive ads",
                        checked = settings.adShieldEnabled,
                        onCheckedChange = { onToggleAdShield() },
                        testTag = "setting_toggle_shield"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsClickableRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear Browsing Data",
                        subtitle = "Clear browsing history and cached data",
                        onClick = { showClearConfirmDialog = true },
                        testTag = "setting_clear_data"
                    )
                }
            }

            // Appearance & Experience Section
            item {
                SettingsSectionCard(title = "Browsing Experience") {
                    SettingsClickableRow(
                        icon = Icons.Default.DarkMode,
                        title = "Theme Appearance",
                        subtitle = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> "System Default"
                            ThemeMode.DARK -> "Dark Mode"
                            ThemeMode.LIGHT -> "Light Mode"
                        },
                        onClick = { showThemeDialog = true },
                        testTag = "setting_theme_mode"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchRow(
                        icon = Icons.Default.Computer,
                        title = "Desktop Site by Default",
                        subtitle = "Request desktop version of web pages automatically",
                        checked = settings.desktopModeDefault,
                        onCheckedChange = { onToggleDesktopDefault() },
                        testTag = "setting_toggle_desktop"
                    )
                }
            }

            // Home Screen Customization Section
            item {
                SettingsSectionCard(title = "Home Screen Customization") {
                    // Tab Groups toggle
                    SettingsSwitchRow(
                        icon = Icons.Default.Layers,
                        title = "Tab Groups",
                        subtitle = "Show active tab groups and quick switch row on home",
                        checked = settings.showTabGroups,
                        onCheckedChange = { onToggleTabGroups() },
                        testTag = "setting_toggle_tab_groups"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Speed Dials toggle
                    SettingsSwitchRow(
                        icon = Icons.Default.GridView,
                        title = "Speed Dials",
                        subtitle = "Display website shortcuts grid on home screen",
                        checked = settings.showSpeedDials,
                        onCheckedChange = { onToggleSpeedDials() },
                        testTag = "setting_toggle_speed_dials"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Discover Feed toggle
                    SettingsSwitchRow(
                        icon = Icons.Default.RssFeed,
                        title = "Discover Feed",
                        subtitle = "Show curated tech and web discovery stories",
                        checked = settings.showDiscoverFeed,
                        onCheckedChange = { onToggleDiscoverFeed() },
                        testTag = "setting_toggle_discover_feed"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Manage Speed Dials Row
                    SettingsClickableRow(
                        icon = Icons.Default.Edit,
                        title = "Manage Speed Dials",
                        subtitle = "${speedDials.size} shortcuts configured • Add, edit, or remove",
                        onClick = { showManageSpeedDials = true },
                        testTag = "setting_manage_speed_dials"
                    )
                }
            }

            // About Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "About Aura Browser",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version 1.0 • Built with Jetpack Compose & Material 3",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Features tab grouping, fast speed dials, customized suggestions engine, and integrated web browsing.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Search Engine Selection Dialog
    if (showEngineDialog) {
        AlertDialog(
            onDismissRequest = { showEngineDialog = false },
            title = { Text("Choose Search Engine", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SearchEngine.values().forEach { engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetSearchEngine(engine)
                                    showEngineDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.searchEngine == engine,
                                onClick = {
                                    onSetSearchEngine(engine)
                                    showEngineDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = engine.displayName, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEngineDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Appearance Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "System Default"
                            ThemeMode.DARK -> "Dark Theme"
                            ThemeMode.LIGHT -> "Light Theme"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Browsing History?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will delete your browsing history and recent search queries.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearBrowsingData()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
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
                showAddSpeedDialDialog = true
            },
            onEdit = { dial ->
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

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
