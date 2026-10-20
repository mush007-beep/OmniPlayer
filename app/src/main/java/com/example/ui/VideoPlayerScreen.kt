package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutoDubLanguage
import com.example.model.VideoItem
import com.example.ui.theme.MxBlue
import com.example.ui.theme.MxCyan
import com.example.ui.theme.MxGreenSend
import com.example.viewmodel.GestureType
import com.example.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    viewModel: PlayerViewModel
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionSec by viewModel.currentPositionSec.collectAsState()
    val totalDurationSec by viewModel.totalDurationSec.collectAsState()
    val isScreenLocked by viewModel.isScreenLocked.collectAsState()
    val isControlsVisible by viewModel.isControlsVisible.collectAsState()
    val hwAcc by viewModel.hwAcceleration.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val activeGesture by viewModel.activeGesture.collectAsState()
    val seekDeltaText by viewModel.seekDeltaText.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val isQuickSettingsOpen by viewModel.isQuickSettingsOpen.collectAsState()
    val playerSettings by viewModel.playerSettings.collectAsState()

    // Gemini Subtitles & Auto-Dubbing state
    val currentSubtitleLine by viewModel.currentSubtitleLine.collectAsState()
    val isSubtitlesVisible by viewModel.isSubtitlesVisible.collectAsState()
    val isAutoDubbingVoiceEnabled by viewModel.isAutoDubbingVoiceEnabled.collectAsState()
    val isGeneratingSubtitles by viewModel.isGeneratingSubtitles.collectAsState()
    val selectedDubLanguage by viewModel.selectedDubLanguage.collectAsState()
    val activeSubtitles by viewModel.activeSubtitles.collectAsState()
    val availableDubLanguages = viewModel.availableDubLanguages

    var showDubbingStudioSheet by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current

    // Reset gesture HUD after 1.5 seconds of inactivity
    LaunchedEffect(activeGesture) {
        if (activeGesture != GestureType.NONE) {
            delay(1500)
            viewModel.resetGesture()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isScreenLocked) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isScreenLocked) {
                            viewModel.togglePlayPause()
                        }
                    },
                    onTap = {
                        viewModel.toggleControlsVisibility()
                    }
                )
            }
            .pointerInput(isScreenLocked) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (!isScreenLocked) {
                        // Pinch to zoom
                        if (zoom != 1.0f) {
                            viewModel.onPinchZoom(zoom)
                        } else {
                            // Drag gestures: left half brightness, right half volume, horizontal seek
                            if (kotlin.math.abs(pan.x) > kotlin.math.abs(pan.y) * 1.5f) {
                                viewModel.onSeekGesture(pan.x / 500f)
                            } else {
                                // Vertical pan
                                val delta = -pan.y / 400f
                                if (pan.y != 0f) {
                                    if (pan.x < 0) {
                                        viewModel.onBrightnessGesture(delta)
                                    } else {
                                        viewModel.onVolumeGesture(delta)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Simulated Video Canvas with Dolphin / Cinematic Scene (matching Screenshot 1 & 8)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(zoomScale)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1B2A),
                            Color(0xFF1B263B),
                            Color(0xFF415A77)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MxCyan.copy(alpha = 0.8f),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = video.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "${video.resolution.name.replace("_", " ")} • 60 FPS • $hwAcc Hybrid Engine",
                    color = MxCyan,
                    fontSize = 13.sp
                )
            }

            // Real-time Gemini Subtitles & Auto-Dubbing Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Auto-Dubbing Status Badge
                if (isAutoDubbingVoiceEnabled) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00C853).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔊 Gemini Auto-Dub: ${selectedDubLanguage.name}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Generating Indicator
                if (isGeneratingSubtitles) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = MxCyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✨ Gemini AI translating into ${selectedDubLanguage.name}...",
                                color = MxCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (isSubtitlesVisible && currentSubtitleLine.isNotBlank()) {
                    // Subtitle Line Overlay
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = currentSubtitleLine,
                            color = Color(0xFFFFEB3B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Gesture HUD Indicators
        AnimatedVisibility(
            visible = activeGesture != GestureType.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (activeGesture) {
                        GestureType.BRIGHTNESS -> {
                            Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = MxCyan, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Brightness: ${(brightness * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        GestureType.VOLUME -> {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MxCyan, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Volume: $volume% ${if (volume > 100) "(Boost 200%)" else ""}",
                                color = if (volume > 100) Color(0xFFFF5252) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        GestureType.SEEK -> {
                            Icon(Icons.Default.FastForward, contentDescription = null, tint = MxCyan, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Seek: $seekDeltaText", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        GestureType.NONE -> {}
                    }
                }
            }
        }

        // Lock Button Overlay (Always visible on left edge if locked or controls visible)
        if (isScreenLocked || isControlsVisible) {
            IconButton(
                onClick = { viewModel.toggleScreenLock() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .testTag("screen_lock_button")
            ) {
                Icon(
                    imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Screen Lock",
                    tint = if (isScreenLocked) MxCyan else Color.White
                )
            }
        }

        // Controls Overlay (Hidden when locked)
        AnimatedVisibility(
            visible = isControlsVisible && !isScreenLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP BAR (Screenshots 1 & 8)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { viewModel.closePlayer() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Cast Icon
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Cast, contentDescription = "Cast", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // HW/HW+ Decoder badge toggle
                        Button(
                            onClick = { viewModel.toggleHwAcceleration() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(hwAcc, color = MxCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Audio Track
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MusicNote, contentDescription = "Audio Track", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Gemini Subtitles & Auto-Dubbing studio trigger
                        IconButton(
                            onClick = { showDubbingStudioSheet = true },
                            modifier = Modifier.testTag("gemini_subtitles_button")
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = "Gemini Subtitles & Dubbing", tint = MxCyan, modifier = Modifier.size(22.dp))
                        }

                        // Quick Settings Drawer Trigger (Screenshot 7)
                        IconButton(
                            onClick = { viewModel.toggleQuickSettingsSheet() },
                            modifier = Modifier.testTag("player_settings_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }

                // DEDICATED AI SUBTITLES & AUTO-DUB UI CONTROL (Overlay Strip)
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .align(Alignment.CenterHorizontally)
                        .testTag("ai_subtitles_overlay_control_strip"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522).copy(alpha = 0.90f)),
                    border = BorderStroke(1.dp, MxCyan.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Language Selector Dropdown
                        Box {
                            OutlinedButton(
                                onClick = { showLanguageDropdown = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("overlay_language_selector_button")
                            ) {
                                Text(
                                    text = "${selectedDubLanguage.flagEmoji} ${selectedDubLanguage.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MxCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Language",
                                    tint = MxCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false },
                                modifier = Modifier.background(Color(0xFF181C2A))
                            ) {
                                availableDubLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${lang.flagEmoji} ${lang.name}",
                                                color = if (selectedDubLanguage.code == lang.code) MxCyan else Color.White,
                                                fontWeight = if (selectedDubLanguage.code == lang.code) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            showLanguageDropdown = false
                                            viewModel.loadSubtitlesForCurrentVideo(lang)
                                        },
                                        modifier = Modifier.testTag("lang_menu_item_${lang.code}")
                                    )
                                }
                            }
                        }

                        // "Generate AI Subtitles" Trigger Button
                        Button(
                            onClick = { viewModel.loadSubtitlesForCurrentVideo(selectedDubLanguage) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MxBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("overlay_generate_ai_subtitles_button")
                        ) {
                            if (isGeneratingSubtitles) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generating...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate AI Subtitles", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // AI Dubbed Audio Voiceover Toggle
                        FilledTonalButton(
                            onClick = { viewModel.toggleAutoDubVoice() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isAutoDubbingVoiceEnabled) MxGreenSend else Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("overlay_toggle_ai_dub_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isAutoDubbingVoiceEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAutoDubbingVoiceEnabled) "Dub: ON" else "Dub: OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Open Studio Modal Sheet
                        IconButton(
                            onClick = { showDubbingStudioSheet = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("overlay_open_studio_button")
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Studio Settings",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. CENTER FLOATING ACTION PILLS (Screenshot 2 & 8)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Equalizer Pill
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Speed Pill
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        val nextSpeed = when (playerSettings.playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            2.0f -> 0.75f
                                            else -> 1.0f
                                        }
                                        viewModel.setPlaybackSpeed(nextSpeed)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${playerSettings.playbackSpeed}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // AI Auto-Dub Quick Pill
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isAutoDubbingVoiceEnabled) MxGreenSend else Color.White.copy(alpha = 0.15f))
                                    .clickable { viewModel.toggleAutoDubVoice() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isAutoDubbingVoiceEnabled) "Dub ON" else "Dub OFF",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Video Cutter Tool
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCut, contentDescription = "Video Cutter", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Screen Rotation Lock
                            IconButton(
                                onClick = { viewModel.updatePlayerSetting { copy(screenRotation = !screenRotation) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = "Rotation", tint = if (playerSettings.screenRotation) MxCyan else Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // 3. BOTTOM TIMELINE & PLAYBACK CONTROLS (Screenshot 1 & 8)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Seekbar with elapsed time and duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.formatTime(currentPositionSec),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Slider(
                            value = if (totalDurationSec > 0) currentPositionSec.toFloat() / totalDurationSec.toFloat() else 0f,
                            onValueChange = { fraction -> viewModel.seekToFraction(fraction) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                                .testTag("player_timeline_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MxBlue,
                                activeTrackColor = MxBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Text(
                            text = viewModel.formatTime(totalDurationSec),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Bottom Controls Row: 10s Rewind, Prev, Play/Pause, Next, 10s Forward, Aspect Ratio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 10s Rewind (Screenshot 1)
                        IconButton(onClick = { viewModel.seekBy(-10) }) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Previous
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Play / Pause FAB
                        FloatingActionButton(
                            onClick = { viewModel.togglePlayPause() },
                            containerColor = MxBlue,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("player_play_pause_fab")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Next
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // 10s Forward (Screenshot 1)
                        IconButton(onClick = { viewModel.seekBy(10) }) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Fullscreen / Aspect Ratio Expand Toggle
                        IconButton(
                            onClick = { viewModel.cycleAspectRatio() },
                            modifier = Modifier.testTag("aspect_ratio_button")
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = aspectRatio.label, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // 4. QUICK SETTINGS SIDE SHEET (Screenshot 7)
        AnimatedVisibility(
            visible = isQuickSettingsOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            PlayerSettingsSideSheet(
                settings = playerSettings,
                onUpdate = { update -> viewModel.updatePlayerSetting(update) },
                onDismiss = { viewModel.toggleQuickSettingsSheet() }
            )
        }

        // 5. GOOGLE GEMINI SUBTITLE & AUTO-DUB STUDIO MODAL SHEET
        if (showDubbingStudioSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDubbingStudioSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("gemini_dubbing_studio_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MxCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Gemini Subtitle & Auto-Dub", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { showDubbingStudioSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Language Selector Chips
                    Text("Select Dubbing & Subtitle Language", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableDubLanguages) { lang ->
                            FilterChip(
                                selected = selectedDubLanguage.code == lang.code,
                                onClick = { viewModel.loadSubtitlesForCurrentVideo(lang) },
                                label = { Text("${lang.flagEmoji} ${lang.name}", fontSize = 12.sp) },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Controls Row: Auto-Dub Voiceover & Subtitles visibility
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MxBlue)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("AI Auto-Dub Voiceover", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Speak dialogue aloud via Text-to-Speech", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = isAutoDubbingVoiceEnabled,
                                    onCheckedChange = { viewModel.toggleAutoDubVoice() },
                                    modifier = Modifier.testTag("autodub_switch")
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = MxBlue)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Display Subtitles on Screen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Synchronized multilingual text", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = isSubtitlesVisible,
                                    onCheckedChange = { viewModel.toggleSubtitleVisibility() },
                                    modifier = Modifier.testTag("subtitles_visibility_switch")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cues Preview List
                    Text("Synchronized Dialogue Cues (${activeSubtitles.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(activeSubtitles) { cue ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cue.speaker, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MxBlue)
                                        Text("${cue.startSec}s - ${cue.endSec}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(cue.translatedText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(cue.originalText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.loadSubtitlesForCurrentVideo(selectedDubLanguage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("regenerate_gemini_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MxBlue)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Regenerate Subtitles via Gemini AI")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// Side Sheet with Checkboxes (Screenshot 7)
@Composable
fun PlayerSettingsSideSheet(
    settings: com.example.model.PlayerQuickSettings,
    onUpdate: (com.example.model.PlayerQuickSettings.() -> com.example.model.PlayerQuickSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .padding(vertical = 16.dp, horizontal = 12.dp)
            .testTag("quick_settings_sheet"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141824).copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // Two-column checkbox items matching Screenshot 7
            val items = listOf(
                "Screen Rotation" to settings.screenRotation,
                "Playback Speed" to (settings.playbackSpeed != 1.0f),
                "Background Play" to settings.backgroundPlay,
                "Loop" to settings.loop,
                "Mute" to settings.mute,
                "Shuffle" to settings.shuffle,
                "Equalizer" to settings.equalizer,
                "Audio Effect" to settings.audioEffect,
                "Sleep Timer" to (settings.sleepTimerMinutes > 0),
                "A - B Repeat" to settings.aToBRepeat,
                "Night Mode" to settings.nightMode,
                "Customise Items" to false,
                "Screenshot" to settings.screenshot,
                "Mirror Mode" to settings.mirrorMode,
                "Vertical Flip" to settings.verticalFlip
            )

            val chunked = items.chunked(2)
            chunked.forEach { rowPairs ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowPairs.forEach { (label, isChecked) ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onUpdate {
                                        when (label) {
                                            "Screen Rotation" -> copy(screenRotation = !screenRotation)
                                            "Background Play" -> copy(backgroundPlay = !backgroundPlay)
                                            "Mute" -> copy(mute = !mute)
                                            "Loop" -> copy(loop = !loop)
                                            "Shuffle" -> copy(shuffle = !shuffle)
                                            "Night Mode" -> copy(nightMode = !nightMode)
                                            "Mirror Mode" -> copy(mirrorMode = !mirrorMode)
                                            "Vertical Flip" -> copy(verticalFlip = !verticalFlip)
                                            "A - B Repeat" -> copy(aToBRepeat = !aToBRepeat)
                                            else -> this
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MxCyan,
                                    uncheckedColor = Color.White.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                    if (rowPairs.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
