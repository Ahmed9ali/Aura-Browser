package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavScreen

/**
 * Refined Material 3 Browser Bottom Navigation Bar.
 * Features a sleek edge-to-edge surface container, spring-animated active pill indicators,
 * an authentic browser Tab Counter Box, and smart bookmark status indication.
 */
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
    modifier: Modifier = Modifier,
    isMoreActive: Boolean = false
) {
    val isHomeSelected = currentScreen == NavScreen.HOME || currentScreen == NavScreen.WEB_PAGE
    val isBookmarkSelected = currentScreen == NavScreen.BOOKMARK
    val isSearchSelected = currentScreen == NavScreen.SEARCH
    val isTabSelected = currentScreen == NavScreen.TAB
    val isMoreSelected = isMoreActive ||
        currentScreen == NavScreen.HISTORY ||
        currentScreen == NavScreen.AUTOMATION ||
        currentScreen == NavScreen.SETTINGS

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                thickness = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp)
            ) {
                // 1. Home
                BrowserNavItem(
                    label = "Home",
                    icon = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                    isSelected = isHomeSelected,
                    onClick = { onNavigate(NavScreen.HOME) },
                    testTag = "nav_home",
                    modifier = Modifier.weight(1f)
                )

                // 2. Bookmarks
                BrowserNavItem(
                    label = "Bookmarks",
                    icon = if (isCurrentPageBookmarked || isBookmarkSelected) {
                        Icons.Filled.Bookmark
                    } else {
                        Icons.Outlined.BookmarkBorder
                    },
                    isSelected = isBookmarkSelected,
                    isHighlightedAccent = isCurrentPageBookmarked && !isBookmarkSelected,
                    accentColor = Color(0xFFF59E0B),
                    onClick = onBookmarkPress,
                    onLongClick = onBookmarkHold,
                    testTag = "nav_bookmark",
                    modifier = Modifier.weight(1f)
                )

                // 3. Search
                BrowserNavItem(
                    label = "Search",
                    icon = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
                    isSelected = isSearchSelected,
                    onClick = { onNavigate(NavScreen.SEARCH) },
                    testTag = "nav_search",
                    modifier = Modifier.weight(1f)
                )

                // 4. Tabs (Authentic Browser Tab Counter Box)
                BrowserNavItem(
                    label = "Tabs",
                    icon = null,
                    isSelected = isTabSelected,
                    onClick = { onNavigate(NavScreen.TAB) },
                    testTag = "nav_tab",
                    modifier = Modifier.weight(1f),
                    customIconContent = { tint ->
                        BrowserTabCounterIcon(
                            count = tabCount,
                            isSelected = isTabSelected,
                            tint = tint
                        )
                    }
                )

                // 5. More Menu
                BrowserNavItem(
                    label = "More",
                    icon = if (isMoreSelected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                    isSelected = isMoreSelected,
                    onClick = onMoreClick,
                    testTag = "nav_more_menu",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserNavItem(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isHighlightedAccent: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    testTag: String = "",
    customIconContent: (@Composable (Color) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    val pillScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_pill_scale"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 54.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_pill_width"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isHighlightedAccent -> accentColor.copy(alpha = 0.14f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_container_color"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            isHighlightedAccent -> accentColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_content_color"
    )

    val labelColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isHighlightedAccent -> accentColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_label_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 32.dp),
                role = Role.Tab,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .semantics { contentDescription = label }
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(pillWidth)
                .height(30.dp)
                .scale(pillScale)
                .clip(RoundedCornerShape(15.dp))
                .background(containerColor)
        ) {
            if (customIconContent != null) {
                customIconContent(contentColor)
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isHighlightedAccent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 10.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (isSelected || isHighlightedAccent) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Authentic Browser Tab Counter Box Icon (like Chrome/Brave/Edge mobile tab switcher).
 * Displays the live open tab count inside a crisp rounded square frame.
 */
@Composable
private fun BrowserTabCounterIcon(
    count: Int,
    isSelected: Boolean,
    tint: Color
) {
    val displayCount = when {
        count <= 0 -> "1"
        count > 99 -> ":D"
        else -> count.toString()
    }

    val shape = RoundedCornerShape(6.dp)
    val backgroundColor = if (isSelected) tint else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else tint

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(21.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(
                    width = 1.8.dp,
                    color = tint
                ),
                shape = shape
            )
    ) {
        Text(
            text = displayCount,
            color = textColor,
            fontSize = if (displayCount.length >= 2) 9.5.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
