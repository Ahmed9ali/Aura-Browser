package com.example.automation

import android.content.Context
import android.view.inputmethod.InputMethodManager
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
    private var pageLoadWaiter: CompletableDeferred<String>? = null
    @Volatile
    private var lastKnownPageUrl: String = ""
    @Volatile
    private var isWebViewPageLoading: Boolean = false
    @Volatile
    private var currentLoadProgress: Int = 100
    // Only true when the user explicitly clicks "Grab Elements"
    private var autoReadyOnNextScan: Boolean = false

    var onRotateProxy: (() -> Unit)? = null
    var webViewProvider: (() -> WebView?)? = null
    var webViewNavigator: ((String) -> Unit)? = null

    private sealed class ExhaustionDecision {
        object Stop : ExhaustionDecision()
        object ContinueWithDefault : ExhaustionDecision()
        data class AddedNewData(val newValues: List<String>) : ExhaustionDecision()
    }

    fun syncCurrentPageUrl(url: String) {
        if (url.isNotBlank() && !url.startsWith("aura://") && url != "about:blank") {
            lastKnownPageUrl = url
        }
    }

    fun normalizePageUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return url.trim().trimEnd('/')
    }

    fun arePageUrlsSame(url1: String?, url2: String?): Boolean {
        val n1 = normalizePageUrl(url1)
        val n2 = normalizePageUrl(url2)
        if (n1.isEmpty() || n2.isEmpty()) return n1 == n2
        return n1.equals(n2, ignoreCase = true)
    }

    /**
     * Resolves Page 1's starting URL for a script so multi-page flows always start from Page 1.
     */
    fun getStartingPageUrl(script: AutomationScript = _currentScript.value): String {
        val firstStepPageUrl = script.steps
            .firstOrNull { it.pageUrl.isNotBlank() && !it.pageUrl.startsWith("aura://") }
            ?.pageUrl
        val firstNavigateUrl = script.steps
            .firstOrNull { it.actionType == AutomationActionType.NAVIGATE && (it.value.isNotBlank() || it.selector.isNotBlank()) }
            ?.let { it.value.ifBlank { it.selector } }
        return when {
            !firstStepPageUrl.isNullOrBlank() -> firstStepPageUrl
            !firstNavigateUrl.isNullOrBlank() -> firstNavigateUrl
            script.targetUrl.isNotBlank() && !script.targetUrl.startsWith("aura://") -> script.targetUrl
            else -> lastKnownPageUrl
        }
    }

    fun countDistinctPages(steps: List<AutomationStep> = _currentScript.value.steps): Int {
        val pages = steps
            .mapNotNull { normalizePageUrl(it.pageUrl).takeIf { u -> u.isNotBlank() && !u.startsWith("aura://") } }
            .distinct()
        return pages.size.coerceAtLeast(if (steps.isNotEmpty()) 1 else 0)
    }

    // JavaScript Bridge exposed to WebView
    inner class AutomationJsInterface {
        @JavascriptInterface
        fun onPageUrlDetected(url: String) {
            if (url.isNotBlank() && !url.startsWith("aura://") && url != "about:blank") {
                lastKnownPageUrl = url
            }
        }

        @JavascriptInterface
        fun onUserAction(actionType: String, selector: String, description: String, fieldKeyFromJs: String) {
            if (_executionState.value.status == AutomationStatus.PLAYING) return
            if (_executionState.value.status != AutomationStatus.RECORDING &&
                _executionState.value.hudMode != HudMode.CREATE_AUTOMATION
            ) return

            hideWebViewSoftKeyboard()

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

            val stepPageUrl = lastKnownPageUrl
            val newStep = AutomationStep(
                actionType = type,
                selector = selector,
                elementDescription = description,
                value = if (resolvedKey != null) "{{$resolvedKey}}" else "",
                variableKey = resolvedKey,
                pageUrl = stepPageUrl,
                delayMs = 750L
            )

            scope.launch {
                val currentSteps = _currentScript.value.steps
                // Prevent duplicate capture of the exact same input/checkbox/radio selector on the SAME page
                val existingIdx = currentSteps.indexOfFirst {
                    it.selector == newStep.selector &&
                            arePageUrlsSame(it.pageUrl, newStep.pageUrl) &&
                            (it.actionType == newStep.actionType ||
                                    it.actionType == AutomationActionType.INPUT ||
                                    it.actionType == AutomationActionType.CHECKBOX ||
                                    it.actionType == AutomationActionType.RADIO)
                }

                val updatedSteps = if (existingIdx >= 0 && type != AutomationActionType.CLICK) {
                    currentSteps.toMutableList().apply { set(existingIdx, newStep.copy(id = currentSteps[existingIdx].id)) }
                } else {
                    val lastStep = currentSteps.lastOrNull()
                    if (lastStep != null &&
                        lastStep.selector == newStep.selector &&
                        arePageUrlsSame(lastStep.pageUrl, newStep.pageUrl) &&
                        lastStep.actionType == newStep.actionType
                    ) {
                        return@launch
                    }
                    currentSteps + newStep
                }

                val page1Url = getStartingPageUrl(_currentScript.value.copy(steps = updatedSteps))
                val domain = extractDomain(page1Url.ifBlank { stepPageUrl })
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
                    targetUrl = page1Url,
                    steps = updatedSteps
                )
                syncCurrentScriptToSavedScripts()
                val totalPages = countDistinctPages(updatedSteps)
                addLog("Grabbed ready element (Page $totalPages): $description -> ${resolvedKey?.let { "Data '{{$it}}'" } ?: type.name}")
            }
        }

        @JavascriptInterface
        fun onScanResults(jsonString: String) {
            scope.launch {
                try {
                    hideWebViewSoftKeyboard()
                    val elements = parseScannedElements(jsonString)
                    _scannedElements.value = elements

                    // ONLY build steps when explicitly requested by user pressing "Grab Elements" (autoReadyOnNextScan == true).
                    // Never auto-rebuild merely because steps are empty (prevents re-grab loop after deleting steps).
                    val shouldBuildReady = autoReadyOnNextScan
                    autoReadyOnNextScan = false

                    if (elements.isNotEmpty() && shouldBuildReady) {
                        buildReadyStepsFromGrabbedElements(elements)
                    } else {
                        addLog("Scanned ${elements.size} elements on page.")
                    }

                    if (_executionState.value.status == AutomationStatus.SCANNING) {
                        _executionState.value = _executionState.value.copy(status = AutomationStatus.RECORDING)
                    }
                } catch (e: Exception) {
                    autoReadyOnNextScan = false
                    addLog("Scanner parse error: ${e.message}")
                }
            }
        }

        private fun parseScannedElements(jsonString: String): List<ScannedElement> {
            return try {
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
                elements
            } catch (_: Throwable) {
                // Safe character-based fallback parser for local JVM unit tests (avoids [^{}] regex on Android ICU)
                val blocks = mutableListOf<String>()
                var inStr = false
                var esc = false
                var startIdx = -1
                for (i in jsonString.indices) {
                    val ch = jsonString[i]
                    if (inStr) {
                        if (esc) esc = false
                        else if (ch == '\\') esc = true
                        else if (ch == '"') inStr = false
                    } else {
                        when (ch) {
                            '"' -> inStr = true
                            '{' -> startIdx = i
                            '}' -> if (startIdx >= 0 && i > startIdx) {
                                blocks.add(jsonString.substring(startIdx, i + 1))
                                startIdx = -1
                            }
                        }
                    }
                }
                blocks.mapNotNull { raw ->
                    fun strVal(key: String): String =
                        Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(raw)?.groupValues?.get(1) ?: ""
                    fun boolVal(key: String): Boolean =
                        Regex("\"$key\"\\s*:\\s*(true|false)").find(raw)?.groupValues?.get(1)?.toBoolean() ?: false
                    val selector = strVal("selector")
                    if (selector.isBlank()) null
                    else {
                        val semStr = strVal("semanticType").ifBlank { "GENERIC_INPUT" }
                        val sem = runCatching { ElementSemanticType.valueOf(semStr) }.getOrDefault(ElementSemanticType.GENERIC_INPUT)
                        ScannedElement(
                            tagName = strVal("tagName"),
                            type = strVal("type"),
                            nameAttr = strVal("nameAttr"),
                            idAttr = strVal("idAttr"),
                            placeholder = strVal("placeholder"),
                            text = strVal("text"),
                            selector = selector,
                            pageUrl = strVal("pageUrl").ifBlank { lastKnownPageUrl },
                            isInput = boolVal("isInput"),
                            isButton = boolVal("isButton"),
                            semanticType = sem
                        )
                    }
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

        val scanPageUrl = elements.firstOrNull { it.pageUrl.isNotBlank() }?.pageUrl?.ifBlank { lastKnownPageUrl } ?: lastKnownPageUrl
        if (scanPageUrl.isNotBlank() && !scanPageUrl.startsWith("aura://")) {
            lastKnownPageUrl = scanPageUrl
        }

        val readySteps = targetList.map { element ->
            val (actionType, varKey, defaultVal, desc) = mapScannedElementDetails(element)
            AutomationStep(
                actionType = actionType,
                selector = element.selector,
                elementDescription = desc,
                value = defaultVal,
                variableKey = varKey,
                pageUrl = element.pageUrl.ifBlank { scanPageUrl },
                delayMs = if (actionType == AutomationActionType.SUBMIT) 1200L else 650L
            )
        }

        // Preserve all steps grabbed on Page 1 and any previous pages; only deduplicate selectors on the SAME pageUrl
        val existingSteps = _currentScript.value.steps
        val finalSteps = if (existingSteps.isEmpty()) {
            readySteps
        } else {
            val existingSelectorsOnCurrentPage = existingSteps
                .filter { arePageUrlsSame(it.pageUrl, scanPageUrl) }
                .map { it.selector }
                .toSet()
            val newStepsForCurrentPage = readySteps.filter { it.selector !in existingSelectorsOnCurrentPage }
            existingSteps + newStepsForCurrentPage
        }

        val page1Url = getStartingPageUrl(_currentScript.value.copy(steps = finalSteps))
        val domain = extractDomain(page1Url.ifBlank { scanPageUrl })
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
            targetUrl = page1Url,
            steps = finalSteps,
            dataSheetId = _currentScript.value.dataSheetId ?: _dataSheets.value.firstOrNull()?.id
        )
        syncCurrentScriptToSavedScripts()

        val pageCount = countDistinctPages(finalSteps)
        addLog("Grabbed ${readySteps.size} elements on page (Total: ${finalSteps.size} steps across $pageCount page(s), starting from Page 1)!")
    }

    private fun syncCurrentScriptToSavedScripts() {
        val current = _currentScript.value
        _executionState.value = _executionState.value.copy(totalSteps = current.steps.size)
        val existingIdx = _savedScripts.value.indexOfFirst { it.id == current.id }
        if (current.steps.isEmpty()) {
            // If all steps were deleted from the current script, remove it from savedScripts so it does not linger or resurrect
            if (existingIdx >= 0) {
                _savedScripts.value = _savedScripts.value.filterNot { it.id == current.id }
            }
            return
        }
        if (existingIdx >= 0) {
            _savedScripts.value = _savedScripts.value.toMutableList().apply { set(existingIdx, current) }
        } else {
            _savedScripts.value = listOf(current) + _savedScripts.value
        }
    }

    private fun detectVariableKeyFromSelector(selector: String, description: String): String? {
        val selLower = selector.lowercase()
        val descLower = description.lowercase()
        val combined = "$selector $description".lowercase()

        // 1. Phone input check: If selector has input or description has phone, prioritize phoneNumber over countryCode
        if ((selLower.contains("input") || descLower.contains("phone") || descLower.contains("mobile")) &&
            (combined.contains("phone") || combined.contains("mobile") || combined.contains("tel") || combined.contains("callingcodephone")) &&
            !combined.contains("countrycode") && !combined.contains("dialcode") && !combined.contains("calling-code")
        ) {
            return "phoneNumber"
        }

        return when {
            combined.contains("email") || combined.contains("mail") -> "email"
            combined.contains("confirm") && combined.contains("pass") -> "confirmPassword"
            combined.contains("pass") -> "password"
            combined.contains("user") || combined.contains("login") || combined.contains("handle") -> "username"
            combined.contains("first") && combined.contains("name") -> "firstName"
            combined.contains("last") && combined.contains("name") -> "lastName"
            combined.contains("full") && combined.contains("name") -> "fullName"
            combined.contains("countrycode") || combined.contains("dial-code") || combined.contains("dialcode") ||
                    combined.contains("countrydropdown") || combined.contains("country_dropdown") ||
                    combined.contains("iti__") || combined.contains("phoneinputcountry") ||
                    combined.contains("selected-flag") || combined.contains("flag-dropdown") ||
                    combined.contains("countrylist") || combined.contains("calling-code") ||
                    (combined.contains("country") && (combined.contains("code") || combined.contains("dial") || combined.contains("prefix") || combined.contains("phone") || combined.contains("region"))) ||
                    (combined.contains("callingcode") && !selLower.contains("input")) -> "countryCode"
            // Check split & full DOB BEFORE phoneNumber so <input type="tel" id="day"|"year"> is never misclassified as phone
            combined.contains("dobday") || combined.contains("birth_day") || combined.contains("bday-day") || combined.contains("birthday_day") ||
                    selector.equals("#day", ignoreCase = true) || selLower.contains("day") ||
                    (Regex("\\b(day|dd)\\b").containsMatchIn(combined) && !combined.contains("today") && !combined.contains("everyday")) -> "dobDay"
            combined.contains("dobmonth") || combined.contains("birth_month") || combined.contains("bday-month") || combined.contains("birthday_month") ||
                    selector.equals("#month", ignoreCase = true) || selLower.contains("month") ||
                    Regex("\\b(month|mm|january|february)\\b").containsMatchIn(combined) -> "dobMonth"
            combined.contains("dobyear") || combined.contains("birth_year") || combined.contains("bday-year") || combined.contains("birthday_year") ||
                    selector.equals("#year", ignoreCase = true) || selLower.contains("year") ||
                    Regex("\\b(year|yyyy|yr)\\b").containsMatchIn(combined) -> "dobYear"
            combined.contains("birth") || combined.contains("dob") || combined.contains("bday") || combined.contains("datepicker") -> "dob"
            combined.contains("phone") || combined.contains("mobile") || combined.contains("tel") || combined.contains("whatsapp") || combined.contains("callingcodephone") -> "phoneNumber"
            combined.contains("gender") || combined.contains("sex") -> "gender"
            combined.contains("zip") || combined.contains("postal") || combined.contains("pin") -> "zipCode"
            combined.contains("city") -> "city"
            combined.contains("state") || combined.contains("province") -> "state"
            combined.contains("address") -> "address"
            combined.contains("file") || combined.contains("upload") || combined.contains("resume") -> "fileName"
            else -> null
        }
    }

    private fun extractRecordFieldValue(record: AutomationDataRecord, fieldKey: String): String {
        return when (fieldKey.lowercase()) {
            "email" -> record.email
            "username", "user", "login" -> record.username
            "password", "pass" -> record.password
            "confirmpassword" -> record.confirmPassword
            "firstname" -> record.firstName
            "lastname" -> record.lastName
            "fullname", "name" -> record.fullName
            "phone", "phonenumber", "mobile" -> record.phoneNumber
            "countrycode" -> record.countryCode
            "gender", "sex" -> record.gender
            "dob", "birthdate" -> record.dob
            "dobday" -> record.dobDay
            "dobmonth" -> record.dobMonth
            "dobyear" -> record.dobYear
            "address" -> record.address
            "addressline2" -> record.addressLine2
            "city" -> record.city
            "state" -> record.state
            "zipcode", "zip", "pin", "pincode" -> record.zipCode
            "filename", "file" -> record.fileName
            else -> AutomationDataGenerator.generateSingleFieldValue(fieldKey)
        }
    }

    /**
     * Called when WebView starts loading a new page (`onPageStarted`).
     * Marks the engine as waiting for website load so functions wait until the page is ready.
     */
    fun onPageLoadStarted(url: String) {
        if (url.isBlank() || url.startsWith("aura://") || url == "about:blank") return
        lastKnownPageUrl = url
        isWebViewPageLoading = true
        currentLoadProgress = 10
        if (pageLoadWaiter == null || pageLoadWaiter?.isCompleted == true) {
            pageLoadWaiter = CompletableDeferred()
        }
        _executionState.value = _executionState.value.copy(
            isWaitingForPageLoad = true,
            pageLoadProgress = 10
        )
    }

    /**
     * Called when WebView reports page loading progress (`onProgressChanged`).
     */
    fun onPageLoadProgress(progress: Int) {
        val clamped = progress.coerceIn(0, 100)
        currentLoadProgress = clamped
        if (clamped >= 100) {
            isWebViewPageLoading = false
            pageLoadWaiter?.complete(lastKnownPageUrl)
            _executionState.value = _executionState.value.copy(
                isWaitingForPageLoad = false,
                pageLoadProgress = 100
            )
        } else {
            _executionState.value = _executionState.value.copy(
                pageLoadProgress = clamped
            )
        }
    }

    fun isWebsiteCurrentlyLoading(): Boolean {
        return isWebViewPageLoading || currentLoadProgress < 100
    }

    /**
     * Called when WebView finishes loading a page (`onPageFinished`).
     * Note: Does NOT auto-trigger bulk scan so user deletions are never overwritten in a loop.
     */
    fun onPageLoaded(url: String) {
        isWebViewPageLoading = false
        currentLoadProgress = 100
        _executionState.value = _executionState.value.copy(
            isWaitingForPageLoad = false,
            pageLoadProgress = 100
        )
        if (url.isBlank() || url.startsWith("aura://")) {
            pageLoadWaiter?.complete(url)
            return
        }
        lastKnownPageUrl = url
        pageLoadWaiter?.complete(url)

        if (_executionState.value.status == AutomationStatus.RECORDING ||
            _executionState.value.hudMode == HudMode.CREATE_AUTOMATION
        ) {
            injectJs(AutomationScripts.RECORDING_INJECTION)
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
        if (currentUrl.isNotBlank() && !currentUrl.startsWith("aura://")) {
            lastKnownPageUrl = currentUrl
        }
        _executionState.value = _executionState.value.copy(hudMode = mode)
        if (mode == HudMode.CREATE_AUTOMATION) {
            val domain = extractDomain(currentUrl)
            if (domain.isNotBlank() && !domain.contains("home")) {
                if (_currentScript.value.targetWebsite == "*" && _currentScript.value.steps.isEmpty()) {
                    _currentScript.value = _currentScript.value.copy(
                        name = if (_currentScript.value.name == "New Automation Flow") "$domain Automation" else _currentScript.value.name,
                        targetWebsite = domain,
                        targetUrl = currentUrl
                    )
                }
            }
            startRecording()
        } else {
            stopRecording()
        }
    }

    /**
     * Clears/resets the current script and all grabbed elements WITHOUT auto-grabbing again.
     */
    fun startFreshRecordingScript(currentUrl: String) {
        autoReadyOnNextScan = false
        val oldId = _currentScript.value.id
        _savedScripts.value = _savedScripts.value.filterNot { it.id == oldId && _currentScript.value.steps.isEmpty() }

        val domain = extractDomain(currentUrl).ifBlank { "*" }
        val fresh = AutomationScript(
            id = UUID.randomUUID().toString(),
            name = if (domain != "*" && !domain.contains("home")) "$domain Automation" else "Custom Web Automation",
            targetWebsite = if (domain.contains("home")) "*" else domain,
            targetUrl = currentUrl,
            steps = emptyList(),
            dataSheetId = _dataSheets.value.firstOrNull()?.id,
            repeatCount = 4,
            iterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
            exhaustionPolicy = DataExhaustionPolicy.ADD_NEW
        )
        _currentScript.value = fresh
        _scannedElements.value = emptyList()
        _executionState.value = _executionState.value.copy(totalSteps = 0, currentStepIndex = 0)
        injectJs(AutomationScripts.CLEAR_SCAN_HIGHLIGHTS)
        startRecording()
        addLog("Cleared script. Tap 'Grab Elements' or tap elements on page to add new steps.")
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
            startRecording()
        } else if (!next) {
            stopRecording()
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
        } else if (!visible) {
            stopRecording()
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
        _executionState.value = _executionState.value.copy(totalSteps = script.steps.size)
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
        val page1Url = getStartingPageUrl(current)

        val newScript = current.copy(
            id = scriptId,
            name = name.ifBlank { "Website Automation" },
            targetWebsite = targetWebsite.ifBlank { "*" },
            targetUrl = page1Url,
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
        _executionState.value = _executionState.value.copy(totalSteps = newScript.steps.size)
        addLog("Saved script '${newScript.name}' (${newScript.steps.size} steps across ${countDistinctPages(newScript.steps)} page(s))")
    }

    fun deleteScript(scriptId: String) {
        _savedScripts.value = _savedScripts.value.filter { it.id != scriptId }
        if (_currentScript.value.id == scriptId) {
            val nextScript = _savedScripts.value.firstOrNull() ?: AutomationScript(
                id = UUID.randomUUID().toString(),
                name = "New Automation Flow",
                targetWebsite = "*",
                steps = emptyList(),
                repeatCount = 1
            )
            _currentScript.value = nextScript
            _executionState.value = _executionState.value.copy(totalSteps = nextScript.steps.size)
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
        hideWebViewSoftKeyboard()
        injectJs(AutomationScripts.RECORDING_INJECTION)
    }

    fun stopRecording() {
        if (_executionState.value.status == AutomationStatus.RECORDING) {
            _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
            injectJs(AutomationScripts.STOP_RECORDING_INJECTION)
        }
    }

    // Scanner (Grab Elements & Ready Use) - Waits for website to load before scanning
    fun triggerScan(autoReadySteps: Boolean = true) {
        autoReadyOnNextScan = autoReadySteps
        if (_executionState.value.status != AutomationStatus.RECORDING) {
            _executionState.value = _executionState.value.copy(status = AutomationStatus.SCANNING)
        }
        hideWebViewSoftKeyboard()
        scope.launch {
            waitForWebsiteLoadAndDomReady(
                requireInteractiveElements = true,
                reason = "Grab Elements",
                timeoutMs = 8000L
            )
            addLog("Scanning & grabbing page elements...")
            injectJs(AutomationScripts.DOM_SCANNER)
        }
    }

    fun clearScanHighlights() {
        autoReadyOnNextScan = false
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
        val stepPageUrl = element.pageUrl.ifBlank { lastKnownPageUrl }
        val newStep = AutomationStep(
            actionType = actionType,
            selector = element.selector,
            elementDescription = desc,
            value = defaultVal,
            variableKey = varKey,
            pageUrl = stepPageUrl,
            delayMs = 750L
        )
        val updatedSteps = _currentScript.value.steps + newStep
        val page1Url = getStartingPageUrl(_currentScript.value.copy(steps = updatedSteps))
        _currentScript.value = _currentScript.value.copy(
            targetUrl = page1Url,
            steps = updatedSteps
        )
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

    private fun getCurrentWebViewUrl(): String {
        val liveUrl = try {
            webViewProvider?.invoke()?.url
        } catch (_: Exception) {
            null
        }
        return if (!liveUrl.isNullOrBlank() && !liveUrl.startsWith("aura://") && liveUrl != "about:blank") {
            liveUrl
        } else {
            lastKnownPageUrl
        }
    }

    /**
     * Evaluates raw JavaScript on the attached WebView safely on the UI thread and returns the result.
     */
    private suspend fun evaluateRawJs(
        webView: WebView,
        js: String,
        timeoutMs: Long = 1800L
    ): String? {
        val deferred = CompletableDeferred<String?>()
        try {
            webView.post {
                try {
                    webView.evaluateJavascript(js) { result ->
                        deferred.complete(result)
                    }
                } catch (e: Exception) {
                    deferred.complete(null)
                }
            }
        } catch (e: Exception) {
            deferred.complete(null)
        }
        return withTimeoutOrNull(timeoutMs) { deferred.await() }
    }

    /**
     * Waits for the website to finish loading both at the WebView network/navigation layer
     * (`onPageStarted` -> `onProgressChanged(100)` / `onPageFinished`) AND at the DOM/SPA layer
     * (`document.readyState === 'complete'`, no blocking full-screen loading spinners, and
     * optional `targetSelector` or interactive elements mounted) before performing functions.
     */
    suspend fun waitForWebsiteLoadAndDomReady(
        targetSelector: String = "",
        requireInteractiveElements: Boolean = false,
        reason: String = "",
        timeoutMs: Long = 10000L
    ): Boolean {
        val startMs = System.currentTimeMillis()
        val prevStepDesc = _executionState.value.currentStepDescription

        // Phase 1: Wait for active WebView page load (network/frame load) to complete
        if (isWebViewPageLoading || currentLoadProgress < 100 || (pageLoadWaiter != null && pageLoadWaiter?.isCompleted == false)) {
            val waitLabel = if (reason.isNotBlank()) "Waiting for website to load ($reason)..." else "Waiting for website to load..."
            _executionState.value = _executionState.value.copy(
                isWaitingForPageLoad = true,
                pageLoadProgress = currentLoadProgress,
                currentStepDescription = waitLabel
            )
            addLog(waitLabel)

            val waiter = pageLoadWaiter ?: CompletableDeferred<String>().also { pageLoadWaiter = it }
            withTimeoutOrNull(timeoutMs) {
                waiter.await()
            }
            isWebViewPageLoading = false
            currentLoadProgress = 100
            pageLoadWaiter = null
        }

        // Phase 2: If a live WebView is attached, poll DOM readiness & SPA hydration via JS
        val webView = webViewProvider?.invoke()
        if (webView != null) {
            val remainingMs = (timeoutMs - (System.currentTimeMillis() - startMs)).coerceIn(1200L, 6000L)
            val checkJs = AutomationScripts.createCheckPageReadyScript(
                targetSelector = targetSelector,
                requireInteractiveElements = requireInteractiveElements
            )
            var domReady = false
            var loggedDomWait = false
            val domDeadline = System.currentTimeMillis() + remainingMs

            while (System.currentTimeMillis() < domDeadline) {
                val res = evaluateRawJs(webView, checkJs, 1200L)
                if (res == null || res.contains("READY", ignoreCase = true)) {
                    domReady = true
                    break
                }
                if (!loggedDomWait) {
                    loggedDomWait = true
                    val waitLabel = if (reason.isNotBlank()) {
                        "Waiting for website DOM to load ($reason)..."
                    } else {
                        "Waiting for website DOM to load..."
                    }
                    _executionState.value = _executionState.value.copy(
                        isWaitingForPageLoad = true,
                        currentStepDescription = waitLabel
                    )
                }
                delay(220L)
            }
            if (loggedDomWait && domReady) {
                delay(180L)
            }
        }

        _executionState.value = _executionState.value.copy(
            isWaitingForPageLoad = false,
            pageLoadProgress = 100,
            currentStepDescription = prevStepDesc
        )
        return true
    }

    private suspend fun navigateWebViewAndWait(targetUrl: String, timeoutMs: Long = 10000L) {
        if (targetUrl.isBlank() || targetUrl.startsWith("aura://")) return
        val waiter = CompletableDeferred<String>()
        pageLoadWaiter = waiter
        lastKnownPageUrl = targetUrl
        isWebViewPageLoading = true
        currentLoadProgress = 15
        _executionState.value = _executionState.value.copy(
            isWaitingForPageLoad = true,
            pageLoadProgress = 15
        )

        val nav = webViewNavigator
        val wv = webViewProvider?.invoke()
        if (nav != null) {
            nav.invoke(targetUrl)
        } else if (wv != null) {
            wv.post { wv.loadUrl(targetUrl) }
        } else {
            isWebViewPageLoading = false
            currentLoadProgress = 100
            pageLoadWaiter = null
            _executionState.value = _executionState.value.copy(
                isWaitingForPageLoad = false,
                pageLoadProgress = 100
            )
            return
        }

        withTimeoutOrNull(timeoutMs) {
            waiter.await()
        }
        isWebViewPageLoading = false
        currentLoadProgress = 100
        pageLoadWaiter = null

        // Wait for DOM & SPA hydration after navigation completes
        waitForWebsiteLoadAndDomReady(
            requireInteractiveElements = true,
            reason = extractDomain(targetUrl).ifBlank { targetUrl },
            timeoutMs = 5000L
        )
        delay(250L)
        hideWebViewSoftKeyboard()
    }

    private suspend fun waitForNaturalPageTransition(expectedPageUrl: String, maxWaitMs: Long = 3500L) {
        if (expectedPageUrl.isBlank() || expectedPageUrl.startsWith("aura://")) return
        var elapsed = 0L
        val stepMs = 150L
        while (elapsed < maxWaitMs) {
            if (arePageUrlsSame(getCurrentWebViewUrl(), expectedPageUrl)) {
                waitForWebsiteLoadAndDomReady(
                    requireInteractiveElements = true,
                    reason = extractDomain(expectedPageUrl).ifBlank { "Page Transition" },
                    timeoutMs = 5000L
                )
                return
            }
            delay(stepMs)
            elapsed += stepMs
        }
    }

    /**
     * Playback Execution:
     * - STRICT GATE: Cannot start if no script or steps exist; switches HUD to Create Automation mode.
     * - ALWAYS starts from Page 1 (the URL where the first elements of the workflow were grabbed) and transitions across pages in order.
     * - Waits for the website and DOM to finish loading before performing functions.
     * - Suppresses the Android soft keyboard throughout filling.
     * - Consumes stored values from the linked Data Sheet 1 by 1, or uses Default Generator if no Data Sheet is linked.
     */
    fun startPlayback() {
        // If currentScript has no steps, check if a valid saved script exists for this website
        if (_currentScript.value.steps.isEmpty() && _savedScripts.value.isNotEmpty()) {
            val matched = getScriptsForCurrentUrl(lastKnownPageUrl).firstOrNull { it.steps.isNotEmpty() }
                ?: _savedScripts.value.firstOrNull { it.steps.isNotEmpty() }
            if (matched != null) {
                _currentScript.value = matched
            }
        }

        val script = _currentScript.value
        if (script.steps.isEmpty()) {
            // Strict gate: When no scripts/steps exist, the engine CANNOT start; user must create one in HUD
            _executionState.value = _executionState.value.copy(
                status = AutomationStatus.RECORDING,
                hudMode = HudMode.CREATE_AUTOMATION,
                isHudVisible = true
            )
            addLog("Cannot start automation: No script created yet. Tap 'Grab Elements' or tap elements on the page to create one.")
            injectJs(AutomationScripts.RECORDING_INJECTION)
            return
        }

        val page1StartingUrl = getStartingPageUrl(script)

        // Auto-save the current script into savedScripts if it isn't saved yet, locking targetUrl to Page 1
        if (_savedScripts.value.none { it.id == script.id }) {
            val domain = extractDomain(page1StartingUrl.ifBlank { lastKnownPageUrl }).ifBlank { script.targetWebsite.ifBlank { "*" } }
            val savedCopy = script.copy(
                name = if (script.name == "New Automation Flow" && domain != "*") "$domain Automation" else script.name,
                targetWebsite = if (script.targetWebsite == "*" && domain != "*") domain else script.targetWebsite,
                targetUrl = page1StartingUrl
            )
            _currentScript.value = savedCopy
            _savedScripts.value = listOf(savedCopy) + _savedScripts.value
        } else if (page1StartingUrl.isNotBlank() && _currentScript.value.targetUrl != page1StartingUrl) {
            val updatedCopy = script.copy(targetUrl = page1StartingUrl)
            _currentScript.value = updatedCopy
            syncCurrentScriptToSavedScripts()
        }

        val activeScript = _currentScript.value
        stopPlayback()
        injectJs(AutomationScripts.STOP_RECORDING_INJECTION)
        hideWebViewSoftKeyboard()

        // Ensure Page 1 URL is immediately set if we are starting playback from Page 2+
        val initialCurrentUrl = getCurrentWebViewUrl()
        val needsInitialPage1Nav = page1StartingUrl.isNotBlank() &&
                !page1StartingUrl.startsWith("aura://") &&
                !arePageUrlsSame(initialCurrentUrl, page1StartingUrl)
        if (needsInitialPage1Nav) {
            lastKnownPageUrl = page1StartingUrl
        }

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

        val fieldUsageCounters = mutableMapOf<String, Int>()
        val fieldShuffledOrders = mutableMapOf<String, List<Int>>()
        val fieldsForcedToDefault = mutableSetOf<String>()

        playbackJob = scope.launch {
            val initialSheet = getDataSheet(activeScript.dataSheetId)
            val totalPagesCount = countDistinctPages(activeScript.steps)
            addLog(
                "Starting '${activeScript.name}' (${activeScript.steps.size} steps across $totalPagesCount page(s), ${activeScript.repeatCount}x, Order: ${activeScript.iterationOrder.shortLabel}). " +
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

                if (activeScript.rotateProxyOnRepeat) {
                    onRotateProxy?.invoke()
                    addLog("[Run $iteration] Rotated proxy node for clean session")
                    delay(450)
                }

                // Always start each iteration from Page 1 if the browser is currently on a later page (Page 2+) or on repeat runs
                if (page1StartingUrl.isNotBlank() && !page1StartingUrl.startsWith("aura://")) {
                    val firstStepIsSameNavigate = activeScript.steps.firstOrNull()?.let {
                        it.actionType == AutomationActionType.NAVIGATE &&
                                arePageUrlsSame(it.value.ifBlank { it.selector }, page1StartingUrl)
                    } ?: false
                    val shouldLoadPage1 = !firstStepIsSameNavigate &&
                            (iteration > 1 || needsInitialPage1Nav || !arePageUrlsSame(getCurrentWebViewUrl(), page1StartingUrl))
                    if (shouldLoadPage1) {
                        _executionState.value = _executionState.value.copy(
                            currentStepIndex = 1,
                            currentStepDescription = "Opening Page 1 (${extractDomain(page1StartingUrl).ifBlank { page1StartingUrl }})"
                        )
                        addLog("[Run $iteration] Navigating to Page 1: $page1StartingUrl")
                        navigateWebViewAndWait(page1StartingUrl)
                    } else {
                        // Even when already on Page 1, ensure the website has finished loading before running Step 1
                        waitForWebsiteLoadAndDomReady(
                            requireInteractiveElements = true,
                            reason = "Page 1"
                        )
                    }
                } else {
                    waitForWebsiteLoadAndDomReady(
                        requireInteractiveElements = true,
                        reason = "Initial Page"
                    )
                }

                var currentDefaultRecord = AutomationDataGenerator.generateSingleRandomRecord()
                var lastResolvedPassword: String? = null

                for ((index, step) in activeScript.steps.withIndex()) {
                    if (_executionState.value.status != AutomationStatus.PLAYING &&
                        _executionState.value.status != AutomationStatus.PAUSED
                    ) break@outerLoop

                    hideWebViewSoftKeyboard()

                    // Handle multi-page transitions (e.g. Page 1 -> Page 2 -> Page 3)
                    val prevPageUrl = if (index > 0) activeScript.steps[index - 1].pageUrl else page1StartingUrl
                    val stepPageUrl = step.pageUrl
                    if (step.actionType != AutomationActionType.NAVIGATE &&
                        stepPageUrl.isNotBlank() &&
                        !stepPageUrl.startsWith("aura://") &&
                        !arePageUrlsSame(stepPageUrl, getCurrentWebViewUrl())
                    ) {
                        if (index > 0 && !arePageUrlsSame(stepPageUrl, prevPageUrl)) {
                            waitForNaturalPageTransition(stepPageUrl, maxWaitMs = 3500L)
                        }
                        if (!arePageUrlsSame(stepPageUrl, getCurrentWebViewUrl())) {
                            addLog("[Run $iteration] Transitioning to page: $stepPageUrl")
                            navigateWebViewAndWait(stepPageUrl)
                        }
                    }

                    // Wait for website and DOM to finish loading before performing this step
                    if (step.actionType != AutomationActionType.NAVIGATE &&
                        step.actionType != AutomationActionType.WAIT
                    ) {
                        waitForWebsiteLoadAndDomReady(
                            targetSelector = step.selector,
                            requireInteractiveElements = step.actionType != AutomationActionType.SCROLL,
                            reason = step.elementDescription
                        )
                    }

                    _executionState.value = _executionState.value.copy(
                        currentStepIndex = index + 1,
                        currentStepDescription = step.elementDescription
                    )

                    val actualDelay = if (activeScript.humanizeDelays) {
                        (step.delayMs * (Random.nextDouble(0.85, 1.18))).toLong()
                    } else step.delayMs

                    delay(actualDelay)

                    // Re-verify website is not in the middle of a sudden redirect/reload right after step delay
                    if (step.actionType != AutomationActionType.NAVIGATE &&
                        step.actionType != AutomationActionType.WAIT &&
                        isWebsiteCurrentlyLoading()
                    ) {
                        waitForWebsiteLoadAndDomReady(
                            targetSelector = step.selector,
                            requireInteractiveElements = true,
                            reason = step.elementDescription
                        )
                    }

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
                                    val v = extractRecordFieldValue(currentDefaultRecord, stepFieldKey)
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
                            navigateWebViewAndWait(targetUrl)
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
                            // If this step was recorded as INPUT but targets a split DOB or full DOB or Country Code element,
                            // route to the smart multi-mode handler so both <input> and custom dropdowns/pickers work seamlessly
                            val ok = when (stepFieldKey) {
                                "dobDay", "dobMonth", "dobYear" -> {
                                    val js = AutomationScripts.createSelectDropdownScript(step.selector, resolvedValue, stepFieldKey)
                                    evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                                }
                                "dob" -> {
                                    val (y, m, d) = parseDobTriplet(resolvedValue, currentDefaultRecord)
                                    val js = AutomationScripts.createSelectDateScript(step.selector, "$y-$m-$d", d, m, y, fallbackHint)
                                    evaluateJsWithRetry(js, maxAttempts = 20, retryDelayMs = 220L)
                                }
                                "countryCode" -> {
                                    val code = resolvedValue.ifBlank { currentDefaultRecord.countryCode }
                                    val js = AutomationScripts.createSelectCountryCodeScript(step.selector, code, fallbackHint)
                                    evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                                }
                                else -> {
                                    val js = AutomationScripts.createInputScript(step.selector, resolvedValue, step.clearBeforeInput, fallbackHint)
                                    evaluateJsWithRetry(js)
                                }
                            }
                            addLog("[Run $iteration] ${if (ok) "Filled" else "Attempted fill"} ${step.elementDescription} -> '$resolvedValue'")
                        }
                        AutomationActionType.SELECT_DROPDOWN -> {
                            val ok = when (stepFieldKey) {
                                "countryCode" -> {
                                    val code = resolvedValue.ifBlank { currentDefaultRecord.countryCode }
                                    val js = AutomationScripts.createSelectCountryCodeScript(step.selector, code, fallbackHint)
                                    evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                                }
                                "dob" -> {
                                    val (y, m, d) = parseDobTriplet(resolvedValue, currentDefaultRecord)
                                    val js = AutomationScripts.createSelectDateScript(step.selector, "$y-$m-$d", d, m, y, fallbackHint)
                                    evaluateJsWithRetry(js, maxAttempts = 20, retryDelayMs = 220L)
                                }
                                "dobDay" -> {
                                    val d = resolvedValue.ifBlank { currentDefaultRecord.dobDay }
                                    val js = AutomationScripts.createSelectDropdownScript(step.selector, d, "day")
                                    evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                                }
                                "dobMonth" -> {
                                    val m = resolvedValue.ifBlank { currentDefaultRecord.dobMonth }
                                    val js = AutomationScripts.createSelectDropdownScript(step.selector, m, "month")
                                    evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                                }
                                "dobYear" -> {
                                    val y = resolvedValue.ifBlank { currentDefaultRecord.dobYear }
                                    val js = AutomationScripts.createSelectDropdownScript(step.selector, y, "year")
                                    evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                                }
                                else -> {
                                    val js = AutomationScripts.createSelectDropdownScript(step.selector, resolvedValue, fallbackHint)
                                    evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                                }
                            }
                            addLog("[Run $iteration] ${if (ok) "Selected" else "Attempted select"} '$resolvedValue' in ${step.elementDescription}")
                        }
                        AutomationActionType.SELECT_COUNTRY_CODE -> {
                            val code = resolvedValue.ifBlank { currentDefaultRecord.countryCode }
                            val js = AutomationScripts.createSelectCountryCodeScript(step.selector, code, fallbackHint)
                            val ok = evaluateJsWithRetry(js, maxAttempts = 12, retryDelayMs = 280L)
                            addLog("[Run $iteration] ${if (ok) "Selected" else "Attempted"} Country Code '$code'")
                        }
                        AutomationActionType.SELECT_DATE -> {
                            val dob = resolvedValue.ifBlank { currentDefaultRecord.dob }
                            val (y, m, d) = parseDobTriplet(dob, currentDefaultRecord)
                            val js = AutomationScripts.createSelectDateScript(step.selector, "$y-$m-$d", d, m, y, fallbackHint)
                            val ok = evaluateJsWithRetry(js, maxAttempts = 20, retryDelayMs = 220L)
                            addLog("[Run $iteration] ${if (ok) "Selected" else "Attempted"} Date: '$y-$m-$d'")
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

                    hideWebViewSoftKeyboard()
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
                isWaitingForPageLoad = false,
                pageLoadProgress = 100,
                isAwaitingExhaustionChoice = false,
                exhaustedFieldKey = null
            )
            addLog("Automation finished.")
        }
    }

    /**
     * Stronger JS Execution with automatic Wait-For-Website-Load and Wait-For-Element retry
     * for dynamic SPAs and delayed renders, while keeping the Android soft keyboard suppressed.
     */
    private suspend fun evaluateJsWithRetry(
        js: String,
        maxAttempts: Int = 6,
        retryDelayMs: Long = 350L
    ): Boolean {
        val webView = webViewProvider?.invoke() ?: return false
        for (attempt in 1..maxAttempts) {
            if (isWebsiteCurrentlyLoading()) {
                waitForWebsiteLoadAndDomReady(reason = "Step Retry $attempt")
            }
            hideWebViewSoftKeyboard()
            val res = evaluateRawJs(webView, js, 1800L)
            hideWebViewSoftKeyboard()
            if (res != null && (res.contains("SUCCESS", ignoreCase = true) || res.contains("CLICKED", ignoreCase = true))) {
                return true
            }
            if (attempt < maxAttempts) {
                delay(retryDelayMs)
            }
        }
        return false
    }

    private fun parseDobTriplet(
        rawDob: String,
        fallbackRecord: AutomationDataRecord
    ): Triple<String, String, String> {
        val monthMap = mapOf(
            "jan" to "01", "january" to "01",
            "feb" to "02", "february" to "02",
            "mar" to "03", "march" to "03",
            "apr" to "04", "april" to "04",
            "may" to "05",
            "jun" to "06", "june" to "06",
            "jul" to "07", "july" to "07",
            "aug" to "08", "august" to "08",
            "sep" to "09", "sept" to "09", "september" to "09",
            "oct" to "10", "october" to "10",
            "nov" to "11", "november" to "11",
            "dec" to "12", "december" to "12"
        )
        val parts = rawDob.trim().split(Regex("[-/.,\\s]+")).filter { it.isNotBlank() }
        if (parts.size >= 3) {
            val namedMonthIdx = parts.indexOfFirst { monthMap.containsKey(it.lowercase()) }
            if (namedMonthIdx >= 0) {
                val m = monthMap[parts[namedMonthIdx].lowercase()] ?: fallbackRecord.dobMonth
                val rest = parts.filterIndexed { i, _ -> i != namedMonthIdx }
                val y = rest.firstOrNull { it.length == 4 } ?: rest.last()
                val d = (rest.firstOrNull { it != y } ?: fallbackRecord.dobDay).padStart(2, '0')
                return Triple(y, m, d)
            }
            return when {
                parts[0].length == 4 -> Triple(
                    parts[0],
                    parts[1].padStart(2, '0'),
                    parts[2].padStart(2, '0')
                )
                parts[2].length == 4 -> {
                    val firstNum = parts[0].toIntOrNull() ?: 1
                    if (firstNum > 12) {
                        Triple(parts[2], parts[1].padStart(2, '0'), parts[0].padStart(2, '0'))
                    } else {
                        Triple(parts[2], parts[0].padStart(2, '0'), parts[1].padStart(2, '0'))
                    }
                }
                else -> Triple(
                    parts[0],
                    parts[1].padStart(2, '0'),
                    parts[2].padStart(2, '0')
                )
            }
        }
        return Triple(fallbackRecord.dobYear, fallbackRecord.dobMonth, fallbackRecord.dobDay)
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
                addLog("All ${storedList.size} '$fieldKey' values in '${activeSheet.name}' have been used 1 by 1. Waiting for user selection...")
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
        autoReadyOnNextScan = false
        val remaining = _currentScript.value.steps.filter { it.id != stepId }
        _currentScript.value = _currentScript.value.copy(steps = remaining)
        syncCurrentScriptToSavedScripts()
        if (remaining.isEmpty()) {
            _scannedElements.value = emptyList()
            injectJs(AutomationScripts.CLEAR_SCAN_HIGHLIGHTS)
            addLog("All grabbed elements removed.")
        } else {
            addLog("Removed element (${remaining.size} remaining).")
        }
    }

    fun clearAllSteps() {
        autoReadyOnNextScan = false
        _currentScript.value = _currentScript.value.copy(steps = emptyList())
        _scannedElements.value = emptyList()
        syncCurrentScriptToSavedScripts()
        injectJs(AutomationScripts.CLEAR_SCAN_HIGHLIGHTS)
        addLog("Cleared all grabbed elements.")
    }

    fun updateCurrentScript(script: AutomationScript) {
        _currentScript.value = script
        syncCurrentScriptToSavedScripts()
    }

    private fun hideWebViewSoftKeyboard() {
        val webView = webViewProvider?.invoke() ?: return
        webView.post {
            try {
                val imm = webView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(webView.windowToken, 0)
            } catch (_: Exception) {
            }
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
