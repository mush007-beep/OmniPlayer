package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerSettings
import com.example.ui.theme.MxBlue
import com.example.viewmodel.PlayerViewModel

@Composable
fun MusicEqScreen(viewModel: PlayerViewModel) {
    val eqSettings by viewModel.equalizerSettings.collectAsState()
    val allVideos = viewModel.allVideosList
    val nowPlaying = allVideos.first()

    var bassBoost by remember { mutableStateOf(eqSettings.bassBoost) }
    var surround3d by remember { mutableStateOf(eqSettings.surround3d) }
    var selectedPreset by remember { mutableStateOf(eqSettings.preset) }

    val presets = listOf("Bio-Acoustic", "Deep Bass", "Rock", "EDM", "Vocal", "Flat")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "HD Music Player & Equalizer",
                color = MxBlue,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Custom audio controls, bass boost & spatial sound tuning",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
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
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MxBlue, modifier = Modifier.size(32.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(nowPlaying.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("HD Audio • Lossless Audio Track", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MxBlue)
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
