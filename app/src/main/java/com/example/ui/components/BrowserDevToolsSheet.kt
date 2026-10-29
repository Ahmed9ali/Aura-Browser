package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.devtools.ConsoleLogEntry
import com.example.devtools.DevToolTab
import com.example.devtools.DevToolsEngine
import com.example.devtools.DomTreeNodeSummary
import com.example.devtools.InspectedDomElement
import com.example.devtools.NetworkLogEntry
import com.example.devtools.NetworkResourceType
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSheetShape
import com.example.ui.theme.ExpressiveSquircleShape
import com.example.ui.theme.groupedListCardShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BrowserDevToolsOverlay(
    devToolsEngine: DevToolsEngine,
    currentUrl: String,
    onCopyText: (String, String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTool by devToolsEngine.activeTool.collectAsStateWithLifecycle()
    val isMinimized by devToolsEngine.isSheetMinimized.collectAsStateWithLifecycle()
    val isInspectTouchMode by devToolsEngine.isInspectTouchMode.collectAsStateWithLifecycle()
    val networkLogs by devToolsEngine.networkLogs.collectAsStateWithLifecycle()

    val currentTool = activeTool ?: return

    Box(modifier = modifier.fillMaxSize()) {
        // Floating Compact Pill when Touch-Inspect Mode or Minimized is active
        if (isMinimized || isInspectTouchMode) {
            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(
                    1.5.dp,
                    if (isInspectTouchMode) Color(0xFF06B6D4) else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("devtools_floating_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isInspectTouchMode) Icons.Default.TouchApp else Icons.Default.Code,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (isInspectTouchMode) {
                                "Tap any element on the page to inspect"
                            } else {
                                "${currentTool.title} Active (${networkLogs.size} reqs)"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isInspectTouchMode) {
                                "Live DOM bounding box & computed CSS picker"
                            } else {
                                "Tap Expand to open DevTools panel"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = ExpressivePillShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(ExpressivePillShape)
                            .clickable {
                                devToolsEngine.setInspectTouchMode(false)
                                devToolsEngine.setSheetMinimized(false)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandLess,
                                contentDescription = "Expand DevTools",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Expand",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { devToolsEngine.closeTool() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close DevTools",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Full Material 3 Expressive Docked DevTools Sheet
            Surface(
                shape = ExpressiveSheetShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.76f)
                    .testTag("devtools_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    // Top Drag Handle + Header Bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Segmented Expressive Tool Switcher
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                DevToolTab.values().forEach { tab ->
                                    val selected = currentTool == tab
                                    val tabIcon = when (tab) {
                                        DevToolTab.SOURCE_VIEW -> Icons.Default.Code
                                        DevToolTab.INSPECT_DOM -> Icons.Default.AdsClick
                                        DevToolTab.NETWORK_SNIFFER -> Icons.Default.NetworkCheck
                                    }
                                    Surface(
                                        shape = ExpressivePillShape,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .clip(ExpressivePillShape)
                                            .clickable {
                                                devToolsEngine.openTool(tab, currentUrl)
                                            }
                                            .testTag("devtools_tab_${tab.name.lowercase()}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = tabIcon,
                                                contentDescription = tab.title,
                                                tint = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = when (tab) {
                                                    DevToolTab.SOURCE_VIEW -> "Source"
                                                    DevToolTab.INSPECT_DOM -> "Inspect"
                                                    DevToolTab.NETWORK_SNIFFER -> "Network (${networkLogs.size})"
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Minimize button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { devToolsEngine.setSheetMinimized(true) }
                                    .testTag("devtools_minimize_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Minimize DevTools",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Close button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { devToolsEngine.closeTool() }
                                    .testTag("devtools_close_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close DevTools",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.75.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )

                    // Tool Content Area
                    when (currentTool) {
                        DevToolTab.SOURCE_VIEW -> {
                            RealSourceViewPanel(
                                devToolsEngine = devToolsEngine,
                                currentUrl = currentUrl,
                                onCopyText = onCopyText,
                                onShowMessage = onShowMessage
                            )
                        }
                        DevToolTab.INSPECT_DOM -> {
                            RealDomInspectorPanel(
                                devToolsEngine = devToolsEngine,
                                onCopyText = onCopyText,
                                onShowMessage = onShowMessage
                            )
                        }
                        DevToolTab.NETWORK_SNIFFER -> {
                            RealNetworkSnifferPanel(
                                devToolsEngine = devToolsEngine,
                                onCopyText = onCopyText,
                                onShowMessage = onShowMessage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealSourceViewPanel(
    devToolsEngine: DevToolsEngine,
    currentUrl: String,
    onCopyText: (String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val htmlSource by devToolsEngine.pageSourceHtml.collectAsStateWithLifecycle()
    val sourceUrl by devToolsEngine.sourceUrl.collectAsStateWithLifecycle()
    val isLoading by devToolsEngine.isSourceLoading.collectAsStateWithLifecycle()
    val sourceMode by devToolsEngine.sourceMode.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var wordWrap by remember { mutableStateOf(true) }

    val allLines = remember(htmlSource) {
        if (htmlSource.isBlank()) emptyList() else htmlSource.lines()
    }

    val filteredIndexedLines = remember(allLines, searchQuery) {
        if (searchQuery.isBlank()) {
            allLines.mapIndexed { idx, line -> (idx + 1) to line }
        } else {
            allLines.mapIndexedNotNull { idx, line ->
                if (line.contains(searchQuery, ignoreCase = true)) (idx + 1) to line else null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Action & Mode Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Mode Switch: Live DOM vs Raw HTTP
            FilterChip(
                selected = sourceMode == "LIVE_DOM",
                onClick = { devToolsEngine.fetchLivePageSource(currentUrl) },
                label = { Text("Live DOM HTML", fontSize = 11.5.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )

            FilterChip(
                selected = sourceMode == "RAW_HTTP",
                onClick = { devToolsEngine.fetchRawHttpSource(currentUrl) },
                label = { Text("Raw HTTP Source", fontSize = 11.5.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Http,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Word Wrap Toggle
            Surface(
                shape = CircleShape,
                color = if (wordWrap) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { wordWrap = !wordWrap }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WrapText,
                        contentDescription = "Toggle Word Wrap",
                        tint = if (wordWrap) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Refresh Source
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (sourceMode == "RAW_HTTP") {
                            devToolsEngine.fetchRawHttpSource(currentUrl)
                        } else {
                            devToolsEngine.fetchLivePageSource(currentUrl)
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Source",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Copy Full Source
            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clip(ExpressivePillShape)
                    .clickable {
                        if (htmlSource.isNotBlank()) {
                            onCopyText(htmlSource, "Page HTML Source")
                            onShowMessage("Copied ${htmlSource.length} chars of HTML source")
                        }
                    }
                    .testTag("source_copy_all_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy HTML Source",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Search in Source + Stats Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search tags, classes, scripts, or text...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "${filteredIndexedLines.size} lines",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "${allLines.size} lines • ${(htmlSource.length / 1024f).let { String.format(Locale.US, "%.1f KB", it) }}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sourceUrl.isNotBlank()) {
            Text(
                text = "Source: $sourceUrl",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // Code Viewer
        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Extracting real HTML source from page...",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val horizontalScrollState = rememberScrollState()
            Surface(
                color = Color(0xFF0B1120),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                    modifier = if (wordWrap) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                    }
                ) {
                    items(
                        items = filteredIndexedLines,
                        key = { it.first }
                    ) { (lineNum, lineContent) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp)
                        ) {
                            Text(
                                text = lineNum.toString().padStart(4, ' '),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                modifier = Modifier
                                    .width(42.dp)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = highlightHtmlLine(lineContent, searchQuery),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                softWrap = wordWrap
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun highlightHtmlLine(line: String, searchQuery: String): AnnotatedString {
    val trimmed = line.take(1500)
    return buildAnnotatedString {
        append(trimmed)
        addStyle(SpanStyle(color = Color(0xFFE2E8F0)), 0, trimmed.length)

        // Highlight HTML comments
        val commentIdx = trimmed.indexOf("<!--")
        if (commentIdx >= 0) {
            addStyle(SpanStyle(color = Color(0xFF64748B)), commentIdx, trimmed.length)
            return@buildAnnotatedString
        }

        // Highlight tags <...>
        var i = 0
        while (i < trimmed.length) {
            val open = trimmed.indexOf('<', i)
            if (open < 0) break
            val close = trimmed.indexOf('>', open)
            val end = if (close >= 0) close + 1 else trimmed.length
            addStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold), open, end)

            // Highlight quoted attribute values inside the tag
            var qStart = trimmed.indexOf('"', open)
            while (qStart in (open + 1) until end) {
                val qEnd = trimmed.indexOf('"', qStart + 1)
                if (qEnd in (qStart + 1) until end) {
                    addStyle(SpanStyle(color = Color(0xFFA7F3D0)), qStart, qEnd + 1)
                    qStart = trimmed.indexOf('"', qEnd + 1)
                } else {
                    break
                }
            }
            i = end
        }

        // Highlight active search match
        if (searchQuery.isNotBlank()) {
            var matchIdx = trimmed.indexOf(searchQuery, ignoreCase = true)
            while (matchIdx >= 0) {
                val matchEnd = (matchIdx + searchQuery.length).coerceAtMost(trimmed.length)
                addStyle(
                    SpanStyle(
                        background = Color(0xFFF59E0B).copy(alpha = 0.45f),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    matchIdx,
                    matchEnd
                )
                matchIdx = trimmed.indexOf(searchQuery, matchEnd, ignoreCase = true)
            }
        }
    }
}

@Composable
private fun RealDomInspectorPanel(
    devToolsEngine: DevToolsEngine,
    onCopyText: (String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val inspectedElement by devToolsEngine.inspectedElement.collectAsStateWithLifecycle()
    val domTreeNodes by devToolsEngine.domTreeNodes.collectAsStateWithLifecycle()
    val consoleLogs by devToolsEngine.consoleLogs.collectAsStateWithLifecycle()
    val isInspectTouchMode by devToolsEngine.isInspectTouchMode.collectAsStateWithLifecycle()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Element Inspector, 1 = Live DOM Tree, 2 = JS Console
    var treeSearchQuery by remember { mutableStateOf("") }
    var consoleInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-navigation Row + Pick Element on Page Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Pick Element on Page Button
            Surface(
                shape = ExpressivePillShape,
                color = if (isInspectTouchMode) Color(0xFF06B6D4) else Color(0xFF06B6D4).copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.55f)),
                modifier = Modifier
                    .clip(ExpressivePillShape)
                    .clickable {
                        devToolsEngine.toggleInspectTouchMode()
                    }
                    .testTag("inspect_pick_element_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Pick Element on Page",
                        tint = if (isInspectTouchMode) Color.White else Color(0xFF06B6D4),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Pick on Page",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isInspectTouchMode) Color.White else Color(0xFF06B6D4)
                    )
                }
            }

            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = { Text("Selected", fontSize = 11.5.sp) }
            )

            FilterChip(
                selected = selectedSubTab == 1,
                onClick = {
                    selectedSubTab = 1
                    devToolsEngine.scanLiveDomTree()
                },
                label = { Text("DOM Tree (${domTreeNodes.size})", fontSize = 11.5.sp) }
            )

            FilterChip(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                label = { Text("Console (${consoleLogs.size})", fontSize = 11.5.sp) }
            )
        }

        when (selectedSubTab) {
            0 -> {
                SelectedElementDetailsView(
                    element = inspectedElement,
                    onPickOnPage = { devToolsEngine.setInspectTouchMode(true) },
                    onBrowseDomTree = {
                        devToolsEngine.scanLiveDomTree()
                        selectedSubTab = 1
                    },
                    onUpdateText = { selector, text ->
                        devToolsEngine.updateInspectedElementText(selector, text)
                        onShowMessage("Updated live DOM element text")
                    },
                    onUpdateStyle = { selector, prop, value ->
                        devToolsEngine.updateInspectedElementStyle(selector, prop, value)
                        onShowMessage("Applied CSS $prop: $value to live element")
                    },
                    onRemoveElement = { selector ->
                        devToolsEngine.removeInspectedElement(selector)
                        onShowMessage("Removed element from live DOM")
                    },
                    onCopyText = onCopyText
                )
            }
            1 -> {
                val filteredNodes = remember(domTreeNodes, treeSearchQuery) {
                    if (treeSearchQuery.isBlank()) domTreeNodes
                    else domTreeNodes.filter {
                        it.tagName.contains(treeSearchQuery, ignoreCase = true) ||
                            it.id.contains(treeSearchQuery, ignoreCase = true) ||
                            it.className.contains(treeSearchQuery, ignoreCase = true) ||
                            it.textSnippet.contains(treeSearchQuery, ignoreCase = true)
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Surface(
                            shape = ExpressivePillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (treeSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Filter DOM nodes by tag, #id, .class, or text...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    BasicTextField(
                                        value = treeSearchQuery,
                                        onValueChange = { treeSearchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { devToolsEngine.scanLiveDomTree() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan DOM",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = filteredNodes,
                            key = { _, node -> "${node.index}_${node.cssSelector}" }
                        ) { idx, node ->
                            val cardShape = groupedListCardShape(index = idx, count = filteredNodes.size)
                            Card(
                                shape = cardShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .clickable {
                                        devToolsEngine.selectDomNodeBySelector(node.cssSelector)
                                        selectedSubTab = 0
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = (12 + (node.depth.coerceAtMost(5) * 10)).dp,
                                            end = 12.dp,
                                            top = 9.dp,
                                            bottom = 9.dp
                                        )
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF06B6D4).copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "<${node.tagName}>",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF06B6D4),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        val identifier = buildString {
                                            if (node.id.isNotBlank()) append("#${node.id} ")
                                            if (node.className.isNotBlank()) {
                                                append(
                                                    node.className.split(Regex("\\s+")).take(2)
                                                        .joinToString(".") { it }
                                                        .let { ".$it" }
                                                )
                                            }
                                        }.ifBlank { node.cssSelector }

                                        Text(
                                            text = identifier,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (node.textSnippet.isNotBlank()) {
                                            Text(
                                                text = "\"${node.textSnippet}\"",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (node.dimensionsLabel.isNotBlank()) {
                                        Text(
                                            text = node.dimensionsLabel,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Live JavaScript Console REPL
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        val quickSnippets = listOf(
                            "document.title",
                            "document.links.length",
                            "window.location.origin",
                            "document.cookie",
                            "performance.now()"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(quickSnippets) { snippet ->
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .clip(ExpressivePillShape)
                                        .clickable {
                                            devToolsEngine.evaluateConsoleCommand(snippet)
                                        }
                                ) {
                                    Text(
                                        text = snippet,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { devToolsEngine.clearConsoleLogs() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Console",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (consoleLogs.isEmpty()) {
                            item {
                                Text(
                                    text = "Live WebView JS Console ready. Run any JavaScript expression below or tap a quick snippet above.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                        items(consoleLogs, key = { it.id }) { entry ->
                            val badgeColor = when (entry.level) {
                                "ERROR" -> MaterialTheme.colorScheme.error
                                "WARN" -> Color(0xFFF59E0B)
                                "RESULT" -> Color(0xFF10B981)
                                "INPUT" -> Color(0xFF38BDF8)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.28f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = entry.level,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.width(48.dp)
                                    )
                                    Text(
                                        text = entry.message,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // JS Expression Input Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = consoleInput,
                            onValueChange = { consoleInput = it },
                            placeholder = {
                                Text(
                                    "Evaluate JS in live WebView (e.g. document.body.style.background)",
                                    fontSize = 11.5.sp
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            shape = ExpressivePillShape,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (consoleInput.isNotBlank()) {
                                        devToolsEngine.evaluateConsoleCommand(consoleInput)
                                        consoleInput = ""
                                    }
                                }
                                .testTag("console_eval_send_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Execute JS",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedElementDetailsView(
    element: InspectedDomElement?,
    onPickOnPage: () -> Unit,
    onBrowseDomTree: () -> Unit,
    onUpdateText: (String, String) -> Unit,
    onUpdateStyle: (String, String, String) -> Unit,
    onRemoveElement: (String) -> Unit,
    onCopyText: (String, String) -> Unit
) {
    if (element == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = ExpressiveSquircleShape,
                    color = Color(0xFF06B6D4).copy(alpha = 0.16f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Inspect Any Live DOM Element",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap 'Pick Element on Page' to tap directly on the live website, or browse the live DOM Tree.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPickOnPage) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Element on Page")
                    }
                    OutlinedButton(onClick = onBrowseDomTree) {
                        Text("Browse DOM Tree")
                    }
                }
            }
        }
        return
    }

    var editedText by remember(element.cssSelector, element.innerText) { mutableStateOf(element.innerText) }
    var stylePropInput by remember { mutableStateOf("background-color") }
    var styleValInput by remember { mutableStateOf("#fef08a") }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Element Header Card
        item {
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF06B6D4).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = buildString {
                                    append("<${element.tagName}")
                                    if (element.id.isNotBlank()) append("#${element.id}")
                                    append(">")
                                },
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF06B6D4),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${element.widthPx} × ${element.heightPx} px • (${element.leftPx}, ${element.topPx})",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { onCopyText(element.cssSelector, "CSS Selector") },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy CSS Selector",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRemoveElement(element.cssSelector) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Element from DOM",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Selector: ${element.cssSelector}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (element.xpath.isNotBlank()) {
                        Text(
                            text = "XPath: ${element.xpath}",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Live DOM Text & Inline Style Editor Card
        item {
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Live DOM Mutation (Real-Time Edit)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            label = { Text("innerText", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onUpdateText(element.cssSelector, editedText) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Apply Text", fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = stylePropInput,
                            onValueChange = { stylePropInput = it },
                            label = { Text("CSS Prop", fontSize = 10.5.sp) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.5.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(0.45f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = styleValInput,
                            onValueChange = { styleValInput = it },
                            label = { Text("Value", fontSize = 10.5.sp) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.5.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(0.45f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (stylePropInput.isNotBlank()) {
                                    onUpdateStyle(element.cssSelector, stylePropInput.trim(), styleValInput.trim())
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("Set", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Attributes Group
        if (element.attributes.isNotEmpty()) {
            item {
                Text(
                    text = "HTML Attributes (${element.attributes.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val entries = element.attributes.entries.toList()
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    entries.forEachIndexed { idx, (k, v) ->
                        val shape = groupedListCardShape(index = idx, count = entries.size)
                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = k,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.width(110.dp)
                                )
                                Text(
                                    text = v,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Computed CSS Styles Group
        if (element.computedStyles.isNotEmpty()) {
            item {
                Text(
                    text = "Computed CSS Styles (${element.computedStyles.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val styles = element.computedStyles.entries.toList()
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    styles.forEachIndexed { idx, (k, v) ->
                        val shape = groupedListCardShape(index = idx, count = styles.size)
                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = k,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA78BFA),
                                    modifier = Modifier.width(130.dp)
                                )
                                Text(
                                    text = v,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Outer HTML Preview
        if (element.outerHtml.isNotBlank()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Element outerHTML",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Copy HTML",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            onCopyText(element.outerHtml, "Element outerHTML")
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0B1120),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = highlightHtmlLine(element.outerHtml.take(2500), ""),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RealNetworkSnifferPanel(
    devToolsEngine: DevToolsEngine,
    onCopyText: (String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val logs by devToolsEngine.networkLogs.collectAsStateWithLifecycle()
    val isPaused by devToolsEngine.isSnifferPaused.collectAsStateWithLifecycle()
    val fetchingId by devToolsEngine.isFetchingPayloadId.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(NetworkResourceType.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedLogId by remember { mutableStateOf<String?>(null) }

    val filteredLogs = remember(logs, selectedType, searchQuery) {
        logs.filter { entry ->
            val matchesType = when (selectedType) {
                NetworkResourceType.ALL -> true
                NetworkResourceType.BLOCKED -> entry.isBlockedByShield
                else -> entry.resourceType == selectedType
            }
            val matchesQuery = searchQuery.isBlank() ||
                entry.url.contains(searchQuery, ignoreCase = true) ||
                entry.method.contains(searchQuery, ignoreCase = true) ||
                (entry.statusCode?.toString()?.contains(searchQuery) == true)
            matchesType && matchesQuery
        }
    }

    val blockedCount = remember(logs) { logs.count { it.isBlockedByShield } }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Control Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Pause / Resume Capture
            Surface(
                shape = ExpressivePillShape,
                color = if (isPaused) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.18f),
                modifier = Modifier
                    .clip(ExpressivePillShape)
                    .clickable { devToolsEngine.toggleSnifferPaused() }
                    .testTag("network_sniffer_pause_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = if (isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPaused) "Paused" else "Live Capturing",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                }
            }

            Text(
                text = "${logs.size} reqs • $blockedCount blocked",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Sync Resource Timing
            IconButton(
                onClick = { devToolsEngine.syncPerformanceResourceEntries() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Timings",
                    modifier = Modifier.size(17.dp)
                )
            }

            // Clear Network Logs
            IconButton(
                onClick = {
                    devToolsEngine.clearNetworkLogs()
                    expandedLogId = null
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Network Logs",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Resource Type Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(NetworkResourceType.values().toList()) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = {
                        Text(
                            text = type.label,
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }

        // URL / Domain Filter Input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Filter captured requests by URL, domain, method, or status...",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear filter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }
        }

        // Network Request List (Material 3 Grouped List Cards)
        if (filteredLogs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Waiting for Real Network Traffic...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Every HTTP/HTTPS request made by WebView is intercepted in real time with headers, timings, and cURL export.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = filteredLogs,
                    key = { _, item -> item.id }
                ) { idx, entry ->
                    val cardShape = groupedListCardShape(index = idx, count = filteredLogs.size)
                    val isExpanded = expandedLogId == entry.id
                    val statusColor = when {
                        entry.isBlockedByShield -> MaterialTheme.colorScheme.error
                        entry.statusCode == null -> Color(0xFF38BDF8)
                        entry.statusCode in 200..299 -> Color(0xFF10B981)
                        entry.statusCode in 300..399 -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.error
                    }

                    Card(
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isExpanded) statusColor.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .clickable {
                                expandedLogId = if (isExpanded) null else entry.id
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Method Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = entry.method,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Status Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = when {
                                            entry.isBlockedByShield -> "BLOCKED"
                                            entry.statusCode != null -> "${entry.statusCode}"
                                            else -> "200"
                                        },
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Type Label
                                Text(
                                    text = entry.resourceType.label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Host
                                Text(
                                    text = entry.host,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Duration & Size
                                val metricsText = buildString {
                                    entry.durationMs?.let { append("${it}ms") }
                                    entry.transferSizeBytes?.takeIf { it > 0 }?.let { bytes ->
                                        if (isNotEmpty()) append(" • ")
                                        if (bytes >= 1024) {
                                            append(String.format(Locale.US, "%.1f KB", bytes / 1024f))
                                        } else {
                                            append("$bytes B")
                                        }
                                    }
                                }.ifBlank { timeFormatter.format(Date(entry.timestamp)) }

                                Text(
                                    text = metricsText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = entry.pathAndQuery,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isExpanded) 4 else 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Expanded Packet Inspector
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action Buttons: Copy URL, Copy cURL, Fetch Response Body
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = { onCopyText(entry.url, "Request URL") },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Copy URL", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                onCopyText(entry.toCurlCommand(), "cURL Command")
                                                onShowMessage("Copied cURL command with real headers")
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Copy cURL", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { devToolsEngine.inspectNetworkEntryResponse(entry) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            if (fetchingId == entry.id) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text("Inspect Payload", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Full URL: ${entry.url}",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (entry.requestHeaders.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Real Request Headers (${entry.requestHeaders.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0B1120),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                entry.requestHeaders.forEach { (k, v) ->
                                                    Text(
                                                        text = "$k: $v",
                                                        fontSize = 10.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFFE2E8F0)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (entry.responseHeaders.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Real Response Headers (${entry.responseHeaders.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0B1120),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                entry.responseHeaders.forEach { (k, v) ->
                                                    Text(
                                                        text = "$k: $v",
                                                        fontSize = 10.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFFA7F3D0)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (!entry.responsePreview.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Response Body Payload",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                            Text(
                                                text = "Copy Payload",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    onCopyText(entry.responsePreview, "Response Payload")
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0B1120),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = entry.responsePreview.take(3000),
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFE2E8F0),
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
