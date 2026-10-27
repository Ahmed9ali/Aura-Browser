package com.example.automation

import android.webkit.JavascriptInterface
import android.webkit.WebView
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.net.URI
import java.util.UUID
import kotlin.random.Random

class AutomationEngine(
    private val scope: CoroutineScope
) {
    private val _executionState = MutableStateFlow(AutomationExecutionState())
    val executionState: StateFlow<AutomationExecutionState> = _executionState.asStateFlow()

    // 1. "My Data" Data Sheets - Starts empty for fresh app
    private val _dataSheets = MutableStateFlow<List<DataSheet>>(emptyList())
    val dataSheets: StateFlow<List<DataSheet>> = _dataSheets.asStateFlow()

    // 2. Saved Scripts - Starts empty for fresh app; engine cannot start until a script is created
    private val _savedScripts = MutableStateFlow<List<AutomationScript>>(emptyList())
    val savedScripts: StateFlow<List<AutomationScript>> = _savedScripts.asStateFlow()

    private val _currentScript = MutableStateFlow(
        AutomationScript(
            id = UUID.randomUUID().toString(),
            name = "New Automation Flow",
            targetWebsite = "*",
            steps = emptyList(),
            repeatCount = 1
        )
    )
    val currentScript: StateFlow<AutomationScript> = _currentScript.asStateFlow()

    private val _scannedElements = MutableStateFlow<List<ScannedElement>>(emptyList())
    val scannedElements: StateFlow<List<ScannedElement>> = _scannedElements.asStateFlow()

    private var playbackJob: Job? = null
    private var exhaustionDecisionDeferred: CompletableDeferred<ExhaustionDecision>? = null
    private var lastKnownPageUrl: String = ""
    private var autoReadyOnNextScan: Boolean = true

    var onRotateProxy: (() -> Unit)? = null
    var webViewProvider: (() -> WebView?)? = null

    private sealed class ExhaustionDecision {
        object Stop : ExhaustionDecision()
        object ContinueWithDefault : ExhaustionDecision()
        data class AddedNewData(val newValues: List<String>) : ExhaustionDecision()
    }

    // JavaScript Bridge exposed to WebView
    inner class AutomationJsInterface {
        @JavascriptInterface
        fun onUserAction(actionType: String, selector: String, description: String, fieldKeyFromJs: String) {
            if (_executionState.value.status != AutomationStatus.RECORDING &&
                _executionState.value.hudMode != HudMode.CREATE_AUTOMATION
            ) return

            val type = when (actionType.uppercase()) {
                "INPUT" -> AutomationActionType.INPUT
                "SELECT_DROPDOWN" -> AutomationActionType.SELECT_DROPDOWN
                "SELECT_COUNTRY_CODE" -> AutomationActionType.SELECT_COUNTRY_CODE
                "SELECT_DATE" -> AutomationActionType.SELECT_DATE
                "UPLOAD_FILE" -> AutomationActionType.UPLOAD_FILE
                "SUBMIT" -> AutomationActionType.SUBMIT
                "CHECKBOX" -> AutomationActionType.CHECKBOX
                "RADIO" -> AutomationActionType.RADIO
                else -> AutomationActionType.CLICK
            }

            // Only elements and their semantic field keys are captured (no user typing required)
            val resolvedKey = fieldKeyFromJs.ifBlank {
                detectVariableKeyFromSelector(selector, description)
                    ?: if (type == AutomationActionType.INPUT) "username" else ""
            }.ifBlank { null }

            val newStep = AutomationStep(
                actionType = type,
                selector = selector,
                elementDescription = description,
                value = if (resolvedKey != null) "{{$resolvedKey}}" else "",
                variableKey = resolvedKey,
                pageUrl = lastKnownPageUrl,
                delayMs = 750L
            )

            scope.launch {
                val currentSteps = _currentScript.value.steps
                // Prevent duplicate capture of the exact same input/checkbox/radio selector
                val existingIdx = currentSteps.indexOfFirst {
                    it.selector == newStep.selector &&
                            it.pageUrl == newStep.pageUrl &&
                            (it.actionType == newStep.actionType ||
                                    it.actionType == AutomationActionType.INPUT ||
                                    it.actionType == AutomationActionType.CHECKBOX ||
                                    it.actionType == AutomationActionType.RADIO)
                }

                val updatedSteps = if (existingIdx >= 0 && type != AutomationActionType.CLICK) {
                    currentSteps.toMutableList().apply { set(existingIdx, newStep.copy(id = currentSteps[existingIdx].id)) }
                } else {
                    val lastStep = currentSteps.lastOrNull()
                    if (lastStep != null && lastStep.selector == newStep.selector && lastStep.actionType == newStep.actionType) {
                        return@launch
                    }
                    currentSteps + newStep
                }

                val domain = extractDomain(lastKnownPageUrl)
                val updatedScriptName = if (_currentScript.value.name == "New Automation Flow" && domain.isNotBlank() && !domain.contains("home")) {
                    "$domain Automation"
                } else {
                    _currentScript.value.name
                }
                val updatedTargetWebsite = if (_currentScript.value.targetWebsite == "*" && domain.isNotBlank() && !domain.contains("home")) {
                    domain
                } else {
                    _currentScript.value.targetWebsite
                }

                _currentScript.value = _currentScript.value.copy(
                    name = updatedScriptName,
                    targetWebsite = updatedTargetWebsite,
                    targetUrl = lastKnownPageUrl.ifBlank { _currentScript.value.targetUrl },
                    steps = updatedSteps
                )
                syncCurrentScriptToSavedScripts()
                addLog("Grabbed ready element: $description -> ${resolvedKey?.let { "Data '{{$it}}'" } ?: type.name}")
            }
        }

        @JavascriptInterface
        fun onScanResults(jsonString: String) {
            scope.launch {
                try {
                    val jsonArray = JSONArray(jsonString)
                    val elements = mutableListOf<ScannedElement>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val semanticStr = obj.optString("semanticType", "GENERIC_INPUT")
                        val semanticEnum = try {
                            ElementSemanticType.valueOf(semanticStr)
                        } catch (e: Exception) {
                            ElementSemanticType.GENERIC_INPUT
                        }

                        val optionsList = mutableListOf<String>()
                        val optsArray = obj.optJSONArray("detectedOptions")
                        if (optsArray != null) {
                            for (j in 0 until optsArray.length()) {
                                optionsList.add(optsArray.optString(j))
                            }
                        }

                        elements.add(
                            ScannedElement(
                                tagName = obj.optString("tagName", ""),
                                type = obj.optString("type", ""),
                                nameAttr = obj.optString("nameAttr", ""),
                                idAttr = obj.optString("idAttr", ""),
                                placeholder = obj.optString("placeholder", ""),
                                text = obj.optString("text", ""),
                                selector = obj.optString("selector", ""),
                                pageUrl = obj.optString("pageUrl", lastKnownPageUrl),
                                isInput = obj.optBoolean("isInput", false),
                                isButton = obj.optBoolean("isButton", false),
                                semanticType = semanticEnum,
                                detectedOptions = optionsList
                            )
                        )
                    }
                    _scannedElements.value = elements

                    // Automatically convert grabbed elements into ready-to-use script steps if requested or if script is empty
                    if (elements.isNotEmpty() && (autoReadyOnNextScan || _currentScript.value.steps.isEmpty())) {
                        autoReadyOnNextScan = false
                        buildReadyStepsFromGrabbedElements(elements)
                    } else {
                        addLog("Grabbed ${elements.size} elements on page (ready to use with Data Sheet or Default)")
                    }

                    if (_executionState.value.status == AutomationStatus.SCANNING) {
                        _executionState.value = _executionState.value.copy(status = AutomationStatus.RECORDING)
                    }
                } catch (e: Exception) {
                    addLog("Scanner parse error: ${e.message}")
                }
            }
        }
    }

    val jsInterface = AutomationJsInterface()

    /**
     * Converts grabbed page elements directly into an ordered, ready-to-use workflow:
     * 1. All Input / Dropdown / Date / Country / Upload fields (bound to Data Sheet variables)
     * 2. Checkboxes & Radio buttons (e.g., Terms & Conditions, options)
     * 3. Primary Submit / Next / Continue button
     */
    private fun buildReadyStepsFromGrabbedElements(elements: List<ScannedElement>) {
        if (elements.isEmpty()) return

        val inputAndSelectElements = elements.filter {
            it.isInput ||
                    it.semanticType == ElementSemanticType.DROPDOWN ||
                    it.semanticType == ElementSemanticType.GENDER ||
                    it.semanticType == ElementSemanticType.COUNTRY_CODE ||
                    it.semanticType == ElementSemanticType.DOB ||
                    it.semanticType == ElementSemanticType.DOB_DAY ||
                    it.semanticType == ElementSemanticType.DOB_MONTH ||
                    it.semanticType == ElementSemanticType.DOB_YEAR ||
                    it.semanticType == ElementSemanticType.FILE_UPLOAD
        }

        val checkAndRadioElements = elements.filter {
            it.semanticType == ElementSemanticType.CHECKBOX_TERMS ||
                    it.semanticType == ElementSemanticType.CHECKBOX ||
                    it.semanticType == ElementSemanticType.RADIO_OPTION
        }

        // Pick the primary submit or next button (or first generic button if no explicit submit/next exists)
        val primarySubmitOrNext = elements.firstOrNull { it.semanticType == ElementSemanticType.SUBMIT_BUTTON }
            ?: elements.firstOrNull { it.semanticType == ElementSemanticType.NEXT_BUTTON }
            ?: elements.firstOrNull { it.isButton && it.semanticType == ElementSemanticType.GENERIC_BUTTON }

        val orderedActionable = buildList {
            addAll(inputAndSelectElements)
            addAll(checkAndRadioElements)
            if (primarySubmitOrNext != null) add(primarySubmitOrNext)
        }.distinctBy { it.selector }

        val targetList = orderedActionable.ifEmpty { elements }

        val readySteps = targetList.map { element ->
            val (actionType, varKey, defaultVal, desc) = mapScannedElementDetails(element)
            AutomationStep(
                actionType = actionType,
                selector = element.selector,
                elementDescription = desc,
                value = defaultVal,
                variableKey = varKey,
                pageUrl = element.pageUrl.ifBlank { lastKnownPageUrl },
                delayMs = if (actionType == AutomationActionType.SUBMIT) 1300L else 750L
            )
        }

        val existingSteps = _currentScript.value.steps
        val hasMultiPageNavigate = existingSteps.any { it.actionType == AutomationActionType.NAVIGATE }
        val finalSteps = if (hasMultiPageNavigate) {
            // Keep steps from earlier pages and append new page's steps
            val existingSelectorsOnPage = existingSteps.filter { it.pageUrl == lastKnownPageUrl }.map { it.selector }.toSet()
            existingSteps + readySteps.filter { it.selector !in existingSelectorsOnPage }
        } else {
            readySteps
        }

        val domain = extractDomain(lastKnownPageUrl)
        val updatedName = if (_currentScript.value.name == "New Automation Flow" && domain.isNotBlank() && !domain.contains("home")) {
            "$domain Automation"
        } else {
            _currentScript.value.name
        }
        val updatedSite = if (_currentScript.value.targetWebsite == "*" && domain.isNotBlank() && !domain.contains("home")) {
            domain
        } else {
            _currentScript.value.targetWebsite
        }

        _currentScript.value = _currentScript.value.copy(
            name = updatedName,
            targetWebsite = updatedSite,
            targetUrl = lastKnownPageUrl.ifBlank { _currentScript.value.targetUrl },
            steps = finalSteps,
            dataSheetId = _currentScript.value.dataSheetId ?: _dataSheets.value.firstOrNull()?.id
        )
        syncCurrentScriptToSavedScripts()

        addLog("Created script '${_currentScript.value.name}' with ${readySteps.size} ready elements (fields + checkboxes + buttons)!")
    }

    private fun syncCurrentScriptToSavedScripts() {
        val current = _currentScript.value
        if (current.steps.isEmpty()) return
        val existingIdx = _savedScripts.value.indexOfFirst { it.id == current.id }
        if (existingIdx >= 0) {
            _savedScripts.value = _savedScripts.value.toMutableList().apply { set(existingIdx, current) }
        } else {
            _savedScripts.value = listOf(current) + _savedScripts.value
        }
    }

    private fun detectVariableKeyFromSelector(selector: String, description: String): String? {
        val combined = "$selector $description".lowercase()
        return when {
            combined.contains("email") || combined.contains("mail") -> "email"
            combined.contains("confirm") && combined.contains("pass") -> "confirmPassword"
            combined.contains("pass") -> "password"
            combined.contains("user") || combined.contains("login") || combined.contains("handle") -> "username"
            combined.contains("first") && combined.contains("name") -> "firstName"
            combined.contains("last") && combined.contains("name") -> "lastName"
            combined.contains("full") && combined.contains("name") -> "fullName"
            combined.contains("country") && (combined.contains("code") || combined.contains("dial")) -> "countryCode"
            combined.contains("phone") || combined.contains("mobile") || combined.contains("tel") -> "phoneNumber"
            combined.contains("gender") || combined.contains("sex") -> "gender"
            combined.contains("birth") || combined.contains("dob") -> "dob"
            combined.contains("zip") || combined.contains("postal") || combined.contains("pin") -> "zipCode"
            combined.contains("city") -> "city"
            combined.contains("state") -> "state"
            combined.contains("address") -> "address"
            combined.contains("file") || combined.contains("upload") || combined.contains("resume") -> "fileName"
            else -> null
        }
    }

    /**
     * Called when WebView finishes loading a page (supports Page 1 -> Page 2 and Website 1 -> Website 2).
     */
    fun onPageLoaded(url: String) {
        if (url.isBlank() || url.startsWith("aura://")) return
        val previousUrl = lastKnownPageUrl
        lastKnownPageUrl = url

        // If user is in Create Automation, Recording mode, or HUD is visible with no script created yet:
        if (_executionState.value.hudMode == HudMode.CREATE_AUTOMATION ||
            _executionState.value.status == AutomationStatus.RECORDING ||
            (_executionState.value.isHudVisible && _savedScripts.value.isEmpty())
        ) {
            if (previousUrl.isNotBlank() && !previousUrl.equals(url, ignoreCase = true)) {
                addLog("Page loaded: $url (Grabbing page elements...)")
                autoReadyOnNextScan = true
            }
            injectJs(AutomationScripts.RECORDING_INJECTION)
            triggerScan(autoReadySteps = _currentScript.value.steps.isEmpty() || autoReadyOnNextScan)

            // Schedule follow-up scans for dynamic SPA pages that render form fields after onPageFinished
            scope.launch {
                delay(900L)
                if (_currentScript.value.steps.isEmpty() &&
                    (_executionState.value.hudMode == HudMode.CREATE_AUTOMATION || _executionState.value.isHudVisible)
                ) {
                    injectJs(AutomationScripts.RECORDING_INJECTION)
                    triggerScan(autoReadySteps = true)
                }
                delay(1200L)
                if (_currentScript.value.steps.isEmpty() &&
                    (_executionState.value.hudMode == HudMode.CREATE_AUTOMATION || _executionState.value.isHudVisible)
                ) {
                    injectJs(AutomationScripts.RECORDING_INJECTION)
                    triggerScan(autoReadySteps = true)
                }
            }
        } else if (_executionState.value.isHudVisible && _executionState.value.status == AutomationStatus.IDLE) {
            autoSelectScriptForWebsite(url)
        }
    }

    fun autoSelectScriptForWebsite(url: String) {
        val domain = extractDomain(url)
        if (domain.isBlank()) return
        val matching = _savedScripts.value.firstOrNull {
            it.targetWebsite != "*" &&
                    it.targetWebsite.isNotBlank() &&
                    (domain.contains(it.targetWebsite, ignoreCase = true) ||
                            it.targetWebsite.contains(domain, ignoreCase = true))
        }
        if (matching != null && _currentScript.value.id != matching.id) {
            _currentScript.value = matching
            addLog("Matched saved script '${matching.name}' for $domain")
        }
    }

    // HUD Mode & Visibility
    fun setHudMode(mode: HudMode, currentUrl: String = lastKnownPageUrl) {
        _executionState.value = _executionState.value.copy(hudMode = mode)
        if (mode == HudMode.CREATE_AUTOMATION) {
            val domain = extractDomain(currentUrl)
            if (domain.isNotBlank() && !domain.contains("home")) {
                if (_currentScript.value.targetWebsite == "*") {
                    _currentScript.value = _currentScript.value.copy(
                        name = if (_currentScript.value.name == "New Automation Flow") "$domain Automation" else _currentScript.value.name,
                        targetWebsite = domain,
                        targetUrl = currentUrl
                    )
                }
            }
            startRecording()
            triggerScan(autoReadySteps = _currentScript.value.steps.isEmpty())
        } else {
            stopRecording()
        }
    }

    fun startFreshRecordingScript(currentUrl: String) {
        val domain = extractDomain(currentUrl).ifBlank { "*" }
        val fresh = AutomationScript(
            id = UUID.randomUUID().toString(),
            name = if (domain != "*") "$domain Automation" else "Custom Web Automation",
            targetWebsite = domain,
            targetUrl = currentUrl,
            steps = emptyList(),
            dataSheetId = _dataSheets.value.firstOrNull()?.id,
            repeatCount = 4,
            iterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
            exhaustionPolicy = DataExhaustionPolicy.ADD_NEW
        )
        _currentScript.value = fresh
        autoReadyOnNextScan = true
        startRecording()
        triggerScan(autoReadySteps = true)
        addLog("Created new script for $domain. Grabbing page elements (no typing needed)...")
    }

    fun toggleHud(currentUrl: String = lastKnownPageUrl) {
        val next = !_executionState.value.isHudVisible
        if (next && currentUrl.isNotBlank()) {
            lastKnownPageUrl = currentUrl
            autoSelectScriptForWebsite(currentUrl)
        }
        val hasAnyScript = _savedScripts.value.isNotEmpty() || _currentScript.value.steps.isNotEmpty()
        val targetMode = if (!hasAnyScript) HudMode.CREATE_AUTOMATION else _executionState.value.hudMode

        _executionState.value = _executionState.value.copy(
            isHudVisible = next,
            hudMode = targetMode
        )

        if (next && !hasAnyScript) {
            addLog("No scripts found. Create a script by grabbing or tapping page elements (no typing required).")
            startRecording()
            triggerScan(autoReadySteps = true)
        }
    }

    fun setHudVisible(visible: Boolean) {
        val hasAnyScript = _savedScripts.value.isNotEmpty() || _currentScript.value.steps.isNotEmpty()
        val targetMode = if (visible && !hasAnyScript) HudMode.CREATE_AUTOMATION else _executionState.value.hudMode
        _executionState.value = _executionState.value.copy(
            isHudVisible = visible,
            hudMode = targetMode
        )
        if (visible && !hasAnyScript) {
            startRecording()
            triggerScan(autoReadySteps = true)
        }
    }

    // Scripts filtering for website
    fun getScriptsForCurrentUrl(url: String): List<AutomationScript> {
        val host = extractDomain(url)
        val exactMatches = _savedScripts.value.filter {
            it.targetWebsite != "*" &&
                    it.targetWebsite.isNotBlank() &&
                    host.isNotBlank() &&
                    (host.contains(it.targetWebsite, ignoreCase = true) ||
                            it.targetWebsite.contains(host, ignoreCase = true) ||
                            (it.targetUrl.isNotBlank() && url.contains(it.targetUrl, ignoreCase = true)))
        }
        val universalMatches = _savedScripts.value.filter {
            it.targetWebsite == "*" || it.targetWebsite.isBlank()
        }
        return (exactMatches + universalMatches).distinctBy { it.id }
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.") ?: ""
        } catch (e: Exception) {
            url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
        }
    }

    fun selectScript(script: AutomationScript) {
        _currentScript.value = script
        addLog("Selected script: ${script.name} (${script.steps.size} steps)")
    }

    fun saveCurrentAsScript(
        name: String,
        targetWebsite: String,
        dataSheetId: String?,
        iterationOrder: DataIterationOrder = _currentScript.value.iterationOrder,
        exhaustionPolicy: DataExhaustionPolicy = _currentScript.value.exhaustionPolicy,
        repeatCount: Int = _currentScript.value.repeatCount
    ) {
        val current = _currentScript.value
        val existingByNameAndSite = _savedScripts.value.firstOrNull {
            it.id == current.id || (it.name.equals(name, ignoreCase = true) && it.targetWebsite.equals(targetWebsite, ignoreCase = true))
        }
        val scriptId = if (existingByNameAndSite != null && current.name == name) current.id else UUID.randomUUID().toString()

        val newScript = current.copy(
            id = scriptId,
            name = name.ifBlank { "Website Automation" },
            targetWebsite = targetWebsite.ifBlank { "*" },
            targetUrl = lastKnownPageUrl.ifBlank { current.targetUrl },
            dataSheetId = dataSheetId,
            iterationOrder = iterationOrder,
            exhaustionPolicy = exhaustionPolicy,
            repeatCount = repeatCount
        )
        val existingIndex = _savedScripts.value.indexOfFirst { it.id == newScript.id }
        val updatedList = if (existingIndex >= 0) {
            _savedScripts.value.toMutableList().apply { set(existingIndex, newScript) }
        } else {
            listOf(newScript) + _savedScripts.value
        }
        _savedScripts.value = updatedList
        _currentScript.value = newScript
        addLog("Saved script '${newScript.name}' (${newScript.steps.size} steps) for '${newScript.targetWebsite}'")
    }

    fun deleteScript(scriptId: String) {
        _savedScripts.value = _savedScripts.value.filter { it.id != scriptId }
        if (_currentScript.value.id == scriptId) {
            _currentScript.value = _savedScripts.value.firstOrNull() ?: AutomationScript(
                id = UUID.randomUUID().toString(),
                name = "New Automation Flow",
                targetWebsite = "*",
                steps = emptyList(),
                repeatCount = 1
            )
        }
        addLog("Deleted script.")
    }

    // Data Sheets Management ("My Data")
    fun saveDataSheet(sheet: DataSheet) {
        val existingIndex = _dataSheets.value.indexOfFirst { it.id == sheet.id }
        val updated = if (existingIndex >= 0) {
            _dataSheets.value.toMutableList().apply { set(existingIndex, sheet) }
        } else {
            _dataSheets.value + sheet
        }
        _dataSheets.value = updated
        // Automatically link newly created Data Sheet to current script if none was linked
        if (_currentScript.value.dataSheetId == null) {
            _currentScript.value = _currentScript.value.copy(dataSheetId = sheet.id)
        }
        addLog("Saved Data Sheet: '${sheet.name}' (${sheet.fields.keys.size} fields, max ${sheet.totalRows()} rows)")
    }

    fun deleteDataSheet(sheetId: String) {
        _dataSheets.value = _dataSheets.value.filter { it.id != sheetId }
        if (_currentScript.value.dataSheetId == sheetId) {
            _currentScript.value = _currentScript.value.copy(dataSheetId = _dataSheets.value.firstOrNull()?.id)
        }
        addLog("Deleted Data Sheet.")
    }

    fun getDataSheet(id: String?): DataSheet? {
        if (id == null || id == "default") return null
        return _dataSheets.value.firstOrNull { it.id == id }
    }

    // Recording (No-Type Element Grabbing)
    fun toggleRecording() {
        if (_executionState.value.status == AutomationStatus.RECORDING) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        _executionState.value = _executionState.value.copy(
            status = AutomationStatus.RECORDING,
            isHudVisible = true
        )
        addLog("No-Type Recording active: tap any field, checkbox, or button to grab it (no typing needed; values fill from Data Sheet/Default on start).")
        injectJs(AutomationScripts.RECORDING_INJECTION)
    }

    fun stopRecording() {
        if (_executionState.value.status == AutomationStatus.RECORDING) {
            _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
            addLog("Recording paused. Ready elements in script: ${_currentScript.value.steps.size}")
        }
    }

    // Scanner (Grab Elements & Ready Use)
    fun triggerScan(autoReadySteps: Boolean = true) {
        autoReadyOnNextScan = autoReadySteps
        if (_executionState.value.status != AutomationStatus.RECORDING) {
            _executionState.value = _executionState.value.copy(status = AutomationStatus.SCANNING)
        }
        addLog("Grabbing page elements and preparing script...")
        injectJs(AutomationScripts.DOM_SCANNER)
    }

    fun clearScanHighlights() {
        injectJs(AutomationScripts.CLEAR_SCAN_HIGHLIGHTS)
        _scannedElements.value = emptyList()
        if (_executionState.value.status == AutomationStatus.SCANNING) {
            _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
        }
    }

    fun updateScannedElement(updated: ScannedElement) {
        _scannedElements.value = _scannedElements.value.map {
            if (it.id == updated.id) updated else it
        }
        addLog("Updated grabbed element: ${updated.selector} (${updated.semanticType.name})")
    }

    fun addScannedElementToScript(element: ScannedElement) {
        val (actionType, varKey, defaultVal, desc) = mapScannedElementDetails(element)
        val newStep = AutomationStep(
            actionType = actionType,
            selector = element.selector,
            elementDescription = desc,
            value = defaultVal,
            variableKey = varKey,
            pageUrl = element.pageUrl.ifBlank { lastKnownPageUrl },
            delayMs = 800L
        )
        _currentScript.value = _currentScript.value.copy(steps = _currentScript.value.steps + newStep)
        syncCurrentScriptToSavedScripts()
        addLog("Added element '$desc' -> bound to ${varKey?.let { "Data '{{$it}}'" } ?: actionType.name}")
    }

    fun autoMapAllScannedElements() {
        val elements = _scannedElements.value
        if (elements.isEmpty()) return
        buildReadyStepsFromGrabbedElements(elements)
    }

    private fun mapScannedElementDetails(element: ScannedElement): Quad<AutomationActionType, String?, String, String> {
        return when (element.semanticType) {
            ElementSemanticType.EMAIL -> Quad(AutomationActionType.INPUT, "email", "{{email}}", "Fill Email")
            ElementSemanticType.USERNAME -> Quad(AutomationActionType.INPUT, "username", "{{username}}", "Fill Username")
            ElementSemanticType.PASSWORD -> Quad(AutomationActionType.INPUT, "password", "{{password}}", "Fill Password")
            ElementSemanticType.CONFIRM_PASSWORD -> Quad(AutomationActionType.INPUT, "confirmPassword", "{{confirmPassword}}", "Confirm Password")
            ElementSemanticType.FIRST_NAME -> Quad(AutomationActionType.INPUT, "firstName", "{{firstName}}", "Fill First Name")
            ElementSemanticType.LAST_NAME -> Quad(AutomationActionType.INPUT, "lastName", "{{lastName}}", "Fill Last Name")
            ElementSemanticType.FULL_NAME -> Quad(AutomationActionType.INPUT, "fullName", "{{fullName}}", "Fill Full Name")
            ElementSemanticType.PHONE -> Quad(AutomationActionType.INPUT, "phoneNumber", "{{phoneNumber}}", "Fill Phone Number")
            ElementSemanticType.COUNTRY_CODE -> Quad(AutomationActionType.SELECT_COUNTRY_CODE, "countryCode", "{{countryCode}}", "Select Country Code")
            ElementSemanticType.GENDER -> Quad(AutomationActionType.SELECT_DROPDOWN, "gender", "{{gender}}", "Select Gender")
            ElementSemanticType.DOB -> Quad(AutomationActionType.SELECT_DATE, "dob", "{{dob}}", "Select Date of Birth")
            ElementSemanticType.DOB_DAY -> Quad(AutomationActionType.SELECT_DROPDOWN, "dobDay", "{{dobDay}}", "Select DOB Day")
            ElementSemanticType.DOB_MONTH -> Quad(AutomationActionType.SELECT_DROPDOWN, "dobMonth", "{{dobMonth}}", "Select DOB Month")
            ElementSemanticType.DOB_YEAR -> Quad(AutomationActionType.SELECT_DROPDOWN, "dobYear", "{{dobYear}}", "Select DOB Year")
            ElementSemanticType.ADDRESS -> Quad(AutomationActionType.INPUT, "address", "{{address}}", "Fill Address")
            ElementSemanticType.ADDRESS_LINE_2 -> Quad(AutomationActionType.INPUT, "addressLine2", "{{addressLine2}}", "Fill Apt/Suite")
            ElementSemanticType.CITY -> Quad(AutomationActionType.INPUT, "city", "{{city}}", "Fill City")
            ElementSemanticType.STATE -> Quad(AutomationActionType.INPUT, "state", "{{state}}", "Fill State")
            ElementSemanticType.ZIP_PIN -> Quad(AutomationActionType.INPUT, "zipCode", "{{zipCode}}", "Fill ZIP / PIN Code")
            ElementSemanticType.FILE_UPLOAD -> Quad(AutomationActionType.UPLOAD_FILE, "fileName", "{{fileName}}", "Upload Document")
            ElementSemanticType.CHECKBOX_TERMS -> Quad(AutomationActionType.CHECKBOX, null, "", "Accept Policies & Terms")
            ElementSemanticType.CHECKBOX -> Quad(
                AutomationActionType.CHECKBOX,
                null,
                "",
                "Check: ${element.text.ifBlank { element.placeholder.ifBlank { element.nameAttr.ifBlank { "Checkbox" } } }}"
            )
            ElementSemanticType.RADIO_OPTION -> Quad(
                AutomationActionType.RADIO,
                null,
                "",
                "Select Radio: ${element.text.ifBlank { element.placeholder.ifBlank { element.nameAttr.ifBlank { "Option" } } }}"
            )
            ElementSemanticType.SUBMIT_BUTTON -> Quad(AutomationActionType.SUBMIT, null, "", "Submit / Create Account")
            ElementSemanticType.NEXT_BUTTON -> Quad(AutomationActionType.CLICK, null, "", "Click Next / Continue")
            ElementSemanticType.DROPDOWN -> {
                val inferredKey = detectVariableKeyFromSelector(element.selector, element.nameAttr + " " + element.idAttr + " " + element.text) ?: "gender"
                Quad(AutomationActionType.SELECT_DROPDOWN, inferredKey, "{{$inferredKey}}", "Select Dropdown ($inferredKey)")
            }
            ElementSemanticType.GENERIC_BUTTON -> Quad(AutomationActionType.CLICK, null, "", "Click ${element.text.ifBlank { "Button" }}")
            ElementSemanticType.GENERIC_INPUT -> {
                val inferredKey = detectVariableKeyFromSelector(element.selector, element.placeholder + " " + element.nameAttr + " " + element.text) ?: "username"
                Quad(
                    AutomationActionType.INPUT,
                    inferredKey,
                    "{{$inferredKey}}",
                    "Fill ${element.placeholder.ifBlank { element.nameAttr.ifBlank { inferredKey } }}"
                )
            }
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * User actions when stored Data Sheet values for a field are exhausted (all used 1 by 1)
     */
    fun resolveExhaustionWithStop() {
        exhaustionDecisionDeferred?.complete(ExhaustionDecision.Stop)
    }

    fun resolveExhaustionWithDefault() {
        exhaustionDecisionDeferred?.complete(ExhaustionDecision.ContinueWithDefault)
    }

    fun resolveExhaustionWithNewData(fieldKey: String, newValues: List<String>) {
        val clean = newValues.map { it.trim() }.filter { it.isNotBlank() }
        val sheetId = _executionState.value.exhaustedSheetId ?: _currentScript.value.dataSheetId
        val sheet = getDataSheet(sheetId)
        if (sheet != null && clean.isNotEmpty()) {
            val updatedSheet = sheet.withAddedFieldValues(fieldKey, clean)
            saveDataSheet(updatedSheet)
        }
        exhaustionDecisionDeferred?.complete(ExhaustionDecision.AddedNewData(clean))
    }

    /**
     * Playback Execution:
     * - STRICT GATE: Cannot start if no script or steps exist; redirects HUD to Create Automation mode.
     * - Auto-registers newly grabbed script in savedScripts when started.
     * - Consumes stored values from the linked Data Sheet 1 by 1, or uses Default Generator if no Data Sheet is linked.
     * - Executes with wait-for-element auto-retry and fallback hint matching so dynamic DOMs succeed reliably.
     */
    fun startPlayback() {
        // If currentScript has no steps, check if a saved script exists for this website
        if (_currentScript.value.steps.isEmpty() && _savedScripts.value.isNotEmpty()) {
            val matched = getScriptsForCurrentUrl(lastKnownPageUrl).firstOrNull() ?: _savedScripts.value.first()
            _currentScript.value = matched
        }

        val script = _currentScript.value
        if (script.steps.isEmpty()) {
            // Strict gate: When no scripts/steps exist, the engine CANNOT start; user must create one in HUD
            _executionState.value = _executionState.value.copy(
                status = AutomationStatus.RECORDING,
                hudMode = HudMode.CREATE_AUTOMATION,
                isHudVisible = true
            )
            addLog("Cannot start automation: No script created yet. Grab or tap elements on the page in HUD to create one first.")
            injectJs(AutomationScripts.RECORDING_INJECTION)
            triggerScan(autoReadySteps = true)
            return
        }

        // Auto-save the current script into savedScripts if it isn't saved yet
        if (_savedScripts.value.none { it.id == script.id }) {
            val domain = extractDomain(lastKnownPageUrl).ifBlank { script.targetWebsite.ifBlank { "*" } }
            val savedCopy = script.copy(
                name = if (script.name == "New Automation Flow" && domain != "*") "$domain Automation" else script.name,
                targetWebsite = if (script.targetWebsite == "*" && domain != "*") domain else script.targetWebsite,
                targetUrl = lastKnownPageUrl.ifBlank { script.targetUrl }
            )
            _currentScript.value = savedCopy
            _savedScripts.value = listOf(savedCopy) + _savedScripts.value
        }

        val activeScript = _currentScript.value
        stopPlayback()
        _executionState.value = _executionState.value.copy(
            status = AutomationStatus.PLAYING,
            hudMode = HudMode.RUN_AUTOMATION,
            currentIteration = 1,
            totalIterations = activeScript.repeatCount,
            currentStepIndex = 0,
            totalSteps = activeScript.steps.size,
            isHudVisible = true,
            isAwaitingExhaustionChoice = false,
            exhaustedFieldKey = null
        )

        // Per-field consumption counter so recurring steps and iterations consume 1 by 1
        val fieldUsageCounters = mutableMapOf<String, Int>()
        // Non-repeating shuffled index list per field when RANDOM order is selected
        val fieldShuffledOrders = mutableMapOf<String, List<Int>>()
        // Track fields where user selected "Continue Using Default" after exhaustion
        val fieldsForcedToDefault = mutableSetOf<String>()

        playbackJob = scope.launch(Dispatchers.Main) {
            val initialSheet = getDataSheet(activeScript.dataSheetId)
            addLog(
                "Starting '${activeScript.name}' (${activeScript.repeatCount}x, Order: ${activeScript.iterationOrder.shortLabel}). " +
                        if (initialSheet != null) "Filling from Data Sheet: '${initialSheet.name}'" else "Filling from Default Data Generator"
            )

            outerLoop@ for (iteration in 1..activeScript.repeatCount) {
                if (_executionState.value.status != AutomationStatus.PLAYING &&
                    _executionState.value.status != AutomationStatus.PAUSED
                ) break

                _executionState.value = _executionState.value.copy(
                    currentIteration = iteration,
                    totalIterations = activeScript.repeatCount
                )

                // Rotate proxy at each iteration if enabled
                if (activeScript.rotateProxyOnRepeat) {
                    onRotateProxy?.invoke()
                    addLog("[Run $iteration] Rotated proxy node for clean session")
                    delay(450)
                }

                // Fresh random synthetic profile from Default generator for this run
                var currentDefaultRecord = AutomationDataGenerator.generateSingleRandomRecord()
                var lastResolvedPassword: String? = null

                for ((index, step) in activeScript.steps.withIndex()) {
                    if (_executionState.value.status != AutomationStatus.PLAYING &&
                        _executionState.value.status != AutomationStatus.PAUSED
                    ) break@outerLoop

                    _executionState.value = _executionState.value.copy(
                        currentStepIndex = index + 1,
                        currentStepDescription = step.elementDescription
                    )

                    val actualDelay = if (activeScript.humanizeDelays) {
                        (step.delayMs * (Random.nextDouble(0.85, 1.18))).toLong()
                    } else step.delayMs

                    delay(actualDelay)

                    // Determine which field key this step consumes
                    val stepFieldKey = inferStepFieldKey(step)
                    val fallbackHint = stepFieldKey ?: step.elementDescription

                    val resolvedValue = if (stepFieldKey != null) {
                        val activeSheetNow = getDataSheet(_currentScript.value.dataSheetId)
                        if (stepFieldKey == "confirmPassword" &&
                            (activeSheetNow == null || activeSheetNow.getFieldValues("confirmPassword").isEmpty()) &&
                            lastResolvedPassword != null
                        ) {
                            lastResolvedPassword!!
                        } else {
                            val consumed = consumeFieldNextValue(
                                fieldKey = stepFieldKey,
                                script = _currentScript.value,
                                usageCounters = fieldUsageCounters,
                                shuffledOrders = fieldShuffledOrders,
                                forcedDefaultFields = fieldsForcedToDefault,
                                defaultFallback = {
                                    currentDefaultRecord = AutomationDataGenerator.generateSingleRandomRecord()
                                    val v = AutomationDataGenerator.generateSingleFieldValue(stepFieldKey)
                                    if (stepFieldKey == "password") lastResolvedPassword = v
                                    v
                                }
                            )
                            if (consumed == null) {
                                break@outerLoop
                            }
                            if (stepFieldKey == "password") {
                                lastResolvedPassword = consumed
                            }
                            _executionState.value = _executionState.value.copy(
                                currentDataSummary = "$stepFieldKey: $consumed"
                            )
                            consumed
                        }
                    } else {
                        step.value
                    }

                    when (step.actionType) {
                        AutomationActionType.NAVIGATE -> {
                            val targetUrl = resolvedValue.ifBlank { step.selector }
                            addLog("[Run $iteration] Navigating to page/website: $targetUrl")
                            webViewProvider?.invoke()?.loadUrl(targetUrl)
                            delay(2000L)
                        }
                        AutomationActionType.CLICK -> {
                            val js = AutomationScripts.createClickScript(step.selector, fallbackHint)
                            val ok = evaluateJsWithRetry(js)
                            addLog("[Run $iteration] ${if (ok) "Clicked" else "Attempted click"}: ${step.elementDescription}")
                        }
                        AutomationActionType.CHECKBOX -> {
                            val js = AutomationScripts.createCheckScript(step.selector, isRadio = false, desiredState = true, fallbackHint = fallbackHint)
                            val ok = evaluateJsWithRetry(js)
                            addLog("[Run $iteration] ${if (ok) "Checked box" else "Attempted check"}: ${step.elementDescription}")
                        }
                        AutomationActionType.RADIO -> {
                            val js = AutomationScripts.createCheckScript(step.selector, isRadio = true, desiredState = true, fallbackHint = fallbackHint)
                            val ok = evaluateJsWithRetry(js)
                            addLog("[Run $iteration] ${if (ok) "Selected radio" else "Attempted radio"}: ${step.elementDescription}")
                        }
                        AutomationActionType.INPUT -> {
                            val js = AutomationScripts.createInputScript(step.selector, resolvedValue, step.clearBeforeInput, fallbackHint)
                            val ok = evaluateJsWithRetry(js)
                            addLog("[Run $iteration] ${if (ok) "Filled" else "Attempted fill"} ${step.elementDescription} -> '$resolvedValue'")
                        }
                        AutomationActionType.SELECT_DROPDOWN -> {
                            val js = AutomationScripts.createSelectDropdownScript(step.selector, resolvedValue, fallbackHint)
                            val ok = evaluateJsWithRetry(js)
                            addLog("[Run $iteration] ${if (ok) "Selected" else "Attempted select"} '$resolvedValue' in ${step.elementDescription}")
                        }
                        AutomationActionType.SELECT_COUNTRY_CODE -> {
                            val code = resolvedValue.ifBlank { currentDefaultRecord.countryCode }
                            val js = AutomationScripts.createSelectCountryCodeScript(step.selector, code)
                            evaluateJsWithRetry(js)
                            addLog("[Run $iteration] Selected Country Code '$code'")
                        }
                        AutomationActionType.SELECT_DATE -> {
                            val dob = resolvedValue.ifBlank { currentDefaultRecord.dob }
                            val parts = dob.split("-")
                            val y = parts.getOrNull(0) ?: currentDefaultRecord.dobYear
                            val m = parts.getOrNull(1) ?: currentDefaultRecord.dobMonth
                            val d = parts.getOrNull(2) ?: currentDefaultRecord.dobDay
                            val js = AutomationScripts.createSelectDateScript(step.selector, dob, d, m, y)
                            evaluateJsWithRetry(js)
                            addLog("[Run $iteration] Selected Date: '$dob'")
                        }
                        AutomationActionType.UPLOAD_FILE -> {
                            val fileName = resolvedValue.ifBlank { currentDefaultRecord.fileName }
                            val js = AutomationScripts.createFileUploadScript(step.selector, fileName)
                            evaluateJsWithRetry(js)
                            addLog("[Run $iteration] Uploaded Document '$fileName'")
                        }
                        AutomationActionType.SUBMIT -> {
                            val js = AutomationScripts.createSubmitScript(step.selector, fallbackHint)
                            val ok = evaluateJsWithRetry(js)
                            addLog("[Run $iteration] ${if (ok) "Submitted form" else "Triggered submit"}: ${step.elementDescription}")
                        }
                        AutomationActionType.WAIT -> {
                            addLog("[Run $iteration] Waiting ${step.delayMs}ms")
                        }
                        AutomationActionType.CLEAR -> {
                            val js = AutomationScripts.createInputScript(step.selector, "", true, fallbackHint)
                            evaluateJsWithRetry(js)
                            addLog("[Run $iteration] Cleared ${step.elementDescription}")
                        }
                        AutomationActionType.SCROLL -> {
                            injectJs("window.scrollBy({ top: 350, behavior: 'smooth' });")
                            addLog("[Run $iteration] Scrolled page")
                        }
                    }
                }

                if (activeScript.clearDataOnComplete && iteration < activeScript.repeatCount) {
                    delay(700)
                    activeScript.steps.filter { it.actionType == AutomationActionType.INPUT }.forEach { inputStep ->
                        val clearJs = AutomationScripts.createInputScript(inputStep.selector, "", true)
                        injectJs(clearJs)
                    }
                }

                delay(1000)
            }

            _executionState.value = _executionState.value.copy(
                status = AutomationStatus.IDLE,
                isAwaitingExhaustionChoice = false,
                exhaustedFieldKey = null
            )
            addLog("Automation finished.")
        }
    }

    /**
     * Stronger JS Execution with automatic Wait-For-Element retry for dynamic SPAs and delayed renders.
     */
    private suspend fun evaluateJsWithRetry(
        js: String,
        maxAttempts: Int = 5,
        retryDelayMs: Long = 350L
    ): Boolean {
        val webView = webViewProvider?.invoke() ?: return false
        for (attempt in 1..maxAttempts) {
            val deferred = CompletableDeferred<String?>()
            try {
                webView.evaluateJavascript(js) { result ->
                    deferred.complete(result)
                }
            } catch (e: Exception) {
                deferred.complete(null)
            }
            val res = withTimeoutOrNull(1800L) { deferred.await() }
            if (res != null && (res.contains("true", ignoreCase = true) || !res.contains("false", ignoreCase = true))) {
                return true
            }
            if (attempt < maxAttempts) {
                delay(retryDelayMs)
            }
        }
        return false
    }

    private fun inferStepFieldKey(step: AutomationStep): String? {
        if (!step.variableKey.isNullOrBlank()) return step.variableKey
        val regex = Regex("\\{\\{([a-zA-Z0-9_]+)\\}\\}")
        val match = regex.find(step.value)
        if (match != null) return match.groupValues[1]
        return when (step.actionType) {
            AutomationActionType.SELECT_COUNTRY_CODE -> "countryCode"
            AutomationActionType.SELECT_DATE -> "dob"
            AutomationActionType.UPLOAD_FILE -> "fileName"
            AutomationActionType.INPUT -> detectVariableKeyFromSelector(step.selector, step.elementDescription) ?: "username"
            AutomationActionType.SELECT_DROPDOWN -> detectVariableKeyFromSelector(step.selector, step.elementDescription) ?: "gender"
            else -> null
        }
    }

    /**
     * Consumes 1 value from the active DataSheet for [fieldKey] according to the configured order
     * (Top to Bottom, Bottom to Top, or Random 1-by-1).
     * If stored values are present, Default is NEVER used until all stored values are finished.
     * When finished, applies exhaustion policy (Stop, Continue with Default, Loop, or Ask/Add New).
     */
    private suspend fun consumeFieldNextValue(
        fieldKey: String,
        script: AutomationScript,
        usageCounters: MutableMap<String, Int>,
        shuffledOrders: MutableMap<String, List<Int>>,
        forcedDefaultFields: MutableSet<String>,
        defaultFallback: () -> String
    ): String? {
        val activeSheet = getDataSheet(script.dataSheetId)
        if (activeSheet == null || forcedDefaultFields.contains(fieldKey)) {
            val generated = defaultFallback()
            addLog("Using Default (Random) for '$fieldKey': $generated")
            return generated
        }

        var storedList = activeSheet.getFieldValues(fieldKey)
        if (storedList.isEmpty()) {
            val generated = defaultFallback()
            addLog("Field '$fieldKey' not in '${activeSheet.name}' -> Default generated: $generated")
            return generated
        }

        val order = script.iterationOrder
        val policy = script.exhaustionPolicy
        val usageIdx = usageCounters.getOrDefault(fieldKey, 0)

        val currentShuffled = shuffledOrders[fieldKey]
        val effectiveShuffle = if (currentShuffled == null || currentShuffled.size != storedList.size) {
            storedList.indices.shuffled().also { shuffledOrders[fieldKey] = it }
        } else {
            currentShuffled
        }

        if (usageIdx < storedList.size) {
            val chosen = when (order) {
                DataIterationOrder.TOP_TO_BOTTOM -> storedList[usageIdx]
                DataIterationOrder.BOTTOM_TO_TOP -> storedList[(storedList.size - 1) - usageIdx]
                DataIterationOrder.RANDOM -> storedList[effectiveShuffle[usageIdx]]
            }
            usageCounters[fieldKey] = usageIdx + 1
            addLog("Used '$fieldKey' [${usageIdx + 1}/${storedList.size}] (${order.shortLabel}) from '${activeSheet.name}': $chosen")
            return chosen
        }

        // All stored values in this field have been used 1 by 1!
        return when (policy) {
            DataExhaustionPolicy.STOP -> {
                addLog("All ${storedList.size} stored '$fieldKey' values in '${activeSheet.name}' finished. Stopping automation.")
                null
            }
            DataExhaustionPolicy.CONTINUE_WITH_DEFAULT -> {
                forcedDefaultFields.add(fieldKey)
                val generated = defaultFallback()
                addLog("All ${storedList.size} '$fieldKey' values used. Continuing with Default random: $generated")
                generated
            }
            DataExhaustionPolicy.LOOP -> {
                val loopIdx = usageIdx % storedList.size
                val chosen = when (order) {
                    DataIterationOrder.TOP_TO_BOTTOM -> storedList[loopIdx]
                    DataIterationOrder.BOTTOM_TO_TOP -> storedList[(storedList.size - 1) - loopIdx]
                    DataIterationOrder.RANDOM -> storedList[effectiveShuffle[loopIdx]]
                }
                usageCounters[fieldKey] = usageIdx + 1
                addLog("Looping '$fieldKey' [${loopIdx + 1}/${storedList.size}] from '${activeSheet.name}': $chosen")
                chosen
            }
            DataExhaustionPolicy.ADD_NEW -> {
                addLog("All ${storedList.size} '$fieldKey' values in '${activeSheet.name}' have been used 1 by 1. Waiting for user selection (Stop / Continue Default / Add New)...")
                val deferred = CompletableDeferred<ExhaustionDecision>()
                exhaustionDecisionDeferred = deferred
                _executionState.value = _executionState.value.copy(
                    status = AutomationStatus.PAUSED,
                    isHudVisible = true,
                    isAwaitingExhaustionChoice = true,
                    exhaustedFieldKey = fieldKey,
                    exhaustedSheetId = activeSheet.id,
                    exhaustedSheetName = activeSheet.name,
                    exhaustedUsedCount = storedList.size
                )

                val decision = deferred.await()
                exhaustionDecisionDeferred = null
                _executionState.value = _executionState.value.copy(
                    status = AutomationStatus.PLAYING,
                    isAwaitingExhaustionChoice = false,
                    exhaustedFieldKey = null
                )

                when (decision) {
                    is ExhaustionDecision.Stop -> {
                        addLog("User selected Stop after '${activeSheet.name}' ($fieldKey) finished.")
                        null
                    }
                    is ExhaustionDecision.ContinueWithDefault -> {
                        forcedDefaultFields.add(fieldKey)
                        val generated = defaultFallback()
                        addLog("User selected Continue Using Default for '$fieldKey' -> $generated")
                        generated
                    }
                    is ExhaustionDecision.AddedNewData -> {
                        val refreshedSheet = getDataSheet(script.dataSheetId)
                        storedList = refreshedSheet?.getFieldValues(fieldKey) ?: emptyList()
                        if (usageIdx < storedList.size) {
                            val newIndices = (usageIdx until storedList.size).toList().shuffled()
                            shuffledOrders[fieldKey] = (effectiveShuffle + newIndices)
                            val picked = storedList[usageIdx]
                            usageCounters[fieldKey] = usageIdx + 1
                            addLog("Added ${decision.newValues.size} new '$fieldKey' items. Using [${usageIdx + 1}/${storedList.size}]: $picked")
                            picked
                        } else {
                            val generated = defaultFallback()
                            addLog("No extra items found; using Default: $generated")
                            generated
                        }
                    }
                }
            }
        }
    }

    fun stopPlayback() {
        exhaustionDecisionDeferred?.complete(ExhaustionDecision.Stop)
        exhaustionDecisionDeferred = null
        playbackJob?.cancel()
        playbackJob = null
        _executionState.value = _executionState.value.copy(
            status = AutomationStatus.IDLE,
            isAwaitingExhaustionChoice = false,
            exhaustedFieldKey = null
        )
        addLog("Automation stopped.")
    }

    // Step Operations in HUD / Manager
    fun updateStep(step: AutomationStep) {
        val updated = _currentScript.value.steps.map { if (it.id == step.id) step else it }
        _currentScript.value = _currentScript.value.copy(steps = updated)
        syncCurrentScriptToSavedScripts()
        addLog("Updated step: ${step.elementDescription}")
    }

    fun addStep(step: AutomationStep) {
        _currentScript.value = _currentScript.value.copy(steps = _currentScript.value.steps + step)
        syncCurrentScriptToSavedScripts()
        addLog("Added step: ${step.elementDescription}")
    }

    fun moveStep(stepId: String, direction: Int) {
        val list = _currentScript.value.steps.toMutableList()
        val idx = list.indexOfFirst { it.id == stepId }
        if (idx < 0) return
        val targetIdx = idx + direction
        if (targetIdx !in list.indices) return
        val item = list.removeAt(idx)
        list.add(targetIdx, item)
        _currentScript.value = _currentScript.value.copy(steps = list)
        syncCurrentScriptToSavedScripts()
    }

    fun removeStep(stepId: String) {
        _currentScript.value = _currentScript.value.copy(
            steps = _currentScript.value.steps.filter { it.id != stepId }
        )
        syncCurrentScriptToSavedScripts()
        addLog("Removed step.")
    }

    fun clearAllSteps() {
        _currentScript.value = _currentScript.value.copy(steps = emptyList())
        addLog("Cleared all steps.")
    }

    fun updateCurrentScript(script: AutomationScript) {
        _currentScript.value = script
        val idx = _savedScripts.value.indexOfFirst { it.id == script.id }
        if (idx >= 0) {
            _savedScripts.value = _savedScripts.value.toMutableList().apply { set(idx, script) }
        }
    }

    private fun addLog(message: String) {
        val currentLogs = _executionState.value.logMessages.takeLast(50)
        _executionState.value = _executionState.value.copy(
            logMessages = currentLogs + message
        )
    }

    private fun injectJs(script: String) {
        val webView = webViewProvider?.invoke() ?: return
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }
}
