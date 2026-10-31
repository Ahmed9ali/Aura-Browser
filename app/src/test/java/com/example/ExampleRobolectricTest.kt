package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.automation.AutomationScripts
import com.example.model.AutomationActionType
import com.example.model.AutomationDataGenerator
import com.example.model.AutomationExecutionState
import com.example.model.AutomationScript
import com.example.model.AutomationStep
import com.example.model.BrowserTab
import com.example.model.DataSheet
import com.example.ui.screens.AutomationManagerScreen
import com.example.ui.screens.TabsScreen
import com.example.ui.theme.AuraBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura", appName)
  }

  @Test
  fun `tabsScreen renders LazyVerticalGrid with multiple tab preview cards and bottom nav controls`() {
    val manyTabs = (1..20).map { index ->
      BrowserTab(
        id = "tab_$index",
        title = "Tab Title $index",
        url = "https://example$index.com"
      )
    }

    var selectedTabId: String? = null
    var newTabRequested = false

    composeTestRule.setContent {
      AuraBrowserTheme {
        TabsScreen(
          tabs = manyTabs,
          activeTab = manyTabs.first(),
          tabGroups = emptyList(),
          showCreateGroupDialog = false,
          tabToMove = null,
          onSelectTab = { selectedTabId = it },
          onCloseTab = {},
          onOpenNewTab = { newTabRequested = true },
          onCloseAllTabs = {},
          onCloseAllInGroup = {},
          onCreateGroup = { _, _ -> },
          onDeleteGroup = {},
          onToggleGroupCollapse = {},
          onOpenCreateGroupDialog = {},
          onCloseCreateGroupDialog = {},
          onOpenMoveTabDialog = {},
          onCloseMoveTabDialog = {},
          onAssignTabToGroup = { _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("tabs_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tabs_lazy_vertical_grid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_toggle_grid_density").assertIsDisplayed()
    composeTestRule.onNodeWithTag("input_filter_tabs").assertIsDisplayed()

    // First several tab preview cards are rendered in the LazyVerticalGrid
    composeTestRule.onNodeWithTag("tab_card_tab_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_card_tab_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_card_tab_3").assertIsDisplayed()

    // Clicking a tab card selects it
    composeTestRule.onNodeWithTag("tab_card_tab_2").performClick()
    assertEquals("tab_2", selectedTabId)

    // Toggling grid density works smoothly
    composeTestRule.onNodeWithTag("btn_toggle_grid_density").performClick()
    composeTestRule.onNodeWithTag("tab_card_tab_1").assertIsDisplayed()

    // Bottom navigation bar has only Add (+) in center and 3-dots on right
    composeTestRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_new_tab").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_tabs_more_menu").assertIsDisplayed()

    composeTestRule.onNodeWithTag("btn_new_tab").performClick()
    assertEquals(true, newTabRequested)
  }

  @Test
  fun `automationScreen renders clean TopAppBar without logs or workflow tab and opens script workflow on click`() {
    val testScript = AutomationScript(
      id = "script_1",
      name = "Signup Flow",
      targetWebsite = "example.com",
      repeatCount = 2,
      steps = listOf(
        AutomationStep(
          actionType = AutomationActionType.SELECT_DATE,
          selector = "#dob",
          elementDescription = "Birthday Picker",
          variableKey = "dob"
        )
      )
    )
    var runClicked = false

    composeTestRule.setContent {
      AuraBrowserTheme {
        AutomationManagerScreen(
          currentScript = testScript,
          savedScripts = listOf(testScript),
          dataSheets = emptyList(),
          executionState = AutomationExecutionState(),
          onSelectScript = {},
          onSaveScript = {},
          onDeleteScript = {},
          onRunScript = { runClicked = true },
          onSaveDataSheet = {},
          onDeleteDataSheet = {},
          onDeleteStep = {},
          onUpdateStep = {},
          onAddStep = {},
          onClearAllSteps = {},
          onNavigateBack = {}
        )
      }
    }

    // Main Automation header shows "Automation" and Back button, with NO Run button on main screen and NO Logs tab
    composeTestRule.onNodeWithText("Automation").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_manager_back").assertIsDisplayed()
    assertTrue(composeTestRule.onAllNodesWithTag("btn_manager_run_script").fetchSemanticsNodes().isEmpty())
    assertTrue(composeTestRule.onAllNodesWithText("Logs").fetchSemanticsNodes().isEmpty())

    // Clicking the script card in Templates opens the Workflow detail view with the Run button inside
    composeTestRule.onNodeWithTag("script_card_0").assertIsDisplayed()
    composeTestRule.onNodeWithTag("script_card_0").performClick()

    composeTestRule.onNodeWithTag("workflow_detail_view").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_manager_run_script").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_inside_script_run").assertIsDisplayed()

    composeTestRule.onNodeWithTag("btn_inside_script_run").performClick()
    assertTrue(runClicked)
  }

  @Test
  fun `dataSheet parses 04 May 2001 birthday and generates calendar picker script for 2001-05-04`() {
    val sheet = DataSheet(
      id = "sheet_1",
      name = "User Sheet",
      fields = mapOf(
        "dob" to listOf("04 May 2001")
      )
    )

    assertEquals(listOf("04 May 2001"), sheet.getFieldValues("dob"))
    assertEquals(listOf("2001"), sheet.getFieldValues("dobYear"))
    assertEquals(listOf("05"), sheet.getFieldValues("dobMonth"))
    assertEquals(listOf("04"), sheet.getFieldValues("dobDay"))

    val (year, month, day) = AutomationDataGenerator.parseFlexibleDob("04 May 2001")
    assertEquals("2001", year)
    assertEquals("05", month)
    assertEquals("04", day)

    val js = AutomationScripts.createSelectDateScript("#birthday", "$year-$month-$day", day, month, year, "dob")
    assertTrue(js.contains("var yearStr = '2001';"))
    assertTrue(js.contains("var monthPad = '05';"))
    assertTrue(js.contains("var dayPad = '04';"))
    assertTrue(js.contains("ZOOM_MONTHS_TO_YEARS"))
  }

  @Test
  fun `searchScreen removes personalized search completely and displays improved search suggestions`() {
    val engine = com.example.data.PersonalizedSuggestionsEngine()
    val suggestions = engine.getSuggestions(
      query = "git",
      history = emptyList(),
      bookmarks = emptyList(),
      tabs = listOf(BrowserTab(id = "t1", title = "GitHub Issues", url = "https://github.com/issues")),
      searchEngine = com.example.model.SearchEngine.GOOGLE
    )

    var currentQuery = "git"
    composeTestRule.setContent {
      AuraBrowserTheme {
        com.example.ui.screens.SearchScreen(
          query = currentQuery,
          suggestions = suggestions,
          currentSearchEngine = com.example.model.SearchEngine.GOOGLE,
          history = emptyList(),
          onQueryChange = { currentQuery = it },
          onExecuteSearch = {},
          onSelectSuggestion = {},
          onClearQuery = { currentQuery = "" },
          onClearHistory = {}
        )
      }
    }

    // Personalized section does not exist anywhere in SearchScreen
    assertTrue(composeTestRule.onAllNodesWithTag("personalized_recommendations_section").fetchSemanticsNodes().isEmpty())
    assertTrue(composeTestRule.onAllNodesWithTag("group_personalized_container").fetchSemanticsNodes().isEmpty())

    // Improved search suggestions and quick URL tokens are displayed
    composeTestRule.onNodeWithTag("smart_autocomplete_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("group_autocomplete_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chip_token_com").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chip_token_com").performClick()
    assertEquals("git.com", currentQuery)
  }
}

