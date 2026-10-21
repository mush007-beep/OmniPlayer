package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoItem
import com.example.ui.theme.MxBlue
import com.example.ui.theme.MxGreenSend
import com.example.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllVideosTabScreen(
    viewModel: PlayerViewModel,
    onOpenDrawer: () -> Unit
) {
    val allVideos by viewModel.allVideosState.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val isScanningMedia by viewModel.isScanningMedia.collectAsState()
    val importedVideos by viewModel.importedVideos.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All Videos", "Local Storage (${importedVideos.size})", "Recently Added")

    // Permission launcher for storage media access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        val anyGranted = map.values.any { it }
        viewModel.onStoragePermissionResult(anyGranted)
    }

    // Video Document Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importVideoUris(uris)
        }
    }

    val displayVideos = when (selectedTab) {
        1 -> allVideos.filter { it.isLocal }
        else -> allVideos
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videos", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                        modifier = Modifier.testTag("import_video_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Import Video", tint = MxBlue)
                    }
                    IconButton(
                        onClick = {
                            if (hasStoragePermission) {
                                viewModel.scanLocalMedia()
                            } else {
                                permissionLauncher.launch(viewModel.localMediaScanner.getRequiredPermissions())
                            }
                        },
                        modifier = Modifier.testTag("rescan_videos_button")
                    ) {
                        Icon(
                            imageVector = if (isScanningMedia) Icons.Default.HourglassTop else Icons.Default.Refresh,
                            contentDescription = "Rescan MediaStore"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                containerColor = MxBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("import_video_fab")
            ) {
                Icon(Icons.Default.VideoCall, contentDescription = "Import Video", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MxBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Storage Permission Card
                if (!hasStoragePermission) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("storage_permission_card_videos")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Grant Storage Permission",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Required to scan device folders for videos and play them with AI auto-dubbing.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            permissionLauncher.launch(viewModel.localMediaScanner.getRequiredPermissions())
                                        },
                                        modifier = Modifier.testTag("grant_storage_permission_videos_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MxBlue)
                                    ) {
                                        Text("Grant Permission", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                                        modifier = Modifier.testTag("open_video_picker_button")
                                    ) {
                                        Text("Pick File", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (displayVideos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No local videos found yet", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap 'Import Video' to select video files from device storage.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MxBlue)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Video File")
                                }
                            }
                        }
                    }
                }

                items(displayVideos) { video ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openVideo(video) }
                            .testTag("all_video_card_${video.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 85.dp, height = 55.dp)
                                    .background(Color(0xFF1E2230), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MxBlue, modifier = Modifier.size(28.dp))
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = video.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (video.isLocal) {
                                        Box(
                                            modifier = Modifier
                                                .background(MxGreenSend.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("DEVICE", color = MxGreenSend, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(video.sizeText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(video.folderName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            IconButton(onClick = {}) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
