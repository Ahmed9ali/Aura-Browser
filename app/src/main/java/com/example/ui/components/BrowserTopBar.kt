package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutomationStatus
import com.example.model.BrowserTab
import com.example.model.TabGroup
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape

/**
 * Professional Material 3 Browser Top App Bar & Omnibox
 * Features an architectural glassmorphic surface, refined squircle SSL/search badge,
 * two-tier domain/title hierarchy, and a dedicated AI Automation Engine capsule button
 * with live status and step counter badges.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserTopBar(
    activeTab: BrowserTab?,
    activeGroup: TabGroup?,
    isLoading: Boolean,
    loadProgress: Int,
    isAutomationActive: Boolean = false,
    automationStatus: AutomationStatus = AutomationStatus.IDLE,
    automationStepCount: Int = 0,
    onSearchClick: () -> Unit,
    onReload: () -> Unit,
    onStopLoading: () -> Unit,
    onToggleAutomation: () -> Unit,
    onCopyUrl: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isHome = activeTab == null ||
            activeTab.url.startsWith("aura://") ||
            activeTab.url == "about:blank" ||
            activeTab.url.isBlank()
    val isSecureHttps = !isHome && activeTab?.url?.startsWith("https://", ignoreCase = true) == true

    val cleanDomain = if (isHome) {
        ""
    } else {
        activeTab?.url.orEmpty()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore("?")
    }

    val cleanSubtitle = if (isHome) {
        ""
    } else {
        val rawTitle = activeTab?.title.orEmpty().trim()
        if (rawTitle.isNotBlank() && !rawTitle.equals(cleanDomain, ignoreCase = true) && rawTitle != "New Tab") {
            rawTitle
        } else {
            activeTab?.url.orEmpty()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
        }
    }

    val automationAccent = when (automationStatus) {
        AutomationStatus.RECORDING -> Color(0xFFEF4444)
        AutomationStatus.PLAYING -> Color(0xFF10B981)
        AutomationStatus.SCANNING -> Color(0xFF06B6D4)
        AutomationStatus.PAUSED -> Color(0xFFF59E0B)
        AutomationStatus.IDLE -> if (isAutomationActive) Color(0xFF6366F1) else MaterialTheme.colorScheme.primary
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Standard Chrome-Style Pill Omnibox Search / Address Bar
                Surface(
                    shape = ExpressivePillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
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
                            .padding(start = 16.dp, end = 8.dp)
                    ) {
                        val leadingIcon = when {
                            isHome -> Icons.Default.Search
                            isSecureHttps -> Icons.Default.Lock
                            else -> Icons.Default.Public
                        }
                        val leadingTint = when {
                            isHome -> MaterialTheme.colorScheme.onSurfaceVariant
                            isSecureHttps -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = if (isHome) "Search" else "Connection Security",
                            tint = leadingTint,
                            modifier = Modifier.size(18.dp)
                        )

                        // Single-line Chrome-style URL / Search prompt
                        Text(
                            text = if (isHome) {
                                "Search or type URL"
                            } else {
                                cleanDomain.ifBlank { activeTab?.url.orEmpty() }
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isHome) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Reload or Stop Loading Button
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
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
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Dedicated AI Automation Engine Action Button
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = if (isAutomationActive) {
                        automationAccent.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        width = if (isAutomationActive) 1.5.dp else 1.dp,
                        color = if (isAutomationActive) {
                            automationAccent.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                        }
                    ),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .clickable { onToggleAutomation() }
                        .testTag("btn_topbar_automation")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Automation Engine",
                                tint = if (isAutomationActive) automationAccent else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isAutomationActive || automationStatus != AutomationStatus.IDLE) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(automationAccent)
                                )
                            }
                        }

                        if (automationStepCount > 0) {
                            Surface(
                                shape = ExpressivePillShape,
                                color = automationAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$automationStepCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = automationAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Web Loading Progress Bar
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
                        .height(2.5.dp)
                )
            }

            HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        }
    }
}

/**
 * Unified, Professional Top App Bar for all Aura Sub-Screens
 * (Tabs, Bookmarks, History, Automation Studio, Settings, Search).
 */
@Composable
fun AuraSubScreenTopBar(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onNavigateBack: (() -> Unit)? = null,
    backButtonTestTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    actionSpacing: Dp = 4.dp,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (onNavigateBack != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .let { mod ->
                                        if (!backButtonTestTag.isNullOrBlank()) mod.testTag(backButtonTestTag) else mod
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (icon != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                    content = actions
                )
            }

            if (bottomContent != null) {
                bottomContent()
            }

            HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        }
    }
}
