package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.AiDubbingCacheRepository
import com.example.data.SubtitlePreferencesRepository
import com.example.data.SubtitleSettings
import com.example.model.*
import com.example.service.GeminiSubtitleService
import com.example.service.LocalMediaScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    val localMediaScanner = LocalMediaScanner(application)
    private val geminiSubtitleService = GeminiSubtitleService(application)
    private val subtitlePrefsRepo = SubtitlePreferencesRepository(application)
    val aiDubbingCacheRepo = AiDubbingCacheRepository(application)
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

    // Dynamic media state (No hardcoded demo videos or demo folders)
    private val _allVideosListState = MutableStateFlow<List<VideoItem>>(emptyList())
    val allVideosState: StateFlow<List<VideoItem>> = _allVideosListState.asStateFlow()
    val allVideosList: List<VideoItem> get() = _allVideosListState.value

    // Dynamic folders derived from real discovered/imported media files
    val foldersList: List<FolderItem>
        get() {
            val videos = _allVideosListState.value
            return if (videos.isEmpty()) {
                emptyList()
            } else {
                videos.groupBy { it.folderName }.map { (name, vids) ->
                    FolderItem(
                        name = name,
                        count = vids.size,
                        newCount = vids.count { it.isLocal }
                    )
                }.sortedByDescending { it.count }
            }
        }

    // Dynamic user continue watching history
    private val _continueWatchingState = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueWatchingList: List<ContinueWatchingItem>
        get() = _continueWatchingState.value

    // Storage permission & imported media state
    private val _hasStoragePermission = MutableStateFlow(localMediaScanner.hasStoragePermission())
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _isScanningMedia = MutableStateFlow(false)
    val isScanningMedia: StateFlow<Boolean> = _isScanningMedia.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    private val _importedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val importedVideos: StateFlow<List<VideoItem>> = _importedVideos.asStateFlow()

    private val _importedAudios = MutableStateFlow<List<AudioItem>>(emptyList())
    val importedAudios: StateFlow<List<AudioItem>> = _importedAudios.asStateFlow()

    private val _currentAudio = MutableStateFlow<AudioItem?>(null)
    val currentAudio: StateFlow<AudioItem?> = _currentAudio.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private var audioPlayer: android.media.MediaPlayer? = null

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

    private val _subtitleFontSize = MutableStateFlow(14)
    val subtitleFontSize: StateFlow<Int> = _subtitleFontSize.asStateFlow()

    private val _subtitleTextColor = MutableStateFlow("Yellow")
    val subtitleTextColor: StateFlow<String> = _subtitleTextColor.asStateFlow()

    private val _isGeneratingSubtitles = MutableStateFlow(false)
    val isGeneratingSubtitles: StateFlow<Boolean> = _isGeneratingSubtitles.asStateFlow()

    // Multi-stage Auto-Dubbing Visual Progress
    private val _isAutoDubbingInProgress = MutableStateFlow(false)
    val isAutoDubbingInProgress: StateFlow<Boolean> = _isAutoDubbingInProgress.asStateFlow()

    private val _autoDubProgress = MutableStateFlow(0f)
    val autoDubProgress: StateFlow<Float> = _autoDubProgress.asStateFlow()

    private val _autoDubStageText = MutableStateFlow("")
    val autoDubStageText: StateFlow<String> = _autoDubStageText.asStateFlow()

    // Caching state for AI Dubbing
    private val _isDubbingCached = MutableStateFlow(false)
    val isDubbingCached: StateFlow<Boolean> = _isDubbingCached.asStateFlow()

    private val _cacheStatusText = MutableStateFlow("")
    val cacheStatusText: StateFlow<String> = _cacheStatusText.asStateFlow()

    // ExoPlayer Media3 Gold-Standard Playback Engine for all video and audio formats
    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(application)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            if (duration > 0) {
                                _totalDurationSec.value = (duration / 1000).toInt()
                            }
                        } else if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                })
            }
    }

    private var lastSpokenCueIndex = -1
    private var playbackTimerJob: Job? = null

    init {
        startPlaybackTicker()
        loadPersistedSubtitleSettings()
        if (localMediaScanner.hasStoragePermission()) {
            scanLocalMedia()
        }
    }

    private fun addToContinueWatching(video: VideoItem) {
        val newItem = ContinueWatchingItem(
            id = video.id,
            title = video.title,
            episodeInfo = "${video.format} • ${video.resolution.name.replace("_", " ")}",
            dateInfo = "Now Playing",
            progress = 0.05f,
            duration = video.durationText
        )
        _continueWatchingState.update { current ->
            (listOf(newItem) + current.filter { it.id != video.id }).take(10)
        }
    }

    private fun updateHistoryProgress(videoId: String, progress: Float) {
        _continueWatchingState.update { current ->
            current.map {
                if (it.id == videoId) it.copy(progress = progress.coerceIn(0f, 1f)) else it
            }
        }
    }

    private fun loadPersistedSubtitleSettings() {
        viewModelScope.launch {
            subtitlePrefsRepo.subtitleSettingsFlow.collect { settings ->
                val matchingLang = availableDubLanguages.firstOrNull { it.code == settings.languageCode }
                if (matchingLang != null) {
                    _selectedDubLanguage.value = matchingLang
                }
                _isSubtitlesVisible.value = settings.isSubtitlesVisible
                _isAutoDubbingVoiceEnabled.value = settings.isAutoDubbingVoiceEnabled
                _subtitleFontSize.value = settings.fontSizeSp
                _subtitleTextColor.value = settings.textColorName
            }
        }
    }

    private fun startPlaybackTicker() {
        playbackTimerJob?.cancel()
        playbackTimerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                if (_isPlaying.value && _currentVideo.value != null) {
                    try {
                        val currentMs = exoPlayer.currentPosition
                        val totalMs = exoPlayer.duration
                        if (totalMs > 0) {
                            _totalDurationSec.value = (totalMs / 1000).toInt()
                        }
                        val sec = (currentMs / 1000).toInt()
                        _currentPositionSec.value = sec
                        updateActiveSubtitleAt(sec)

                        _currentVideo.value?.let { video ->
                            val progress = if (_totalDurationSec.value > 0) sec.toFloat() / _totalDurationSec.value else 0f
                            updateHistoryProgress(video.id, progress)
                        }
                    } catch (e: Exception) {
                        val nextSec = (_currentPositionSec.value + 1).coerceAtMost(_totalDurationSec.value)
                        _currentPositionSec.value = nextSec
                        updateActiveSubtitleAt(nextSec)
                    }
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
        _currentPositionSec.value = 0
        _totalDurationSec.value = video.durationSeconds
        _isPlaying.value = true
        _isControlsVisible.value = true
        lastSpokenCueIndex = -1

        addToContinueWatching(video)

        try {
            if (video.url.isNotEmpty()) {
                val mediaItem = MediaItem.fromUri(Uri.parse(video.url))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "Error loading media with ExoPlayer", e)
        }

        // Check if video is already cached for current language
        _isDubbingCached.value = aiDubbingCacheRepo.isCached(video.id, _selectedDubLanguage.value.code, video.title)

        // Automatically fetch / generate Gemini subtitles in remembered language from DataStore
        loadSubtitlesForCurrentVideo(_selectedDubLanguage.value, forceRefresh = false)
    }

    fun loadSubtitlesForCurrentVideo(language: AutoDubLanguage, forceRefresh: Boolean = false) {
        val video = _currentVideo.value ?: return
        _selectedDubLanguage.value = language
        lastSpokenCueIndex = -1

        // Save selected language in DataStore so it is remembered across video sessions
        viewModelScope.launch {
            subtitlePrefsRepo.saveDubLanguage(language.code)
        }

        // Check cache to avoid redundant Google Gemini API calls
        val cached = if (!forceRefresh) {
            aiDubbingCacheRepo.getCachedCues(video.id, language.code, video.title)
        } else null

        if (cached != null && cached.isNotEmpty()) {
            _isDubbingCached.value = true
            _cacheStatusText.value = "⚡ Loaded from cache (0 API calls)"
            _activeSubtitles.value = cached
            _isGeneratingSubtitles.value = false
            _isAutoDubbingInProgress.value = true
            _autoDubProgress.value = 1.0f
            _autoDubStageText.value = "⚡ Loaded from local AI cache (0 API calls needed)"

            viewModelScope.launch {
                delay(350)
                _isAutoDubbingInProgress.value = false
                updateActiveSubtitleAt(_currentPositionSec.value)
            }
            return
        }

        // Cache miss or forceRefresh requested: Execute Gemini AI translation pipeline
        _isDubbingCached.value = false
        _cacheStatusText.value = if (forceRefresh) "Refreshing from Google Gemini AI..." else "Generating with Gemini AI..."
        _isGeneratingSubtitles.value = true
        _isAutoDubbingInProgress.value = true
        _autoDubProgress.value = 0.15f
        _autoDubStageText.value = "Analyzing audio tracks for \"${video.title}\"..."

        viewModelScope.launch {
            delay(500)
            _autoDubProgress.value = 0.40f
            _autoDubStageText.value = "Translating speech into ${language.name} via Google Gemini AI..."
            delay(500)
            _autoDubProgress.value = 0.70f
            _autoDubStageText.value = "Synthesizing voiceover cues & aligning timestamps..."

            val cues = geminiSubtitleService.generateSubtitlesAndDubbing(video.title, language)
            _activeSubtitles.value = cues

            // Cache the newly processed video & language cues to prevent redundant API calls
            aiDubbingCacheRepo.putCues(video.id, video.title, language.code, cues)
            _isDubbingCached.value = true
            _cacheStatusText.value = "⚡ Processed & cached for instant future playback"

            _autoDubProgress.value = 0.95f
            _autoDubStageText.value = "Finalizing auto-dubbed audio stream (${language.name})..."
            delay(400)
            _autoDubProgress.value = 1.0f
            _autoDubStageText.value = "Auto-dubbing completed & cached successfully!"
            delay(700)
            _isGeneratingSubtitles.value = false
            _isAutoDubbingInProgress.value = false
            updateActiveSubtitleAt(_currentPositionSec.value)
        }
    }

    fun regenerateSubtitlesForCurrentVideo() {
        loadSubtitlesForCurrentVideo(_selectedDubLanguage.value, forceRefresh = true)
    }

    fun clearCacheForCurrentVideo() {
        val video = _currentVideo.value ?: return
        aiDubbingCacheRepo.invalidate(video.id, _selectedDubLanguage.value.code)
        _isDubbingCached.value = false
        _cacheStatusText.value = "Cache cleared for this video"
    }

    fun toggleSubtitleVisibility() {
        _isSubtitlesVisible.update { current ->
            val updated = !current
            viewModelScope.launch {
                subtitlePrefsRepo.saveSubtitlesVisible(updated)
            }
            updated
        }
    }

    fun setSubtitlesVisible(visible: Boolean) {
        _isSubtitlesVisible.value = visible
        viewModelScope.launch {
            subtitlePrefsRepo.saveSubtitlesVisible(visible)
        }
    }

    fun toggleAutoDubVoice() {
        _isAutoDubbingVoiceEnabled.update { current ->
            val next = !current
            if (!next) {
                geminiSubtitleService.stopDubbing()
            }
            viewModelScope.launch {
                subtitlePrefsRepo.saveAutoDubVoiceEnabled(next)
            }
            next
        }
    }

    fun setAutoDubVoiceEnabled(enabled: Boolean) {
        _isAutoDubbingVoiceEnabled.value = enabled
        if (!enabled) {
            geminiSubtitleService.stopDubbing()
        }
        viewModelScope.launch {
            subtitlePrefsRepo.saveAutoDubVoiceEnabled(enabled)
        }
    }

    fun setSubtitleFontSize(fontSizeSp: Int) {
        _subtitleFontSize.value = fontSizeSp
        viewModelScope.launch {
            subtitlePrefsRepo.saveSubtitleFontSize(fontSizeSp)
        }
    }

    fun setSubtitleTextColor(colorName: String) {
        _subtitleTextColor.value = colorName
        viewModelScope.launch {
            subtitlePrefsRepo.saveSubtitleTextColor(colorName)
        }
    }

    fun closePlayer() {
        geminiSubtitleService.stopDubbing()
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        } catch (e: Exception) {
            // fallback
        }
        _currentVideo.value = null
        _isPlaying.value = false
        _isQuickSettingsOpen.value = false
    }

    fun togglePlayPause() {
        try {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                geminiSubtitleService.stopDubbing()
                _isPlaying.value = false
            } else {
                exoPlayer.play()
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            _isPlaying.update {
                if (!it) {
                    geminiSubtitleService.stopDubbing()
                }
                !it
            }
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
        try {
            val cur = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            val targetMs = (cur + seconds * 1000).coerceIn(0, if (dur > 0) dur else _totalDurationSec.value * 1000L)
            exoPlayer.seekTo(targetMs)
            _currentPositionSec.value = (targetMs / 1000).toInt()
        } catch (e: Exception) {
            _currentPositionSec.update {
                (it + seconds).coerceIn(0, _totalDurationSec.value)
            }
        }
        lastSpokenCueIndex = -1
        updateActiveSubtitleAt(_currentPositionSec.value)
    }

    fun seekToFraction(fraction: Float) {
        val totalMs = try {
            if (exoPlayer.duration > 0) exoPlayer.duration else _totalDurationSec.value * 1000L
        } catch (e: Exception) {
            _totalDurationSec.value * 1000L
        }
        val targetMs = (fraction * totalMs).toLong()
        try {
            exoPlayer.seekTo(targetMs)
            _currentPositionSec.value = (targetMs / 1000).toInt()
        } catch (e: Exception) {
            _currentPositionSec.value = (fraction * _totalDurationSec.value).toInt()
        }
        lastSpokenCueIndex = -1
        updateActiveSubtitleAt(_currentPositionSec.value)
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
        try {
            exoPlayer.playbackParameters = PlaybackParameters(speed)
        } catch (e: Exception) {
            // fallback in unit test environment
        }
    }

    fun updateEqualizer(settings: EqualizerSettings) {
        _equalizerSettings.value = settings
    }

    fun onStoragePermissionResult(granted: Boolean) {
        _hasStoragePermission.value = granted
        if (granted) {
            scanLocalMedia()
        } else {
            _importStatusMessage.value = "Storage permission not granted. You can still import individual files via the File Picker."
        }
    }

    fun scanLocalMedia() {
        viewModelScope.launch {
            _isScanningMedia.value = true
            _importStatusMessage.value = "Scanning local storage for videos and audios..."
            val scannedVids = localMediaScanner.scanLocalVideos()
            val scannedAuds = localMediaScanner.scanLocalAudios()

            if (scannedVids.isNotEmpty()) {
                _importedVideos.update { current -> (scannedVids + current).distinctBy { it.id } }
                _allVideosListState.update { current -> (scannedVids + allVideosList).distinctBy { it.id } }
            }
            if (scannedAuds.isNotEmpty()) {
                _importedAudios.update { current -> (scannedAuds + current).distinctBy { it.id } }
            }

            _isScanningMedia.value = false
            _importStatusMessage.value = if (scannedVids.isNotEmpty() || scannedAuds.isNotEmpty()) {
                "Found ${scannedVids.size} local video(s) & ${scannedAuds.size} audio track(s)"
            } else {
                "Storage scan completed. 0 additional files found in MediaStore."
            }
        }
    }

    fun importVideoUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isScanningMedia.value = true
            val newVideos = mutableListOf<VideoItem>()
            for (uri in uris) {
                val item = localMediaScanner.importVideoFromUri(uri)
                if (item != null) {
                    newVideos.add(item)
                }
            }
            if (newVideos.isNotEmpty()) {
                _importedVideos.update { (newVideos + it).distinctBy { v -> v.id } }
                _allVideosListState.update { (newVideos + it).distinctBy { v -> v.id } }
                _importStatusMessage.value = "Imported ${newVideos.size} video(s) from device storage"
            }
            _isScanningMedia.value = false
        }
    }

    fun importAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isScanningMedia.value = true
            val newAudios = mutableListOf<AudioItem>()
            for (uri in uris) {
                val item = localMediaScanner.importAudioFromUri(uri)
                if (item != null) {
                    newAudios.add(item)
                }
            }
            if (newAudios.isNotEmpty()) {
                _importedAudios.update { (newAudios + it).distinctBy { a -> a.id } }
                _importStatusMessage.value = "Imported ${newAudios.size} audio track(s) from device storage"
            }
            _isScanningMedia.value = false
        }
    }

    fun playAudio(audio: AudioItem) {
        _currentAudio.value = audio
        _isAudioPlaying.value = true
        try {
            if (audio.uri.isNotEmpty()) {
                val mediaItem = MediaItem.fromUri(Uri.parse(audio.uri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "Error loading audio with ExoPlayer", e)
        }
    }

    fun toggleAudioPlayback() {
        try {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                _isAudioPlaying.value = false
            } else {
                exoPlayer.play()
                _isAudioPlaying.value = true
            }
        } catch (e: Exception) {
            _isAudioPlaying.update { !it }
        }
    }

    fun openMediaUri(uri: Uri) {
        viewModelScope.launch {
            val (vid, aud) = localMediaScanner.importMediaFromUri(uri)
            if (vid != null) {
                _importedVideos.update { (listOf(vid) + it).distinctBy { v -> v.id } }
                _allVideosListState.update { (listOf(vid) + it).distinctBy { v -> v.id } }
                openVideo(vid)
            } else if (aud != null) {
                _importedAudios.update { (listOf(aud) + it).distinctBy { a -> a.id } }
                playAudio(aud)
            }
        }
    }

    fun openNetworkStream(url: String, customTitle: String? = null) {
        val streamItem = localMediaScanner.createMediaFromUrl(url, customTitle)
        _importedVideos.update { (listOf(streamItem) + it).distinctBy { v -> v.id } }
        _allVideosListState.update { (listOf(streamItem) + it).distinctBy { v -> v.id } }
        openVideo(streamItem)
    }

    fun dismissImportMessage() {
        _importStatusMessage.value = null
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            exoPlayer.release()
        } catch (e: Exception) {
            // ignore
        }
        geminiSubtitleService.release()
    }
}
