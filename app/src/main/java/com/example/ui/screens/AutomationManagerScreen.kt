package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.example.model.AutomationStep
import com.example.model.DataExhaustionPolicy
import com.example.model.DataIterationOrder
import com.example.model.DataSheet
import com.example.ui.components.StepEditDialog
import java.util.UUID

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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Manage Scripts, 1: My Data, 2: Live Logs
    var showNewScriptDialog by remember { mutableStateOf(false) }
    var showNewDataSheetDialog by remember { mutableStateOf(false) }
    var stepToEdit by remember { mutableStateOf<AutomationStep?>(null) }
    var showAddNavigateDialog by remember { mutableStateOf(false) }
    var showAddCustomStepDialog by remember { mutableStateOf(false) }

    val availableFieldKeys = remember(dataSheets) {
        val standard = listOf(
            "email", "username", "password", "confirmPassword", "phoneNumber",
            "firstName", "lastName", "fullName", "countryCode", "gender",
            "dob", "address", "city", "state", "zipCode", "fileName"
        )
        val custom = dataSheets.flatMap { it.fields.keys }
        (standard + custom).distinct()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("automation_manager_screen")
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_manager_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Column {
                            Text(
                                text = "Automation Studio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${savedScripts.size} saved scripts • ${dataSheets.size} data sheets + Default",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onRunScript,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_manager_run_script")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Run Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text("Manage Scripts (${savedScripts.size})", fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text("My Data (${dataSheets.size})", fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text("Live Logs", fontSize = 11.sp)
                            }
                        }
                    )
                }
            }
        }

        // Body Content
        when (selectedTab) {
            0 -> ScriptsTabContent(
                currentScript = currentScript,
                savedScripts = savedScripts,
                dataSheets = dataSheets,
                onSelectScript = onSelectScript,
                onSaveScript = onSaveScript,
                onDeleteScript = onDeleteScript,
                onEditStep = { stepToEdit = it },
                onMoveStep = onMoveStep,
                onDeleteStep = onDeleteStep,
                onOpenNewScript = { showNewScriptDialog = true },
                onOpenAddNavigate = { showAddNavigateDialog = true },
                onOpenAddCustomStep = { showAddCustomStepDialog = true },
                onClearAllSteps = onClearAllSteps
            )
            1 -> MyDataTabContent(
                dataSheets = dataSheets,
                onSaveDataSheet = onSaveDataSheet,
                onDeleteDataSheet = onDeleteDataSheet,
                onOpenNewDataSheet = { showNewDataSheetDialog = true }
            )
            2 -> LogsTabContent(
                logMessages = executionState.logMessages
            )
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
            }
        )
    }

    // Add Navigate Step Dialog
    if (showAddNavigateDialog) {
        AddNavigateStepDialog(
            onDismiss = { showAddNavigateDialog = false },
            onAdd = { url ->
                onAddStep(
                    AutomationStep(
                        actionType = AutomationActionType.NAVIGATE,
                        selector = url,
                        elementDescription = "Navigate to $url",
                        value = url,
                        delayMs = 2000L
                    )
                )
                showAddNavigateDialog = false
            }
        )
    }

    // Add Custom Step Dialog
    if (showAddCustomStepDialog) {
        AddCustomStepDialog(
            availableFieldKeys = availableFieldKeys,
            onDismiss = { showAddCustomStepDialog = false },
            onAdd = { action, selector, desc, varKey, delay ->
                onAddStep(
                    AutomationStep(
                        actionType = action,
                        selector = selector,
                        elementDescription = desc.ifBlank { "${action.name}: ${selector.take(20)}" },
                        value = if (varKey != null) "{{$varKey}}" else "",
                        variableKey = varKey,
                        delayMs = delay
                    )
                )
                showAddCustomStepDialog = false
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
 * TAB 0: Saved Scripts Management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptsTabContent(
    currentScript: AutomationScript,
    savedScripts: List<AutomationScript>,
    dataSheets: List<DataSheet>,
    onSelectScript: (AutomationScript) -> Unit,
    onSaveScript: (AutomationScript) -> Unit,
    onDeleteScript: (String) -> Unit,
    onEditStep: (AutomationStep) -> Unit,
    onMoveStep: (String, Int) -> Unit,
    onDeleteStep: (String) -> Unit,
    onOpenNewScript: () -> Unit,
    onOpenAddNavigate: () -> Unit,
    onOpenAddCustomStep: () -> Unit,
    onClearAllSteps: () -> Unit
) {
    var dataSheetMenuOpen by remember { mutableStateOf(false) }
    var orderMenuOpen by remember { mutableStateOf(false) }
    var policyMenuOpen by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Saved Scripts Horizontal Selector
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text("Saved Website Scripts", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenNewScript) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("New Script", fontSize = 11.sp)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(savedScripts) { script ->
                    val isSelected = script.id == currentScript.id
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .width(215.dp)
                            .clickable { onSelectScript(script) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (script.targetWebsite == "*") "All Sites (*)" else script.targetWebsite.take(18),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                if (savedScripts.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteScript(script.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = script.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${script.steps.size} captured elements • ${script.repeatCount}x runs",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Active Script Settings Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Manage Selected Script",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentScript.name,
                            onValueChange = { onSaveScript(currentScript.copy(name = it)) },
                            label = { Text("Script Name", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = currentScript.targetWebsite,
                            onValueChange = { onSaveScript(currentScript.copy(targetWebsite = it)) },
                            label = { Text("Website (* for all)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Data Sheet Selector
                    ExposedDropdownMenuBox(
                        expanded = dataSheetMenuOpen,
                        onExpandedChange = { dataSheetMenuOpen = it }
                    ) {
                        val activeSheet = dataSheets.firstOrNull { it.id == currentScript.dataSheetId }
                        val labelText = activeSheet?.let { "${it.name} (${it.totalRows()} rows stored — used 1 by 1)" }
                            ?: "Default Section (Randomly Generated Every Step)"
                        OutlinedTextField(
                            value = labelText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Data Sheet Used in Script", fontSize = 10.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dataSheetMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dataSheetMenuOpen,
                            onDismissRequest = { dataSheetMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default Section (Randomly Generated Every Step)", fontSize = 11.sp) },
                                onClick = {
                                    onSaveScript(currentScript.copy(dataSheetId = null))
                                    dataSheetMenuOpen = false
                                }
                            )
                            dataSheets.forEach { sheet ->
                                DropdownMenuItem(
                                    text = { Text("${sheet.name} (${sheet.totalRows()} rows stored)", fontSize = 11.sp) },
                                    onClick = {
                                        onSaveScript(currentScript.copy(dataSheetId = sheet.id))
                                        dataSheetMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Ordering & Exhaustion Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // Order
                        ExposedDropdownMenuBox(
                            expanded = orderMenuOpen,
                            onExpandedChange = { orderMenuOpen = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = currentScript.iterationOrder.shortLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("1-by-1 Order", fontSize = 9.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(expanded = orderMenuOpen, onDismissRequest = { orderMenuOpen = false }) {
                                DataIterationOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label, fontSize = 11.sp) },
                                        onClick = {
                                            onSaveScript(currentScript.copy(iterationOrder = order))
                                            orderMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Exhaustion
                        ExposedDropdownMenuBox(
                            expanded = policyMenuOpen,
                            onExpandedChange = { policyMenuOpen = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = currentScript.exhaustionPolicy.shortLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("When Data Finished", fontSize = 9.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(expanded = policyMenuOpen, onDismissRequest = { policyMenuOpen = false }) {
                                DataExhaustionPolicy.entries.forEach { policy ->
                                    DropdownMenuItem(
                                        text = { Text(policy.label, fontSize = 11.sp) },
                                        onClick = {
                                            onSaveScript(currentScript.copy(exhaustionPolicy = policy))
                                            policyMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Repeat Count Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        Text("Repeat Runs (Uses next stored data 1 by 1):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onSaveScript(currentScript.copy(repeatCount = (currentScript.repeatCount - 1).coerceAtLeast(1))) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("-", fontSize = 14.sp) }

                            Text("${currentScript.repeatCount}x", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            OutlinedButton(
                                onClick = { onSaveScript(currentScript.copy(repeatCount = currentScript.repeatCount + 1)) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("+", fontSize = 14.sp) }
                        }
                    }
                }
            }
        }

        // Script Steps Header & Action Buttons (+ Website/Page, + Element Step, Clear)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text("Captured Elements (${currentScript.steps.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onOpenAddNavigate,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+ Website / Page", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenAddCustomStep,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+ Element", fontSize = 10.sp)
                    }

                    if (currentScript.steps.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAllSteps,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Steps List
        itemsIndexed(currentScript.steps) { index, step ->
            ScriptStepDetailCard(
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

/**
 * Detailed Step Card in Studio
 */
@Composable
private fun ScriptStepDetailCard(
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
        AutomationActionType.NAVIGATE -> Color(0xFF8B5CF6)
        AutomationActionType.SELECT_DROPDOWN -> Color(0xFFD946EF)
        AutomationActionType.SELECT_COUNTRY_CODE -> Color(0xFF10B981)
        AutomationActionType.SELECT_DATE -> Color(0xFF0284C7)
        AutomationActionType.UPLOAD_FILE -> Color(0xFFF59E0B)
        AutomationActionType.SUBMIT -> Color(0xFF10B981)
        AutomationActionType.WAIT -> Color(0xFF64748B)
        AutomationActionType.CLEAR -> Color(0xFFEF4444)
        AutomationActionType.SCROLL -> Color(0xFF6366F1)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = actionColor.copy(alpha = 0.15f)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Element Selector: ${step.selector}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!step.variableKey.isNullOrBlank() || step.value.isNotBlank()) {
                        Text(
                            text = if (!step.variableKey.isNullOrBlank()) {
                                "Fills from My Data field: '${step.variableKey}' (1 by 1)"
                            } else {
                                "Target / Value: ${step.value}"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                    }
                }
                if (index < totalCount - 1) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

/**
 * TAB 1: My Data (Custom Data Sheets e.g. "XYZ Data" + Default Random Generator Section)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyDataTabContent(
    dataSheets: List<DataSheet>,
    onSaveDataSheet: (DataSheet) -> Unit,
    onDeleteDataSheet: (String) -> Unit,
    onOpenNewDataSheet: () -> Unit
) {
    // "__DEFAULT__" represents viewing the Default Randomly Generated Data Sheet section
    var selectedSheetId by remember { mutableStateOf(dataSheets.firstOrNull()?.id ?: "__DEFAULT__") }
    val isViewingDefault = selectedSheetId == "__DEFAULT__" || dataSheets.isEmpty()
    val activeSheet = if (!isViewingDefault) {
        dataSheets.firstOrNull { it.id == selectedSheetId } ?: dataSheets.firstOrNull()
    } else null

    var fieldKeyToEdit by remember { mutableStateOf<String?>(null) }
    var showAddFieldDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Strip: Data Sheets horizontal selector (Custom Data Sheets + Default Random Sheet)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text("Select Data Sheet or Default Generator", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onOpenNewDataSheet,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Data Sheet", fontSize = 11.sp)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. User-created Data Sheets (e.g. "XYZ Data")
                items(dataSheets) { sheet ->
                    val isSelected = !isViewingDefault && sheet.id == activeSheet?.id
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .width(195.dp)
                            .clickable { selectedSheetId = sheet.id }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = sheet.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (dataSheets.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteDataSheet(sheet.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Text(
                                text = "${sheet.fields.keys.size} fields • ${sheet.totalRows()} items (1 by 1)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Default Section Card (Randomly Generated Data Sheet every time)
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isViewingDefault) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (isViewingDefault) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .width(210.dp)
                            .clickable { selectedSheetId = "__DEFAULT__" }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Default Data Sheet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "New random email/pass every step",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // If viewing a user-created Data Sheet (e.g. "XYZ Data")
        if (activeSheet != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = activeSheet.name,
                                onValueChange = { onSaveDataSheet(activeSheet.copy(name = it)) },
                                label = { Text("Data Sheet Name (e.g. XYZ Data)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${activeSheet.totalRows()} Max Rows",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Text(
                            text = "Inside '${activeSheet.name}', add fields like email, username, password, etc. Paste or upload 10s of items inside each field. When present, these stored values are ALWAYS used 1 by 1 (Default is NOT taken unless finished and you select Continue Using Default).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Iteration Order Selector (Top to Bottom, Bottom to Top, Random)
                            var orderMenu by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = orderMenu,
                                onExpandedChange = { orderMenu = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = activeSheet.iterationOrder.shortLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("1-by-1 Order", fontSize = 9.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(expanded = orderMenu, onDismissRequest = { orderMenu = false }) {
                                    DataIterationOrder.entries.forEach { ord ->
                                        DropdownMenuItem(
                                            text = { Text(ord.label, fontSize = 11.sp) },
                                            onClick = {
                                                onSaveDataSheet(activeSheet.copy(iterationOrder = ord))
                                                orderMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Exhaustion Policy Selector (Stop, Continue Default, Add New, Loop)
                            var policyMenu by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = policyMenu,
                                onExpandedChange = { policyMenu = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = activeSheet.exhaustionPolicy.shortLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("When Finished", fontSize = 9.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(expanded = policyMenu, onDismissRequest = { policyMenu = false }) {
                                    DataExhaustionPolicy.entries.forEach { pol ->
                                        DropdownMenuItem(
                                            text = { Text(pol.label, fontSize = 11.sp) },
                                            onClick = {
                                                onSaveDataSheet(activeSheet.copy(exhaustionPolicy = pol))
                                                policyMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fields List in Sheet
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        "Fields inside '${activeSheet.name}' (Tap to Paste / Upload 10s of items)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showAddFieldDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("+ Add Field", fontSize = 10.sp)
                    }
                }
            }

            val standardFieldKeys = listOf(
                "email" to "Email Field (email)",
                "username" to "Username Field (username)",
                "password" to "Password Field (password)",
                "phoneNumber" to "Phone Number Field (phoneNumber)",
                "firstName" to "First Name Field (firstName)",
                "lastName" to "Last Name Field (lastName)",
                "countryCode" to "Country Code Field (countryCode)",
                "gender" to "Gender Field (gender)",
                "dob" to "Date of Birth Field (dob)",
                "address" to "Address Field (address)",
                "city" to "City Field (city)",
                "state" to "State Field (state)",
                "zipCode" to "ZIP / PIN Code Field (zipCode)",
                "fileName" to "Upload File Name Field (fileName)"
            )

            items(standardFieldKeys) { (key, label) ->
                val values = activeSheet.getFieldValues(key)
                DataFieldCard(
                    fieldKey = key,
                    fieldLabel = label,
                    values = values,
                    onEdit = { fieldKeyToEdit = key }
                )
            }

            // Custom fields defined outside standard
            val customKeys = activeSheet.fields.keys.filterNot { k -> standardFieldKeys.any { it.first == k } }
            items(customKeys) { key ->
                val values = activeSheet.getFieldValues(key)
                DataFieldCard(
                    fieldKey = key,
                    fieldLabel = "Custom Field ($key)",
                    values = values,
                    onEdit = { fieldKeyToEdit = key }
                )
            }
        }

        // DEFAULT SECTION: Randomly Generated Data Sheet every time
        item {
            DefaultRandomDataSheetSection()
        }
    }

    // Edit Field Values Dialog (Supports manual paste of 10s of items AND file upload .txt / .csv)
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
 * Dedicated Card explaining and previewing the Default Section (Randomly Generated Data Sheet every time)
 */
@Composable
private fun DefaultRandomDataSheetSection() {
    var sampleList by remember {
        mutableStateOf(
            List(3) { AutomationDataGenerator.generateSingleRandomRecord() }
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Default Section: Dynamic Random Data Sheet",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = {
                        sampleList = List(3) { AutomationDataGenerator.generateSingleRandomRecord() }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Preview New", fontSize = 10.sp)
                }
            }

            Text(
                text = "How Default works: Unlike your custom Data Sheets (like 'XYZ Data'), the Default Data Sheet dynamically generates brand-new, unique values for email, username, password, phone, DOB, and address on every single iteration. When a field IS present in your custom Data Sheet, Default is NOT used unless your stored list finishes and you choose 'Continue Using Default'.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            sampleList.forEachIndexed { idx, rec ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Random Run #${idx + 1} (Generated on the fly):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "email: ${rec.email}  |  username: ${rec.username}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "password: ${rec.password}  |  phone: ${rec.countryCode} ${rec.phoneNumber}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data Field Card with indexed stored values (#1, #2, #3, #4...)
 */
@Composable
private fun DataFieldCard(
    fieldKey: String,
    fieldLabel: String,
    values: List<String>,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = fieldLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (values.isNotEmpty()) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (values.isNotEmpty()) "${values.size} stored (Used 1 by 1)" else "Empty (Uses Default)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (values.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Values", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                }
            }

            if (values.isEmpty()) {
                Text(
                    text = "No stored values in this field. Tap to paste or upload 10s of $fieldKey items.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    itemsIndexed(values.take(8)) { idx, valStr ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = valStr,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    if (values.size > 8) {
                        item {
                            Text(
                                "+${values.size - 8} more",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
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

    // File Picker Launcher for uploading .txt / .csv lists of emails, usernames, passwords, etc.
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste manually or upload a file (.txt / .csv) with 10s of '$fieldKey' entries. These will be consumed 1 by 1 (Default is not used while these are present).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Upload File & Quick Append 10 Sample Items Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("text/*") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload File (.txt/.csv)", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val generatedBatch = (1..10).map {
                                AutomationDataGenerator.generateSingleFieldValue(fieldKey)
                            }.joinToString("\n")
                            rawText = if (rawText.isBlank()) generatedBatch else "$rawText\n$generatedBatch"
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("+10 Sample Items", fontSize = 10.sp)
                    }
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Stored '$fieldKey' List (1 per line or comma-separated)", fontSize = 11.sp) },
                    placeholder = { Text("item1\nitem2\nitem3\nitem4") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
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
                        text = "Total stored items: $detectedCount (Used 1 by 1)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (rawText.isNotBlank()) {
                        Text(
                            text = "Clear All",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { rawText = "" }
                        )
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
 * TAB 2: Live Logs
 */
@Composable
private fun LogsTabContent(
    logMessages: List<String>
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (logMessages.isEmpty()) {
            item {
                Text(
                    text = "No execution logs yet. Run an automation script to inspect 1-by-1 field consumption and page actions.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logMessages) { msg ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
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

    var sheetMenuOpen by remember { mutableStateOf(false) }
    var orderMenuOpen by remember { mutableStateOf(false) }
    var policyMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Website Automation Script", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Script Name", fontSize = 11.sp) },
                    placeholder = { Text("E.g. Website X Registration") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Target Website Domain (* for any)", fontSize = 11.sp) },
                    placeholder = { Text("x.com or github.com") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = sheetMenuOpen,
                    onExpandedChange = { sheetMenuOpen = it }
                ) {
                    val label = dataSheets.firstOrNull { it.id == sheetId }?.name ?: "Default (Random Every Time)"
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data Sheet", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = sheetMenuOpen, onDismissRequest = { sheetMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Default (Random Every Time)", fontSize = 11.sp) },
                            onClick = {
                                sheetId = null
                                sheetMenuOpen = false
                            }
                        )
                        dataSheets.forEach { sheet ->
                            DropdownMenuItem(
                                text = { Text("${sheet.name} (${sheet.totalRows()} rows)", fontSize = 11.sp) },
                                onClick = {
                                    sheetId = sheet.id
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
                        value = order.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("1-by-1 Order", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = orderMenuOpen, onDismissRequest = { orderMenuOpen = false }) {
                        DataIterationOrder.entries.forEach { ord ->
                            DropdownMenuItem(
                                text = { Text(ord.label, fontSize = 11.sp) },
                                onClick = {
                                    order = ord
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
                        value = policy.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("When Stored Data Finishes", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = policyMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = policyMenuOpen, onDismissRequest = { policyMenuOpen = false }) {
                        DataExhaustionPolicy.entries.forEach { pol ->
                            DropdownMenuItem(
                                text = { Text(pol.label, fontSize = 11.sp) },
                                onClick = {
                                    policy = pol
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
                onClick = { onCreate(name, website, sheetId, order, policy) },
                enabled = name.isNotBlank()
            ) {
                Text("Create Script")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to add an element step in Studio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomStepDialog(
    availableFieldKeys: List<String>,
    onDismiss: () -> Unit,
    onAdd: (action: AutomationActionType, selector: String, desc: String, varKey: String?, delay: Long) -> Unit
) {
    var selectedAction by remember { mutableStateOf(AutomationActionType.INPUT) }
    var selector by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var varKey by remember { mutableStateOf("email") }
    var delayStr by remember { mutableStateOf("800") }
    var actionMenuOpen by remember { mutableStateOf(false) }
    var fieldMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Captured Element Step", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
                    ExposedDropdownMenu(expanded = actionMenuOpen, onDismissRequest = { actionMenuOpen = false }) {
                        AutomationActionType.entries.forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act.name, fontSize = 11.sp) },
                                onClick = {
                                    selectedAction = act
                                    actionMenuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = selector,
                    onValueChange = { selector = it },
                    label = { Text("Element CSS Selector", fontSize = 11.sp) },
                    placeholder = { Text("#email, input[name='username']") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Step Description", fontSize = 11.sp) },
                    placeholder = { Text("Fill Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Field from My Data
                ExposedDropdownMenuBox(
                    expanded = fieldMenuOpen,
                    onExpandedChange = { fieldMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = varKey,
                        onValueChange = { varKey = it },
                        label = { Text("My Data Field Key (Leave blank for clicks)", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(expanded = fieldMenuOpen, onDismissRequest = { fieldMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("None (Click / Submit / Wait)", fontSize = 11.sp) },
                            onClick = {
                                varKey = ""
                                fieldMenuOpen = false
                            }
                        )
                        availableFieldKeys.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(key, fontSize = 11.sp) },
                                onClick = {
                                    varKey = key
                                    fieldMenuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = delayStr,
                    onValueChange = { delayStr = it },
                    label = { Text("Delay (ms)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        selectedAction,
                        selector,
                        desc,
                        varKey.trim().ifBlank { null },
                        delayStr.toLongOrNull() ?: 800L
                    )
                },
                enabled = selector.isNotBlank() || selectedAction == AutomationActionType.WAIT
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
        title = { Text("Add Website / Page Transition", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Automate moving from 1 website to another or from 1 page to another:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Next Website or Page URL", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(urlInput.trim()) },
                enabled = urlInput.isNotBlank() && urlInput != "https://"
            ) {
                Text("Add Navigation Step")
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDataSheetDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, order: DataIterationOrder, policy: DataExhaustionPolicy) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var order by remember { mutableStateOf(DataIterationOrder.TOP_TO_BOTTOM) }
    var policy by remember { mutableStateOf(DataExhaustionPolicy.ADD_NEW) }
    var orderMenuOpen by remember { mutableStateOf(false) }
    var policyMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Data Sheet", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Data Sheet Name", fontSize = 11.sp) },
                    placeholder = { Text("E.g. XYZ Data") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = orderMenuOpen,
                    onExpandedChange = { orderMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = order.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("1-by-1 Consumption Order", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = orderMenuOpen, onDismissRequest = { orderMenuOpen = false }) {
                        DataIterationOrder.entries.forEach { ord ->
                            DropdownMenuItem(text = { Text(ord.label, fontSize = 11.sp) }, onClick = { order = ord; orderMenuOpen = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = policyMenuOpen,
                    onExpandedChange = { policyMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = policy.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("When Stored Data Finishes", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = policyMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = policyMenuOpen, onDismissRequest = { policyMenuOpen = false }) {
                        DataExhaustionPolicy.entries.forEach { pol ->
                            DropdownMenuItem(text = { Text(pol.label, fontSize = 11.sp) }, onClick = { policy = pol; policyMenuOpen = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, order, policy) },
                enabled = name.isNotBlank()
            ) {
                Text("Create Data Sheet")
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
                label = { Text("Field Name", fontSize = 11.sp) },
                placeholder = { Text("e.g. email, username, password, company") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(fieldKey.trim()) },
                enabled = fieldKey.isNotBlank()
            ) {
                Text("Add Field & Enter Values")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
