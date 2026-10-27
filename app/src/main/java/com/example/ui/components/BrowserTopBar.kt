package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.model.TabGroup
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape

/**
 * Material 3 Expressive Browser Top Bar & Omnibox
 * Features pill-shaped address container, expressive security badges,
 * and high-contrast tonal container styling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserTopBar(
    activeTab: BrowserTab?,
    activeGroup: TabGroup?,
    isLoading: Boolean,
    loadProgress: Int,
    isAutomationActive: Boolean = false,
    onSearchClick: () -> Unit,
    onReload: () -> Unit,
    onStopLoading: () -> Unit,
    onToggleAutomation: () -> Unit,
    onCopyUrl: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Interactive Address Bar Pill (M3 Expressive Pill shape)
                Surface(
                    shape = ExpressivePillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(ExpressivePillShape)
                        .combinedClickable(
                            onClick = onSearchClick,
                            onLongClick = onCopyUrl
                        )
                        .testTag("address_bar_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        // SSL Lock or Home Shield Icon Badge
                        val isHome = activeTab?.url?.startsWith("aura://") == true || activeTab?.url == "about:blank"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isHome) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    else Color(0xFF10B981).copy(alpha = 0.16f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isHome) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = if (isHome) "Aura Home" else "Secure Connection",
                                tint = if (isHome) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // If tab is in a group, show expressive colored group tag pill
                        if (activeGroup != null) {
                            val groupColor = try {
                                Color(android.graphics.Color.parseColor(activeGroup.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                shape = ExpressiveChipShape,
                                color = groupColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, groupColor.copy(alpha = 0.35f)),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(groupColor)
                                    )
                                    Text(
                                        text = activeGroup.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = groupColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Display Title / Domain
                        val displayUrl = when {
                            activeTab == null || isHome -> "Search or enter web address"
                            else -> {
                                activeTab.url
                                    .removePrefix("https://")
                                    .removePrefix("http://")
                                    .removePrefix("www.")
                            }
                        }

                        Text(
                            text = displayUrl,
                            fontSize = 14.5.sp,
                            color = if (isHome) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isHome) FontWeight.Normal else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Automation Engine HUD trigger button
                        IconButton(
                            onClick = onToggleAutomation,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_topbar_automation")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Automation Engine",
                                tint = if (isAutomationActive) Color(0xFF06B6D4) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Reload or Stop Loading
                        if (isLoading) {
                            IconButton(
                                onClick = onStopLoading,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("btn_stop_load")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop loading",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        } else if (!isHome) {
                            IconButton(
                                onClick = onReload,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("btn_reload")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload page",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Web Loading Linear Progress Bar with expressive height
            AnimatedVisibility(
                visible = isLoading && loadProgress in 1..99,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    }
}
