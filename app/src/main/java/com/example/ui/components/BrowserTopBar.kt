package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import androidx.compose.material.icons.filled.SmartToy

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
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Interactive Address Bar Pill (Full Width) with high contrast in both themes
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onSearchClick)
                        .testTag("address_bar_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    ) {
                        // SSL Lock or Home Shield Icon Badge
                        val isHome = activeTab?.url?.startsWith("aura://") == true || activeTab?.url == "about:blank"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isHome) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color(0xFF10B981).copy(alpha = 0.14f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isHome) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = if (isHome) "Aura Home" else "Secure Connection",
                                tint = if (isHome) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // If tab is in a group, show colored group tag pill
                        if (activeGroup != null) {
                            val groupColor = try {
                                Color(android.graphics.Color.parseColor(activeGroup.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = groupColor.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(groupColor)
                                    )
                                    Text(
                                        text = activeGroup.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
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
                            fontSize = 14.sp,
                            color = if (isHome) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isHome) FontWeight.Normal else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Automation Engine HUD trigger button
                        IconButton(
                            onClick = onToggleAutomation,
                            modifier = Modifier.size(32.dp).testTag("btn_topbar_automation")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Automation Engine",
                                tint = if (isAutomationActive) Color(0xFF06B6D4) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reload or Stop Loading
                        if (isLoading) {
                            IconButton(
                                onClick = onStopLoading,
                                modifier = Modifier.size(32.dp).testTag("btn_stop_load")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop loading",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (!isHome) {
                            IconButton(
                                onClick = onReload,
                                modifier = Modifier.size(32.dp).testTag("btn_reload")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload page",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Web Loading Linear Progress Bar
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
