package com.example

import com.example.data.BrowserRepository
import com.example.data.PersonalizedSuggestionsEngine
import com.example.model.SearchEngine
import com.example.model.SuggestionType
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

        repository.removeBookmarkByUrl(testUrl)
        assertFalse(repository.isBookmarked(testUrl))
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
}


