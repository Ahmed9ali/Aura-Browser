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
    }
}
