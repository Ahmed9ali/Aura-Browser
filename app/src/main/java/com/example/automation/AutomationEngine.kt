package com.example.automation

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.model.AutomationActionType
import com.example.model.AutomationDataGenerator
import com.example.model.AutomationDataRecord
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
import org.json.JSONArray
import java.net.URI
import java.util.UUID
import kotlin.random.Random

class AutomationEngine(
    private val scope: CoroutineScope
) {
    private val _executionState = MutableStateFlow(AutomationExecutionState())
    val executionState: StateFlow<AutomationExecutionState> = _executionState.asStateFlow()

    // 1. "My Data" Data Sheets - e.g. "XYZ Data" with email, username, password sections
    private val _dataSheets = MutableStateFlow<List<DataSheet>>(
        listOf(
            DataSheet(
                id = "sheet_xyz",
                name = "XYZ Data",
                fields = mapOf(
                    "email" to listOf(
                        "alex.lead.01@example.com",
                        "jordan.corp.02@example.com",
                        "casey.alpha.03@example.com",
                        "morgan.tech.04@example.com"
                    ),
                    "username" to listOf(
                        "alex_lead01",
                        "jordan_corp02",
                        "casey_alpha03",
                        "morgan_tech04"
                    ),
                    "password" to listOf(
                        "P@ssw0rd!Xyz1",
                        "P@ssw0rd!Xyz2",
                        "P@ssw0rd!Xyz3",
                        "P@ssw0rd!Xyz4"
                    ),
                    "phoneNumber" to listOf(
                        "5550191001",
                        "5550191002",
                        "5550191003",
                        "5550191004"
                    ),
                    "firstName" to listOf("Alex", "Jordan", "Casey", "Morgan"),
                    "lastName" to listOf("Vance", "Mercer", "Sterling", "Kovacs"),
                    "countryCode" to listOf("+1", "+44", "+1", "+49"),
                    "gender" to listOf("Male", "Female", "Male", "Female"),
                    "dob" to listOf("1995-04-12", "1997-08-23", "1992-11-05", "1999-01-30"),
                    "address" to listOf(
                        "120 Market Street",
                        "450 Broadway Ave",
                        "780 Sunset Blvd",
                        "310 Pine Street"
                    ),
                    "city" to listOf("San Francisco", "New York", "Los Angeles", "Seattle"),
                    "state" to listOf("CA", "NY", "CA", "WA"),
                    "zipCode" to listOf("94105", "10001", "90028", "98101"),
                    "fileName" to listOf("id_document.pdf", "avatar.png", "resume.pdf", "passport.pdf")
                ),
                iterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
                exhaustionPolicy = DataExhaustionPolicy.ADD_NEW
            )
        )
    )
    val dataSheets: StateFlow<List<DataSheet>> = _dataSheets.asStateFlow()

    // 2. Saved Scripts - Website-specific scripts list
    private val defaultInitialSteps = listOf(
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[type=\"email\"], input[name*=\"email\"], #email",
            elementDescription = "Fill Email Address",
            value = "{{email}}",
            variableKey = "email",
            delayMs = 800L
        ),
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[name*=\"user\"], input[name*=\"login\"], #username",
            elementDescription = "Fill Username",
            value = "{{username}}",
            variableKey = "username",
            delayMs = 800L
        ),
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[type=\"password\"]:not([name*=\"confirm\"]):not([id*=\"confirm\"]), #password",
            elementDescription = "Fill Secure Password",
            value = "{{password}}",
            variableKey = "password",
            delayMs = 800L
        ),
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[name*=\"confirm\"], #confirmPassword, input[placeholder*=\"Confirm\" i]",
            elementDescription = "Confirm Password",
            value = "{{confirmPassword}}",
            variableKey = "confirmPassword",
            delayMs = 700L
        ),
        AutomationStep(
            actionType = AutomationActionType.SELECT_COUNTRY_CODE,
            selector = ".iti__selected-flag, select[name*=\"country_code\"], #country_code",
            elementDescription = "Select Country Code",
            value = "{{countryCode}}",
            variableKey = "countryCode",
            delayMs = 750L
        ),
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[type=\"tel\"], input[name*=\"phone\"], #phone",
            elementDescription = "Fill Phone Number",
            value = "{{phoneNumber}}",
            variableKey = "phoneNumber",
            delayMs = 750L
        ),
        AutomationStep(
            actionType = AutomationActionType.SELECT_DROPDOWN,
            selector = "select[name*=\"gender\"], #gender, [role=\"combobox\"][name*=\"gender\"]",
            elementDescription = "Select Gender Dropdown",
            value = "{{gender}}",
            variableKey = "gender",
            delayMs = 700L
        ),
        AutomationStep(
            actionType = AutomationActionType.SELECT_DATE,
            selector = "input[type=\"date\"], [data-datepicker], input[name*=\"dob\"]",
            elementDescription = "Select Date of Birth (Calendar)",
            value = "{{dob}}",
            variableKey = "dob",
            delayMs = 800L
        ),
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[name*=\"zip\"], input[name*=\"postal\"], input[name*=\"pin\"], #zip",
            elementDescription = "Fill ZIP / PIN Code",
            value = "{{zipCode}}",
            variableKey = "zipCode",
            delayMs = 700L
        ),
        AutomationStep(
            actionType = AutomationActionType.UPLOAD_FILE,
            selector = "input[type=\"file\"], .dropzone",
            elementDescription = "Upload Document / Avatar",
            value = "{{fileName}}",
            variableKey = "fileName",
            delayMs = 900L
        ),
        AutomationStep(
            actionType = AutomationActionType.CLICK,
            selector = "input[type=\"checkbox\"], #terms, [name*=\"terms\"]",
            elementDescription = "Accept Terms & Policies",
            delayMs = 600L
        ),
        AutomationStep(
            actionType = AutomationActionType.SUBMIT,
            selector = "button[type=\"submit\"], .btn-primary, #submit-btn",
            elementDescription = "Click Submit / Create Account",
            delayMs = 1500L
        )
    )

    private val multiPageSteps = listOf(
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[type=\"email\"], #email",
            elementDescription = "Page 1: Enter Email (1st Data)",
            value = "{{email}}",
            variableKey = "email",
            delayMs = 800L
        ),
        AutomationStep(
            actionType = AutomationActionType.CLICK,
            selector = "button[type=\"submit\"], #next-btn, .next-step",
            elementDescription = "Page 1: Click Continue / Next Page",
            delayMs = 1200L
        ),
        AutomationStep(
            actionType = AutomationActionType.WAIT,
            selector = "",
            elementDescription = "Wait for Page 2 to Load",
            delayMs = 1400L
        ),
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[name*=\"user\"], #username",
            elementDescription = "Page 2: Enter Username",
            value = "{{username}}",
            variableKey = "username",
            delayMs = 800L
        ),
        AutomationStep(
            actionType = AutomationActionType.INPUT,
            selector = "input[type=\"password\"], #password",
            elementDescription = "Page 2: Enter Password",
            value = "{{password}}",
            variableKey = "password",
            delayMs = 800L
        ),
        AutomationStep(
            actionType = AutomationActionType.SUBMIT,
            selector = "button[type=\"submit\"], #finish-btn",
            elementDescription = "Page 2: Complete Registration",
            delayMs = 1500L
        )
    )

    private val _savedScripts = MutableStateFlow<List<AutomationScript>>(
        listOf(
            AutomationScript(
                id = "script_all_in_one",
                name = "Universal Form & Account Automation",
                targetWebsite = "*",
                steps = defaultInitialSteps,
                dataSheetId = "sheet_xyz",
                repeatCount = 5,
                iterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
                exhaustionPolicy = DataExhaustionPolicy.ADD_NEW,
                rotateProxyOnRepeat = true
            ),
            AutomationScript(
                id = "script_multipage",
                name = "Multi-Page & Cross-Site Workflow",
                targetWebsite = "*",
                steps = multiPageSteps,
                dataSheetId = "sheet_xyz",
                repeatCount = 4,
                iterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
                exhaustionPolicy = DataExhaustionPolicy.ADD_NEW
            ),
            AutomationScript(
                id = "script_github",
                name = "GitHub Sign-up Automation",
                targetWebsite = "github.com",
                targetUrl = "https://github.com/signup",
                steps = listOf(
                    AutomationStep(
                        actionType = AutomationActionType.INPUT,
                        selector = "#email, input[name=\"email\"]",
                        elementDescription = "Enter Email",
                        value = "{{email}}",
                        variableKey = "email",
                        delayMs = 900L
                    ),
                    AutomationStep(
                        actionType = AutomationActionType.CLICK,
                        selector = "button[data-continue-to=\"password-container\"]",
                        elementDescription = "Continue to Password",
                        delayMs = 1000L
                    ),
                    AutomationStep(
                        actionType = AutomationActionType.INPUT,
                        selector = "#password, input[name=\"password\"]",
                        elementDescription = "Create Password",
                        value = "{{password}}",
                        variableKey = "password",
                        delayMs = 900L
                    ),
                    AutomationStep(
                        actionType = AutomationActionType.CLICK,
                        selector = "button[data-continue-to=\"username-container\"]",
                        elementDescription = "Continue to Username",
                        delayMs = 1000L
                    ),
                    AutomationStep(
                        actionType = AutomationActionType.INPUT,
                        selector = "#login, input[name=\"login\"]",
                        elementDescription = "Enter Username",
                        value = "{{username}}",
                        variableKey = "username",
                        delayMs = 900L
                    )
                ),
                dataSheetId = "sheet_xyz",
                repeatCount = 4,
                iterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
                exhaustionPolicy = DataExhaustionPolicy.ADD_NEW
            )
        )
    )
    val savedScripts: StateFlow<List<AutomationScript>> = _savedScripts.asStateFlow()

    private val _currentScript = MutableStateFlow<AutomationScript>(_savedScripts.value.first())
    val currentScript: StateFlow<AutomationScript> = _currentScript.asStateFlow()

    private val _scannedElements = MutableStateFlow<List<ScannedElement>>(emptyList())
    val scannedElements: StateFlow<List<ScannedElement>> = _scannedElements.asStateFlow()

    private var playbackJob: Job? = null
    private var exhaustionDecisionDeferred: CompletableDeferred<ExhaustionDecision>? = null
    private var lastKnownPageUrl: String = ""

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
                else -> AutomationActionType.CLICK
            }

            // Only elements and their semantic field keys are captured (no personal raw data)
            val resolvedKey = fieldKeyFromJs.ifBlank {
                detectVariableKeyFromSelector(selector, description) ?: if (type == AutomationActionType.INPUT) "email" else ""
            }.ifBlank { null }

            val newStep = AutomationStep(
                actionType = type,
                selector = selector,
                elementDescription = description,
                value = if (resolvedKey != null) "{{$resolvedKey}}" else "",
                variableKey = resolvedKey,
                pageUrl = lastKnownPageUrl,
                delayMs = 800L
            )

            scope.launch {
                val currentSteps = _currentScript.value.steps
                // Prevent duplicate consecutive capture of the exact same selector + actionType
                val lastStep = currentSteps.lastOrNull()
                if (lastStep != null && lastStep.selector == newStep.selector && lastStep.actionType == newStep.actionType) {
                    return@launch
                }
                val updatedSteps = currentSteps + newStep
                _currentScript.value = _currentScript.value.copy(steps = updatedSteps)
                addLog("Captured element: $description ($selector) -> ${resolvedKey?.let { "{{$it}}" } ?: type.name}")
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
                    addLog("Grabbed ${elements.size} elements on page (only elements captured; values fill from My Data)")
                } catch (e: Exception) {
                    addLog("Scanner parse error: ${e.message}")
                }
            }
        }
    }

    val jsInterface = AutomationJsInterface()

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

        // If user is in Create Automation or Recording mode and navigated to a new page/website:
        if (_executionState.value.hudMode == HudMode.CREATE_AUTOMATION ||
            _executionState.value.status == AutomationStatus.RECORDING
        ) {
            if (previousUrl.isNotBlank() && !previousUrl.equals(url, ignoreCase = true)) {
                addLog("Page transition detected: $url (Grabbing new page elements...)")
            }
            injectJs(AutomationScripts.RECORDING_INJECTION)
            triggerScan()
        } else if (_executionState.value.isHudVisible && _executionState.value.status == AutomationStatus.IDLE) {
            // Auto-select website-specific saved script if one matches this domain
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
            // If current script is a pre-bundled preset and user starts Create Automation on a website,
            // update targetWebsite to current domain if needed
            val domain = extractDomain(currentUrl)
            if (domain.isNotBlank() && !domain.contains("home")) {
                if (_currentScript.value.targetWebsite == "*") {
                    _currentScript.value = _currentScript.value.copy(
                        targetWebsite = domain,
                        targetUrl = currentUrl
                    )
                }
            }
            triggerScan()
            startRecording()
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
        triggerScan()
        startRecording()
        addLog("Started new script for $domain. Grabbing page elements...")
    }

    fun toggleHud(currentUrl: String = lastKnownPageUrl) {
        val next = !_executionState.value.isHudVisible
        _executionState.value = _executionState.value.copy(isHudVisible = next)
        if (next && currentUrl.isNotBlank()) {
            lastKnownPageUrl = currentUrl
            autoSelectScriptForWebsite(currentUrl)
        }
    }

    fun setHudVisible(visible: Boolean) {
        _executionState.value = _executionState.value.copy(isHudVisible = visible)
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
        addLog("Saved script '${newScript.name}' for website '${newScript.targetWebsite}'")
    }

    fun deleteScript(scriptId: String) {
        _savedScripts.value = _savedScripts.value.filter { it.id != scriptId }
        if (_currentScript.value.id == scriptId && _savedScripts.value.isNotEmpty()) {
            _currentScript.value = _savedScripts.value.first()
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
        addLog("Saved Data Sheet: '${sheet.name}' (${sheet.fields.keys.size} fields, max ${sheet.totalRows()} rows)")
    }

    fun deleteDataSheet(sheetId: String) {
        _dataSheets.value = _dataSheets.value.filter { it.id != sheetId }
        addLog("Deleted Data Sheet.")
    }

    fun getDataSheet(id: String?): DataSheet? {
        if (id == null || id == "default") return null
        return _dataSheets.value.firstOrNull { it.id == id }
    }

    // Recording
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
        addLog("Recording active: tap any input, dropdown, link, or button on the page to capture its element.")
        injectJs(AutomationScripts.RECORDING_INJECTION)
    }

    fun stopRecording() {
        if (_executionState.value.status == AutomationStatus.RECORDING) {
            _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
            addLog("Recording paused. Elements in script: ${_currentScript.value.steps.size}")
        }
    }

    // Scanner (Grab Elements)
    fun triggerScan() {
        _executionState.value = _executionState.value.copy(status = AutomationStatus.SCANNING)
        addLog("Grabbing elements from current page...")
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
            delayMs = 850L
        )
        _currentScript.value = _currentScript.value.copy(steps = _currentScript.value.steps + newStep)
        addLog("Added element '$desc' -> bound to ${varKey?.let { "My Data '{{$it}}'" } ?: actionType.name}")
    }

    fun autoMapAllScannedElements() {
        val elements = _scannedElements.value
        if (elements.isEmpty()) return

        val newSteps = elements.map { element ->
            val (actionType, varKey, defaultVal, desc) = mapScannedElementDetails(element)
            AutomationStep(
                actionType = actionType,
                selector = element.selector,
                elementDescription = desc,
                value = defaultVal,
                variableKey = varKey,
                pageUrl = element.pageUrl.ifBlank { lastKnownPageUrl },
                delayMs = 800L
            )
        }

        // Append to existing steps if user is building a multi-page workflow, or set if empty
        val combinedSteps = if (_currentScript.value.steps.isNotEmpty() &&
            _currentScript.value.steps.any { it.actionType == AutomationActionType.NAVIGATE }
        ) {
            _currentScript.value.steps + newSteps
        } else {
            newSteps
        }

        _currentScript.value = _currentScript.value.copy(steps = combinedSteps)
        addLog("Captured ${newSteps.size} elements into script (all inputs linked to My Data fields).")
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
            ElementSemanticType.CHECKBOX_TERMS -> Quad(AutomationActionType.CLICK, null, "", "Accept Policies & Terms")
            ElementSemanticType.SUBMIT_BUTTON -> Quad(AutomationActionType.SUBMIT, null, "", "Submit / Create Account")
            ElementSemanticType.NEXT_BUTTON -> Quad(AutomationActionType.CLICK, null, "", "Click Next / Continue")
            ElementSemanticType.DROPDOWN -> {
                val inferredKey = detectVariableKeyFromSelector(element.selector, element.nameAttr + " " + element.idAttr) ?: "gender"
                Quad(AutomationActionType.SELECT_DROPDOWN, inferredKey, "{{$inferredKey}}", "Select Dropdown ($inferredKey)")
            }
            ElementSemanticType.GENERIC_BUTTON -> Quad(AutomationActionType.CLICK, null, "", "Click ${element.text.ifBlank { "Button" }}")
            ElementSemanticType.GENERIC_INPUT -> {
                val inferredKey = detectVariableKeyFromSelector(element.selector, element.placeholder + " " + element.nameAttr) ?: "username"
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
     * - Consumes stored values from the linked Data Sheet 1 by 1 (never using Default while stored values exist).
     * - For recurring steps of the same field or across iterations, each step/run consumes the next value 1 by 1
     *   in Top-to-Bottom, Bottom-to-Top (Vice Versa), or Random non-repeating order.
     * - When stored values finish (e.g. 4 emails used), triggers Stop, Continue with Default (random), or Prompt to Add New Data.
     */
    fun startPlayback() {
        val script = _currentScript.value
        if (script.steps.isEmpty()) {
            addLog("No steps to execute. Grab elements or pick a saved script first.")
            return
        }

        stopPlayback()
        _executionState.value = _executionState.value.copy(
            status = AutomationStatus.PLAYING,
            hudMode = HudMode.RUN_AUTOMATION,
            currentIteration = 1,
            totalIterations = script.repeatCount,
            currentStepIndex = 0,
            totalSteps = script.steps.size,
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
            val initialSheet = getDataSheet(script.dataSheetId)
            addLog(
                "Starting '${script.name}' (${script.repeatCount}x, Order: ${script.iterationOrder.shortLabel}). " +
                        if (initialSheet != null) "Using Data Sheet: '${initialSheet.name}'" else "Using Default Random Data Generator"
            )

            outerLoop@ for (iteration in 1..script.repeatCount) {
                if (_executionState.value.status != AutomationStatus.PLAYING &&
                    _executionState.value.status != AutomationStatus.PAUSED
                ) break

                _executionState.value = _executionState.value.copy(
                    currentIteration = iteration,
                    totalIterations = script.repeatCount
                )

                // Rotate proxy at each iteration if enabled
                if (script.rotateProxyOnRepeat) {
                    onRotateProxy?.invoke()
                    addLog("[Run $iteration] Rotated proxy node for clean session")
                    delay(450)
                }

                // Fresh random synthetic profile from Default generator (only used when field is NOT in Data Sheet or after Continue with Default)
                var currentDefaultRecord = AutomationDataGenerator.generateSingleRandomRecord()
                var lastResolvedPassword: String? = null

                for ((index, step) in script.steps.withIndex()) {
                    if (_executionState.value.status != AutomationStatus.PLAYING &&
                        _executionState.value.status != AutomationStatus.PAUSED
                    ) break@outerLoop

                    _executionState.value = _executionState.value.copy(
                        currentStepIndex = index + 1,
                        currentStepDescription = step.elementDescription
                    )

                    val actualDelay = if (script.humanizeDelays) {
                        (step.delayMs * (Random.nextDouble(0.85, 1.2))).toLong()
                    } else step.delayMs

                    delay(actualDelay)

                    // Determine which field key this step consumes
                    val stepFieldKey = inferStepFieldKey(step)

                    val resolvedValue = if (stepFieldKey != null) {
                        // Special case: if confirmPassword and user has no separate confirmPassword column, reuse lastResolvedPassword
                        val activeSheetNow = getDataSheet(script.dataSheetId)
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
                                    // Generate fresh random value every time Default is used
                                    currentDefaultRecord = AutomationDataGenerator.generateSingleRandomRecord()
                                    val v = AutomationDataGenerator.generateSingleFieldValue(stepFieldKey)
                                    if (stepFieldKey == "password") lastResolvedPassword = v
                                    v
                                }
                            )
                            if (consumed == null) {
                                // Stopped by exhaustion policy or user choice
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
                            val js = AutomationScripts.createClickScript(step.selector)
                            injectJs(js)
                            addLog("[Run $iteration] Clicked: ${step.elementDescription}")
                        }
                        AutomationActionType.INPUT -> {
                            val js = AutomationScripts.createInputScript(step.selector, resolvedValue, step.clearBeforeInput)
                            injectJs(js)
                            addLog("[Run $iteration] Filled ${step.elementDescription} -> '$resolvedValue'")
                        }
                        AutomationActionType.SELECT_DROPDOWN -> {
                            val js = AutomationScripts.createSelectDropdownScript(step.selector, resolvedValue)
                            injectJs(js)
                            addLog("[Run $iteration] Selected '$resolvedValue' in ${step.elementDescription}")
                        }
                        AutomationActionType.SELECT_COUNTRY_CODE -> {
                            val code = resolvedValue.ifBlank { currentDefaultRecord.countryCode }
                            val js = AutomationScripts.createSelectCountryCodeScript(step.selector, code)
                            injectJs(js)
                            addLog("[Run $iteration] Selected Country Code '$code'")
                        }
                        AutomationActionType.SELECT_DATE -> {
                            val dob = resolvedValue.ifBlank { currentDefaultRecord.dob }
                            val parts = dob.split("-")
                            val y = parts.getOrNull(0) ?: currentDefaultRecord.dobYear
                            val m = parts.getOrNull(1) ?: currentDefaultRecord.dobMonth
                            val d = parts.getOrNull(2) ?: currentDefaultRecord.dobDay
                            val js = AutomationScripts.createSelectDateScript(step.selector, dob, d, m, y)
                            injectJs(js)
                            addLog("[Run $iteration] Selected Date: '$dob'")
                        }
                        AutomationActionType.UPLOAD_FILE -> {
                            val fileName = resolvedValue.ifBlank { currentDefaultRecord.fileName }
                            val js = AutomationScripts.createFileUploadScript(step.selector, fileName)
                            injectJs(js)
                            addLog("[Run $iteration] Uploaded Document '$fileName'")
                        }
                        AutomationActionType.SUBMIT -> {
                            val js = AutomationScripts.createSubmitScript(step.selector)
                            injectJs(js)
                            addLog("[Run $iteration] Submitted Form: ${step.elementDescription}")
                        }
                        AutomationActionType.WAIT -> {
                            addLog("[Run $iteration] Waiting ${step.delayMs}ms")
                        }
                        AutomationActionType.CLEAR -> {
                            val js = AutomationScripts.createInputScript(step.selector, "", true)
                            injectJs(js)
                            addLog("[Run $iteration] Cleared ${step.elementDescription}")
                        }
                        AutomationActionType.SCROLL -> {
                            injectJs("window.scrollBy({ top: 350, behavior: 'smooth' });")
                            addLog("[Run $iteration] Scrolled page")
                        }
                    }
                }

                if (script.clearDataOnComplete && iteration < script.repeatCount) {
                    delay(700)
                    script.steps.filter { it.actionType == AutomationActionType.INPUT }.forEach { inputStep ->
                        val clearJs = AutomationScripts.createInputScript(inputStep.selector, "", true)
                        injectJs(clearJs)
                    }
                }

                delay(1100)
            }

            _executionState.value = _executionState.value.copy(
                status = AutomationStatus.IDLE,
                isAwaitingExhaustionChoice = false,
                exhaustedFieldKey = null
            )
            addLog("Automation finished.")
        }
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
            // Field not present in Data Sheet -> fall back to Default random generator
            val generated = defaultFallback()
            addLog("Field '$fieldKey' not in '${activeSheet.name}' -> Default generated: $generated")
            return generated
        }

        val order = script.iterationOrder
        val policy = script.exhaustionPolicy
        val usageIdx = usageCounters.getOrDefault(fieldKey, 0)

        // Ensure shuffled order is initialized and matches current list size
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
                // Pause and prompt the user in HUD: Stop, Continue Using Default, or Add New Data
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
                        // Reload updated sheet and consume the newly added item!
                        val refreshedSheet = getDataSheet(script.dataSheetId)
                        storedList = refreshedSheet?.getFieldValues(fieldKey) ?: emptyList()
                        if (usageIdx < storedList.size) {
                            // Rebuild shuffle for new indices if needed
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
        addLog("Updated step: ${step.elementDescription}")
    }

    fun addStep(step: AutomationStep) {
        _currentScript.value = _currentScript.value.copy(steps = _currentScript.value.steps + step)
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
    }

    fun removeStep(stepId: String) {
        _currentScript.value = _currentScript.value.copy(
            steps = _currentScript.value.steps.filter { it.id != stepId }
        )
        addLog("Removed step.")
    }

    fun clearAllSteps() {
        _currentScript.value = _currentScript.value.copy(steps = emptyList())
        addLog("Cleared all steps.")
    }

    fun updateCurrentScript(script: AutomationScript) {
        _currentScript.value = script
        // Also sync back if this script is already in savedScripts
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
        scope.launch(Dispatchers.Main) {
            val webView = webViewProvider?.invoke()
            webView?.evaluateJavascript(script, null)
        }
    }
}
