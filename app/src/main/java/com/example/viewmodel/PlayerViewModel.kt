package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.service.GeminiSubtitleService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GestureType {
    NONE, BRIGHTNESS, VOLUME, SEEK
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit to Screen"),
    STRETCH("Stretch"),
    ORIGINAL_100("100% Original"),
    CROP("Crop to Fill")
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiSubtitleService = GeminiSubtitleService(application)
    val availableDubLanguages = geminiSubtitleService.availableLanguages

    // Theme Mode
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.update { !it }
    }

    // Navigation Tab
    private val _activeTab = MutableStateFlow("Local")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    // Media Library data
    val continueWatchingList = listOf(
        ContinueWatchingItem("cw1", "Cyberpunk 4K • Neon Syndicate", "S1 E1, 08 Jan", "Watched 85%", 0.85f, "42:35"),
        ContinueWatchingItem("cw2", "Dolphin Ocean Odyssey", "4K Ultra HD", "Watched 45%", 0.45f, "05:49"),
        ContinueWatchingItem("cw3", "Mystic Realm Chronicles", "S1 E3, 15 Sep 2018", "Watched 15%", 0.15f, "28:10"),
        ContinueWatchingItem("cw4", "Bio-Acoustic Symphony", "Lossless Master", "Watched 90%", 0.90f, "04:12")
    )

    val foldersList = listOf(
        FolderItem("Download", 1, 0),
        FolderItem("Video", 8, 5),
        FolderItem("Camera", 4, 0),
        FolderItem("Movies", 12, 2),
        FolderItem("Best Hindi TV Shows", 6, 0),
        FolderItem("Family Videos", 12, 0),
        FolderItem("Movie Cut", 15, 0),
        FolderItem("Private Folder", 3, 0, isProtected = true)
    )

    val allVideosList = listOf(
        VideoItem(
            id = "v1",
            title = "Movie Cut 03-01-14",
            folderName = "Movie Cut",
            durationText = "05:49",
            durationSeconds = 349,
            resolution = ResolutionBadge.ULTRA_4K,
            sizeText = "450 MB",
            progressPercent = 0.45f,
            episodeBadge = "4K HDR"
        ),
        VideoItem(
            id = "v2",
            title = "Dolphin Ocean High Speed Leap",
            folderName = "Video",
            durationText = "42:35",
            durationSeconds = 2555,
            resolution = ResolutionBadge.ULTRA_8K,
            sizeText = "1.8 GB",
            progressPercent = 0.15f,
            episodeBadge = "8K UHD"
        ),
        VideoItem(
            id = "v3",
            title = "Cyberpunk Night Drive 60FPS",
            folderName = "Download",
            durationText = "12:20",
            durationSeconds = 740,
            resolution = ResolutionBadge.FULL_HD,
            sizeText = "320 MB",
            progressPercent = 0.80f,
            episodeBadge = "1080p"
        ),
        VideoItem(
            id = "v4",
            title = "Nature Sanctuary & Bio-Rhythm",
            folderName = "Movies",
            durationText = "24:18",
            durationSeconds = 1458,
            resolution = ResolutionBadge.FULL_HD,
            sizeText = "680 MB",
            progressPercent = 0.0f,
            episodeBadge = "FHD"
        )
    )

    // View & Sort Settings
    private val _viewMode = MutableStateFlow(ViewMode.FOLDERS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.LIST)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.TITLE)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.A_TO_Z)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }
    fun setLayoutMode(mode: LayoutMode) { _layoutMode.value = mode }
    fun setSortBy(sort: SortBy) { _sortBy.value = sort }
    fun setSortDirection(dir: SortDirection) { _sortDirection.value = dir }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // Playback Engine State
    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(5) // Start at 5s to showcase immediate subtitles
    val currentPositionSec: StateFlow<Int> = _currentPositionSec.asStateFlow()

    private val _totalDurationSec = MutableStateFlow(349)
    val totalDurationSec: StateFlow<Int> = _totalDurationSec.asStateFlow()

    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _hwAcceleration = MutableStateFlow("HW+") // HW+, HW, SW
    val hwAcceleration: StateFlow<String> = _hwAcceleration.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    // Gesture State
    private val _brightness = MutableStateFlow(0.75f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _volume = MutableStateFlow(125)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _activeGesture = MutableStateFlow(GestureType.NONE)
    val activeGesture: StateFlow<GestureType> = _activeGesture.asStateFlow()

    private val _seekDeltaText = MutableStateFlow("")
    val seekDeltaText: StateFlow<String> = _seekDeltaText.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    // Quick Settings Side Sheet
    private val _isQuickSettingsOpen = MutableStateFlow(false)
    val isQuickSettingsOpen: StateFlow<Boolean> = _isQuickSettingsOpen.asStateFlow()

    private val _playerSettings = MutableStateFlow(PlayerQuickSettings())
    val playerSettings: StateFlow<PlayerQuickSettings> = _playerSettings.asStateFlow()

    private val _equalizerSettings = MutableStateFlow(EqualizerSettings())
    val equalizerSettings: StateFlow<EqualizerSettings> = _equalizerSettings.asStateFlow()

    // MX Share State
    private val _mxShareTransfer = MutableStateFlow(MxShareTransfer())
    val mxShareTransfer: StateFlow<MxShareTransfer> = _mxShareTransfer.asStateFlow()

    // Gemini Subtitles & Auto-Dubbing State
    private val _selectedDubLanguage = MutableStateFlow(availableDubLanguages[0]) // Default: Urdu (اردو)
    val selectedDubLanguage: StateFlow<AutoDubLanguage> = _selectedDubLanguage.asStateFlow()

    private val _activeSubtitles = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val activeSubtitles: StateFlow<List<SubtitleCue>> = _activeSubtitles.asStateFlow()

    private val _currentSubtitleLine = MutableStateFlow("")
    val currentSubtitleLine: StateFlow<String> = _currentSubtitleLine.asStateFlow()

    private val _isSubtitlesVisible = MutableStateFlow(true)
    val isSubtitlesVisible: StateFlow<Boolean> = _isSubtitlesVisible.asStateFlow()

    private val _isAutoDubbingVoiceEnabled = MutableStateFlow(true) // AI Voice Dubbing
    val isAutoDubbingVoiceEnabled: StateFlow<Boolean> = _isAutoDubbingVoiceEnabled.asStateFlow()

    private val _isGeneratingSubtitles = MutableStateFlow(false)
    val isGeneratingSubtitles: StateFlow<Boolean> = _isGeneratingSubtitles.asStateFlow()

    private var lastSpokenCueIndex = -1
    private var playbackTimerJob: Job? = null

    init {
        startPlaybackTicker()
    }

    private fun startPlaybackTicker() {
        playbackTimerJob?.cancel()
        playbackTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isPlaying.value && _currentVideo.value != null) {
                    val nextSec = (_currentPositionSec.value + 1).coerceAtMost(_totalDurationSec.value)
                    _currentPositionSec.value = nextSec
                    updateActiveSubtitleAt(nextSec)
                }
            }
        }
    }

    private fun updateActiveSubtitleAt(sec: Int) {
        val cues = _activeSubtitles.value
        val currentCueIndex = cues.indexOfFirst { sec in it.startSec..it.endSec }
        if (currentCueIndex != -1) {
            val cue = cues[currentCueIndex]
            _currentSubtitleLine.value = "${cue.speaker}: ${cue.translatedText}"

            if (_isAutoDubbingVoiceEnabled.value && currentCueIndex != lastSpokenCueIndex) {
                lastSpokenCueIndex = currentCueIndex
                geminiSubtitleService.speakDubbedLine(cue.translatedText, _selectedDubLanguage.value.code)
            }
        } else {
            // Check if within bounds or clear if past cue
            if (cues.isNotEmpty() && (sec > cues.last().endSec || sec < cues.first().startSec)) {
                _currentSubtitleLine.value = ""
            }
        }
    }

    fun openVideo(video: VideoItem) {
        _currentVideo.value = video
        _currentPositionSec.value = 2
        _totalDurationSec.value = video.durationSeconds
        _isPlaying.value = true
        _isControlsVisible.value = true
        lastSpokenCueIndex = -1

        // Automatically fetch / generate Gemini subtitles in selected language
        loadSubtitlesForCurrentVideo(_selectedDubLanguage.value)
    }

    fun loadSubtitlesForCurrentVideo(language: AutoDubLanguage) {
        val video = _currentVideo.value ?: return
        _selectedDubLanguage.value = language
        _isGeneratingSubtitles.value = true
        lastSpokenCueIndex = -1

        viewModelScope.launch {
            val cues = geminiSubtitleService.generateSubtitlesAndDubbing(video.title, language)
            _activeSubtitles.value = cues
            _isGeneratingSubtitles.value = false
            updateActiveSubtitleAt(_currentPositionSec.value)
        }
    }

    fun toggleSubtitleVisibility() {
        _isSubtitlesVisible.update { !it }
    }

    fun toggleAutoDubVoice() {
        _isAutoDubbingVoiceEnabled.update {
            val next = !it
            if (!next) {
                geminiSubtitleService.stopDubbing()
            }
            next
        }
    }

    fun closePlayer() {
        geminiSubtitleService.stopDubbing()
        _currentVideo.value = null
        _isPlaying.value = false
        _isQuickSettingsOpen.value = false
    }

    fun togglePlayPause() {
        _isPlaying.update {
            if (!it) {
                geminiSubtitleService.stopDubbing()
            }
            !it
        }
    }

    fun toggleControlsVisibility() {
        if (!_isScreenLocked.value) {
            _isControlsVisible.update { !it }
        } else {
            _isControlsVisible.value = true
        }
    }

    fun toggleScreenLock() {
        _isScreenLocked.update { !it }
    }

    fun toggleHwAcceleration() {
        _hwAcceleration.update {
            when (it) {
                "HW+" -> "HW"
                "HW" -> "SW"
                else -> "HW+"
            }
        }
    }

    fun cycleAspectRatio() {
        _aspectRatio.update {
            val modes = AspectRatioMode.values()
            val nextIndex = (it.ordinal + 1) % modes.size
            modes[nextIndex]
        }
    }

    fun seekBy(seconds: Int) {
        _currentPositionSec.update {
            val target = (it + seconds).coerceIn(0, _totalDurationSec.value)
            lastSpokenCueIndex = -1
            updateActiveSubtitleAt(target)
            target
        }
    }

    fun seekToFraction(fraction: Float) {
        val target = (fraction * _totalDurationSec.value).toInt()
        _currentPositionSec.value = target
        lastSpokenCueIndex = -1
        updateActiveSubtitleAt(target)
    }

    // Gesture Handlers
    fun onBrightnessGesture(delta: Float) {
        _activeGesture.value = GestureType.BRIGHTNESS
        _brightness.update { (it + delta).coerceIn(0.05f, 1.0f) }
    }

    fun onVolumeGesture(delta: Float) {
        _activeGesture.value = GestureType.VOLUME
        _volume.update { (it + (delta * 100).toInt()).coerceIn(0, 200) }
    }

    fun onSeekGesture(deltaFraction: Float) {
        _activeGesture.value = GestureType.SEEK
        val deltaSeconds = (deltaFraction * 120).toInt()
        val targetSeconds = (_currentPositionSec.value + deltaSeconds).coerceIn(0, _totalDurationSec.value)
        _currentPositionSec.value = targetSeconds
        lastSpokenCueIndex = -1
        updateActiveSubtitleAt(targetSeconds)
        val sign = if (deltaSeconds >= 0) "+" else ""
        _seekDeltaText.value = "$sign${deltaSeconds}s (${formatTime(targetSeconds)})"
    }

    fun onPinchZoom(scaleMultiplier: Float) {
        _zoomScale.update { (it * scaleMultiplier).coerceIn(1.0f, 3.0f) }
    }

    fun resetGesture() {
        _activeGesture.value = GestureType.NONE
    }

    // Quick Settings Sheet Handlers
    fun toggleQuickSettingsSheet() {
        _isQuickSettingsOpen.update { !it }
    }

    fun updatePlayerSetting(update: PlayerQuickSettings.() -> PlayerQuickSettings) {
        _playerSettings.update { it.update() }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerSettings.update { it.copy(playbackSpeed = speed) }
    }

    fun updateEqualizer(settings: EqualizerSettings) {
        _equalizerSettings.value = settings
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onCleared() {
        super.onCleared()
        geminiSubtitleService.release()
    }
}
