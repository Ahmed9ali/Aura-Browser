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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.History
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
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.theme.ExpressiveSheetShape
import com.example.ui.theme.ExpressiveSquircleShape

/**
 * Material 3 Expressive More Menu Sheet
 * Features 32.dp top corner radii, expressive squircle tiles, and tonal surface containers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMoreMenuSheet(
    activeTab: BrowserTab?,
    isDesktopMode: Boolean,
    isProxyEnabled: Boolean,
    proxyNode: String,
    adShieldEnabled: Boolean,
    onDismiss: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onTranslatePage: () -> Unit,
    onToggleProxy: () -> Unit,
    onOpenProxySettings: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleAdShield: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .testTag("more_menu_sheet")
        ) {
            // Header Info Bar (Current page title & domain)
            if (activeTab != null && !activeTab.url.startsWith("aura://")) {
                Surface(
                    shape = ExpressiveCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTab.title.ifBlank { "Current Page" },
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeTab.url.removePrefix("https://").removePrefix("http://"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Actions Header
            Text(
                text = "Browser Tools",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Quick Actions Row 1 (Bookmark, History, Translate, Proxy)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                // 1. Bookmark
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

                // 2. History
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

                // 3. Translate
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

                // 4. Proxy
                MenuActionItem(
                    icon = Icons.Default.VpnLock,
                    label = if (isProxyEnabled) "Proxy On" else "Proxy",
                    tint = if (isProxyEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleProxy,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_action_proxy"
                )
            }

            // Quick Actions Row 2 (Desktop, Share, Copy Link, Settings)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Desktop Site
                MenuActionItem(
                    icon = if (isDesktopMode) Icons.Default.Computer else Icons.Outlined.Computer,
                    label = "Desktop",
                    tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleDesktopMode,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_action_desktop"
                )

                // Share
                MenuActionItem(
                    icon = Icons.Default.Share,
                    label = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onDismiss()
                        activeTab?.let { tab ->
                            if (!tab.url.startsWith("aura://")) {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, tab.url)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Web Page")
                                context.startActivity(shareIntent)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "menu_action_share"
                )

                // Copy Link
                MenuActionItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy Link",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onDismiss()
                        activeTab?.let { tab ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("URL", tab.url)
                            clipboard.setPrimaryClip(clip)
                            onShowMessage("Link copied to clipboard: ${tab.url}")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "menu_action_copy"
                )

                // Settings
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

            // Web Automation Engine Card
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF06B6D4).copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.2.dp, Color(0xFF06B6D4).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(ExpressiveCardShape)
                    .clickable {
                        onDismiss()
                        onOpenAutomation()
                    }
                    .testTag("menu_card_automation")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        shape = ExpressiveSquircleShape,
                        color = Color(0xFF06B6D4).copy(alpha = 0.22f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Web Automation Engine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record actions, scan elements & auto-fill forms",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = ExpressiveChipShape,
                        color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Ad Shield Card
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = ExpressiveSquircleShape,
                        color = (if (adShieldEnabled) Color(0xFF6366F1) else MaterialTheme.colorScheme.outline).copy(alpha = 0.16f),
                        modifier = Modifier.size(36.dp)
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
                            text = if (adShieldEnabled) "Active • Blocking trackers" else "Shield is turned off",
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

            // Proxy Status Card
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isProxyEnabled) Color(0xFF10B981).copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(
                    1.dp,
                    if (isProxyEnabled) Color(0xFF10B981).copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveCardShape)
                    .clickable(onClick = onOpenProxySettings)
                    .testTag("proxy_status_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = ExpressiveSquircleShape,
                        color = (if (isProxyEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.outline).copy(alpha = 0.16f),
                        modifier = Modifier.size(36.dp)
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
                            fontWeight = FontWeight.Bold,
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

@Composable
fun MenuActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            color = tint.copy(alpha = 0.14f),
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
