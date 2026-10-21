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
import com.example.model.EqualizerSettings
import com.example.ui.theme.MxBlue
import com.example.ui.theme.MxGreenSend
import com.example.viewmodel.PlayerViewModel

@Composable
fun MusicEqScreen(viewModel: PlayerViewModel) {
    val eqSettings by viewModel.equalizerSettings.collectAsState()
    val allVideos = viewModel.allVideosList
    val importedAudios by viewModel.importedAudios.collectAsState()
    val currentAudio by viewModel.currentAudio.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val isScanningMedia by viewModel.isScanningMedia.collectAsState()

    var bassBoost by remember { mutableStateOf(eqSettings.bassBoost) }
    var surround3d by remember { mutableStateOf(eqSettings.surround3d) }
    var selectedPreset by remember { mutableStateOf(eqSettings.preset) }

    val presets = listOf("Bio-Acoustic", "Deep Bass", "Rock", "EDM", "Vocal", "Flat")

    // Permission launcher for storage media access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        val anyGranted = map.values.any { it }
        viewModel.onStoragePermissionResult(anyGranted)
    }

    // Audio Document Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioUris(uris)
        }
    }

    val activeTrackTitle = currentAudio?.title ?: allVideos.firstOrNull()?.title ?: "Select or Import Audio Track"
    val activeTrackArtist = currentAudio?.artist ?: allVideos.firstOrNull()?.let { "Video Audio Track • ${it.format}" } ?: "Lossless & Hi-Res Audio Engine"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HD Music & Equalizer",
                        color = MxBlue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Local audio import, bass boost & spatial sound",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                FilledTonalButton(
                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("import_audio_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Storage Permission Card
        if (!hasStoragePermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_permission_card_audio")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MxBlue)
                            Text("Audio Storage Access Needed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant READ_MEDIA_AUDIO or READ_EXTERNAL_STORAGE to scan and play music files from device storage.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(viewModel.localMediaScanner.getRequiredPermissions()) },
                                modifier = Modifier.testTag("grant_audio_permission_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MxBlue)
                            ) {
                                Text("Grant Permission", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }
                            ) {
                                Text("Pick Audio File", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Now Playing Music Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MxBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MxBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeTrackTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeTrackArtist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleAudioPlayback() },
                        modifier = Modifier.testTag("toggle_audio_play_button")
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Play/Pause",
                            tint = MxBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Local Audio Library List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Local Audio Tracks (${importedAudios.size})",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(
                    onClick = {
                        if (hasStoragePermission) {
                            viewModel.scanLocalMedia()
                        } else {
                            permissionLauncher.launch(viewModel.localMediaScanner.getRequiredPermissions())
                        }
                    },
                    modifier = Modifier.testTag("scan_audio_button")
                ) {
                    Icon(
                        imageVector = if (isScanningMedia) Icons.Default.HourglassTop else Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isScanningMedia) "Scanning..." else "Scan Storage", fontSize = 12.sp)
                }
            }
        }

        items(importedAudios) { audio ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.playAudio(audio) }
                    .testTag("audio_track_card_${audio.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentAudio?.id == audio.id) MxBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (currentAudio?.id == audio.id) MxBlue else MaterialTheme.colorScheme.surface,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentAudio?.id == audio.id && isAudioPlaying) Icons.Default.Equalizer else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (currentAudio?.id == audio.id) Color.White else MxBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(audio.artist, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(audio.durationText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(audio.sizeText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = { viewModel.playAudio(audio) }) {
                        Icon(
                            imageVector = if (currentAudio?.id == audio.id && isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Audio",
                            tint = MxBlue
                        )
                    }
                }
            }
        }

        // Presets Selector
        item {
            Text("Acoustic Presets", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.take(4).forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = {
                            selectedPreset = preset
                            viewModel.updateEqualizer(eqSettings.copy(preset = preset))
                        },
                        label = { Text(preset, fontSize = 11.sp) },
                        modifier = Modifier.testTag("preset_chip_$preset")
                    )
                }
            }
        }

        // Bass Boost & 3D Surround Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bass Booster", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text("${(bassBoost * 100).toInt()}%", color = MxBlue, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = bassBoost,
                        onValueChange = {
                            bassBoost = it
                            viewModel.updateEqualizer(eqSettings.copy(bassBoost = it))
                        },
                        colors = SliderDefaults.colors(thumbColor = MxBlue, activeTrackColor = MxBlue),
                        modifier = Modifier.testTag("bass_boost_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D Spatial Surround", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text("${(surround3d * 100).toInt()}%", color = MxBlue, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = surround3d,
                        onValueChange = {
                            surround3d = it
                            viewModel.updateEqualizer(eqSettings.copy(surround3d = it))
                        },
                        colors = SliderDefaults.colors(thumbColor = MxBlue, activeTrackColor = MxBlue),
                        modifier = Modifier.testTag("surround_3d_slider")
                    )
                }
            }
        }

        // 5-Band Equalizer Graphic Sliders
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = MxBlue)
                        Text("Multi-Band Frequency Spectrum", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                    bands.forEachIndexed { index, bandName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bandName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                            Slider(
                                value = 0.6f,
                                onValueChange = {},
                                colors = SliderDefaults.colors(thumbColor = MxBlue, activeTrackColor = MxBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_$index")
                            )
                        }
                    }
                }
            }
        }
    }
}
