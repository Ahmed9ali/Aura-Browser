package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSheetShape
import com.example.ui.theme.ExpressiveSquircleShape
import com.example.ui.theme.groupedListCardShape

/**
 * Material 3 Expressive Settings & Tools Bottom Sheet.
 * Features Material 3 Expressive Grouped List Cards, squircle quick-action tiles,
 * working system Share sheet integration, and real WebView DevTools triggers
 * (Live Source View, Interactive DOM Inspector, and Real-Time Network Log Sniffer).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMoreMenuSheet(
    activeTab: BrowserTab?,
    isDesktopMode: Boolean,
    isProxyEnabled: Boolean,
    proxyNode: String,
    adShieldEnabled: Boolean,
    networkRequestCount: Int = 0,
    defaultFallbackUrl: String = "https://www.google.com",
    onDismiss: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onTranslatePage: () -> Unit,
    onToggleProxy: () -> Unit,
    onOpenProxySettings: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleAdShield: () -> Unit,
    onOpenSourceView: () -> Unit = {},
    onOpenInspector: () -> Unit = {},
    onOpenNetworkSniffer: () -> Unit = {},
    onOpenAutomation: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val isLiveWebPage = activeTab != null &&
        activeTab.url.isNotBlank() &&
        !activeTab.url.startsWith("aura://") &&
        activeTab.url != "about:blank"

    val resolvedShareUrl = if (isLiveWebPage) {
        activeTab!!.url
    } else {
        defaultFallbackUrl
    }

    val resolvedShareTitle = if (isLiveWebPage && activeTab!!.title.isNotBlank()) {
        activeTab.title
    } else {
        "Aura Browser"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ExpressiveSheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .testTag("more_menu_sheet")
        ) {
            // 1. Expressive Active Page Status Header Card
            Surface(
                shape = ExpressiveCardShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Surface(
                        shape = ExpressiveSquircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLiveWebPage) Icons.Default.Security else Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLiveWebPage) {
                                activeTab!!.title.ifBlank { "Active Web Page" }
                            } else {
                                "Aura Browser Home"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isLiveWebPage) {
                                activeTab!!.url.removePrefix("https://").removePrefix("http://")
                            } else {
                                "Ready • ${defaultFallbackUrl.removePrefix("https://")}"
                            },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = ExpressivePillShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clip(ExpressivePillShape)
                            .clickable {
                                sharePageUrl(
                                    context = context,
                                    title = resolvedShareTitle,
                                    url = resolvedShareUrl,
                                    onShowMessage = onShowMessage
                                )
                                onDismiss()
                            }
                            .testTag("header_quick_share_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Page",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Share",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // 2. Quick Actions Section (Expressive Squircle Grid)
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
            )

            Surface(
                shape = ExpressiveCardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    // Row 1: Bookmarks, History, Translate, Desktop
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MenuActionItem(
                            icon = Icons.Default.Bookmark,
                            label = "Bookmarks",
                            tint = Color(0xFFF59E0B),
                            onClick = {
                                onDismiss()
                                onOpenBookmarks()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_bookmarks"
                        )

                        MenuActionItem(
                            icon = Icons.Default.History,
                            label = "History",
                            tint = Color(0xFF8B5CF6),
                            onClick = {
                                onDismiss()
                                onOpenHistory()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_history"
                        )

                        MenuActionItem(
                            icon = Icons.Default.GTranslate,
                            label = "Translate",
                            tint = Color(0xFF0284C7),
                            onClick = {
                                onDismiss()
                                onTranslatePage()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_translate"
                        )

                        MenuActionItem(
                            icon = if (isDesktopMode) Icons.Default.Computer else Icons.Outlined.Computer,
                            label = if (isDesktopMode) "Desktop On" else "Desktop",
                            tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            isActive = isDesktopMode,
                            onClick = onToggleDesktopMode,
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_desktop"
                        )
                    }

                    // Row 2: Share, Copy Link, Proxy, Settings
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MenuActionItem(
                            icon = Icons.Default.Share,
                            label = "Share",
                            tint = Color(0xFFEC4899),
                            onClick = {
                                sharePageUrl(
                                    context = context,
                                    title = resolvedShareTitle,
                                    url = resolvedShareUrl,
                                    onShowMessage = onShowMessage
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_share"
                        )

                        MenuActionItem(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy Link",
                            tint = Color(0xFF06B6D4),
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("URL", resolvedShareUrl)
                                clipboard.setPrimaryClip(clip)
                                onShowMessage("Link copied to clipboard: $resolvedShareUrl")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_copy"
                        )

                        MenuActionItem(
                            icon = Icons.Default.VpnLock,
                            label = if (isProxyEnabled) "Proxy On" else "Proxy",
                            tint = if (isProxyEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            isActive = isProxyEnabled,
                            onClick = onToggleProxy,
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_proxy"
                        )

                        MenuActionItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                onDismiss()
                                onOpenSettings()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "menu_action_settings"
                        )
                    }
                }
            }

            // 3. Developer & Web Inspection Tools (Material 3 Expressive Grouped List Cards)
            Text(
                text = "Developer & Inspection Tools",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Tool 1: Real Source View
                ExpressiveGroupedSheetCard(
                    shape = groupedListCardShape(index = 0, count = 3),
                    icon = Icons.Default.Code,
                    badgeColor = Color(0xFF0EA5E9),
                    title = "Source View",
                    subtitle = "Live rendered DOM outerHTML & raw HTTP source viewer",
                    badgeText = "HTML",
                    onClick = {
                        onDismiss()
                        onOpenSourceView()
                    },
                    testTag = "menu_devtools_source_view"
                )

                // Tool 2: Real DOM Inspector & Console
                ExpressiveGroupedSheetCard(
                    shape = groupedListCardShape(index = 1, count = 3),
                    icon = Icons.Default.AdsClick,
                    badgeColor = Color(0xFF8B5CF6),
                    title = "Inspect Element & Console",
                    subtitle = "Tap page elements, inspect computed CSS, mutate DOM & run JS",
                    badgeText = "DOM",
                    onClick = {
                        onDismiss()
                        onOpenInspector()
                    },
                    testTag = "menu_devtools_inspect"
                )

                // Tool 3: Real Network Log Sniffer
                ExpressiveGroupedSheetCard(
                    shape = groupedListCardShape(index = 2, count = 3),
                    icon = Icons.Default.NetworkCheck,
                    badgeColor = Color(0xFF10B981),
                    title = "Network Log Sniffer",
                    subtitle = "Real-time HTTP/XHR request interceptor, headers, cURL & payloads",
                    badgeText = if (networkRequestCount > 0) "$networkRequestCount reqs" else "LIVE",
                    onClick = {
                        onDismiss()
                        onOpenNetworkSniffer()
                    },
                    testTag = "menu_devtools_network_sniffer"
                )
            }

            // 4. Automation & Privacy Suite (Material 3 Expressive Grouped List Cards)
            Text(
                text = "Automation & Privacy Shield",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Item 1: Web Automation Engine
                ExpressiveGroupedSheetCard(
                    shape = groupedListCardShape(index = 0, count = 3),
                    icon = Icons.Default.SmartToy,
                    badgeColor = Color(0xFF06B6D4),
                    title = "Web Automation Engine",
                    subtitle = "Record actions, scan page elements & auto-fill forms",
                    badgeText = "Open",
                    onClick = {
                        onDismiss()
                        onOpenAutomation()
                    },
                    testTag = "menu_card_automation"
                )

                // Item 2: Ad & Tracker Shield
                Card(
                    shape = groupedListCardShape(index = 1, count = 3),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(groupedListCardShape(index = 1, count = 3))
                        .clickable { onToggleAdShield() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Surface(
                            shape = ExpressiveSquircleShape,
                            color = (if (adShieldEnabled) Color(0xFF6366F1) else MaterialTheme.colorScheme.outline).copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (adShieldEnabled) Icons.Default.Shield else Icons.Outlined.Shield,
                                    contentDescription = "Ad Shield",
                                    tint = if (adShieldEnabled) Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ad & Tracker Shield",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (adShieldEnabled) "Active • Blocking trackers in WebView" else "Shield is turned off",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = adShieldEnabled,
                            onCheckedChange = { onToggleAdShield() }
                        )
                    }
                }

                // Item 3: Secure Proxy Status Card
                Card(
                    shape = groupedListCardShape(index = 2, count = 3),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProxyEnabled) {
                            Color(0xFF10B981).copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isProxyEnabled) Color(0xFF10B981).copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(groupedListCardShape(index = 2, count = 3))
                        .clickable(onClick = onOpenProxySettings)
                        .testTag("proxy_status_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Surface(
                            shape = ExpressiveSquircleShape,
                            color = (if (isProxyEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.outline).copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VpnLock,
                                    contentDescription = "Proxy",
                                    tint = if (isProxyEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isProxyEnabled) "Secure Proxy: Connected" else "Secure Proxy: Disconnected",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isProxyEnabled) "Routing traffic via $proxyNode" else "Tap to configure proxy server node",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isProxyEnabled,
                            onCheckedChange = { onToggleProxy() }
                        )
                    }
                }
            }
        }
    }
}

private fun sharePageUrl(
    context: Context,
    title: String,
    url: String,
    onShowMessage: (String) -> Unit
) {
    val cleanUrl = url.trim().ifBlank { "https://www.google.com" }
    val cleanTitle = title.trim().ifBlank { "Shared Web Page" }
    val shareBody = "$cleanTitle\n$cleanUrl"

    // Always copy to clipboard first so user has immediate access even if chooser is dismissed
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(cleanTitle, cleanUrl)
        clipboard.setPrimaryClip(clip)
    }

    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, cleanTitle)
            putExtra(Intent.EXTRA_SUBJECT, cleanTitle)
            putExtra(Intent.EXTRA_TEXT, shareBody)
            clipData = ClipData.newPlainText(cleanTitle, cleanUrl)
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share $cleanTitle").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }.onFailure {
        onShowMessage("Link copied for sharing: $cleanUrl")
    }
}

@Composable
private fun ExpressiveGroupedSheetCard(
    shape: Shape,
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = ExpressiveSquircleShape,
                color = badgeColor.copy(alpha = 0.16f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = ExpressiveChipShape,
                color = badgeColor.copy(alpha = 0.16f)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun MenuActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        Surface(
            shape = ExpressiveSquircleShape,
            color = if (isActive) tint.copy(alpha = 0.24f) else tint.copy(alpha = 0.13f),
            border = if (isActive) BorderStroke(1.2.dp, tint.copy(alpha = 0.6f)) else null,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) tint else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
