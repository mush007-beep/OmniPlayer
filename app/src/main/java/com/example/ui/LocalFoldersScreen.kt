package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.MxBlue
import com.example.ui.theme.MxCyan
import com.example.ui.theme.MxGreenSend
import com.example.ui.theme.MxRedProgress
import com.example.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFoldersScreen(
    viewModel: PlayerViewModel,
    onOpenDrawer: () -> Unit
) {
    val layoutMode by viewModel.layoutMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val isScanningMedia by viewModel.isScanningMedia.collectAsState()
    val importStatusMessage by viewModel.importStatusMessage.collectAsState()
    val allVideos by viewModel.allVideosState.collectAsState()
    val importedVideos by viewModel.importedVideos.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showNetworkStreamDialog by remember { mutableStateOf(false) }
    var networkUrlInput by remember { mutableStateOf("") }
    var showImportMenu by remember { mutableStateOf(false) }

    val folders = viewModel.foldersList
    val continueWatching = viewModel.continueWatchingList

    // Runtime Permission Launcher for Media Access (READ_MEDIA_VIDEO/AUDIO or READ_EXTERNAL_STORAGE)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val anyGranted = permissionsMap.values.any { it }
        viewModel.onStoragePermissionResult(anyGranted)
    }

    // Direct Local Storage Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importVideoUris(uris)
        }
    }

    // Direct Local Storage Audio Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioUris(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search videos or folders...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("search_input")
                        )
                    } else {
                        Text(
                            text = "Folders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("menu_drawer_button")) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showImportMenu = true },
                            modifier = Modifier.testTag("import_media_menu_button")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Import Media", tint = MxBlue)
                        }
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import Video from Files") },
                                onClick = {
                                    showImportMenu = false
                                    videoPickerLauncher.launch(arrayOf("video/*"))
                                },
                                leadingIcon = { Icon(Icons.Default.VideoCall, contentDescription = null, tint = MxBlue) }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Audio from Files") },
                                onClick = {
                                    showImportMenu = false
                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                },
                                leadingIcon = { Icon(Icons.Default.AudioFile, contentDescription = null, tint = MxGreenSend) }
                            )
                            DropdownMenuItem(
                                text = { Text("Play Online Stream / URL") },
                                onClick = {
                                    showImportMenu = false
                                    showNetworkStreamDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MxCyan) }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan Device Storage") },
                                onClick = {
                                    showImportMenu = false
                                    if (hasStoragePermission) {
                                        viewModel.scanLocalMedia()
                                    } else {
                                        permissionLauncher.launch(viewModel.localMediaScanner.getRequiredPermissions())
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                        }
                    }

                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.setLayoutMode(if (layoutMode == LayoutMode.LIST) LayoutMode.GRID else LayoutMode.LIST)
                        },
                        modifier = Modifier.testTag("layout_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (layoutMode == LayoutMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Toggle Grid/List"
                        )
                    }
                    IconButton(
                        onClick = { showSortDialog = true },
                        modifier = Modifier.testTag("sort_filter_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Sort and View Mode")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    allVideos.firstOrNull()?.let { viewModel.openVideo(it) }
                },
                containerColor = MxBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("quick_resume_fab")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Resume Video", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Storage Permission Request Banner (READ_MEDIA_VIDEO/AUDIO & READ_EXTERNAL_STORAGE)
            if (!hasStoragePermission) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("storage_permission_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = MxBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Device Storage Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Allow access to READ_MEDIA_VIDEO & READ_MEDIA_AUDIO (or READ_EXTERNAL_STORAGE) so OmniPlayer can automatically discover, play, and auto-dub local videos & songs on this device.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(viewModel.localMediaScanner.getRequiredPermissions())
                                    },
                                    modifier = Modifier.testTag("grant_storage_permission_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MxBlue)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grant Storage Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                                    modifier = Modifier.testTag("open_file_picker_button")
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick File", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Import Status Feedback Banner
            if (importStatusMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("import_status_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MxGreenSend, modifier = Modifier.size(18.dp))
                                Text(text = importStatusMessage ?: "", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            IconButton(onClick = { viewModel.dismissImportMessage() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Quick Import Action Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                        label = { Text("+ Import Video", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp), tint = MxBlue) },
                        modifier = Modifier.testTag("quick_import_video_chip")
                    )
                    AssistChip(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        label = { Text("+ Import Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MxGreenSend) },
                        modifier = Modifier.testTag("quick_import_audio_chip")
                    )
                    AssistChip(
                        onClick = { showNetworkStreamDialog = true },
                        label = { Text("🌐 Stream URL", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = MxCyan) },
                        modifier = Modifier.testTag("quick_stream_url_chip")
                    )
                    AssistChip(
                        onClick = {
                            if (hasStoragePermission) {
                                viewModel.scanLocalMedia()
                            } else {
                                permissionLauncher.launch(viewModel.localMediaScanner.getRequiredPermissions())
                            }
                        },
                        label = {
                            Text(if (isScanningMedia) "Scanning..." else "🔄 Rescan Storage", fontSize = 11.sp)
                        },
                        modifier = Modifier.testTag("quick_rescan_storage_chip")
                    )
                }
            }

            // Continue Watching Carousel (Only shown when real history exists)
            if (continueWatching.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue Watching",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            continueWatching.forEach { item ->
                                ContinueWatchingCard(
                                    item = item,
                                    onClick = {
                                        val matchedVideo = allVideos.firstOrNull { it.id == item.id } ?: allVideos.firstOrNull()
                                        if (matchedVideo != null) {
                                            viewModel.openVideo(matchedVideo)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Universal Media Hub Empty State (when no media is discovered yet)
            if (folders.isEmpty() && allVideos.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("all_formats_player_hub_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MxBlue.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MxBlue, modifier = Modifier.size(36.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Universal Media Player",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Play any video or audio format directly on your device with HW+ acceleration & AI subtitles.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val formats = listOf("MP4", "MKV", "WEBM", "AVI", "TS", "FLAC", "MP3", "WAV", "AAC", "HLS")
                                Text(
                                    text = formats.joinToString(" • "),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MxCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { videoPickerLauncher.launch(arrayOf("video/*", "audio/*")) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MxBlue)
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showNetworkStreamDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stream URL", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Folders Section Header
            if (folders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Folders (${folders.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${sortDirection.name.replace("_", " ")} • ${layoutMode.name}",
                            fontSize = 12.sp,
                            color = MxBlue
                        )
                    }
                }

                // 4. Folders Content (List or Grid)
                if (layoutMode == LayoutMode.LIST) {
                    items(folders) { folder ->
                        FolderListItem(
                            folder = folder,
                            onClick = {
                                val target = allVideos.firstOrNull { it.folderName == folder.name } ?: allVideos.firstOrNull()
                                if (target != null) viewModel.openVideo(target)
                            }
                        )
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val chunked = folders.chunked(3)
                            chunked.forEach { rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { folder ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            FolderGridItem(
                                                folder = folder,
                                                onClick = {
                                                    val target = allVideos.firstOrNull { it.folderName == folder.name } ?: allVideos.firstOrNull()
                                                    if (target != null) viewModel.openVideo(target)
                                                }
                                            )
                                        }
                                    }
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Direct Video Library List (Screenshot 8 Left Pane)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recent Videos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            items(allVideos) { video ->
                VideoListItem(
                    video = video,
                    onClick = { viewModel.openVideo(video) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Sort & View Mode Bottom Sheet (Screenshot 6)
    if (showSortDialog) {
        SortAndLayoutSheet(
            currentViewMode = viewMode,
            currentLayout = layoutMode,
            currentSort = sortBy,
            currentDirection = sortDirection,
            onSelectViewMode = { viewModel.setViewMode(it) },
            onSelectLayout = { viewModel.setLayoutMode(it) },
            onSelectSort = { viewModel.setSortBy(it) },
            onSelectDirection = { viewModel.setSortDirection(it) },
            onDismiss = { showSortDialog = false }
        )
    }

    // Play from Network URL / Stream Dialog
    if (showNetworkStreamDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkStreamDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MxCyan)
                    Text("Play Online Stream / URL", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Paste any media URL (MP4, MKV, WebM, MP3, AAC, FLAC, HLS .m3u8, DASH .mpd):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = networkUrlInput,
                        onValueChange = { networkUrlInput = it },
                        placeholder = { Text("https://example.com/video.mp4") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("network_url_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (networkUrlInput.isNotBlank()) {
                            viewModel.openNetworkStream(networkUrlInput.trim())
                            showNetworkStreamDialog = false
                            networkUrlInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MxBlue),
                    modifier = Modifier.testTag("play_network_stream_button")
                ) {
                    Text("Play Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkStreamDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
            .testTag("continue_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(0xFF262A38)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(36.dp)
                )

                // Episode badge (Top right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Red Progress bar at the bottom (matching Screenshot 3)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress)
                            .fillMaxHeight()
                            .background(MxRedProgress)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.episodeInfo,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FolderListItem(
    folder: FolderItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("folder_item_${folder.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(MxBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (folder.isProtected) Icons.Default.Lock else Icons.Default.Folder,
                contentDescription = null,
                tint = MxBlue,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${folder.count} videos",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (folder.newCount > 0) {
            Box(
                modifier = Modifier
                    .background(MxRedProgress, CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${folder.newCount}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FolderGridItem(
    folder: FolderItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("folder_grid_${folder.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Icon(
                    imageVector = if (folder.isProtected) Icons.Default.Lock else Icons.Default.Folder,
                    contentDescription = null,
                    tint = MxBlue,
                    modifier = Modifier.size(36.dp)
                )
                if (folder.newCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(MxRedProgress, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${folder.newCount}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = folder.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${folder.count} videos",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VideoListItem(
    video: VideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("video_item_${video.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 50.dp)
                .background(Color(0xFF202330), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))

            // Duration badge at bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(video.durationText, color = Color.White, fontSize = 9.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(video.folderName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(video.sizeText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Badge(containerColor = MxBlue.copy(alpha = 0.2f)) {
                    Text(video.resolution.name.replace("ULTRA_", "").replace("_", " "), color = MxBlue, fontSize = 9.sp)
                }
            }
        }

        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Modal Sort & View Mode Bottom Sheet (Screenshot 6)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortAndLayoutSheet(
    currentViewMode: ViewMode,
    currentLayout: LayoutMode,
    currentSort: SortBy,
    currentDirection: SortDirection,
    onSelectViewMode: (ViewMode) -> Unit,
    onSelectLayout: (LayoutMode) -> Unit,
    onSelectSort: (SortBy) -> Unit,
    onSelectDirection: (SortDirection) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("sort_layout_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // View Mode & Layout row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("View Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentViewMode == ViewMode.ALL_FOLDERS,
                            onClick = { onSelectViewMode(ViewMode.ALL_FOLDERS) },
                            label = { Text("All Folders", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = currentViewMode == ViewMode.FOLDERS,
                            onClick = { onSelectViewMode(ViewMode.FOLDERS) },
                            label = { Text("Folders", fontSize = 11.sp) }
                        )
                    }
                }

                Column {
                    Text("Layout", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentLayout == LayoutMode.LIST,
                            onClick = { onSelectLayout(LayoutMode.LIST) },
                            label = { Text("List", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = currentLayout == LayoutMode.GRID,
                            onClick = { onSelectLayout(LayoutMode.GRID) },
                            label = { Text("Grid", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sort Options
            Text("Sort By", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val sortOptions = listOf(
                SortBy.TITLE to "Title",
                SortBy.DATE to "Date",
                SortBy.PLAYED_TIME to "Played Time",
                SortBy.LENGTH to "Length",
                SortBy.SIZE to "Size",
                SortBy.RESOLUTION to "Resolution"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortOptions.forEach { (option, label) ->
                    FilterChip(
                        selected = currentSort == option,
                        onClick = { onSelectSort(option) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direction Buttons (A to Z vs Z to A)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSelectDirection(SortDirection.A_TO_Z) },
                    modifier = Modifier.weight(1f),
                    colors = if (currentDirection == SortDirection.A_TO_Z)
                        ButtonDefaults.outlinedButtonColors(containerColor = MxBlue.copy(alpha = 0.15f))
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("A to Z")
                }

                OutlinedButton(
                    onClick = { onSelectDirection(SortDirection.Z_TO_A) },
                    modifier = Modifier.weight(1f),
                    colors = if (currentDirection == SortDirection.Z_TO_A)
                        ButtonDefaults.outlinedButtonColors(containerColor = MxBlue.copy(alpha = 0.15f))
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Z to A")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("done_sort_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MxBlue)
            ) {
                Text("Done")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
