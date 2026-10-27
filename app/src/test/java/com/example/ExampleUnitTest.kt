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
        val history = repository.history.value
        val bookmarks = repository.bookmarks.value

        // Test empty query returns personalized history + trending suggestions
        val emptySuggestions = suggestionsEngine.getSuggestions(
            query = "",
            history = history,
            bookmarks = bookmarks
        )
        assertTrue("Empty query should offer personalized suggestions", emptySuggestions.isNotEmpty())
        assertTrue("Should contain personalized suggestions", emptySuggestions.any { it.type == SuggestionType.PERSONALIZED })

        // Test partial domain typing (e.g., 'git')
        val gitSuggestions = suggestionsEngine.getSuggestions(
            query = "git",
            history = history,
            bookmarks = bookmarks
        )
        assertTrue("Should suggest github domain", gitSuggestions.any { it.query.contains("github", ignoreCase = true) })

        // Test category discovery
        val techSuggestions = suggestionsEngine.getSuggestions(
            query = "",
            history = history,
            bookmarks = bookmarks,
            selectedCategory = "Technology"
        )
        assertTrue(techSuggestions.all { it.type == SuggestionType.DISCOVERY })

        // Test URL resolution: direct domain vs search query
        val directUrl = suggestionsEngine.resolveUrl("kotlinlang.org", SearchEngine.GOOGLE)
        assertEquals("https://kotlinlang.org", directUrl)

        val searchUrl = suggestionsEngine.resolveUrl("jetpack compose tips", SearchEngine.DUCKDUCKGO)
        assertTrue(searchUrl.startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun testBookmarksManagement() {
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
        val initialHistory = repository.history.value
        assertTrue("Initial history should exist", initialHistory.isNotEmpty())

        val testItem = initialHistory.first()
        repository.removeHistoryItem(testItem.id)
        assertFalse(repository.history.value.any { it.id == testItem.id })

        repository.clearHistory()
        assertTrue("History should be empty after clear", repository.history.value.isEmpty())
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
}
