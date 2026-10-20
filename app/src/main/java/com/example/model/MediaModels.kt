package com.example.model

enum class MediaType {
    VIDEO, AUDIO
}

enum class ResolutionBadge {
    ULTRA_8K, ULTRA_4K, FULL_HD, HD
}

data class VideoItem(
    val id: String,
    val title: String,
    val folderName: String,
    val durationText: String,
    val durationSeconds: Int,
    val resolution: ResolutionBadge = ResolutionBadge.FULL_HD,
    val sizeText: String,
    val progressPercent: Float = 0f,
    val episodeBadge: String? = null,
    val dateText: String = "Today",
    val url: String = "",
    val subtitles: List<String> = listOf("English [Auto-dub]", "Español", "हिन्दी")
)

data class FolderItem(
    val name: String,
    val count: Int,
    val newCount: Int = 0,
    val isProtected: Boolean = false
)

data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val episodeInfo: String,
    val dateInfo: String,
    val progress: Float,
    val duration: String
)

enum class ViewMode {
    ALL_FOLDERS, FOLDERS, FILES
}

enum class LayoutMode {
    LIST, GRID
}

enum class SortBy {
    TITLE, DATE, PLAYED_TIME, STATUS, LENGTH, SIZE, RESOLUTION
}

enum class SortDirection {
    A_TO_Z, Z_TO_A
}

data class PlayerQuickSettings(
    val screenRotation: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val backgroundPlay: Boolean = true,
    val loop: Boolean = false,
    val mute: Boolean = false,
    val shuffle: Boolean = false,
    val equalizer: Boolean = true,
    val audioEffect: Boolean = false,
    val sleepTimerMinutes: Int = 0,
    val aToBRepeat: Boolean = false,
    val nightMode: Boolean = false,
    val screenshot: Boolean = false,
    val mirrorMode: Boolean = false,
    val verticalFlip: Boolean = false
)

data class EqualizerSettings(
    val bassBoost: Float = 0.6f,
    val surround3d: Float = 0.4f,
    val volumeBoostPercent: Int = 120, // 100% to 200%
    val preset: String = "Bio-Acoustic Balanced",
    val bands: List<Float> = listOf(0.4f, 0.6f, 0.5f, 0.7f, 0.8f)
)

data class MxShareTransfer(
    val sentMb: Int = 108,
    val totalMb: Int = 3600,
    val timeLeftSeconds: Int = 120,
    val completedFiles: Int = 3,
    val totalFiles: Int = 14,
    val senderName: String = "ME",
    val receiverName: String = "L58041",
    val isTransferring: Boolean = true
)

data class SubtitleCue(
    val startSec: Int,
    val endSec: Int,
    val originalText: String,
    val translatedText: String,
    val speaker: String = "Narrator"
)

data class AutoDubLanguage(
    val code: String,
    val name: String,
    val flagEmoji: String
)
