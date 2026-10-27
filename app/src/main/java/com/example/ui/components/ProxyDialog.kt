package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ProxyServer(
    val name: String,
    val location: String,
    val pingMs: Int
)

@Composable
fun ProxyDialog(
    isEnabled: Boolean,
    currentNode: String,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectNode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val servers = listOf(
        ProxyServer("US Fast Gateway", "Chicago, USA", 28),
        ProxyServer("EU Privacy Guard", "Frankfurt, Germany", 45),
        ProxyServer("Asia-Pacific Edge", "Tokyo, Japan", 72),
        ProxyServer("UK High-Speed Node", "London, UK", 39),
        ProxyServer("Decentralized Onion Relay", "Distributed", 110)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = if (isEnabled) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = "Proxy",
                        tint = if (isEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Secure Web Proxy",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Main toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = if (isEnabled) "Proxy Status: Active" else "Proxy Status: Disabled",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Encrypts traffic & hides original IP",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("dialog_proxy_switch")
                    )
                }

                Text(
                    text = "Select Server Location:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                servers.forEach { server ->
                    val isSelected = server.name == currentNode
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectNode(server.name)
                            }
                            .testTag("proxy_node_${server.name.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${server.location} • ${server.pingMs}ms",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
