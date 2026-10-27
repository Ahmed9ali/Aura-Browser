package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedDial

fun getSpeedDialIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "search" -> Icons.Default.Search
        "code" -> Icons.Default.Code
        "news" -> Icons.Default.Newspaper
        "video" -> Icons.Default.VideoLibrary
        "privacy" -> Icons.Default.Shield
        "social" -> Icons.Default.Language
        "tech" -> Icons.Default.Bookmark
        else -> Icons.Default.Public
    }
}

val SPEED_DIAL_CATEGORIES = listOf(
    "web" to "General Web",
    "search" to "Search",
    "social" to "Social",
    "code" to "Developer",
    "news" to "News",
    "video" to "Streaming",
    "tech" to "Technology",
    "privacy" to "Privacy"
)

val SPEED_DIAL_COLORS = listOf(
    "#4285F4", // Google Blue
    "#FF4500", // Reddit Orange
    "#24292E", // Dark Slate
    "#3DDC84", // Android Green
    "#FF0000", // YouTube Red
    "#DE5833", // DuckDuckGo Rust
    "#6366F1", // Indigo
    "#9C27B0", // Purple
    "#0284C7"  // Sky Blue
)

@Composable
fun SpeedDialEditDialog(
    initialDial: SpeedDial? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String, category: String, colorHex: String) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialDial?.title ?: "") }
    var url by remember { mutableStateOf(initialDial?.url ?: "") }
    var selectedCategory by remember { mutableStateOf(initialDial?.iconCategory ?: "web") }
    var selectedColor by remember { mutableStateOf(initialDial?.colorHex ?: "#4285F4") }
    var urlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialDial == null) "Add Speed Dial Shortcut" else "Edit Speed Dial Shortcut",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Shortcut Name") },
                    placeholder = { Text("e.g. Google, GitHub, Portal") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_speed_dial_title")
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (urlError && it.isNotBlank()) urlError = false
                    },
                    label = { Text("Website Address (URL)") },
                    placeholder = { Text("e.g. https://example.com") },
                    isError = urlError,
                    supportingText = if (urlError) {
                        { Text("Please enter a valid website address") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_speed_dial_url")
                )

                Text(
                    text = "Icon Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(SPEED_DIAL_CATEGORIES) { (catKey, catLabel) ->
                        val isSelected = selectedCategory == catKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = catKey }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = getSpeedDialIcon(catKey),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = catLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Shortcut Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(SPEED_DIAL_COLORS) { colorHex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isBlank()) {
                        urlError = true
                        return@Button
                    }
                    val finalTitle = title.ifBlank {
                        url.removePrefix("https://").removePrefix("http://").substringBefore("/")
                    }
                    onSave(finalTitle, url, selectedCategory, selectedColor)
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_save_speed_dial")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialDial != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(initialDial.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("btn_delete_speed_dial")
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_cancel_speed_dial")
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun ManageSpeedDialsDialog(
    speedDials: List<SpeedDial>,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (SpeedDial) -> Unit,
    onDelete: (String) -> Unit,
    onReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Manage Speed Dials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${speedDials.size} shortcuts configured",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onAddNew,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_dialog_add_speed_dial")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new shortcut",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (speedDials.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No speed dials configured.\nTap + Add Shortcut to add one.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        items(speedDials, key = { it.id }) { dial ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(dial.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = color.copy(alpha = 0.15f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = getSpeedDialIcon(dial.iconCategory),
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dial.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dial.url,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onEdit(dial) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit shortcut ${dial.title}",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDelete(dial.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete shortcut ${dial.title}",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("btn_reset_speed_dials")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Defaults", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onAddNew,
                        modifier = Modifier.testTag("btn_add_speed_dial_footer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Shortcut", fontSize = 11.sp)
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
