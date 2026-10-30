package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.model.BrowserTab
import com.example.ui.screens.TabsScreen
import com.example.ui.theme.AuraBrowserTheme
import org.junit.Assert.assertEquals
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
}

