package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BookmarkItem
import com.example.model.BookmarkSortOption
import com.example.ui.components.AuraSubScreenTopBar
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressiveDialogShape
import com.example.ui.theme.ExpressivePillShape
import com.example.ui.theme.ExpressiveSquircleShape

/**
 * Material 3 Expressive Bookmarks Screen
 * - App bar left side: only "Bookmarks" text (no bookmark icon).
 * - App bar right side: action icons for Add Bookmark, Add Group, Sort Menu (with A–Z, Z–A, Pin, etc.),
 *   and 3-Dots Menu (with Import Bookmarks & Export Bookmarks).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkItem>,
    bookmarkFolders: List<String> = listOf("Favorites", "Work", "Reading List"),
    selectedFolder: String,
    sortOption: BookmarkSortOption = BookmarkSortOption.PINNED,
    onSelectFolder: (String) -> Unit,
    onSelectSortOption: (BookmarkSortOption) -> Unit = {},
    onOpenBookmark: (String) -> Unit,
    onOpenInNewTab: (String, String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onTogglePinBookmark: (String) -> Unit = {},
    onAddBookmark: (title: String, url: String, folder: String, isPinned: Boolean) -> Unit,
    onAddGroup: (String) -> Unit = {},
    onImportBookmarks: (String, String) -> Int = { _, _ -> 0 },
    onExportBookmarksJson: () -> String = { "[]" },
    onExportBookmarksHtml: () -> String = { "" },
    onCopyUrl: ((String) -> Unit)? = null,
    onShowMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var keepPinnedFirst by remember { mutableStateOf(true) }
    var importTextContent by remember { mutableStateOf("") }
    var exportFormatJson by remember { mutableStateOf(true) }

    // File Picker Launcher for Importing Bookmarks from a local file
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    importTextContent = content
                    val targetFolder = if (selectedFolder == "All") "Favorites" else selectedFolder
                    val count = onImportBookmarks(content, targetFolder)
                    if (count > 0) {
                        showImportDialog = false
                    }
                }
            }.onFailure {
                onShowMessage?.invoke("Could not read selected file")
            }
        }
    }

    // File Saver Launcher for Exporting Bookmarks to a local file
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (exportFormatJson) "application/json" else "text/html"
        )
    ) { uri ->
        if (uri != null) {
            runCatching {
                val payload = if (exportFormatJson) onExportBookmarksJson() else onExportBookmarksHtml()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(payload)
                }
                onShowMessage?.invoke("Bookmarks exported successfully")
                showExportDialog = false
            }.onFailure {
                onShowMessage?.invoke("Failed to save exported bookmarks")
            }
        }
    }

    val allFolders = remember(bookmarkFolders, bookmarks) {
        val dynamicFolders = bookmarks.map { it.folder }.filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
        (listOf("All") + bookmarkFolders + dynamicFolders).distinctBy { it.lowercase() }
    }

    val filteredBookmarks = remember(bookmarks, selectedFolder, searchQuery, sortOption, keepPinnedFirst) {
        val base = bookmarks.filter { item ->
            val matchesFolder = selectedFolder == "All" || item.folder.equals(selectedFolder, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.url.contains(searchQuery, ignoreCase = true) ||
                    item.folder.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesQuery
        }

        val comparator: Comparator<BookmarkItem> = when (sortOption) {
            BookmarkSortOption.PINNED -> compareByDescending<BookmarkItem> { it.isPinned }
                .thenBy { it.title.lowercase() }
            BookmarkSortOption.A_TO_Z -> if (keepPinnedFirst) {
                compareByDescending<BookmarkItem> { it.isPinned }.thenBy { it.title.lowercase() }
            } else {
                compareBy { it.title.lowercase() }
            }
            BookmarkSortOption.Z_TO_A -> if (keepPinnedFirst) {
                compareByDescending<BookmarkItem> { it.isPinned }.thenByDescending { it.title.lowercase() }
            } else {
                compareByDescending { it.title.lowercase() }
            }
            BookmarkSortOption.NEWEST -> if (keepPinnedFirst) {
                compareByDescending<BookmarkItem> { it.isPinned }.thenByDescending { it.createdAt }
            } else {
                compareByDescending { it.createdAt }
            }
            BookmarkSortOption.OLDEST -> if (keepPinnedFirst) {
                compareByDescending<BookmarkItem> { it.isPinned }.thenBy { it.createdAt }
            } else {
                compareBy { it.createdAt }
            }
            BookmarkSortOption.GROUP -> if (keepPinnedFirst) {
                compareByDescending<BookmarkItem> { it.isPinned }
                    .thenBy { it.folder.lowercase() }
                    .thenBy { it.title.lowercase() }
            } else {
                compareBy<BookmarkItem> { it.folder.lowercase() }
                    .thenBy { it.title.lowercase() }
            }
        }

        base.sortedWith(comparator)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("bookmarks_screen")
    ) {
        // Bookmarks App Bar:
        // Left side: ONLY "Bookmarks" text (no bookmark icon, no subtitle).
        // Right side: Action icons for Add Bookmark, Add Group, Sort Menu, and 3-Dots Menu (Import/Export).
        AuraSubScreenTopBar(
            title = stringResource(R.string.bookmarks_title),
            subtitle = null,
            icon = null,
            actions = {
                // 1. Add Bookmark Icon
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_add_bookmark")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.bookmarks_add),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 2. Add Group Icon
                IconButton(
                    onClick = { showAddGroupDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_add_bookmark_group")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = stringResource(R.string.bookmarks_add_group),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3. Sort Menu Icon + DropdownMenu (A-Z, Z-A, Pin, Newest, Oldest, Group)
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_sort_bookmarks")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.bookmarks_sort),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("menu_sort_bookmarks")
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Pin (Pinned First)",
                                    fontWeight = if (sortOption == BookmarkSortOption.PINNED) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = if (sortOption == BookmarkSortOption.PINNED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (sortOption == BookmarkSortOption.PINNED) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOption(BookmarkSortOption.PINNED)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_option_pinned")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "A–Z",
                                    fontWeight = if (sortOption == BookmarkSortOption.A_TO_Z) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha,
                                    contentDescription = null,
                                    tint = if (sortOption == BookmarkSortOption.A_TO_Z) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (sortOption == BookmarkSortOption.A_TO_Z) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOption(BookmarkSortOption.A_TO_Z)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_option_a_to_z")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Z–A",
                                    fontWeight = if (sortOption == BookmarkSortOption.Z_TO_A) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha,
                                    contentDescription = null,
                                    tint = if (sortOption == BookmarkSortOption.Z_TO_A) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (sortOption == BookmarkSortOption.Z_TO_A) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOption(BookmarkSortOption.Z_TO_A)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_option_z_to_a")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Newest First",
                                    fontWeight = if (sortOption == BookmarkSortOption.NEWEST) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (sortOption == BookmarkSortOption.NEWEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (sortOption == BookmarkSortOption.NEWEST) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOption(BookmarkSortOption.NEWEST)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_option_newest")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Oldest First",
                                    fontWeight = if (sortOption == BookmarkSortOption.OLDEST) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (sortOption == BookmarkSortOption.OLDEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (sortOption == BookmarkSortOption.OLDEST) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOption(BookmarkSortOption.OLDEST)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_option_oldest")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Group (A–Z)",
                                    fontWeight = if (sortOption == BookmarkSortOption.GROUP) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (sortOption == BookmarkSortOption.GROUP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (sortOption == BookmarkSortOption.GROUP) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOption(BookmarkSortOption.GROUP)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_option_group")
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Pin to Top Always",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (keepPinnedFirst) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = if (keepPinnedFirst) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (keepPinnedFirst) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                keepPinnedFirst = !keepPinnedFirst
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_toggle_keep_pinned")
                        )
                    }
                }

                // 4. 3-Dots Menu Icon + DropdownMenu (Import & Export Bookmarks)
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_bookmarks_more")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.bookmarks_more_options),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("menu_bookmarks_more")
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookmarks_import)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showImportDialog = true
                            },
                            modifier = Modifier.testTag("menu_import_bookmarks")
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookmarks_export)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showExportDialog = true
                            },
                            modifier = Modifier.testTag("menu_export_bookmarks")
                        )
                    }
                }
            }
        )

        // Expressive Search within Bookmarks
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search saved bookmarks...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = ExpressivePillShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_bookmarks_input")
        )

        // Folders / Groups Filter Chips + Active Sort Badge
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            items(allFolders) { folder ->
                val isSelected = folder.equals(selectedFolder, ignoreCase = true)
                Surface(
                    shape = ExpressivePillShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .clip(ExpressivePillShape)
                        .clickable { onSelectFolder(folder) }
                        .testTag("folder_chip_${folder.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = folder,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Bookmarks List
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredBookmarks.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 54.dp)
                    ) {
                        Surface(
                            shape = ExpressiveSquircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No bookmarks found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bookmark favorite pages using the + icon or import existing bookmarks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredBookmarks, key = { it.id }) { bookmark ->
                    Card(
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (bookmark.isPinned) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (bookmark.isPinned) {
                                Color(0xFFF59E0B).copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveCardShape)
                            .clickable { onOpenBookmark(bookmark.url) }
                            .testTag("bookmark_item_${bookmark.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Surface(
                                shape = ExpressiveSquircleShape,
                                color = if (bookmark.isPinned) {
                                    Color(0xFFF59E0B).copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (bookmark.isPinned) Icons.Default.PushPin else Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (bookmark.isPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (bookmark.isPinned) {
                                        Surface(
                                            shape = ExpressiveChipShape,
                                            color = Color(0xFFF59E0B).copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "Pinned",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF59E0B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = bookmark.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = ExpressiveChipShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = bookmark.folder,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pin / Unpin Bookmark Icon Button
                                IconButton(
                                    onClick = { onTogglePinBookmark(bookmark.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_pin_bookmark_${bookmark.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = if (bookmark.isPinned) {
                                            stringResource(R.string.bookmarks_unpin)
                                        } else {
                                            stringResource(R.string.bookmarks_pin)
                                        },
                                        tint = if (bookmark.isPinned) {
                                            Color(0xFFF59E0B)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        },
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                if (onCopyUrl != null) {
                                    IconButton(
                                        onClick = { onCopyUrl(bookmark.url) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("btn_copy_bookmark_${bookmark.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Bookmark Link",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onOpenInNewTab(bookmark.url, bookmark.title) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_open_new_tab_${bookmark.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open in New Tab",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteBookmark(bookmark.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_delete_bookmark_${bookmark.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Bookmark",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Add Bookmark Dialog
    if (showAddDialog) {
        val selectableFolders = allFolders.filter { !it.equals("All", ignoreCase = true) }.ifEmpty { listOf("Favorites") }
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("https://") }
        var selectedNewFolder by remember {
            mutableStateOf(
                if (selectedFolder != "All" && selectableFolders.any { it.equals(selectedFolder, ignoreCase = true) }) {
                    selectedFolder
                } else {
                    selectableFolders.first()
                }
            )
        }
        var isNewPinned by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = ExpressiveDialogShape,
            title = {
                Text("Add Bookmark", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. My Favorite Site") },
                        singleLine = true,
                        shape = ExpressiveCardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_bookmark_title")
                    )
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("URL") },
                        singleLine = true,
                        shape = ExpressiveCardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_bookmark_url")
                    )

                    Text(
                        text = "Group",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectableFolders.forEach { folderOption ->
                            FilterChip(
                                selected = selectedNewFolder.equals(folderOption, ignoreCase = true),
                                onClick = { selectedNewFolder = folderOption },
                                label = { Text(folderOption, fontSize = 12.sp) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isNewPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pin to top",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = isNewPinned,
                            onCheckedChange = { isNewPinned = it },
                            modifier = Modifier.testTag("switch_bookmark_pin")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                            onAddBookmark(newTitle.trim(), newUrl.trim(), selectedNewFolder, isNewPinned)
                            showAddDialog = false
                        }
                    },
                    enabled = newTitle.isNotBlank() && newUrl.isNotBlank(),
                    shape = ExpressivePillShape,
                    modifier = Modifier.testTag("btn_confirm_add_bookmark")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Add Group / Folder Dialog
    if (showAddGroupDialog) {
        var newGroupName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            shape = ExpressiveDialogShape,
            title = {
                Text("Add Bookmark Group", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Create a new group to organize your bookmarks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g. Research, Shopping, Dev") },
                        singleLine = true,
                        shape = ExpressiveCardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_bookmark_group_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onAddGroup(newGroupName.trim())
                            showAddGroupDialog = false
                        }
                    },
                    enabled = newGroupName.isNotBlank(),
                    shape = ExpressivePillShape,
                    modifier = Modifier.testTag("btn_confirm_add_group")
                ) {
                    Text("Create Group")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddGroupDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("dialog_add_bookmark_group")
        )
    }

    // 3. Import Bookmarks Dialog
    if (showImportDialog) {
        val targetFolder = if (selectedFolder == "All") "Favorites" else selectedFolder

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            shape = ExpressiveDialogShape,
            title = {
                Text("Import Bookmarks", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select a JSON/HTML file or paste bookmarks (JSON, HTML, or Title | URL | Group per line).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { importFileLauncher.launch("*/*") },
                            shape = ExpressivePillShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_import_choose_file")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose File", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                    if (!clipText.isNullOrBlank()) {
                                        importTextContent = clipText
                                    } else {
                                        onShowMessage?.invoke("Clipboard is empty")
                                    }
                                }
                            },
                            shape = ExpressivePillShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_import_paste_clipboard")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = importTextContent,
                        onValueChange = { importTextContent = it },
                        label = { Text("Bookmarks Data (JSON / HTML / URLs)") },
                        placeholder = {
                            Text("GitHub | https://github.com | Work\nKotlin Docs | https://kotlinlang.org | Favorites")
                        },
                        minLines = 4,
                        maxLines = 7,
                        shape = ExpressiveCardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_import_bookmarks_data")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importTextContent.isNotBlank()) {
                            onImportBookmarks(importTextContent, targetFolder)
                            importTextContent = ""
                            showImportDialog = false
                        }
                    },
                    enabled = importTextContent.isNotBlank(),
                    shape = ExpressivePillShape,
                    modifier = Modifier.testTag("btn_confirm_import_bookmarks")
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("dialog_import_bookmarks")
        )
    }

    // 4. Export Bookmarks Dialog
    if (showExportDialog) {
        val exportedPayload = remember(bookmarks, exportFormatJson) {
            if (exportFormatJson) onExportBookmarksJson() else onExportBookmarksHtml()
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            shape = ExpressiveDialogShape,
            title = {
                Text("Export Bookmarks", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = exportFormatJson,
                            onClick = { exportFormatJson = true },
                            label = { Text("JSON Format") },
                            modifier = Modifier.testTag("chip_export_json")
                        )
                        FilterChip(
                            selected = !exportFormatJson,
                            onClick = { exportFormatJson = false },
                            label = { Text("HTML Format") },
                            modifier = Modifier.testTag("chip_export_html")
                        )
                    }

                    OutlinedTextField(
                        value = exportedPayload,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Export Preview (${bookmarks.size} bookmarks)") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        minLines = 4,
                        maxLines = 7,
                        shape = ExpressiveCardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .testTag("export_bookmarks_preview")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                onCopyUrl?.invoke(exportedPayload)
                            },
                            shape = ExpressivePillShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_copy")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val fileName = if (exportFormatJson) "aura_bookmarks.json" else "aura_bookmarks.html"
                                exportFileLauncher.launch(fileName)
                            },
                            shape = ExpressivePillShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_save_file")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save File", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Done")
                }
            },
            modifier = Modifier.testTag("dialog_export_bookmarks")
        )
    }
}
