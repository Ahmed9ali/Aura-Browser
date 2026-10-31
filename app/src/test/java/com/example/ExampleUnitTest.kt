package com.example

import com.example.data.BrowserRepository
import com.example.data.PersonalizedSuggestionsEngine
import com.example.model.SearchEngine
import com.example.model.SuggestionType
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    private lateinit var repository: BrowserRepository
    private lateinit var suggestionsEngine: PersonalizedSuggestionsEngine

    @Before
    fun setup() {
        repository = BrowserRepository()
        suggestionsEngine = PersonalizedSuggestionsEngine()
    }

    @Test
    fun testTabManagementOperations() {
        val initialTabCount = repository.tabs.value.size
        assertTrue("Should have initial tabs", initialTabCount > 0)

        // Open new tab
        val newTabId = repository.openNewTab(url = "https://example.org", title = "Example")
        assertEquals(initialTabCount + 1, repository.tabs.value.size)
        assertEquals(newTabId, repository.activeTabId.value)

        // Select an existing tab
        val firstTab = repository.tabs.value.first()
        repository.selectTab(firstTab.id)
        assertEquals(firstTab.id, repository.activeTabId.value)

        // Close tab
        repository.closeTab(newTabId)
        assertEquals(initialTabCount, repository.tabs.value.size)
        assertFalse(repository.tabs.value.any { it.id == newTabId })
    }

    @Test
    fun testTabGroupingCapabilities() {
        // Create custom group
        val groupId = repository.createGroup(name = "Dev Work", colorHex = "#6366F1")
        assertTrue(repository.tabGroups.value.any { it.id == groupId && it.name == "Dev Work" })

        // Assign a tab to the group
        val tabId = repository.openNewTab(url = "https://github.com", title = "GitHub")
        repository.assignTabToGroup(tabId, groupId)

        val updatedTab = repository.tabs.value.find { it.id == tabId }
        assertNotNull(updatedTab)
        assertEquals(groupId, updatedTab?.groupId)

        // Toggle group collapse
        val beforeCollapse = repository.tabGroups.value.find { it.id == groupId }?.isCollapsed ?: false
        repository.toggleGroupCollapse(groupId)
        val afterCollapse = repository.tabGroups.value.find { it.id == groupId }?.isCollapsed ?: false
        assertEquals(!beforeCollapse, afterCollapse)

        // Delete group and ungroup tabs
        repository.deleteGroup(groupId, keepTabs = true)
        assertFalse(repository.tabGroups.value.any { it.id == groupId })
        val unGroupedTab = repository.tabs.value.find { it.id == tabId }
        assertEquals(null, unGroupedTab?.groupId)
    }

    @Test
    fun testPersonalizedSuggestionsEngine() {
        // Record visit and add bookmark to test suggestions
        repository.recordVisit("https://github.com", "GitHub: Build software")
        repository.addBookmark(title = "Wikipedia - The Free Encyclopedia", url = "https://www.wikipedia.org", folder = "Favorites")

        val history = repository.history.value
        val bookmarks = repository.bookmarks.value

        // Test empty query returns pure history/bookmarks without trending suggestions
        val emptySuggestions = suggestionsEngine.getSuggestions(
            query = "",
            history = history,
            bookmarks = bookmarks
        )
        assertTrue("Query should offer history/bookmark suggestions", emptySuggestions.isNotEmpty())
        assertTrue("Should contain history suggestions", emptySuggestions.any { it.type == SuggestionType.HISTORY })
        assertFalse("Should not contain trending suggestions", emptySuggestions.any { it.type == SuggestionType.TRENDING })

        // Test partial domain typing (e.g., 'git')
        val gitSuggestions = suggestionsEngine.getSuggestions(
            query = "git",
            history = history,
            bookmarks = bookmarks
        )
        assertTrue("Should suggest github domain", gitSuggestions.any { it.query.contains("github", ignoreCase = true) })

        // Test URL resolution: direct domain vs search query
        val directUrl = suggestionsEngine.resolveUrl("kotlinlang.org", SearchEngine.GOOGLE)
        assertEquals("https://kotlinlang.org", directUrl)

        val searchUrl = suggestionsEngine.resolveUrl("jetpack compose tips", SearchEngine.DUCKDUCKGO)
        assertTrue(searchUrl.startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun testBookmarksManagement() {
        // Fresh app starts with 0 bookmarks
        assertTrue("Bookmarks start empty in fresh app", repository.bookmarks.value.isEmpty())

        val testUrl = "https://developer.mozilla.org/en-US/docs/Web"
        val testTitle = "MDN Web Docs Documentation"
        val testFolder = "Work"

        assertFalse(repository.isBookmarked(testUrl))
        repository.addBookmark(title = testTitle, url = testUrl, folder = testFolder)
        assertTrue(repository.isBookmarked(testUrl))

        // Pin and unpin bookmark
        val addedItem = repository.bookmarks.value.first { it.url == testUrl }
        assertFalse(addedItem.isPinned)
        repository.toggleBookmarkPin(addedItem.id)
        assertTrue(repository.bookmarks.value.first { it.id == addedItem.id }.isPinned)

        // Add custom bookmark group
        assertTrue(repository.addBookmarkFolder("Research"))
        assertTrue(repository.bookmarkFolders.value.contains("Research"))
        assertFalse("Duplicate group should return false", repository.addBookmarkFolder("research"))

        // Export JSON and re-import after clearing
        val exportedJson = repository.exportBookmarksJson()
        assertTrue(exportedJson.contains(testUrl))

        repository.removeBookmarkByUrl(testUrl)
        assertFalse(repository.isBookmarked(testUrl))

        val importedCount = repository.importBookmarksFromText(exportedJson)
        assertEquals(1, importedCount)
        assertTrue(repository.isBookmarked(testUrl))
        assertTrue(repository.bookmarks.value.first { it.url == testUrl }.isPinned)
    }

    @Test
    fun testHistoryManagementOperations() {
        // Fresh app starts with empty history
        assertTrue("History starts empty in fresh app", repository.history.value.isEmpty())

        repository.recordVisit("https://github.com", "GitHub")
        val initialHistory = repository.history.value
        assertTrue("History should exist after recordVisit", initialHistory.isNotEmpty())

        val testItem = initialHistory.first()
        repository.removeHistoryItem(testItem.id)
        assertFalse(repository.history.value.any { it.id == testItem.id })

        repository.clearHistory()
        assertTrue("History should be empty after clear", repository.history.value.isEmpty())
    }

    @Test
    fun testFreshAppCleanState() {
        assertEquals(1, repository.tabs.value.size)
        assertEquals("aura://home", repository.tabs.value.first().url)
        assertTrue("Bookmarks should start empty", repository.bookmarks.value.isEmpty())
        assertTrue("History should start empty", repository.history.value.isEmpty())
        assertTrue("Tab groups should start empty", repository.tabGroups.value.isEmpty())
    }

    @Test
    fun testSettingsOperations() {
        val initialSettings = repository.settings.value
        assertEquals(SearchEngine.GOOGLE, initialSettings.searchEngine)

        repository.updateSettings { it.copy(searchEngine = SearchEngine.DUCKDUCKGO, adShieldEnabled = false) }
        assertEquals(SearchEngine.DUCKDUCKGO, repository.settings.value.searchEngine)
        assertFalse(repository.settings.value.adShieldEnabled)
    }

    @Test
    fun testAutomationEngineScriptAndDataset() {
        val testScript = com.example.model.AutomationScript(
            name = "Test Form Flow",
            steps = listOf(
                com.example.model.AutomationStep(
                    actionType = com.example.model.AutomationActionType.INPUT,
                    selector = "#email",
                    value = "{{email}}"
                ),
                com.example.model.AutomationStep(
                    actionType = com.example.model.AutomationActionType.CLICK,
                    selector = "button.submit"
                )
            ),
            repeatCount = 10,
            datasetRawText = "alpha@example.com\nbeta@example.com\ngamma@example.com"
        )

        assertEquals(2, testScript.steps.size)
        assertEquals(10, testScript.repeatCount)

        val datasetLines = testScript.datasetRawText.lines().filter { it.isNotBlank() }
        assertEquals(3, datasetLines.size)
        assertEquals("alpha@example.com", datasetLines[0])
        assertEquals("beta@example.com", datasetLines[1])

        // Test DataSheet ("XYZ Data") with 4 emails used 1 by 1
        val xyzSheet = com.example.model.DataSheet(
            name = "XYZ Data",
            fields = mapOf(
                "email" to listOf("e1@xyz.com", "e2@xyz.com", "e3@xyz.com", "e4@xyz.com")
            ),
            iterationOrder = com.example.model.DataIterationOrder.TOP_TO_BOTTOM,
            exhaustionPolicy = com.example.model.DataExhaustionPolicy.STOP
        )
        assertEquals(4, xyzSheet.totalRows())
        assertEquals("e1@xyz.com", xyzSheet.getValueForField("email", 0) { "fallback" })
        assertEquals("e2@xyz.com", xyzSheet.getValueForField("email", 1) { "fallback" })
        assertEquals("e3@xyz.com", xyzSheet.getValueForField("email", 2) { "fallback" })
        assertEquals("e4@xyz.com", xyzSheet.getValueForField("email", 3) { "fallback" })
        // 5th index (index 4) should return null when policy is STOP
        assertEquals(null, xyzSheet.getValueForField("email", 4) { "fallback" })
        // When policy is CONTINUE_WITH_DEFAULT, 5th index returns fallback
        assertEquals(
            "random_default@xyz.com",
            xyzSheet.getValueForField(
                key = "email",
                iterationIndex = 4,
                policyOverride = com.example.model.DataExhaustionPolicy.CONTINUE_WITH_DEFAULT
            ) { "random_default@xyz.com" }
        )
        // Bottom to Top (Vice Versa)
        assertEquals(
            "e4@xyz.com",
            xyzSheet.getValueForField(
                key = "email",
                iterationIndex = 0,
                orderOverride = com.example.model.DataIterationOrder.BOTTOM_TO_TOP
            ) { "fallback" }
        )
    }

    @Test
    fun testAutomationEngineNoScriptGateAndGrabReadyUse() {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val engine = com.example.automation.AutomationEngine(testScope)

        // Fresh engine starts with 0 saved scripts, 0 data sheets, and 0 steps
        assertTrue(engine.savedScripts.value.isEmpty())
        assertTrue(engine.dataSheets.value.isEmpty())
        assertTrue(engine.currentScript.value.steps.isEmpty())

        // Attempt to start playback when no scripts exist -> cannot start, switches to CREATE_AUTOMATION
        engine.startPlayback()
        assertEquals(com.example.model.HudMode.CREATE_AUTOMATION, engine.executionState.value.hudMode)
        assertEquals(com.example.model.AutomationStatus.RECORDING, engine.executionState.value.status)

        // Grab elements via No-Type recording (user taps input, checkbox, and submit button without typing)
        engine.jsInterface.onUserAction("INPUT", "#user_email", "Fill Email (#user_email)", "email")
        engine.jsInterface.onUserAction("CHECKBOX", "#agree_terms", "Check Box (#agree_terms)", "")
        engine.jsInterface.onUserAction("SUBMIT", "button[type=\"submit\"]", "Submit Form", "")

        val steps = engine.currentScript.value.steps
        assertEquals(3, steps.size)
        assertEquals(com.example.model.AutomationActionType.INPUT, steps[0].actionType)
        assertEquals("{{email}}", steps[0].value)
        assertEquals("email", steps[0].variableKey)
        assertEquals(com.example.model.AutomationActionType.CHECKBOX, steps[1].actionType)
        assertEquals(com.example.model.AutomationActionType.SUBMIT, steps[2].actionType)
        assertEquals(1, engine.savedScripts.value.size)
        assertEquals(3, engine.savedScripts.value.first().steps.size)

        // Delete all steps 1 by 1 and verify it does NOT re-grab in a loop
        steps.forEach { step -> engine.removeStep(step.id) }
        assertTrue("Steps should be empty after deleting all", engine.currentScript.value.steps.isEmpty())
        assertTrue("Saved scripts should not retain deleted empty script", engine.savedScripts.value.isEmpty())

        // Simulate page load or passive scan result after deleting all -> must stay empty (no loop)
        engine.onPageLoaded("https://github.com/signup")
        val mockScanJson = """
            [{"tagName":"input","type":"email","nameAttr":"email","idAttr":"email","placeholder":"Email","text":"","selector":"#email","pageUrl":"https://github.com/signup","isInput":true,"isButton":false,"semanticType":"EMAIL","detectedOptions":[]}]
        """.trimIndent()
        engine.jsInterface.onScanResults(mockScanJson)
        assertTrue("Passive scan after deleting all steps must NOT auto-regrab steps in a loop", engine.currentScript.value.steps.isEmpty())

        // Explicit user press of "Grab Elements" (triggerScan(autoReadySteps = true)) SHOULD grab elements
        engine.triggerScan(autoReadySteps = true)
        engine.jsInterface.onScanResults(mockScanJson)
        assertEquals(1, engine.currentScript.value.steps.size)

        // Clear All / Reset Script should wipe steps and stay empty
        engine.startFreshRecordingScript("https://github.com/signup")
        assertTrue("Clear All must leave steps empty without re-grabbing", engine.currentScript.value.steps.isEmpty())
    }

    @Test
    fun testMultiPageElementGrabbingAndPage1Start() {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val engine = com.example.automation.AutomationEngine(testScope)

        val page1Url = "https://example.com/signup/step1"
        val page2Url = "https://example.com/signup/step2"
        val page3Url = "https://example.com/signup/step3"

        // 1. User opens Page 1 and presses "Grab Elements"
        engine.onPageLoaded(page1Url)
        engine.triggerScan(autoReadySteps = true)
        val page1ScanJson = """
            [
              {"tagName":"input","type":"email","nameAttr":"email","idAttr":"email","placeholder":"Email","text":"","selector":"#email","pageUrl":"https://example.com/signup/step1","isInput":true,"isButton":false,"semanticType":"EMAIL","detectedOptions":[]},
              {"tagName":"button","type":"submit","nameAttr":"","idAttr":"next1","placeholder":"","text":"Next","selector":"#next1","pageUrl":"https://example.com/signup/step1","isInput":false,"isButton":true,"semanticType":"SUBMIT_BUTTON","detectedOptions":[]}
            ]
        """.trimIndent()
        engine.jsInterface.onScanResults(page1ScanJson)

        assertEquals("Page 1 should have 2 grabbed steps", 2, engine.currentScript.value.steps.size)
        assertEquals("Starting targetUrl must be Page 1", page1Url, engine.currentScript.value.targetUrl)

        // 2. User navigates to Page 2 and presses "Grab Elements"
        engine.onPageLoaded(page2Url)
        engine.triggerScan(autoReadySteps = true)
        val page2ScanJson = """
            [
              {"tagName":"input","type":"text","nameAttr":"username","idAttr":"username","placeholder":"Username","text":"","selector":"#username","pageUrl":"https://example.com/signup/step2","isInput":true,"isButton":false,"semanticType":"USERNAME","detectedOptions":[]},
              {"tagName":"input","type":"password","nameAttr":"password","idAttr":"password","placeholder":"Password","text":"","selector":"#password","pageUrl":"https://example.com/signup/step2","isInput":true,"isButton":false,"semanticType":"PASSWORD","detectedOptions":[]},
              {"tagName":"button","type":"submit","nameAttr":"","idAttr":"next2","placeholder":"","text":"Continue","selector":"#next2","pageUrl":"https://example.com/signup/step2","isInput":false,"isButton":true,"semanticType":"SUBMIT_BUTTON","detectedOptions":[]}
            ]
        """.trimIndent()
        engine.jsInterface.onScanResults(page2ScanJson)

        // Verify Page 1 steps are STILL included in total step count (2 from Page 1 + 3 from Page 2 = 5 steps)
        val stepsAfterPage2 = engine.currentScript.value.steps
        assertEquals("Total steps must include Page 1 (2) + Page 2 (3) = 5 steps", 5, stepsAfterPage2.size)
        assertEquals("First step must still belong to Page 1", page1Url, stepsAfterPage2[0].pageUrl)
        assertEquals("Second step must still belong to Page 1", page1Url, stepsAfterPage2[1].pageUrl)
        assertEquals("Third step must belong to Page 2", page2Url, stepsAfterPage2[2].pageUrl)
        assertEquals("Starting URL must remain Page 1, NOT overwritten by Page 2", page1Url, engine.getStartingPageUrl())

        // 3. User visits Page 3 and taps an element via Tap-Grab
        engine.onPageLoaded(page3Url)
        engine.jsInterface.onPageUrlDetected(page3Url)
        engine.jsInterface.onUserAction("CHECKBOX", "#terms", "Check Box (#terms)", "")
        engine.jsInterface.onUserAction("SUBMIT", "#finish", "Finish Signup", "")

        val allSteps = engine.currentScript.value.steps
        assertEquals("Total steps must include Page 1 (2) + Page 2 (3) + Page 3 (2) = 7 steps", 7, allSteps.size)
        assertEquals("Execution state totalSteps must reflect all 7 steps across all 3 pages", 7, engine.executionState.value.totalSteps)
        assertEquals("Starting URL must still be Page 1", page1Url, engine.getStartingPageUrl())

        // 4. Verify that when automation is started while sitting on Page 3, it navigates back to Page 1 first
        var navigatedToUrl: String? = null
        engine.webViewNavigator = { url ->
            navigatedToUrl = url
            engine.onPageLoaded(url)
        }
        engine.startPlayback()
        assertEquals("When starting playback from Page 3, engine must navigate to Page 1 first", page1Url, navigatedToUrl)
        engine.stopPlayback()
    }

    @Test
    fun testBottomSheetsDropdownsDobAndCountryCodeEngine() {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val engine = com.example.automation.AutomationEngine(testScope)

        // 1. Verify DataSheet automatically derives dobDay, dobMonth, dobYear from 'dob' column
        val sheet = com.example.model.DataSheet(
            name = "Profile Data",
            fields = mapOf(
                "dob" to listOf("1998-05-14", "23/11/2001"),
                "countryCode" to listOf("+92", "+44")
            )
        )
        assertEquals(listOf("14", "23"), sheet.getFieldValues("dobDay"))
        assertEquals(listOf("05", "11"), sheet.getFieldValues("dobMonth"))
        assertEquals(listOf("1998", "2001"), sheet.getFieldValues("dobYear"))

        // 2. Verify scanning custom bottom-sheet triggers for Country Code, DOB, Split DOB, and Dropdowns
        val pageUrl = "https://example.com/register"
        engine.onPageLoaded(pageUrl)
        engine.triggerScan(autoReadySteps = true)
        val scanJson = """
            [
              {"tagName":"div","type":"","nameAttr":"","idAttr":"country_trigger","placeholder":"","text":"+1","selector":"#country_trigger","pageUrl":"https://example.com/register","isInput":true,"isButton":false,"semanticType":"COUNTRY_CODE","detectedOptions":[]},
              {"tagName":"button","type":"button","nameAttr":"","idAttr":"dob_month_sheet","placeholder":"Month","text":"Select Month","selector":"#dob_month_sheet","pageUrl":"https://example.com/register","isInput":true,"isButton":false,"semanticType":"DOB_MONTH","detectedOptions":[]},
              {"tagName":"button","type":"button","nameAttr":"","idAttr":"dob_day_sheet","placeholder":"Day","text":"Select Day","selector":"#dob_day_sheet","pageUrl":"https://example.com/register","isInput":true,"isButton":false,"semanticType":"DOB_DAY","detectedOptions":[]},
              {"tagName":"button","type":"button","nameAttr":"","idAttr":"dob_year_sheet","placeholder":"Year","text":"Select Year","selector":"#dob_year_sheet","pageUrl":"https://example.com/register","isInput":true,"isButton":false,"semanticType":"DOB_YEAR","detectedOptions":[]},
              {"tagName":"input","type":"text","nameAttr":"dob","idAttr":"dob_picker","placeholder":"MM/DD/YYYY","text":"","selector":"#dob_picker","pageUrl":"https://example.com/register","isInput":true,"isButton":false,"semanticType":"DOB","detectedOptions":[]},
              {"tagName":"div","type":"","nameAttr":"gender","idAttr":"gender_sheet","placeholder":"Gender","text":"Select Gender","selector":"#gender_sheet","pageUrl":"https://example.com/register","isInput":true,"isButton":false,"semanticType":"GENDER","detectedOptions":[]}
            ]
        """.trimIndent()
        engine.jsInterface.onScanResults(scanJson)

        val steps = engine.currentScript.value.steps
        assertEquals(6, steps.size)
        assertEquals(com.example.model.AutomationActionType.SELECT_COUNTRY_CODE, steps[0].actionType)
        assertEquals("countryCode", steps[0].variableKey)
        assertEquals(com.example.model.AutomationActionType.SELECT_DROPDOWN, steps[1].actionType)
        assertEquals("dobMonth", steps[1].variableKey)
        assertEquals(com.example.model.AutomationActionType.SELECT_DROPDOWN, steps[2].actionType)
        assertEquals("dobDay", steps[2].variableKey)
        assertEquals(com.example.model.AutomationActionType.SELECT_DROPDOWN, steps[3].actionType)
        assertEquals("dobYear", steps[3].variableKey)
        assertEquals(com.example.model.AutomationActionType.SELECT_DATE, steps[4].actionType)
        assertEquals("dob", steps[4].variableKey)
        assertEquals(com.example.model.AutomationActionType.SELECT_DROPDOWN, steps[5].actionType)
        assertEquals("gender", steps[5].variableKey)

        // 3. Verify generated JS scripts include multi-phase bottom-sheet handling, confirm clickers, and synonym tables
        val dropdownJs = com.example.automation.AutomationScripts.createSelectDropdownScript("#dob_month_sheet", "05", "dobMonth")
        assertTrue(dropdownJs.contains("PENDING:OPENED_DROPDOWN_TRIGGER"))
        assertTrue(dropdownJs.contains("clickBottomSheetConfirmIfPresent"))
        assertTrue(dropdownJs.contains("MONTH_TABLE"))

        val countryJs = com.example.automation.AutomationScripts.createSelectCountryCodeScript("#country_trigger", "+92", "countryCode")
        assertTrue(countryJs.contains("PENDING:OPENED_COUNTRY_SHEET"))
        assertTrue(countryJs.contains("pakistan"))
        assertTrue(countryJs.contains("clickBottomSheetConfirmIfPresent"))

        val dateJs = com.example.automation.AutomationScripts.createSelectDateScript("#dob_picker", "1998-05-14", "14", "05", "1998", "dob")
        assertTrue(dateJs.contains("PENDING:OPENED_DATE_PICKER"))
        assertTrue(dateJs.contains("fillSplitDobSelects"))
        assertTrue(dateJs.contains("clickBottomSheetConfirmIfPresent"))
    }

    @Test
    fun testBookmarkImportResilienceAndComplexDobPhoneAutomation() {
        // 1. Test crash-free Bookmark Import across JSON, Netscape HTML with folders, plain URLs, and malformed text
        val htmlExport = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
                <DT><H3>Dev Tools</H3>
                <DL><p>
                    <DT><A HREF="https://kotlinlang.org">Kotlin Language</A>
                    <DT><A HREF="https://developer.android.com">Android Developers</A>
                </DL><p>
            </DL><p>
        """.trimIndent()
        val importedHtmlCount = repository.importBookmarksFromText(htmlExport)
        assertEquals(2, importedHtmlCount)
        assertTrue(repository.bookmarkFolders.value.contains("Dev Tools"))
        assertEquals("Dev Tools", repository.bookmarks.value.first { it.url == "https://kotlinlang.org" }.folder)

        val malformedAndPlainInput = """
            {{{unclosed json { "url": "https://openai.com", "title": "OpenAI", "folder": "AI", "isPinned": true } }}}
            https://news.ycombinator.com
        """.trimIndent()
        val importedMixedCount = repository.importBookmarksFromText(malformedAndPlainInput)
        assertEquals(2, importedMixedCount)
        assertTrue(repository.isBookmarked("https://openai.com"))
        assertTrue(repository.isBookmarked("https://news.ycombinator.com"))

        // 2. Test DataSheet flexible DOB parsing ("May 14, 1998", "05/14/1998") and countryCode derivation from phoneNumber
        val sheet = com.example.model.DataSheet(
            name = "Complex Profiles",
            fields = mapOf(
                "dob" to listOf("May 14, 1998", "08/09/2000"),
                "phoneNumber" to listOf("+91 9876543210", "+44 7911123456")
            )
        )
        assertEquals(listOf("14", "09"), sheet.getFieldValues("dobDay"))
        assertEquals(listOf("05", "08"), sheet.getFieldValues("dobMonth"))
        assertEquals(listOf("1998", "2000"), sheet.getFieldValues("dobYear"))
        assertEquals(listOf("+91", "+44"), sheet.getFieldValues("countryCode"))

        // 3. Verify Google-style split birthday (<input type="tel" id="day">, <input type="tel" id="year">, #month) and #countryList
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val engine = com.example.automation.AutomationEngine(testScope)
        engine.startRecording()
        engine.jsInterface.onUserAction("INPUT", "#day", "Fill DOB_DAY (#day)", "")
        engine.jsInterface.onUserAction("SELECT_DROPDOWN", "#month", "Select DOB_MONTH (#month)", "")
        engine.jsInterface.onUserAction("INPUT", "#year", "Fill DOB_YEAR (#year)", "")
        engine.jsInterface.onUserAction("SELECT_COUNTRY_CODE", "#countryList", "Select Country Code (#countryList)", "")

        val recordedSteps = engine.currentScript.value.steps
        assertEquals(4, recordedSteps.size)
        assertEquals("dobDay", recordedSteps[0].variableKey)
        assertEquals("dobMonth", recordedSteps[1].variableKey)
        assertEquals("dobYear", recordedSteps[2].variableKey)
        assertEquals("countryCode", recordedSteps[3].variableKey)

        // Verify 0-indexed month protection and masked date fallback in generated JS
        val dropdownMonthJs = com.example.automation.AutomationScripts.createSelectDropdownScript("#month", "05", "dobMonth")
        assertTrue(dropdownMonthJs.contains("isZeroIndexedMonthSelect"))
        assertTrue(dropdownMonthJs.contains("resolveTargetMonthObject"))
    }

    @Test
    fun testHuaweiIdDobAndPhoneAutomation() {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val engine = com.example.automation.AutomationEngine(testScope)

        // 1. Test DOM Scanner JSON scan results from Huawei ID registration page
        val huaweiScannedJson = """
            [
                {
                    "tagName": "div",
                    "type": "",
                    "nameAttr": "",
                    "idAttr": "",
                    "placeholder": "Country/Region",
                    "text": "Algeria",
                    "selector": ".hwid-countryDropDown-root",
                    "pageUrl": "https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html",
                    "isInput": true,
                    "isButton": false,
                    "semanticType": "COUNTRY_CODE"
                },
                {
                    "tagName": "div",
                    "type": "",
                    "nameAttr": "",
                    "idAttr": "",
                    "placeholder": "Calling Code",
                    "text": "+213(Algeria)",
                    "selector": ".hwid-callingCodePhone-root > div.hwid-callingCode",
                    "pageUrl": "https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html",
                    "isInput": true,
                    "isButton": false,
                    "semanticType": "COUNTRY_CODE"
                },
                {
                    "tagName": "input",
                    "type": "text",
                    "nameAttr": "phone",
                    "idAttr": "phoneInput",
                    "placeholder": "Phone",
                    "text": "",
                    "selector": ".hwid-callingCodePhone-root input",
                    "pageUrl": "https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html",
                    "isInput": true,
                    "isButton": false,
                    "semanticType": "PHONE"
                },
                {
                    "tagName": "div",
                    "type": "",
                    "nameAttr": "",
                    "idAttr": "",
                    "placeholder": "Date of birth (Day)",
                    "text": "28",
                    "selector": ".hwid-register-inputsContainer .hwid-birth-day",
                    "pageUrl": "https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html",
                    "isInput": true,
                    "isButton": false,
                    "semanticType": "DOB_DAY"
                },
                {
                    "tagName": "div",
                    "type": "",
                    "nameAttr": "",
                    "idAttr": "",
                    "placeholder": "Date of birth (Month)",
                    "text": "10",
                    "selector": ".hwid-register-inputsContainer .hwid-birth-month",
                    "pageUrl": "https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html",
                    "isInput": true,
                    "isButton": false,
                    "semanticType": "DOB_MONTH"
                },
                {
                    "tagName": "div",
                    "type": "",
                    "nameAttr": "",
                    "idAttr": "",
                    "placeholder": "Date of birth (Year)",
                    "text": "2006",
                    "selector": ".hwid-register-inputsContainer .hwid-birth-year",
                    "pageUrl": "https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html",
                    "isInput": true,
                    "isButton": false,
                    "semanticType": "DOB_YEAR"
                }
            ]
        """.trimIndent()

        // Trigger scan and verify ready steps were built
        engine.triggerScan(autoReadySteps = true)
        engine.jsInterface.onScanResults(huaweiScannedJson)

        val scannedSteps = engine.currentScript.value.steps
        assertEquals(6, scannedSteps.size)

        // Calling code
        assertEquals(com.example.model.AutomationActionType.SELECT_COUNTRY_CODE, scannedSteps[1].actionType)
        assertEquals("countryCode", scannedSteps[1].variableKey)
        assertEquals("{{countryCode}}", scannedSteps[1].value)

        // Phone number input
        assertEquals(com.example.model.AutomationActionType.INPUT, scannedSteps[2].actionType)
        assertEquals("phoneNumber", scannedSteps[2].variableKey)
        assertEquals("{{phoneNumber}}", scannedSteps[2].value)

        // Date of birth Day
        assertEquals(com.example.model.AutomationActionType.SELECT_DROPDOWN, scannedSteps[3].actionType)
        assertEquals("dobDay", scannedSteps[3].variableKey)
        assertEquals("{{dobDay}}", scannedSteps[3].value)

        // Date of birth Month
        assertEquals(com.example.model.AutomationActionType.SELECT_DROPDOWN, scannedSteps[4].actionType)
        assertEquals("dobMonth", scannedSteps[4].variableKey)
        assertEquals("{{dobMonth}}", scannedSteps[4].value)

        // Date of birth Year
        assertEquals(com.example.model.AutomationActionType.SELECT_DROPDOWN, scannedSteps[5].actionType)
        assertEquals("dobYear", scannedSteps[5].variableKey)
        assertEquals("{{dobYear}}", scannedSteps[5].value)

        // 2. Test direct tap-to-record on Huawei ID DOB and Phone fields
        engine.startFreshRecordingScript("https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html")
        engine.jsInterface.onUserAction("INPUT", ".hwid-callingCodePhone-root input", "Fill Phone", "phoneNumber")
        engine.jsInterface.onUserAction("SELECT_DROPDOWN", ".hwid-birth-day", "Select Date of birth (Day)", "dobDay")
        engine.jsInterface.onUserAction("SELECT_DROPDOWN", ".hwid-birth-month", "Select Date of birth (Month)", "dobMonth")
        engine.jsInterface.onUserAction("SELECT_DROPDOWN", ".hwid-birth-year", "Select Date of birth (Year)", "dobYear")

        val userTappedSteps = engine.currentScript.value.steps
        assertEquals(4, userTappedSteps.size)
        assertEquals("phoneNumber", userTappedSteps[0].variableKey)
        assertEquals("dobDay", userTappedSteps[1].variableKey)
        assertEquals("dobMonth", userTappedSteps[2].variableKey)
        assertEquals("dobYear", userTappedSteps[3].variableKey)

        // 3. Verify JavaScript script generation for Phone and DOB selectors
        val phoneJs = com.example.automation.AutomationScripts.createInputScript(".hwid-callingCodePhone-root input", "5551234567", true, "phoneNumber")
        assertTrue(phoneJs.contains("inputmode"))
        assertTrue(phoneJs.contains("5551234567"))

        val countryCodeJs = com.example.automation.AutomationScripts.createSelectCountryCodeScript(".hwid-callingCodePhone-root > div.hwid-callingCode", "+213", "countryCode")
        assertTrue(countryCodeJs.contains("algeria"))
        assertTrue(countryCodeJs.contains("hwid-callingCodePhone-root"))

        val dobDayJs = com.example.automation.AutomationScripts.createSelectDropdownScript(".hwid-birth-day", "28", "dobDay")
        assertTrue(dobDayJs.contains("hwid-"))
        assertTrue(dobDayJs.contains("collectClickableOptionsFromOverlays"))
    }

    @Test
    fun testWaitForWebsiteLoadBeforePerformingFunctions() = kotlinx.coroutines.test.runTest {
        val engine = com.example.automation.AutomationEngine(this)

        // Initially not loading
        assertFalse(engine.isWebsiteCurrentlyLoading())
        assertFalse(engine.executionState.value.isWaitingForPageLoad)
        assertEquals(100, engine.executionState.value.pageLoadProgress)

        // Simulate website starting to load
        val testUrl = "https://id7.cloud.huawei.com/CAS/portal/userRegister/regbyphone.html"
        engine.onPageLoadStarted(testUrl)
        assertTrue(engine.isWebsiteCurrentlyLoading())
        assertTrue(engine.executionState.value.isWaitingForPageLoad)
        assertEquals(10, engine.executionState.value.pageLoadProgress)

        // Progress updates to 55% -> still loading
        engine.onPageLoadProgress(55)
        assertTrue(engine.isWebsiteCurrentlyLoading())
        assertEquals(55, engine.executionState.value.pageLoadProgress)

        // Schedule page load completion after 300ms while waitForWebsiteLoadAndDomReady is waiting
        var pageFinishedBeforeAction = false
        launch {
            kotlinx.coroutines.delay(300L)
            pageFinishedBeforeAction = true
            engine.onPageLoaded(testUrl)
        }

        val ready = engine.waitForWebsiteLoadAndDomReady(
            targetSelector = ".hwid-callingCodePhone-root input",
            requireInteractiveElements = true,
            reason = "Fill Phone",
            timeoutMs = 5000L
        )

        assertTrue(ready)
        assertTrue(pageFinishedBeforeAction)
        assertFalse(engine.isWebsiteCurrentlyLoading())
        assertFalse(engine.executionState.value.isWaitingForPageLoad)
        assertEquals(100, engine.executionState.value.pageLoadProgress)

        // Verify CHECK_PAGE_READY JS checks document.readyState, spinners, and target selector
        val checkJs = com.example.automation.AutomationScripts.createCheckPageReadyScript(
            targetSelector = ".hwid-callingCodePhone-root input",
            requireInteractiveElements = true
        )
        assertTrue(checkJs.contains("document.readyState"))
        assertTrue(checkJs.contains("LOADING:SPINNER_ACTIVE"))
        assertTrue(checkJs.contains("READY:TARGET_MOUNTED"))
        assertTrue(checkJs.contains("READY:COMPLETE"))
    }

    @Test
    fun testMultiStageDatePickerDobSelectionScript() {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val engine = com.example.automation.AutomationEngine(testScope)
        engine.startFreshRecordingScript("https://account.fydeos.io")

        // Verify that a single Birthday input (input[name="birthday"]) is NEVER misclassified as "dobDay" due to substring "day"
        engine.jsInterface.onUserAction("INPUT", "input[name=\"birthday\"]", "Birthday", "")
        val steps = engine.currentScript.value.steps
        assertEquals(1, steps.size)
        assertEquals("dob", steps[0].variableKey)
        assertEquals("{{dob}}", steps[0].value)

        val dobJs = com.example.automation.AutomationScripts.createSelectDateScript(
            selector = "input[name='birthday']",
            isoDate = "1980-08-19",
            day = "19",
            month = "08",
            year = "1980",
            fallbackHint = "dob"
        )
        // Verify multi-stage Decade -> Year -> Month -> Day navigation logic is present
        assertTrue(dobJs.contains("stepSingleCalendarOverlay"))
        assertTrue(dobJs.contains("STEPPED:DECADE_PREV"))
        assertTrue(dobJs.contains("STEPPED:YEAR_SELECTED"))
        assertTrue(dobJs.contains("STEPPED:MONTH_SELECTED"))
        assertTrue(dobJs.contains("PENDING:CALENDAR_OVERLAY_OPEN"))
        assertTrue(dobJs.contains("SUCCESS:DATE_PICKER_SELECTED"))
        assertTrue(dobJs.contains("var yearStr = '1980'"))
        assertTrue(dobJs.contains("var monthPad = '08'"))
        assertTrue(dobJs.contains("var dayNumStr = '19'"))
    }

    @Test
    fun testRecentsAppearOnlyWhenWebsiteVisitedMultipleTimes() {
        // Fresh repository starts with empty Recents (no pre-populated dummy shortcuts)
        assertTrue(repository.speedDials.value.isEmpty())

        // 1st visit to github.com -> recorded in history with visitCount = 1, NOT in Recents yet
        repository.updateActiveTab(title = "GitHub", url = "https://github.com")
        assertEquals(1, repository.history.value.size)
        assertEquals(1, repository.history.value.first().visitCount)
        assertTrue(repository.speedDials.value.isEmpty())

        // Page load progress/title update for the same tab URL does not increment visitCount
        repository.updateActiveTab(
            title = "GitHub · Build and ship software",
            url = "https://github.com",
            recordVisit = false
        )
        assertEquals(1, repository.history.value.first().visitCount)
        assertEquals("GitHub · Build and ship software", repository.history.value.first().title)
        assertTrue(repository.speedDials.value.isEmpty())

        // 2nd visit to github.com -> visitCount becomes 2, website now appears in Recents
        repository.updateActiveTab(title = "GitHub", url = "https://github.com")
        assertEquals(2, repository.history.value.first().visitCount)
        assertEquals(1, repository.speedDials.value.size)
        assertEquals("Github", repository.speedDials.value.first().title)
        assertEquals("https://github.com", repository.speedDials.value.first().url)

        // Clear history also clears Recents
        repository.clearHistory()
        assertTrue(repository.speedDials.value.isEmpty())
    }

    @Test
    fun testWelcomeOnboardingStateAndVarelaRoundTypography() {
        // 1. Verify fresh install starts with Welcome onboarding incomplete (hasCompletedOnboarding = false)
        assertFalse(
            "New user install should display Welcome Screen by default",
            repository.settings.value.hasCompletedOnboarding
        )

        // 2. Verify completing onboarding updates settings to true
        repository.completeOnboarding()
        assertTrue(
            "Completing Welcome Screen should mark onboarding as completed",
            repository.settings.value.hasCompletedOnboarding
        )

        // 3. Verify replaying welcome tour resets onboarding to false
        repository.resetOnboarding()
        assertFalse(
            "Resetting onboarding should allow replaying the Welcome Screen",
            repository.settings.value.hasCompletedOnboarding
        )

        // 4. Verify entire app Typography uses VarelaRoundFontFamily
        val typography = com.example.ui.theme.Typography
        val expectedFont = com.example.ui.theme.VarelaRoundFontFamily
        assertEquals(expectedFont, typography.displayLarge.fontFamily)
        assertEquals(expectedFont, typography.displayMedium.fontFamily)
        assertEquals(expectedFont, typography.displaySmall.fontFamily)
        assertEquals(expectedFont, typography.headlineLarge.fontFamily)
        assertEquals(expectedFont, typography.headlineMedium.fontFamily)
        assertEquals(expectedFont, typography.headlineSmall.fontFamily)
        assertEquals(expectedFont, typography.titleLarge.fontFamily)
        assertEquals(expectedFont, typography.titleMedium.fontFamily)
        assertEquals(expectedFont, typography.titleSmall.fontFamily)
        assertEquals(expectedFont, typography.bodyLarge.fontFamily)
        assertEquals(expectedFont, typography.bodyMedium.fontFamily)
        assertEquals(expectedFont, typography.bodySmall.fontFamily)
        assertEquals(expectedFont, typography.labelLarge.fontFamily)
        assertEquals(expectedFont, typography.labelMedium.fontFamily)
        assertEquals(expectedFont, typography.labelSmall.fontFamily)
    }

    @Test
    fun testPersonalizedContentRecommendationsAndSmartSearchService() {
        // 1. Seed user browsing activity and bookmarks
        repository.recordVisit("https://kotlinlang.org/docs", "Kotlin Coroutines Guide")
        repository.recordVisit("https://kotlinlang.org/docs", "Kotlin Coroutines Guide")
        repository.recordVisit("https://github.com/JetBrains/kotlin", "JetBrains Kotlin Repository")
        repository.addBookmark(
            title = "Android Jetpack Compose Documentation",
            url = "https://developer.android.com/jetpack/compose",
            folder = "Development"
        )
        val openTabId = repository.openNewTab(
            url = "https://news.ycombinator.com",
            title = "Hacker News - Tech & Startups"
        )

        val history = repository.history.value
        val bookmarks = repository.bookmarks.value
        val openTabs = repository.tabs.value

        // 2. Verify personalized recommendations are completely removed
        val typedRecommendations = suggestionsEngine.getPersonalizedRecommendations(
            query = "kotlin",
            history = history,
            bookmarks = bookmarks,
            tabs = openTabs,
            selectedCategory = null
        )
        assertTrue(
            "Personalized recommendations must be empty",
            typedRecommendations.isEmpty()
        )

        val kotlinSuggestions = suggestionsEngine.getSuggestions(
            query = "kotlin",
            history = history,
            bookmarks = bookmarks,
            tabs = openTabs,
            searchEngine = SearchEngine.GOOGLE
        )
        assertTrue("Typing 'kotlin' should return smart search suggestions", kotlinSuggestions.isNotEmpty())
        assertFalse(
            "Search suggestions must not include PERSONALIZED items",
            kotlinSuggestions.any { it.type == SuggestionType.PERSONALIZED }
        )
        assertTrue(
            "Search suggestions should include domain, history, or autocomplete matches for 'kotlin'",
            kotlinSuggestions.any {
                (it.type == SuggestionType.DOMAIN || it.type == SuggestionType.HISTORY || it.type == SuggestionType.SEARCH_AUTOCOMPLETE) &&
                    !it.reasonBadge.isNullOrBlank()
            }
        )

        // 3. Verify open-tab match surfaces SWITCH_TO_TAB suggestion when typing 'hacker'
        val tabSuggestions = suggestionsEngine.getSuggestions(
            query = "hacker",
            history = history,
            bookmarks = bookmarks,
            tabs = openTabs,
            searchEngine = SearchEngine.GOOGLE
        )
        val switchTabSuggestion = tabSuggestions.firstOrNull { it.type == SuggestionType.SWITCH_TO_TAB }
        assertNotNull("Typing 'hacker' should suggest switching to the open Hacker News tab", switchTabSuggestion)
        assertEquals(openTabId, switchTabSuggestion?.tabId)

        // 4. Verify live search autocomplete suggestions respect the selected search engine
        val braveSuggestions = suggestionsEngine.getSuggestions(
            query = "compose",
            history = history,
            bookmarks = bookmarks,
            tabs = openTabs,
            searchEngine = SearchEngine.BRAVE
        )
        val autocompleteItem = braveSuggestions.firstOrNull { it.type == SuggestionType.SEARCH_AUTOCOMPLETE }
        assertNotNull("Should include live search autocomplete completions", autocompleteItem)
        assertTrue(
            "Autocomplete subtitle/badge should reflect selected Brave search engine",
            autocompleteItem!!.subtitle?.contains("Brave", ignoreCase = true) == true ||
                autocompleteItem.reasonBadge?.contains("Brave", ignoreCase = true) == true
        )
    }

    @Test
    fun testCustomWebViewAndAllSearchEnginesLoadReliably() {
        // 1. Verify search query extraction works across Google, DuckDuckGo, Bing, and Brave
        assertEquals(
            "jetpack compose",
            com.example.devtools.RealHtmlPageLoader.extractSearchQueryIfSearchUrl("https://www.google.com/search?q=jetpack+compose")
        )
        assertEquals(
            "apple sign in",
            com.example.devtools.RealHtmlPageLoader.extractSearchQueryIfSearchUrl("https://duckduckgo.com/?q=apple+sign+in")
        )
        assertEquals(
            "kotlin coroutines",
            com.example.devtools.RealHtmlPageLoader.extractSearchQueryIfSearchUrl("https://www.bing.com/search?q=kotlin+coroutines")
        )
        assertEquals(
            "browser automation",
            com.example.devtools.RealHtmlPageLoader.extractSearchQueryIfSearchUrl("https://search.brave.com/search?q=browser+automation")
        )

        // 2. Verify loading search results across all search engines succeeds with interactive search box and result cards
        val engines = listOf(
            "https://www.google.com/search?q=kotlin+android",
            "https://duckduckgo.com/?q=kotlin+android",
            "https://www.bing.com/search?q=kotlin+android",
            "https://search.brave.com/search?q=kotlin+android"
        )
        engines.forEach { searchUrl ->
            val result = com.example.devtools.RealHtmlPageLoader.loadWebPage(searchUrl, isDesktopMode = false)
            assertTrue("Search URL $searchUrl must load successfully", result.isSuccess)
            val loaded = result.getOrThrow()
            assertTrue("Loaded search page should contain DOM nodes", loaded.domNodes.isNotEmpty())
            assertTrue(
                "Loaded search page should have an interactive #search-input",
                loaded.domNodes.any { it.id == "search-input" && it.tagName == "input" }
            )
            assertTrue(
                "Loaded search page should have clickable search result cards",
                loaded.domNodes.any { it.className == "search-result-card" && !it.href.isNullOrBlank() }
            )
        }

        // 3. Verify loading Search Engine Homepages (e.g. https://www.google.com, https://duckduckgo.com) renders interactive portal
        val googleHome = com.example.devtools.RealHtmlPageLoader.loadWebPage("https://www.google.com", isDesktopMode = false)
        assertTrue(googleHome.isSuccess)
        val googleNodes = googleHome.getOrThrow().domNodes
        assertTrue(googleNodes.any { it.id == "search-input" })
        assertTrue(googleNodes.any { it.className == "search-result-card" })

        // 4. Verify loading SPA / Auth websites (e.g. Apple Sign-In) synthesizes interactive form fields without failing
        val appleSignIn = com.example.devtools.RealHtmlPageLoader.loadWebPage("https://account.apple.com/sign-in", isDesktopMode = false)
        assertTrue(appleSignIn.isSuccess)
        val appleNodes = appleSignIn.getOrThrow().domNodes
        assertTrue("Apple Sign-In should include email input", appleNodes.any { it.id == "account_name_text_field" })
        assertTrue("Apple Sign-In should include birthday picker", appleNodes.any { it.tagName == "dob" })
        assertTrue("Apple Sign-In should include country code selector", appleNodes.any { it.tagName == "select" })
    }
}


