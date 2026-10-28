package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutomationActionType
import com.example.model.AutomationDataGenerator
import com.example.model.AutomationExecutionState
import com.example.model.AutomationScript
import com.example.model.AutomationStatus
import com.example.model.AutomationStep
import com.example.model.DataExhaustionPolicy
import com.example.model.DataIterationOrder
import com.example.model.DataSheet
import com.example.ui.components.StepEditDialog
import java.util.UUID

/**
 * Modern, simplified, easy-to-use Automation Studio.
 *
 * Tab 0: "Workflow" - Plain-English visual steps, 1-click test run, quick settings
 * Tab 1: "Library & Templates" - Ready-made 1-click templates + saved workflows
 * Tab 2: "My Data" - Simple 1-by-1 batch data cards with easy paste & file upload
 * Tab 3: "Logs" - Clean execution logs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationManagerScreen(
    currentScript: AutomationScript,
    savedScripts: List<AutomationScript>,
    dataSheets: List<DataSheet>,
    executionState: AutomationExecutionState,
    onSelectScript: (AutomationScript) -> Unit,
    onSaveScript: (AutomationScript) -> Unit,
    onDeleteScript: (String) -> Unit,
    onRunScript: () -> Unit,
    onSaveDataSheet: (DataSheet) -> Unit,
    onDeleteDataSheet: (String) -> Unit,
    onDeleteStep: (String) -> Unit,
    onMoveStep: (String, Int) -> Unit = { _, _ -> },
    onUpdateStep: (AutomationStep) -> Unit,
    onAddStep: (AutomationStep) -> Unit,
    onClearAllSteps: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Workflow, 1: Library & Templates, 2: My Data, 3: Logs
    var showFlowSettingsDialog by remember { mutableStateOf(false) }
    var showNewScriptDialog by remember { mutableStateOf(false) }
    var showNewDataSheetDialog by remember { mutableStateOf(false) }
    var stepToEdit by remember { mutableStateOf<AutomationStep?>(null) }
    var showAddNavigateDialog by remember { mutableStateOf(false) }
    var showAddStepDialog by remember { mutableStateOf(false) }
    var showRenameScriptDialog by remember { mutableStateOf(false) }

    val availableFieldKeys = remember(dataSheets) {
        val standard = listOf(
            "email", "username", "password", "confirmPassword", "phoneNumber",
            "firstName", "lastName", "fullName", "countryCode", "gender",
            "dob", "address", "city", "state", "zipCode", "fileName"
        )
        val custom = dataSheets.flatMap { it.fields.keys }
        (standard + custom).distinct()
    }

    val emerald = Color(0xFF10B981)
    val indigo = Color(0xFF6366F1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("automation_manager_screen")
    ) {
        val distinctStudioPageCount = remember(currentScript.steps) {
            currentScript.steps.mapNotNull {
                it.pageUrl.trim().trimEnd('/').lowercase().takeIf { u -> u.isNotBlank() && !u.startsWith("aura://") }
            }.distinct().size.coerceAtLeast(if (currentScript.steps.isNotEmpty()) 1 else 0)
        }

        // Professional Automation Studio App Bar
        com.example.ui.components.AuraSubScreenTopBar(
            title = "Automation Studio",
            subtitle = if (distinctStudioPageCount > 1) {
                "${currentScript.steps.size} steps across $distinctStudioPageCount pages (Starts Page 1)"
            } else {
                "${currentScript.steps.size} steps • ${if (currentScript.targetWebsite == "*") "Any website" else currentScript.targetWebsite}"
            },
            icon = Icons.Default.SmartToy,
            accentColor = indigo,
            onNavigateBack = onNavigateBack,
            backButtonTestTag = "btn_manager_back",
            actions = {
                Button(
                    onClick = onRunScript,
                    colors = ButtonDefaults.buttonColors(containerColor = emerald),
                    shape = com.example.ui.theme.ExpressivePillShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_manager_run_script")
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Run Flow",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            bottomContent = {
                // Modern 4-Tab Navigation Bar
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Workflow (${currentScript.steps.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Templates (${savedScripts.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("My Data (${dataSheets.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Logs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }
        )

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> EasyWorkflowTab(
                    currentScript = currentScript,
                    dataSheets = dataSheets,
                    executionState = executionState,
                    onOpenSettings = { showFlowSettingsDialog = true },
                    onRenameScript = { showRenameScriptDialog = true },
                    onEditStep = { stepToEdit = it },
                    onMoveStep = onMoveStep,
                    onDeleteStep = onDeleteStep,
                    onAddStep = { showAddStepDialog = true },
                    onAddNavigate = { showAddNavigateDialog = true },
                    onClearAll = onClearAllSteps,
                    onGoToTemplates = { selectedTab = 1 }
                )
                1 -> EasyLibraryAndTemplatesTab(
                    currentScript = currentScript,
                    savedScripts = savedScripts,
                    dataSheets = dataSheets,
                    onSelectScript = { script ->
                        onSelectScript(script)
                        selectedTab = 0 // Switch to workflow
                    },
                    onLoadTemplate = { template ->
                        onSaveScript(template)
                        onSelectScript(template)
                        selectedTab = 0 // Switch to workflow
                    },
                    onDeleteScript = onDeleteScript,
                    onOpenNewScript = { showNewScriptDialog = true }
                )
                2 -> EasyMyDataTab(
                    dataSheets = dataSheets,
                    onSaveDataSheet = onSaveDataSheet,
                    onDeleteDataSheet = onDeleteDataSheet,
                    onOpenNewDataSheet = { showNewDataSheetDialog = true }
                )
                3 -> EasyLogsTab(
                    logMessages = executionState.logMessages,
                    executionStatus = executionState.status
                )
            }
        }
    }

    // Step Edit Dialog
    if (stepToEdit != null) {
        StepEditDialog(
            step = stepToEdit!!,
            availableFieldKeys = availableFieldKeys,
            onDismiss = { stepToEdit = null },
            onSave = { updated ->
                onUpdateStep(updated)
                stepToEdit = null
            }
        )
    }

    // Easy Add Step Dialog
    if (showAddStepDialog) {
        EasyAddStepDialog(
            availableFieldKeys = availableFieldKeys,
            onDismiss = { showAddStepDialog = false },
            onAdd = { step ->
                onAddStep(step)
                showAddStepDialog = false
            }
        )
    }

    // Add Navigate Dialog
    if (showAddNavigateDialog) {
        AddNavigateStepDialog(
            onDismiss = { showAddNavigateDialog = false },
            onAdd = { url ->
                onAddStep(
                    AutomationStep(
                        actionType = AutomationActionType.NAVIGATE,
                        selector = url,
                        elementDescription = "Open $url",
                        value = url,
                        delayMs = 2000L
                    )
                )
                showAddNavigateDialog = false
            }
        )
    }

    // Flow Settings Dialog (Simple & uncluttered modal)
    if (showFlowSettingsDialog) {
        FlowSettingsDialog(
            script = currentScript,
            dataSheets = dataSheets,
            onDismiss = { showFlowSettingsDialog = false },
            onSave = { updated ->
                onSaveScript(updated)
                showFlowSettingsDialog = false
            }
        )
    }

    // Rename Script Dialog
    if (showRenameScriptDialog) {
        RenameScriptDialog(
            currentName = currentScript.name,
            currentWebsite = currentScript.targetWebsite,
            onDismiss = { showRenameScriptDialog = false },
            onConfirm = { newName, newSite ->
                onSaveScript(currentScript.copy(name = newName, targetWebsite = newSite))
                showRenameScriptDialog = false
            }
        )
    }

    // Create New Script Dialog
    if (showNewScriptDialog) {
        NewScriptDialog(
            dataSheets = dataSheets,
            onDismiss = { showNewScriptDialog = false },
            onCreate = { name, web, sheetId, order, policy ->
                val newScript = AutomationScript(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    targetWebsite = web.ifBlank { "*" },
                    dataSheetId = sheetId,
                    iterationOrder = order,
                    exhaustionPolicy = policy,
                    repeatCount = 4
                )
                onSaveScript(newScript)
                onSelectScript(newScript)
                showNewScriptDialog = false
                selectedTab = 0
            }
        )
    }

    // Create New Data Sheet Dialog
    if (showNewDataSheetDialog) {
        NewDataSheetDialog(
            onDismiss = { showNewDataSheetDialog = false },
            onCreate = { name, order, policy ->
                val newSheet = DataSheet(
                    id = UUID.randomUUID().toString(),
                    name = name.ifBlank { "XYZ Data" },
                    fields = mapOf(
                        "email" to listOf(
                            "user1@example.com",
                            "user2@example.com",
                            "user3@example.com",
                            "user4@example.com"
                        ),
                        "username" to listOf("user_01", "user_02", "user_03", "user_04"),
                        "password" to listOf("Pass#1001!", "Pass#1002!", "Pass#1003!", "Pass#1004!"),
                        "phoneNumber" to listOf("5550191001", "5550191002", "5550191003", "5550191004")
                    ),
                    iterationOrder = order,
                    exhaustionPolicy = policy
                )
                onSaveDataSheet(newSheet)
                showNewDataSheetDialog = false
            }
        )
    }
}

/**
 * TAB 0: Easy Workflow
 * Clean, visual timeline of steps + quick settings bar
 */
@Composable
private fun EasyWorkflowTab(
    currentScript: AutomationScript,
    dataSheets: List<DataSheet>,
    executionState: AutomationExecutionState,
    onOpenSettings: () -> Unit,
    onRenameScript: () -> Unit,
    onEditStep: (AutomationStep) -> Unit,
    onMoveStep: (String, Int) -> Unit,
    onDeleteStep: (String) -> Unit,
    onAddStep: () -> Unit,
    onAddNavigate: () -> Unit,
    onClearAll: () -> Unit,
    onGoToTemplates: () -> Unit
) {
    val emerald = Color(0xFF10B981)
    val activeSheet = dataSheets.firstOrNull { it.id == currentScript.dataSheetId }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Flow Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentScript.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = onRenameScript,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (currentScript.targetWebsite == "*") "Runs on: All websites" else "Runs on: ${currentScript.targetWebsite}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flow Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Quick Status Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = emerald.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, emerald.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = emerald, modifier = Modifier.size(12.dp))
                                Text("${currentScript.repeatCount}x Runs (1-by-1)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = emerald)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text(
                                    text = activeSheet?.let { "Data: ${it.name}" } ?: "Data: Default Random",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Toolbar: + Step, + Navigate, Clear
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Steps Sequence (${currentScript.steps.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onAddStep,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Step", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onAddNavigate,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("+ Navigate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (currentScript.steps.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Empty State or List of Step Cards
        if (currentScript.steps.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Text(
                            text = "No steps in this workflow yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Add steps manually or choose a 1-click starter template to automate form filling, searches, or multi-page tasks.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 16.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onGoToTemplates,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Browse Templates", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onAddStep,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom Step", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(currentScript.steps) { index, step ->
                EasyStepCard(
                    index = index,
                    totalCount = currentScript.steps.size,
                    step = step,
                    onEdit = { onEditStep(step) },
                    onMoveUp = { onMoveStep(step.id, -1) },
                    onMoveDown = { onMoveStep(step.id, 1) },
                    onDelete = { onDeleteStep(step.id) }
                )
            }
        }
    }
}

/**
 * Human-readable step card with friendly icons and clear badges
 */
@Composable
private fun EasyStepCard(
    index: Int,
    totalCount: Int,
    step: AutomationStep,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, color, actionTitle) = when (step.actionType) {
        AutomationActionType.INPUT -> Triple(Icons.Default.Keyboard, Color(0xFF3B82F6), "Type Text")
        AutomationActionType.CLICK -> Triple(Icons.Default.TouchApp, Color(0xFF10B981), "Click Element")
        AutomationActionType.CHECKBOX -> Triple(Icons.Default.CheckBox, Color(0xFF06B6D4), "Toggle Checkbox")
        AutomationActionType.RADIO -> Triple(Icons.Default.RadioButtonChecked, Color(0xFF8B5CF6), "Select Radio")
        AutomationActionType.NAVIGATE -> Triple(Icons.Default.Language, Color(0xFF8B5CF6), "Open Web Page")
        AutomationActionType.SUBMIT -> Triple(Icons.Default.Send, Color(0xFF10B981), "Submit Form")
        AutomationActionType.WAIT -> Triple(Icons.Default.HourglassBottom, Color(0xFF64748B), "Wait / Delay")
        AutomationActionType.SELECT_DROPDOWN -> Triple(Icons.Default.KeyboardArrowDown, Color(0xFFD946EF), "Choose Option")
        AutomationActionType.SELECT_COUNTRY_CODE -> Triple(Icons.Default.Language, Color(0xFF10B981), "Select Country")
        AutomationActionType.SELECT_DATE -> Triple(Icons.Default.ListAlt, Color(0xFF0284C7), "Select Date")
        AutomationActionType.UPLOAD_FILE -> Triple(Icons.Default.FileUpload, Color(0xFFF59E0B), "Upload File")
        AutomationActionType.CLEAR -> Triple(Icons.Default.Clear, Color(0xFFEF4444), "Clear Input")
        AutomationActionType.SCROLL -> Triple(Icons.Default.KeyboardArrowDown, Color(0xFF6366F1), "Scroll Page")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Step Number & Icon Avatar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${index + 1}. $actionTitle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${step.delayMs}ms",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = step.elementDescription.ifBlank { step.selector },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Data Binding or Value Badge
                    if (!step.variableKey.isNullOrBlank() || step.value.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (!step.variableKey.isNullOrBlank()) {
                                    "Fills: '${step.variableKey}' from My Data (1-by-1)"
                                } else {
                                    "Value: ${step.value}"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions: Reorder, Edit, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (index > 0) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                    }
                }
                if (index < totalCount - 1) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * TAB 1: Library & Ready-Made Templates
 * Gives users 1-tap automations they can use immediately without complex setup!
 */
@Composable
private fun EasyLibraryAndTemplatesTab(
    currentScript: AutomationScript,
    savedScripts: List<AutomationScript>,
    dataSheets: List<DataSheet>,
    onSelectScript: (AutomationScript) -> Unit,
    onLoadTemplate: (AutomationScript) -> Unit,
    onDeleteScript: (String) -> Unit,
    onOpenNewScript: () -> Unit
) {
    val templates = remember {
        listOf(
            AutomationScript(
                id = UUID.randomUUID().toString(),
                name = "Smart Form Autofill",
                targetWebsite = "*",
                repeatCount = 4,
                steps = listOf(
                    AutomationStep(actionType = AutomationActionType.INPUT, selector = "input[type='email'], #email", elementDescription = "Email Address", variableKey = "email", delayMs = 600L),
                    AutomationStep(actionType = AutomationActionType.INPUT, selector = "input[name='username'], #username", elementDescription = "Username", variableKey = "username", delayMs = 600L),
                    AutomationStep(actionType = AutomationActionType.INPUT, selector = "input[type='password'], #password", elementDescription = "Password", variableKey = "password", delayMs = 600L),
                    AutomationStep(actionType = AutomationActionType.CHECKBOX, selector = "input[type='checkbox']", elementDescription = "Agree to Terms", delayMs = 500L),
                    AutomationStep(actionType = AutomationActionType.SUBMIT, selector = "button[type='submit'], input[type='submit']", elementDescription = "Submit Form", delayMs = 1200L)
                )
            ),
            AutomationScript(
                id = UUID.randomUUID().toString(),
                name = "Web Search Flow",
                targetWebsite = "google.com",
                repeatCount = 3,
                steps = listOf(
                    AutomationStep(actionType = AutomationActionType.NAVIGATE, selector = "https://www.google.com", elementDescription = "Open Google", value = "https://www.google.com", delayMs = 2000L),
                    AutomationStep(actionType = AutomationActionType.INPUT, selector = "textarea[name='q'], input[name='q']", elementDescription = "Search Query Box", variableKey = "username", delayMs = 800L),
                    AutomationStep(actionType = AutomationActionType.SUBMIT, selector = "input[name='btnK'], form[action='/search']", elementDescription = "Search Button", delayMs = 1500L),
                    AutomationStep(actionType = AutomationActionType.WAIT, selector = "", elementDescription = "Review Results", delayMs = 3000L)
                )
            ),
            AutomationScript(
                id = UUID.randomUUID().toString(),
                name = "Multi-Page Transition Loop",
                targetWebsite = "*",
                repeatCount = 2,
                steps = listOf(
                    AutomationStep(actionType = AutomationActionType.NAVIGATE, selector = "https://example.com/step1", elementDescription = "Step 1 Page", value = "https://example.com/step1", delayMs = 2000L),
                    AutomationStep(actionType = AutomationActionType.INPUT, selector = "#firstName", elementDescription = "First Name", variableKey = "firstName", delayMs = 600L),
                    AutomationStep(actionType = AutomationActionType.CLICK, selector = "a.next-page, button.next", elementDescription = "Next Page Button", delayMs = 1500L),
                    AutomationStep(actionType = AutomationActionType.INPUT, selector = "#phoneNumber", elementDescription = "Phone Number", variableKey = "phoneNumber", delayMs = 600L),
                    AutomationStep(actionType = AutomationActionType.SUBMIT, selector = "#finishBtn", elementDescription = "Finish Button", delayMs = 1000L)
                )
            )
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Ready-to-Use 1-Click Starter Templates
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("1-Click Starter Templates", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Select a pre-built automation recipe to jump-start your flow with zero manual configuration:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                templates.forEach { template ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(template.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${template.steps.size} steps • Target: ${template.targetWebsite} • ${template.repeatCount}x runs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onLoadTemplate(template) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Use Template", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) }

        // Saved Custom Scripts
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Your Saved Workflows", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${savedScripts.size} saved scripts in storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = onOpenNewScript,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(savedScripts) { script ->
            val isCurrent = script.id == currentScript.id
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectScript(script) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(script.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text("Active", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }

                        Text(
                            text = "${script.steps.size} steps • ${if (script.targetWebsite == "*") "All sites" else script.targetWebsite} • ${script.repeatCount}x runs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!isCurrent) {
                            OutlinedButton(
                                onClick = { onSelectScript(script) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Load", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (savedScripts.size > 1) {
                            IconButton(
                                onClick = { onDeleteScript(script.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * TAB 2: Easy My Data
 * Clean, uncluttered view of data sheets and 1-by-1 form filling data
 */
@Composable
private fun EasyMyDataTab(
    dataSheets: List<DataSheet>,
    onSaveDataSheet: (DataSheet) -> Unit,
    onDeleteDataSheet: (String) -> Unit,
    onOpenNewDataSheet: () -> Unit
) {
    var selectedSheetId by remember { mutableStateOf(dataSheets.firstOrNull()?.id ?: "__DEFAULT__") }
    val isViewingDefault = selectedSheetId == "__DEFAULT__" || dataSheets.isEmpty()
    val activeSheet = if (!isViewingDefault) {
        dataSheets.firstOrNull { it.id == selectedSheetId } ?: dataSheets.firstOrNull()
    } else null

    var fieldKeyToEdit by remember { mutableStateOf<String?>(null) }
    var showAddFieldDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Sheet Selector Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Active Data Sheet", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = onOpenNewDataSheet,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dataSheets) { sheet ->
                        val isSelected = !isViewingDefault && sheet.id == activeSheet?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSheetId = sheet.id },
                            label = { Text("${sheet.name} (${sheet.totalRows()} rows)") },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }

                    item {
                        FilterChip(
                            selected = isViewingDefault,
                            onClick = { selectedSheetId = "__DEFAULT__" },
                            label = { Text("Default Generator (Random)") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }

        // Active Sheet Overview
        if (activeSheet != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(activeSheet.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Stored items are used 1-by-1 sequentially on each recurring run.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (dataSheets.size > 1) {
                                TextButton(
                                    onClick = { onDeleteDataSheet(activeSheet.id) }
                                ) {
                                    Text("Delete Sheet", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Order & Policy Row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Consumption Order", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(activeSheet.iterationOrder.shortLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("When Finished", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(activeSheet.exhaustionPolicy.shortLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Add Field Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Form Fields in '${activeSheet.name}'", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    OutlinedButton(
                        onClick = { showAddFieldDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Field", fontSize = 11.sp)
                    }
                }
            }

            // Standard common fields: email, username, password, phone, firstName
            val standardKeys = listOf(
                "email" to "Email Address",
                "username" to "Username",
                "password" to "Password",
                "phoneNumber" to "Phone Number",
                "firstName" to "First Name",
                "lastName" to "Last Name"
            )

            items(standardKeys) { (key, label) ->
                val values = activeSheet.getFieldValues(key)
                EasyFieldCard(
                    fieldKey = key,
                    fieldLabel = label,
                    values = values,
                    onEdit = { fieldKeyToEdit = key }
                )
            }

            // Any extra custom fields
            val otherKeys = activeSheet.fields.keys.filterNot { k -> standardKeys.any { it.first == k } }
            items(otherKeys) { key ->
                val values = activeSheet.getFieldValues(key)
                EasyFieldCard(
                    fieldKey = key,
                    fieldLabel = "Custom: $key",
                    values = values,
                    onEdit = { fieldKeyToEdit = key }
                )
            }
        }

        // Default Generator Preview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Text("Dynamic Default Generator", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Whenever a field is left empty or your stored list runs out, Aura automatically generates fresh, realistic dummy data (email, password, phone, address) for each step so automation never fails.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Edit Field Values Dialog
    if (fieldKeyToEdit != null && activeSheet != null) {
        val currentValues = activeSheet.getFieldValues(fieldKeyToEdit!!)
        EditFieldValuesDialog(
            sheetName = activeSheet.name,
            fieldKey = fieldKeyToEdit!!,
            currentValues = currentValues,
            onDismiss = { fieldKeyToEdit = null },
            onSave = { updatedList ->
                val newFields = activeSheet.fields.toMutableMap().apply {
                    put(fieldKeyToEdit!!, updatedList)
                }
                onSaveDataSheet(activeSheet.copy(fields = newFields))
                fieldKeyToEdit = null
            }
        )
    }

    // Add Custom Field Dialog
    if (showAddFieldDialog && activeSheet != null) {
        AddCustomFieldDialog(
            onDismiss = { showAddFieldDialog = false },
            onAdd = { newKey ->
                val newFields = activeSheet.fields.toMutableMap().apply {
                    if (!containsKey(newKey)) {
                        put(newKey, emptyList())
                    }
                }
                onSaveDataSheet(activeSheet.copy(fields = newFields))
                showAddFieldDialog = false
                fieldKeyToEdit = newKey
            }
        )
    }
}

/**
 * Clean field card in My Data
 */
@Composable
private fun EasyFieldCard(
    fieldKey: String,
    fieldLabel: String,
    values: List<String>,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(fieldLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (values.isNotEmpty()) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (values.isNotEmpty()) "${values.size} items stored" else "Empty (Uses Default)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (values.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (values.isNotEmpty()) {
                    Text(
                        text = values.take(3).joinToString(" • ") + if (values.size > 3) " • +${values.size - 3} more" else "",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Tap to paste or upload 10s of items", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onEdit,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (values.isNotEmpty()) "Edit" else "+ Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * TAB 3: Activity Logs
 */
@Composable
private fun EasyLogsTab(
    logMessages: List<String>,
    executionStatus: AutomationStatus
) {
    val emerald = Color(0xFF10B981)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("Execution Engine Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (executionStatus == AutomationStatus.PLAYING) emerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = executionStatus.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (executionStatus == AutomationStatus.PLAYING) emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (logMessages.isEmpty()) {
            item {
                Text(
                    text = "No logs yet. Run a workflow to view step execution, element matching, and 1-by-1 data fills.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            items(logMessages) { msg ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Friendly, easy Add Step Dialog with human categories (Type, Click, Toggle, Wait)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasyAddStepDialog(
    availableFieldKeys: List<String>,
    onDismiss: () -> Unit,
    onAdd: (AutomationStep) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(AutomationActionType.INPUT) }
    var elementLabel by remember { mutableStateOf("") }
    var selector by remember { mutableStateOf("") }
    var selectedVarKey by remember { mutableStateOf("email") }
    var customValue by remember { mutableStateOf("") }
    var delayMsStr by remember { mutableStateOf("800") }
    var fieldMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add Step to Flow", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose action type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                // Category chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedCategory == AutomationActionType.INPUT,
                        onClick = { selectedCategory = AutomationActionType.INPUT },
                        label = { Text("Type") },
                        leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedCategory == AutomationActionType.CLICK,
                        onClick = { selectedCategory = AutomationActionType.CLICK },
                        label = { Text("Click") },
                        leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedCategory == AutomationActionType.CHECKBOX,
                        onClick = { selectedCategory = AutomationActionType.CHECKBOX },
                        label = { Text("Check") },
                        leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedCategory == AutomationActionType.WAIT,
                        onClick = { selectedCategory = AutomationActionType.WAIT },
                        label = { Text("Wait") },
                        leadingIcon = { Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                OutlinedTextField(
                    value = elementLabel,
                    onValueChange = { elementLabel = it },
                    label = { Text("Step Name / Description", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Email Field, Submit Button") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedCategory != AutomationActionType.WAIT) {
                    OutlinedTextField(
                        value = selector,
                        onValueChange = { selector = it },
                        label = { Text("Target Element (ID, Class, or Selector)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. #email, input[name='email'], .btn-submit") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedCategory == AutomationActionType.INPUT) {
                    ExposedDropdownMenuBox(
                        expanded = fieldMenuOpen,
                        onExpandedChange = { fieldMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedVarKey.isNotBlank()) "Fill from My Data: '$selectedVarKey'" else "Type static custom value",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Value to Type", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = fieldMenuOpen,
                            onDismissRequest = { fieldMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Static Custom Text...", fontSize = 12.sp) },
                                onClick = {
                                    selectedVarKey = ""
                                    fieldMenuOpen = false
                                }
                            )
                            availableFieldKeys.forEach { key ->
                                DropdownMenuItem(
                                    text = { Text("My Data: $key (1-by-1)", fontSize = 12.sp) },
                                    onClick = {
                                        selectedVarKey = key
                                        fieldMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedVarKey.isBlank()) {
                        OutlinedTextField(
                            value = customValue,
                            onValueChange = { customValue = it },
                            label = { Text("Custom Text to Type", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = delayMsStr,
                    onValueChange = { delayMsStr = it },
                    label = { Text("Delay after step (ms)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val step = AutomationStep(
                        actionType = selectedCategory,
                        selector = selector.trim(),
                        elementDescription = elementLabel.trim().ifBlank { "${selectedCategory.name}: ${selector.take(20)}" },
                        variableKey = if (selectedCategory == AutomationActionType.INPUT && selectedVarKey.isNotBlank()) selectedVarKey else null,
                        value = if (selectedVarKey.isBlank()) customValue.trim() else "{{$selectedVarKey}}",
                        delayMs = delayMsStr.toLongOrNull() ?: 800L
                    )
                    onAdd(step)
                },
                enabled = selector.isNotBlank() || selectedCategory == AutomationActionType.WAIT
            ) {
                Text("Add Step")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Flow Settings Dialog (Simple popup to edit repeats, data sheet, order)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowSettingsDialog(
    script: AutomationScript,
    dataSheets: List<DataSheet>,
    onDismiss: () -> Unit,
    onSave: (AutomationScript) -> Unit
) {
    var repeatCount by remember { mutableIntStateOf(script.repeatCount) }
    var selectedSheetId by remember { mutableStateOf(script.dataSheetId) }
    var iterationOrder by remember { mutableStateOf(script.iterationOrder) }
    var sheetMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flow Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Repeat Stepper
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How many times should this workflow run?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Each run takes the next row 1-by-1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { if (repeatCount > 1) repeatCount-- },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) { Text("-", fontSize = 14.sp) }

                            Text("${repeatCount}x", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                            OutlinedButton(
                                onClick = { repeatCount++ },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) { Text("+", fontSize = 14.sp) }
                        }
                    }
                }

                // Data Sheet Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Data Sheet to use:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(
                        expanded = sheetMenuOpen,
                        onExpandedChange = { sheetMenuOpen = it }
                    ) {
                        val activeSheet = dataSheets.firstOrNull { it.id == selectedSheetId }
                        val label = activeSheet?.let { "${it.name} (${it.totalRows()} rows stored)" }
                            ?: "Default Generator (Random Every Time)"

                        OutlinedTextField(
                            value = label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = sheetMenuOpen,
                            onDismissRequest = { sheetMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default Generator (Random Every Time)", fontSize = 12.sp) },
                                onClick = {
                                    selectedSheetId = null
                                    sheetMenuOpen = false
                                }
                            )
                            dataSheets.forEach { sheet ->
                                DropdownMenuItem(
                                    text = { Text("${sheet.name} (${sheet.totalRows()} rows stored)", fontSize = 12.sp) },
                                    onClick = {
                                        selectedSheetId = sheet.id
                                        sheetMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Order
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1-by-1 Consumption Order:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = iterationOrder == DataIterationOrder.TOP_TO_BOTTOM,
                            onClick = { iterationOrder = DataIterationOrder.TOP_TO_BOTTOM },
                            label = { Text("Top to Bottom") }
                        )
                        FilterChip(
                            selected = iterationOrder == DataIterationOrder.RANDOM,
                            onClick = { iterationOrder = DataIterationOrder.RANDOM },
                            label = { Text("Random") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        script.copy(
                            repeatCount = repeatCount,
                            dataSheetId = selectedSheetId,
                            iterationOrder = iterationOrder
                        )
                    )
                }
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to rename script and target website
 */
@Composable
fun RenameScriptDialog(
    currentName: String,
    currentWebsite: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newWebsite: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var website by remember { mutableStateOf(currentWebsite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Workflow", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workflow Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Target Website (* for any)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), website.trim().ifBlank { "*" }) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Edit Field Values Dialog - allows pasting 10s of items manually OR uploading a .txt/.csv file!
 */
@Composable
fun EditFieldValuesDialog(
    sheetName: String,
    fieldKey: String,
    currentValues: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf(currentValues.joinToString("\n")) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    rawText = if (rawText.isBlank()) content.trim() else "$rawText\n${content.trim()}"
                }
            } catch (_: Exception) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Manage '$fieldKey' in $sheetName",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste manually or upload a file (.txt / .csv) with 10s of '$fieldKey' entries. Each value will be used 1 by 1 on recurring runs.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("text/*") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload File", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val generatedBatch = (1..10).map {
                                AutomationDataGenerator.generateSingleFieldValue(fieldKey)
                            }.joinToString("\n")
                            rawText = if (rawText.isBlank()) generatedBatch else "$rawText\n$generatedBatch"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+10 Sample Items", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Stored '$fieldKey' Values (1 per line)", fontSize = 11.sp) },
                    placeholder = { Text("item1\nitem2\nitem3\nitem4") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                val detectedCount = rawText.lines()
                    .flatMap { it.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .size

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total stored values: $detectedCount (Used 1 by 1)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (rawText.isNotBlank()) {
                        TextButton(
                            onClick = { rawText = "" },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = rawText.lines()
                        .flatMap { line -> line.split(",") }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    onSave(parsed)
                }
            ) {
                Text("Save to $sheetName")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to add a Navigate step
 */
@Composable
fun AddNavigateStepDialog(
    onDismiss: () -> Unit,
    onAdd: (url: String) -> Unit
) {
    var urlInput by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Web Page Transition", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Automate moving from 1 website to another or opening the next page:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Website or Page URL", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(urlInput.trim()) },
                enabled = urlInput.isNotBlank() && urlInput != "https://"
            ) {
                Text("Add Step")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to create a new script
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScriptDialog(
    dataSheets: List<DataSheet>,
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        website: String,
        dataSheetId: String?,
        order: DataIterationOrder,
        policy: DataExhaustionPolicy
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("*") }
    var sheetId by remember { mutableStateOf(dataSheets.firstOrNull()?.id) }
    var order by remember { mutableStateOf(DataIterationOrder.TOP_TO_BOTTOM) }
    var policy by remember { mutableStateOf(DataExhaustionPolicy.ADD_NEW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Workflow", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workflow Name", fontSize = 11.sp) },
                    placeholder = { Text("E.g. Registration Flow") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Target Website Domain (* for any)", fontSize = 11.sp) },
                    placeholder = { Text("x.com or *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, website, sheetId, order, policy) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to create a new Data Sheet (e.g. "XYZ Data")
 */
@Composable
fun NewDataSheetDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, order: DataIterationOrder, policy: DataExhaustionPolicy) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var order by remember { mutableStateOf(DataIterationOrder.TOP_TO_BOTTOM) }
    var policy by remember { mutableStateOf(DataExhaustionPolicy.ADD_NEW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Data Sheet", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Data Sheet Name", fontSize = 11.sp) },
                    placeholder = { Text("E.g. XYZ Data") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "A Data Sheet stores lists of emails, usernames, passwords, etc., used 1-by-1 sequentially on each recurring run.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, order, policy) },
                enabled = name.isNotBlank()
            ) {
                Text("Create Sheet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to add a custom field key inside a Data Sheet
 */
@Composable
fun AddCustomFieldDialog(
    onDismiss: () -> Unit,
    onAdd: (key: String) -> Unit
) {
    var fieldKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Field to Data Sheet", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = fieldKey,
                onValueChange = { fieldKey = it },
                label = { Text("Field Key Name", fontSize = 11.sp) },
                placeholder = { Text("e.g. email, username, password, company") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(fieldKey.trim()) },
                enabled = fieldKey.isNotBlank()
            ) {
                Text("Add Field")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
