package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserBottomBar(
    currentScreen: NavScreen,
    tabCount: Int,
    isCurrentPageBookmarked: Boolean,
    onNavigate: (NavScreen) -> Unit,
    onBookmarkPress: () -> Unit,
    onBookmarkHold: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 4.dp)
        ) {
            // 1. Home
            BottomNavItem(
                label = "Home",
                icon = if (currentScreen == NavScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                isSelected = currentScreen == NavScreen.HOME,
                onClick = { onNavigate(NavScreen.HOME) },
                testTag = "nav_home",
                modifier = Modifier.weight(1f)
            )

            // 2. Bookmark (Tap = Bookmark Web Page, Hold = Open Bookmarks)
            BottomNavItem(
                label = "Bookmark",
                icon = if (isCurrentPageBookmarked || currentScreen == NavScreen.BOOKMARK) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                isSelected = currentScreen == NavScreen.BOOKMARK,
                activeIconColor = if (isCurrentPageBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                onClick = onBookmarkPress,
                onLongClick = onBookmarkHold,
                testTag = "nav_bookmark",
                modifier = Modifier.weight(1f)
            )

            // 3. Search
            BottomNavItem(
                label = "Search",
                icon = if (currentScreen == NavScreen.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                isSelected = currentScreen == NavScreen.SEARCH,
                onClick = { onNavigate(NavScreen.SEARCH) },
                testTag = "nav_search",
                modifier = Modifier.weight(1f)
            )

            // 4. Tab (with badge)
            BottomNavItem(
                label = "Tabs",
                icon = if (currentScreen == NavScreen.TAB) Icons.Filled.Layers else Icons.Outlined.Layers,
                isSelected = currentScreen == NavScreen.TAB,
                badgeCount = tabCount,
                onClick = { onNavigate(NavScreen.TAB) },
                testTag = "nav_tab",
                modifier = Modifier.weight(1f)
            )

            // 5. 3-Dots Menu (More features: Bookmark, Translate, Proxy, Desktop, Settings)
            BottomNavItem(
                label = "More",
                icon = Icons.Default.MoreVert,
                isSelected = false,
                onClick = onMoreClick,
                testTag = "nav_more_menu",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
    activeIconColor: Color = MaterialTheme.colorScheme.primary,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 28.dp),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                modifier = Modifier.size(width = 44.dp, height = 28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) activeIconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (badgeCount != null && badgeCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
