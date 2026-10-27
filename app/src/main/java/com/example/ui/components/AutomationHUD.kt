package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.model.ElementSemanticType
import com.example.model.HudMode
import com.example.model.ScannedElement
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationHUD(
    executionState: AutomationExecutionState,
    currentScript: AutomationScript,
    savedScripts: List<AutomationScript>,
    dataSheets: List<DataSheet>,
    currentUrl: String,
    scannedElements: List<ScannedElement>,
    onSelectMode: (HudMode) -> Unit,
    onSelectScript: (AutomationScript) -> Unit,
    onUpdateCurrentScript: (AutomationScript) -> Unit,
    onStartFreshScript: () -> Unit,
    onSaveCurrentScript: (
        name: String,
        targetWebsite: String,
        dataSheetId: String?,
        order: DataIterationOrder,
        policy: DataExhaustionPolicy,
        repeatCount: Int
    ) -> Unit,
    onUpdateStep: (AutomationStep) -> Unit,
    onMoveStep: (String, Int) -> Unit,
    onDeleteStep: (String) -> Unit,
    onAddStep: (AutomationStep) -> Unit,
    onUpdateScannedElement: (ScannedElement) -> Unit,
    onToggleRecord: () -> Unit,
    onStartPlay: () -> Unit,
    onStopPlay: () -> Unit,
    onTriggerScan: () -> Unit,
    onClearScan: () -> Unit,
    onAddScannedElement: (ScannedElement) -> Unit,
    onAutoMapAllScanned: () -> Unit,
    onNavigateWebViewToUrl: (String) -> Unit,
    onResolveExhaustionStop: () -> Unit,
    onResolveExhaustionDefault: () -> Unit,
    onResolveExhaustionAddNew: (fieldKey: String, newValues: List<String>) -> Unit,
    onSaveDataSheet: (DataSheet) -> Unit = {},
    onOpenScriptManager: () -> Unit,
    onCloseHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMinimized by remember { mutableStateOf(false) }
    var stepToEdit by remember { mutableStateOf<AutomationStep?>(null) }
    var scannedElementToEdit by remember { mutableStateOf<ScannedElement?>(null) }
    var showSaveScriptDialog by remember { mutableStateOf(false) }
    var showAddNavigateDialog by remember { mutableStateOf(false) }
    var showAddNewExhaustedDataDialog by remember { mutableStateOf(false) }
    var showQuickCreateDataSheetDialog by remember { mutableStateOf(false) }
    var showNoDataSheetBeforeRunDialog by remember { mutableStateOf(false) }
    var confirmedDefaultDataForSession by remember { mutableStateOf(false) }

    val websiteDomain = remember(currentUrl) {
        currentUrl.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
    }

    val availableFieldKeys = remember(dataSheets) {
        val standard = listOf(
            "email", "username", "password", "confirmPassword", "phoneNumber",
            "firstName", "lastName", "fullName", "countryCode", "gender",
            "dob", "address", "city", "state", "zipCode", "fileName"
        )
        val custom = dataSheets.flatMap { it.fields.keys }
        (standard + custom).distinct()
    }

    val handleRequestStartPlay = {
        if (currentScript.steps.isEmpty() && savedScripts.isEmpty()) {
            // Cannot start when no scripts exist — switch to Create mode
            onSelectMode(HudMode.CREATE_AUTOMATION)
        } else if (dataSheets.isEmpty() && currentScript.dataSheetId == null && !confirmedDefaultDataForSession) {
            // No data sheet exists yet — let user either create a Data Sheet or confirm Default Data
            showNoDataSheetBeforeRunDialog = true
        } else {
            onStartPlay()
        }
    }

    val statusColor = when (executionState.status) {
        AutomationStatus.RECORDING -> Color(0xFFEF4444)
        AutomationStatus.PLAYING -> Color(0xFF10B981)
        AutomationStatus.SCANNING -> Color(0xFF06B6D4)
        AutomationStatus.PAUSED -> Color(0xFFF59E0B)
        AutomationStatus.IDLE -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .testTag("automation_hud")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Top Bar: Title, Website Badge, Minimize & Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = "Automation HUD",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (websiteDomain.isNotBlank() && !websiteDomain.contains("home")) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = websiteDomain.take(18),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMinimized) "Expand" else "Minimize",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isMinimized = !isMinimized }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    IconButton(
                        onClick = onCloseHud,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("btn_close_hud")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUD",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isMinimized,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Primary Tabs: Run Automation vs Create Automation
                    PrimaryTabRow(
                        selectedTabIndex = if (executionState.hudMode == HudMode.RUN_AUTOMATION) 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = executionState.hudMode == HudMode.RUN_AUTOMATION,
                            onClick = { onSelectMode(HudMode.RUN_AUTOMATION) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Run Automation", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                        Tab(
                            selected = executionState.hudMode == HudMode.CREATE_AUTOMATION,
                            onClick = { onSelectMode(HudMode.CREATE_AUTOMATION) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Create Script", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Interactive Data Exhausted Banner if stored data finished
                    if (executionState.isAwaitingExhaustionChoice) {
                        DataExhaustedHudPrompt(
                            fieldKey = executionState.exhaustedFieldKey ?: "email",
                            sheetName = executionState.exhaustedSheetName.ifBlank { "My Data" },
                            usedCount = executionState.exhaustedUsedCount,
                            onStop = onResolveExhaustionStop,
                            onContinueDefault = onResolveExhaustionDefault,
                            onOpenAddNew = { showAddNewExhaustedDataDialog = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (executionState.hudMode == HudMode.RUN_AUTOMATION) {
                        RunAutomationHudContent(
                            executionState = executionState,
                            currentScript = currentScript,
                            savedScripts = savedScripts,
                            dataSheets = dataSheets,
                            websiteDomain = websiteDomain,
                            confirmedDefaultData = confirmedDefaultDataForSession,
                            onSelectScript = onSelectScript,
                            onUpdateCurrentScript = onUpdateCurrentScript,
                            onSwitchToCreateScript = { onSelectMode(HudMode.CREATE_AUTOMATION) },
                            onOpenCreateDataSheet = { showQuickCreateDataSheetDialog = true },
                            onConfirmUseDefaultData = {
                                confirmedDefaultDataForSession = true
                                onUpdateCurrentScript(currentScript.copy(dataSheetId = null))
                            },
                            onStartPlay = handleRequestStartPlay,
                            onStopPlay = onStopPlay,
                            onOpenScriptManager = onOpenScriptManager
                        )
                    } else {
                        CreateAutomationHudContent(
                            executionState = executionState,
                            currentScript = currentScript,
                            scannedElements = scannedElements,
                            dataSheets = dataSheets,
                            currentUrl = currentUrl,
                            websiteDomain = websiteDomain,
                            onStartFreshScript = onStartFreshScript,
                            onToggleRecord = onToggleRecord,
                            onTriggerScan = onTriggerScan,
                            onClearScan = onClearScan,
                            onAddScannedElement = onAddScannedElement,
                            onEditScannedElement = { scannedElementToEdit = it },
                            onAutoMapAllScanned = onAutoMapAllScanned,
                            onEditStep = { stepToEdit = it },
                            onMoveStep = onMoveStep,
                            onDeleteStep = onDeleteStep,
                            onOpenAddNavigate = { showAddNavigateDialog = true },
                            onOpenSaveScript = { showSaveScriptDialog = true },
                            onOpenCreateDataSheet = { showQuickCreateDataSheetDialog = true },
                            onNavigateWebViewToUrl = onNavigateWebViewToUrl,
                            onReadyAndRun = handleRequestStartPlay,
                            onOpenScriptManager = onOpenScriptManager
                        )
                    }
                }
            }
        }
    }

    // Prompt if no Data Sheet exists when starting automation
    if (showNoDataSheetBeforeRunDialog) {
        DataSheetChoiceBeforeRunDialog(
            onDismiss = { showNoDataSheetBeforeRunDialog = false },
            onUseDefaultAndRun = {
                confirmedDefaultDataForSession = true
                showNoDataSheetBeforeRunDialog = false
                onUpdateCurrentScript(currentScript.copy(dataSheetId = null))
                onStartPlay()
            },
            onCreateDataSheet = {
                showNoDataSheetBeforeRunDialog = false
                showQuickCreateDataSheetDialog = true
            }
        )
    }

    // Quick Create Data Sheet Dialog right inside HUD
    if (showQuickCreateDataSheetDialog) {
        QuickCreateDataSheetDialog(
            onDismiss = { showQuickCreateDataSheetDialog = false },
            onSaveSheet = { newSheet ->
                onSaveDataSheet(newSheet)
                onUpdateCurrentScript(currentScript.copy(dataSheetId = newSheet.id))
                showQuickCreateDataSheetDialog = false
            }
        )
    }

    // Edit Captured Step Dialog in HUD
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

    // Edit Grabbed Element Dialog in HUD
    if (scannedElementToEdit != null) {
        ScannedElementEditDialog(
            element = scannedElementToEdit!!,
            availableFieldKeys = availableFieldKeys,
            onDismiss = { scannedElementToEdit = null },
            onSaveElementOnly = { updated ->
                onUpdateScannedElement(updated)
                scannedElementToEdit = null
            },
            onSaveAndAddStep = { updated, customFieldKey ->
                onUpdateScannedElement(updated)
                val step = AutomationStep(
                    actionType = if (updated.isButton) AutomationActionType.CLICK else AutomationActionType.INPUT,
                    selector = updated.selector,
                    elementDescription = if (customFieldKey != null) "Fill $customFieldKey" else "Interact ${updated.selector.take(18)}",
                    value = if (customFieldKey != null) "{{$customFieldKey}}" else "",
                    variableKey = customFieldKey,
                    delayMs = 800L
                )
                onAddStep(step)
                scannedElementToEdit = null
            }
        )
    }

    // Save Script Dialog
    if (showSaveScriptDialog) {
        SaveScriptDialog(
            defaultName = currentScript.name.ifBlank { "$websiteDomain Automation" },
            defaultWebsite = websiteDomain.ifBlank { "*" },
            dataSheets = dataSheets,
            initialDataSheetId = currentScript.dataSheetId,
            initialOrder = currentScript.iterationOrder,
            initialPolicy = currentScript.exhaustionPolicy,
            initialRepeatCount = currentScript.repeatCount,
            onDismiss = { showSaveScriptDialog = false },
            onSave = { name, web, sheetId, order, policy, repeatCount ->
                onSaveCurrentScript(name, web, sheetId, order, policy, repeatCount)
                showSaveScriptDialog = false
            }
        )
    }

    // Add Navigate Step Dialog (Page 1 -> Page 2 or Website 1 -> Website 2)
    if (showAddNavigateDialog) {
        AddNavigateDialog(
            currentUrl = currentUrl,
            onDismiss = { showAddNavigateDialog = false },
            onAddOnly = { targetUrl ->
                onAddStep(
                    AutomationStep(
                        actionType = AutomationActionType.NAVIGATE,
                        selector = targetUrl,
                        elementDescription = "Navigate to $targetUrl",
                        value = targetUrl,
                        pageUrl = currentUrl,
                        delayMs = 2000L
                    )
                )
                showAddNavigateDialog = false
            },
            onAddAndNavigateNow = { targetUrl ->
                onAddStep(
                    AutomationStep(
                        actionType = AutomationActionType.NAVIGATE,
                        selector = targetUrl,
                        elementDescription = "Navigate to $targetUrl",
                        value = targetUrl,
                        pageUrl = currentUrl,
                        delayMs = 2000L
                    )
                )
                showAddNavigateDialog = false
                onNavigateWebViewToUrl(targetUrl)
            }
        )
    }

    // Add New Data when Exhausted Dialog
    if (showAddNewExhaustedDataDialog) {
        val fieldKey = executionState.exhaustedFieldKey ?: "email"
        AddExhaustedFieldDataDialog(
            fieldKey = fieldKey,
            sheetName = executionState.exhaustedSheetName.ifBlank { "My Data" },
            onDismiss = { showAddNewExhaustedDataDialog = false },
            onAddAndContinue = { newItems ->
                showAddNewExhaustedDataDialog = false
                onResolveExhaustionAddNew(fieldKey, newItems)
            }
        )
    }
}

/**
 * Interactive Prompt shown in HUD when stored values in a Data Sheet are all used 1 by 1.
 */
@Composable
private fun DataExhaustedHudPrompt(
    fieldKey: String,
    sheetName: String,
    usedCount: Int,
    onStop: () -> Unit,
    onContinueDefault: () -> Unit,
    onOpenAddNew: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "All $usedCount '$fieldKey' values in '$sheetName' used 1 by 1!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Select how to proceed for the remaining steps:",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onContinueDefault,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("Use Default", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenAddNew,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("+ Add New", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tab Content: Run Automation
 * Enforces the Script Creation Gate (cannot start if no script/steps exist) and Data Sheet vs Default choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunAutomationHudContent(
    executionState: AutomationExecutionState,
    currentScript: AutomationScript,
    savedScripts: List<AutomationScript>,
    dataSheets: List<DataSheet>,
    websiteDomain: String,
    confirmedDefaultData: Boolean,
    onSelectScript: (AutomationScript) -> Unit,
    onUpdateCurrentScript: (AutomationScript) -> Unit,
    onSwitchToCreateScript: () -> Unit,
    onOpenCreateDataSheet: () -> Unit,
    onConfirmUseDefaultData: () -> Unit,
    onStartPlay: () -> Unit,
    onStopPlay: () -> Unit,
    onOpenScriptManager: () -> Unit
) {
    val isPlaying = executionState.status == AutomationStatus.PLAYING || executionState.status == AutomationStatus.PAUSED
    val hasValidScript = currentScript.steps.isNotEmpty() || savedScripts.any { it.steps.isNotEmpty() }

    var scriptMenuExpanded by remember { mutableStateOf(false) }
    var sheetMenuExpanded by remember { mutableStateOf(false) }
    var orderMenuExpanded by remember { mutableStateOf(false) }
    var policyMenuExpanded by remember { mutableStateOf(false) }
    var filterWebsiteOnly by remember { mutableStateOf(true) }

    val websiteMatchedScripts = remember(savedScripts, websiteDomain, filterWebsiteOnly) {
        val exactSite = savedScripts.filter {
            it.targetWebsite != "*" &&
                    it.targetWebsite.isNotBlank() &&
                    websiteDomain.isNotBlank() &&
                    (websiteDomain.contains(it.targetWebsite, ignoreCase = true) ||
                            it.targetWebsite.contains(websiteDomain, ignoreCase = true))
        }
        val universal = savedScripts.filter { it.targetWebsite == "*" || it.targetWebsite.isBlank() }
        if (filterWebsiteOnly && websiteDomain.isNotBlank() && !websiteDomain.contains("home")) {
            (exactSite + universal).distinctBy { it.id }
        } else {
            savedScripts
        }
    }

    val assignedSheet = dataSheets.firstOrNull { it.id == currentScript.dataSheetId }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        // Gate 1: When NO scripts exist, engine cannot be started — user must create one in HUD
        if (!hasValidScript && !isPlaying) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "No Script Created — Engine Cannot Start",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Create a script first by grabbing or tapping page elements (no typing in fields needed). When started, the engine will fill fields and click buttons/boxes from your Data Sheet or Default.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSwitchToCreateScript,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_create_first_script_hud")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Create Script Now (Grab Page Elements)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Execution Progress Bar
        if (isPlaying) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Run ${executionState.currentIteration}/${executionState.totalIterations} • Step ${executionState.currentStepIndex}/${executionState.totalSteps}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        if (currentScript.rotateProxyOnRepeat) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.VpnLock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                    Text("Rotating Proxy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                                }
                            }
                        }
                    }

                    val progress = if (executionState.totalSteps > 0) {
                        executionState.currentStepIndex.toFloat() / executionState.totalSteps.toFloat()
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        color = Color(0xFF10B981),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    if (executionState.currentDataSummary.isNotBlank()) {
                        Text(
                            text = "Filling Data -> ${executionState.currentDataSummary}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (executionState.currentStepDescription.isNotBlank()) {
                        Text(
                            text = "Action: ${executionState.currentStepDescription}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Script Selector Card with Quick Stepper
        if (hasValidScript) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = scriptMenuExpanded,
                            onExpandedChange = { scriptMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .clickable { scriptMenuExpanded = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentScript.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val isExactSiteMatch = currentScript.targetWebsite != "*" &&
                                            websiteDomain.contains(currentScript.targetWebsite, ignoreCase = true)
                                    if (isExactSiteMatch) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "READY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${currentScript.steps.size} grabbed steps • Target: ${currentScript.targetWebsite}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            ExposedDropdownMenu(
                                expanded = scriptMenuExpanded,
                                onDismissRequest = { scriptMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "+ Create New Script for ${websiteDomain.ifBlank { "Page" }}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        scriptMenuExpanded = false
                                        onSwitchToCreateScript()
                                    }
                                )
                                if (savedScripts.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (filterWebsiteOnly) "Filter: Only ${websiteDomain.ifBlank { "this site" }} (Tap for all)"
                                                else "Filter: Showing all scripts (Tap to filter site)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = { filterWebsiteOnly = !filterWebsiteOnly }
                                    )
                                }
                                websiteMatchedScripts.forEach { script ->
                                    val isSiteMatch = script.targetWebsite != "*" &&
                                            websiteDomain.isNotBlank() &&
                                            websiteDomain.contains(script.targetWebsite, ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(script.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    if (isSiteMatch) {
                                                        Text("• Match", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Text(
                                                    "${script.steps.size} steps • ${script.targetWebsite}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectScript(script)
                                            scriptMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Stepper: repeat count
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val next = (currentScript.repeatCount - 1).coerceAtLeast(1)
                                        onUpdateCurrentScript(currentScript.copy(repeatCount = next))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${currentScript.repeatCount}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val next = currentScript.repeatCount + 1
                                        onUpdateCurrentScript(currentScript.copy(repeatCount = next))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gate 2: Data Sheet Selector or Create Data Sheet / Use Default
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (assignedSheet != null) Color(0xFF6366F1).copy(alpha = 0.08f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, if (assignedSheet != null) Color(0xFF6366F1).copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().clickable { sheetMenuExpanded = true }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                assignedSheet != null -> "Data Sheet: ${assignedSheet.name}"
                                dataSheets.isEmpty() && !confirmedDefaultData -> "No Data Sheet Created (Create or Use Default)"
                                else -> "Data Source: Default Generator"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (assignedSheet != null) Color(0xFF4F46E5) else MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = if (assignedSheet != null) {
                                "${assignedSheet.totalRows()} rows stored • Fills fields 1 by 1 on each run"
                            } else {
                                "Fills grabbed fields & checkboxes using Data Sheet or Default random values"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select sheet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Quick buttons when no Data Sheet exists yet
                if (dataSheets.isEmpty() && assignedSheet == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onOpenCreateDataSheet,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(34.dp).testTag("btn_hud_create_datasheet")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Data Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onConfirmUseDefaultData,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(34.dp).testTag("btn_hud_use_default_data")
                        ) {
                            Text(
                                text = if (confirmedDefaultData) "✓ Using Default Data" else "Use Default Data",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = sheetMenuExpanded,
                    onDismissRequest = { sheetMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("+ Create New Data Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                        },
                        onClick = {
                            sheetMenuExpanded = false
                            onOpenCreateDataSheet()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Use Default Random Generator", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Generates synthetic email, username, password & profile each run", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            onConfirmUseDefaultData()
                            sheetMenuExpanded = false
                        }
                    )
                    dataSheets.forEach { sheet ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(sheet.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${sheet.fields.keys.size} fields • ${sheet.totalRows()} rows stored (1 by 1)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                onUpdateCurrentScript(currentScript.copy(dataSheetId = sheet.id))
                                sheetMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Two-Column Configuration: 1-by-1 Order and When Finished Policy
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { orderMenuExpanded = true }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("1-by-1 Order", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentScript.iterationOrder.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = orderMenuExpanded,
                    onDismissRequest = { orderMenuExpanded = false }
                ) {
                    DataIterationOrder.entries.forEach { ord ->
                        DropdownMenuItem(
                            text = { Text(ord.label, fontSize = 12.sp) },
                            onClick = {
                                onUpdateCurrentScript(currentScript.copy(iterationOrder = ord))
                                orderMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { policyMenuExpanded = true }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("When Finished", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentScript.exhaustionPolicy.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = policyMenuExpanded,
                    onDismissRequest = { policyMenuExpanded = false }
                ) {
                    DataExhaustionPolicy.entries.forEach { pol ->
                        DropdownMenuItem(
                            text = { Text(pol.label, fontSize = 12.sp) },
                            onClick = {
                                onUpdateCurrentScript(currentScript.copy(exhaustionPolicy = pol))
                                policyMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Action Buttons Row (Strict gate: disabled when no script exists)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        ) {
            Button(
                onClick = { if (isPlaying) onStopPlay() else onStartPlay() },
                enabled = isPlaying || hasValidScript,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color(0xFFEF4444) else Color(0xFF10B981),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_hud_run_automation")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isPlaying -> "Stop Automation"
                        !hasValidScript -> "Create Script First"
                        else -> "Run Automation (${currentScript.repeatCount}x)"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onOpenScriptManager,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.height(48.dp).testTag("btn_hud_manage_studio")
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Studio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Tab Content: Create Automation (No-Type Element Grabbing & Instant Ready Use)
 */
@Composable
private fun CreateAutomationHudContent(
    executionState: AutomationExecutionState,
    currentScript: AutomationScript,
    scannedElements: List<ScannedElement>,
    dataSheets: List<DataSheet>,
    currentUrl: String,
    websiteDomain: String,
    onStartFreshScript: () -> Unit,
    onToggleRecord: () -> Unit,
    onTriggerScan: () -> Unit,
    onClearScan: () -> Unit,
    onAddScannedElement: (ScannedElement) -> Unit,
    onEditScannedElement: (ScannedElement) -> Unit,
    onAutoMapAllScanned: () -> Unit,
    onEditStep: (AutomationStep) -> Unit,
    onMoveStep: (String, Int) -> Unit,
    onDeleteStep: (String) -> Unit,
    onOpenAddNavigate: () -> Unit,
    onOpenSaveScript: () -> Unit,
    onOpenCreateDataSheet: () -> Unit,
    onNavigateWebViewToUrl: (String) -> Unit,
    onReadyAndRun: () -> Unit,
    onOpenScriptManager: () -> Unit
) {
    val isRecording = executionState.status == AutomationStatus.RECORDING
    val isOnHomeScreen = currentUrl.isBlank() || currentUrl.startsWith("aura://")
    var targetSiteInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        // If user clicked the Robot icon on the Main Screen (aura://home), let them enter a website URL right here to open & auto-create script
        if (isOnHomeScreen) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Step 1: Open a Website to Create Your Script",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = targetSiteInput,
                            onValueChange = { targetSiteInput = it },
                            placeholder = { Text("Enter website URL (e.g. github.com/signup)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val raw = targetSiteInput.trim()
                                if (raw.isNotBlank()) {
                                    onNavigateWebViewToUrl(raw)
                                }
                            },
                            enabled = targetSiteInput.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text("Open & Grab", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // No-Type Recording Helper Banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF10B981).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No Typing Required — Tap or Grab Elements",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                    Text(
                        text = "Tapped/grabbed fields, checkboxes & buttons are auto-mapped to fill from your Data Sheet or Default on start.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (dataSheets.isEmpty()) {
                    TextButton(
                        onClick = onOpenCreateDataSheet,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("+ Data Sheet", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Row 1: Primary active capture controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            Button(
                onClick = onToggleRecord,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isRecording) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f).height(44.dp).testTag("btn_record_clicks")
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = if (isRecording) Color.White else Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRecording) "Tap Elements (Live)" else "Tap to Grab Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { onTriggerScan() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF06B6D4).copy(alpha = 0.18f),
                    contentColor = Color(0xFF0891B2)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f).height(44.dp).testTag("btn_grab_elements")
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = Color(0xFF0891B2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Grab Elements",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Row 2: Workflow Actions: New Script, + Next Page / URL, Save Script
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            OutlinedButton(
                onClick = onStartFreshScript,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).testTag("btn_new_script_hud")
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Script", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onOpenAddNavigate,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1.1f).testTag("btn_add_navigate_step")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Next Page", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onOpenSaveScript,
                enabled = currentScript.steps.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1.1f).testTag("btn_save_script_hud")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Instant "Ready — Start Automation Now" button once elements are grabbed
        if (currentScript.steps.isNotEmpty()) {
            Button(
                onClick = onReadyAndRun,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .height(42.dp)
                    .testTag("btn_ready_start_from_create")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ready (${currentScript.steps.size} Elements) — Start Automation Now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Scanned DOM Elements Strip
        if (scannedElements.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "Detected Page Elements (${scannedElements.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0891B2)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onAutoMapAllScanned() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(13.dp))
                                Text("Rebuild Ready Steps", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                            }
                        }

                        Text(
                            text = "Hide",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onClearScan() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    items(scannedElements) { element ->
                        ScannedElementCard(
                            element = element,
                            onAdd = { onAddScannedElement(element) },
                            onEdit = { onEditScannedElement(element) }
                        )
                    }
                }
            }
        }

        // Captured Script Steps List in HUD
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = "Ready Script Elements (${currentScript.steps.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage in Studio →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onOpenScriptManager() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (currentScript.steps.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "No elements grabbed yet. Tap 'Grab Elements' to auto-prepare all fields, checkboxes, and buttons on the page, or simply tap elements on the page directly (no typing required).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 165.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(currentScript.steps) { index, step ->
                        EditableHudStepCard(
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
    }
}

/**
 * Dialog shown when user clicks Run Automation and no Data Sheet exists yet:
 * User can either Create a Data Sheet or Use Default Data.
 */
@Composable
fun DataSheetChoiceBeforeRunDialog(
    onDismiss: () -> Unit,
    onUseDefaultAndRun: () -> Unit,
    onCreateDataSheet: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Data Source to Fill Fields", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "No custom Data Sheet has been created yet. How should the automation engine fill your grabbed fields?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• Create Data Sheet: Paste your own emails, usernames, passwords & phone numbers (used 1 by 1).", fontSize = 11.sp)
                        Text("• Use Default Data: Automatically generate realistic random credentials for every run.", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCreateDataSheet) {
                    Text("+ Create Data Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onUseDefaultAndRun,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Use Default & Run", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Quick Dialog to create a Data Sheet directly from the HUD
 */
@Composable
fun QuickCreateDataSheetDialog(
    onDismiss: () -> Unit,
    onSaveSheet: (DataSheet) -> Unit
) {
    var sheetName by remember { mutableStateOf("My Data Sheet") }
    var emailsRaw by remember { mutableStateOf("") }
    var usernamesRaw by remember { mutableStateOf("") }
    var passwordsRaw by remember { mutableStateOf("") }
    var phonesRaw by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Data Sheet (Used 1 by 1)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = sheetName,
                        onValueChange = { sheetName = it },
                        label = { Text("Sheet Name", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val records = List(5) { AutomationDataGenerator.generateSingleRandomRecord() }
                            emailsRaw = records.joinToString("\n") { it.email }
                            usernamesRaw = records.joinToString("\n") { it.username }
                            passwordsRaw = records.joinToString("\n") { it.password }
                            phonesRaw = records.joinToString("\n") { it.phoneNumber }
                        }
                    ) {
                        Text("Auto-Fill 5 Rows", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = emailsRaw,
                    onValueChange = { emailsRaw = it },
                    label = { Text("Emails (1 per line or comma-separated)", fontSize = 11.sp) },
                    placeholder = { Text("user1@mail.com\nuser2@mail.com") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = usernamesRaw,
                    onValueChange = { usernamesRaw = it },
                    label = { Text("Usernames (1 per line)", fontSize = 11.sp) },
                    placeholder = { Text("alex_01\nalex_02") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = passwordsRaw,
                        onValueChange = { passwordsRaw = it },
                        label = { Text("Passwords", fontSize = 11.sp) },
                        placeholder = { Text("Pass#1234") },
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phonesRaw,
                        onValueChange = { phonesRaw = it },
                        label = { Text("Phones", fontSize = 11.sp) },
                        placeholder = { Text("4155551234") },
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    fun parseList(raw: String) = raw.lines()
                        .flatMap { it.split(",") }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val fieldsMap = mutableMapOf<String, List<String>>()
                    parseList(emailsRaw).takeIf { it.isNotEmpty() }?.let { fieldsMap["email"] = it }
                    parseList(usernamesRaw).takeIf { it.isNotEmpty() }?.let { fieldsMap["username"] = it }
                    parseList(passwordsRaw).takeIf { it.isNotEmpty() }?.let {
                        fieldsMap["password"] = it
                        fieldsMap["confirmPassword"] = it
                    }
                    parseList(phonesRaw).takeIf { it.isNotEmpty() }?.let { fieldsMap["phoneNumber"] = it }

                    if (fieldsMap.isEmpty()) {
                        val generated = List(4) { AutomationDataGenerator.generateSingleRandomRecord() }
                        fieldsMap["email"] = generated.map { it.email }
                        fieldsMap["username"] = generated.map { it.username }
                        fieldsMap["password"] = generated.map { it.password }
                        fieldsMap["confirmPassword"] = generated.map { it.password }
                        fieldsMap["phoneNumber"] = generated.map { it.phoneNumber }
                    }

                    onSaveSheet(
                        DataSheet(
                            id = UUID.randomUUID().toString(),
                            name = sheetName.ifBlank { "My Data Sheet" },
                            fields = fieldsMap
                        )
                    )
                }
            ) {
                Text("Save & Link Data Sheet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Step Card with direct Edit, Reorder, and Delete buttons in HUD
 */
@Composable
private fun EditableHudStepCard(
    index: Int,
    totalCount: Int,
    step: AutomationStep,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val actionColor = when (step.actionType) {
        AutomationActionType.INPUT -> Color(0xFF3B82F6)
        AutomationActionType.CLICK -> Color(0xFF10B981)
        AutomationActionType.CHECKBOX -> Color(0xFF06B6D4)
        AutomationActionType.RADIO -> Color(0xFF8B5CF6)
        AutomationActionType.NAVIGATE -> Color(0xFFEC4899)
        AutomationActionType.SELECT_DROPDOWN -> Color(0xFFD946EF)
        AutomationActionType.SELECT_COUNTRY_CODE -> Color(0xFF10B981)
        AutomationActionType.SELECT_DATE -> Color(0xFF0284C7)
        AutomationActionType.UPLOAD_FILE -> Color(0xFFF59E0B)
        AutomationActionType.SUBMIT -> Color(0xFF10B981)
        AutomationActionType.WAIT -> Color(0xFF64748B)
        AutomationActionType.CLEAR -> Color(0xFFEF4444)
        AutomationActionType.SCROLL -> Color(0xFF6366F1)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, actionColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = actionColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${index + 1}. ${step.actionType.name}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = actionColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.elementDescription.ifBlank { step.selector },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dataBindingLabel = if (!step.variableKey.isNullOrBlank()) {
                        "Fills from Data Sheet/Default: {{${step.variableKey}}} • ${step.selector}"
                    } else if (step.actionType == AutomationActionType.NAVIGATE) {
                        "Target URL: ${step.value.ifBlank { step.selector }}"
                    } else {
                        "Selector: ${step.selector}"
                    }
                    Text(
                        text = dataBindingLabel,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("btn_edit_step_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Step",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("btn_delete_step_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Step",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog to edit a captured step directly inside the HUD or Manage Script screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditDialog(
    step: AutomationStep,
    availableFieldKeys: List<String> = listOf(
        "email", "username", "password", "confirmPassword", "phoneNumber",
        "firstName", "lastName", "countryCode", "gender", "dob", "address", "city", "state", "zipCode"
    ),
    onDismiss: () -> Unit,
    onSave: (AutomationStep) -> Unit
) {
    var desc by remember { mutableStateOf(step.elementDescription) }
    var selector by remember { mutableStateOf(step.selector) }
    var varKey by remember { mutableStateOf(step.variableKey ?: "") }
    var value by remember { mutableStateOf(step.value) }
    var delayStr by remember { mutableStateOf(step.delayMs.toString()) }
    var selectedAction by remember { mutableStateOf(step.actionType) }
    var actionMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Captured Element Step", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = actionMenuOpen,
                    onExpandedChange = { actionMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = selectedAction.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action Type", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = actionMenuOpen,
                        onDismissRequest = { actionMenuOpen = false }
                    ) {
                        AutomationActionType.entries.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.name, fontSize = 12.sp) },
                                onClick = {
                                    selectedAction = action
                                    actionMenuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Step Description", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = selector,
                    onValueChange = { selector = it },
                    label = { Text("Captured Element CSS Selector / URL", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Bind to 'My Data' Field (used 1 by 1 on repeat):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableFieldKeys) { key ->
                        FilterChip(
                            selected = varKey.equals(key, ignoreCase = true),
                            onClick = {
                                varKey = key
                                value = "{{$key}}"
                                if (desc.startsWith("Fill ") || desc.isBlank()) {
                                    desc = "Fill $key"
                                }
                            },
                            label = { Text(key, fontSize = 10.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = varKey,
                        onValueChange = {
                            varKey = it
                            if (it.isNotBlank()) value = "{{$it}}"
                        },
                        label = { Text("My Data Field Key", fontSize = 11.sp) },
                        placeholder = { Text("email, username...") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = delayStr,
                        onValueChange = { delayStr = it },
                        label = { Text("Delay (ms)", fontSize = 11.sp) },
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanKey = varKey.trim().ifBlank { null }
                    onSave(
                        step.copy(
                            actionType = selectedAction,
                            elementDescription = desc,
                            selector = selector,
                            value = if (cleanKey != null) "{{$cleanKey}}" else value,
                            variableKey = cleanKey,
                            delayMs = delayStr.toLongOrNull() ?: 800L
                        )
                    )
                }
            ) {
                Text("Save Element")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to edit a grabbed DOM element right inside the HUD before or while adding it to the script.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedElementEditDialog(
    element: ScannedElement,
    availableFieldKeys: List<String>,
    onDismiss: () -> Unit,
    onSaveElementOnly: (ScannedElement) -> Unit,
    onSaveAndAddStep: (ScannedElement, String?) -> Unit
) {
    var selector by remember { mutableStateOf(element.selector) }
    var placeholder by remember { mutableStateOf(element.placeholder) }
    var semanticType by remember { mutableStateOf(element.semanticType) }
    var customFieldKey by remember {
        mutableStateOf(
            when (element.semanticType) {
                ElementSemanticType.EMAIL -> "email"
                ElementSemanticType.USERNAME -> "username"
                ElementSemanticType.PASSWORD -> "password"
                ElementSemanticType.CONFIRM_PASSWORD -> "confirmPassword"
                ElementSemanticType.PHONE -> "phoneNumber"
                ElementSemanticType.FIRST_NAME -> "firstName"
                ElementSemanticType.LAST_NAME -> "lastName"
                ElementSemanticType.FULL_NAME -> "fullName"
                ElementSemanticType.COUNTRY_CODE -> "countryCode"
                ElementSemanticType.GENDER -> "gender"
                ElementSemanticType.DOB -> "dob"
                ElementSemanticType.ADDRESS -> "address"
                ElementSemanticType.CITY -> "city"
                ElementSemanticType.STATE -> "state"
                ElementSemanticType.ZIP_PIN -> "zipCode"
                ElementSemanticType.FILE_UPLOAD -> "fileName"
                else -> ""
            }
        )
    }
    var menuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Grabbed Element", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selector,
                    onValueChange = { selector = it },
                    label = { Text("Element CSS Selector", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = menuOpen,
                    onExpandedChange = { menuOpen = it }
                ) {
                    OutlinedTextField(
                        value = semanticType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Element Type", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ElementSemanticType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name, fontSize = 11.sp) },
                                onClick = {
                                    semanticType = t
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Map to 'My Data' Field:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableFieldKeys) { key ->
                        FilterChip(
                            selected = customFieldKey == key,
                            onClick = { customFieldKey = key },
                            label = { Text(key, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        onSaveElementOnly(
                            element.copy(
                                selector = selector,
                                placeholder = placeholder,
                                semanticType = semanticType
                            )
                        )
                    }
                ) {
                    Text("Update", fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        onSaveAndAddStep(
                            element.copy(
                                selector = selector,
                                placeholder = placeholder,
                                semanticType = semanticType
                            ),
                            customFieldKey.ifBlank { null }
                        )
                    }
                ) {
                    Text("Add to Script", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to save recorded/captured elements as a named script for the website
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveScriptDialog(
    defaultName: String,
    defaultWebsite: String,
    dataSheets: List<DataSheet>,
    initialDataSheetId: String?,
    initialOrder: DataIterationOrder,
    initialPolicy: DataExhaustionPolicy,
    initialRepeatCount: Int,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        website: String,
        dataSheetId: String?,
        order: DataIterationOrder,
        policy: DataExhaustionPolicy,
        repeatCount: Int
    ) -> Unit
) {
    var scriptName by remember { mutableStateOf(defaultName) }
    var website by remember { mutableStateOf(defaultWebsite) }
    var selectedSheetId by remember { mutableStateOf(initialDataSheetId ?: dataSheets.firstOrNull()?.id) }
    var selectedOrder by remember { mutableStateOf(initialOrder) }
    var selectedPolicy by remember { mutableStateOf(initialPolicy) }
    var repeatCountStr by remember { mutableStateOf(initialRepeatCount.toString()) }

    var sheetMenuOpen by remember { mutableStateOf(false) }
    var orderMenuOpen by remember { mutableStateOf(false) }
    var policyMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Website Automation Script", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = scriptName,
                    onValueChange = { scriptName = it },
                    label = { Text("Script Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website Domain (* for all)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = repeatCountStr,
                        onValueChange = { repeatCountStr = it },
                        label = { Text("Repeats", fontSize = 11.sp) },
                        modifier = Modifier.weight(0.6f)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = sheetMenuOpen,
                    onExpandedChange = { sheetMenuOpen = it }
                ) {
                    val currentSheetName = dataSheets.firstOrNull { it.id == selectedSheetId }
                        ?.let { "${it.name} (${it.totalRows()} rows)" }
                        ?: "Default (Random Synthetic Every Time)"
                    OutlinedTextField(
                        value = currentSheetName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data Sheet (My Data)", fontSize = 11.sp) },
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
                            text = { Text("Default (Random Synthetic Every Time)", fontSize = 12.sp) },
                            onClick = {
                                selectedSheetId = null
                                sheetMenuOpen = false
                            }
                        )
                        dataSheets.forEach { sheet ->
                            DropdownMenuItem(
                                text = { Text("${sheet.name} (${sheet.totalRows()} rows)", fontSize = 12.sp) },
                                onClick = {
                                    selectedSheetId = sheet.id
                                    sheetMenuOpen = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = orderMenuOpen,
                    onExpandedChange = { orderMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = selectedOrder.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("1-by-1 Data Order", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = orderMenuOpen,
                        onDismissRequest = { orderMenuOpen = false }
                    ) {
                        DataIterationOrder.entries.forEach { ord ->
                            DropdownMenuItem(
                                text = { Text(ord.label, fontSize = 12.sp) },
                                onClick = {
                                    selectedOrder = ord
                                    orderMenuOpen = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = policyMenuOpen,
                    onExpandedChange = { policyMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = selectedPolicy.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("When Stored Data Finishes", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = policyMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = policyMenuOpen,
                        onDismissRequest = { policyMenuOpen = false }
                    ) {
                        DataExhaustionPolicy.entries.forEach { pol ->
                            DropdownMenuItem(
                                text = { Text(pol.label, fontSize = 12.sp) },
                                onClick = {
                                    selectedPolicy = pol
                                    policyMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        scriptName,
                        website,
                        selectedSheetId,
                        selectedOrder,
                        selectedPolicy,
                        (repeatCountStr.toIntOrNull() ?: 4).coerceAtLeast(1)
                    )
                },
                enabled = scriptName.isNotBlank()
            ) {
                Text("Save to Scripts")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to automate from 1 website to another or 1 page to another
 */
@Composable
fun AddNavigateDialog(
    currentUrl: String = "",
    onDismiss: () -> Unit,
    onAddOnly: (url: String) -> Unit,
    onAddAndNavigateNow: ((url: String) -> Unit)? = null
) {
    var urlInput by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Automate Next Page / Website", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Automate transitioning from 1 website to another or from Page 1 to Page 2. Tap 'Add & Load Page' to open that page now and continue grabbing its elements into this same script!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Next Page or Website URL", fontSize = 11.sp) },
                    placeholder = { Text("https://example.com/step2") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { onAddOnly(urlInput.trim()) },
                    enabled = urlInput.isNotBlank() && urlInput != "https://"
                ) {
                    Text("Add Step", fontSize = 11.sp)
                }
                if (onAddAndNavigateNow != null) {
                    Button(
                        onClick = { onAddAndNavigateNow(urlInput.trim()) },
                        enabled = urlInput.isNotBlank() && urlInput != "https://"
                    ) {
                        Text("Add & Load Page", fontSize = 11.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to paste/add new items when a field in the Data Sheet finishes during playback
 */
@Composable
fun AddExhaustedFieldDataDialog(
    fieldKey: String,
    sheetName: String,
    onDismiss: () -> Unit,
    onAddAndContinue: (List<String>) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New '$fieldKey' to $sheetName", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste 1 or 10s of new '$fieldKey' values (one per line or comma-separated). Automation will immediately resume using these 1 by 1:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    label = { Text("New '$fieldKey' values", fontSize = 11.sp) },
                    placeholder = { Text("new1@example.com\nnew2@example.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = rawInput.lines()
                        .flatMap { it.split(",") }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    onAddAndContinue(parsed)
                },
                enabled = rawInput.isNotBlank()
            ) {
                Text("Save & Continue Run")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ScannedElementCard(
    element: ScannedElement,
    onAdd: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (badgeText, badgeColor) = when (element.semanticType) {
        ElementSemanticType.EMAIL -> Pair("EMAIL", Color(0xFF06B6D4))
        ElementSemanticType.USERNAME -> Pair("USER", Color(0xFF2563EB))
        ElementSemanticType.PASSWORD -> Pair("PASS", Color(0xFF8B5CF6))
        ElementSemanticType.CONFIRM_PASSWORD -> Pair("CONFIRM", Color(0xFF9333EA))
        ElementSemanticType.PHONE -> Pair("PHONE", Color(0xFFF97316))
        ElementSemanticType.COUNTRY_CODE -> Pair("COUNTRY", Color(0xFF10B981))
        ElementSemanticType.GENDER -> Pair("GENDER", Color(0xFFEC4899))
        ElementSemanticType.DOB, ElementSemanticType.DOB_DAY, ElementSemanticType.DOB_MONTH, ElementSemanticType.DOB_YEAR -> Pair("DOB", Color(0xFF3B82F6))
        ElementSemanticType.FILE_UPLOAD -> Pair("UPLOAD", Color(0xFFF59E0B))
        ElementSemanticType.FIRST_NAME, ElementSemanticType.LAST_NAME, ElementSemanticType.FULL_NAME -> Pair("NAME", Color(0xFF14B8A6))
        ElementSemanticType.ADDRESS, ElementSemanticType.ADDRESS_LINE_2, ElementSemanticType.CITY, ElementSemanticType.STATE -> Pair("ADDRESS", Color(0xFF64748B))
        ElementSemanticType.ZIP_PIN -> Pair("ZIP", Color(0xFF6366F1))
        ElementSemanticType.CHECKBOX_TERMS -> Pair("TERMS", Color(0xFF059669))
        ElementSemanticType.CHECKBOX -> Pair("CHECKBOX", Color(0xFF06B6D4))
        ElementSemanticType.RADIO_OPTION -> Pair("RADIO", Color(0xFF8B5CF6))
        ElementSemanticType.SUBMIT_BUTTON -> Pair("SUBMIT", Color(0xFF10B981))
        ElementSemanticType.NEXT_BUTTON -> Pair("NEXT", Color(0xFF0284C7))
        ElementSemanticType.DROPDOWN -> Pair("SELECT", Color(0xFFD946EF))
        ElementSemanticType.GENERIC_BUTTON -> Pair("BUTTON", Color(0xFF64748B))
        ElementSemanticType.GENERIC_INPUT -> Pair("INPUT", Color(0xFF3B82F6))
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
        modifier = modifier
            .width(195.dp)
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit grabbed element",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add element to script",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val displayTitle = element.placeholder.ifBlank {
                element.nameAttr.ifBlank {
                    element.text.ifBlank { element.idAttr.ifBlank { element.selector } }
                }
            }

            Text(
                text = displayTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = element.selector,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
