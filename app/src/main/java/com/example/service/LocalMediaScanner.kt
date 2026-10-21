package com.example.service

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.AudioItem
import com.example.model.ResolutionBadge
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "LocalMediaScanner"

class LocalMediaScanner(private val context: Context) {

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val videoGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            val audioGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            videoGranted || audioGranted
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    suspend fun scanLocalVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        if (!hasStoragePermission()) {
            Log.w(TAG, "Storage permission not granted, skipping MediaStore video scan")
            return@withContext videoList
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "Video_$id"
                    val durationMs = it.getLong(durationColumn)
                    val sizeBytes = it.getLong(sizeColumn)
                    val width = it.getInt(widthColumn)
                    val bucketName = it.getString(bucketColumn) ?: "Local Videos"

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val durationSec = (durationMs / 1000).toInt()
                    val resolution = when {
                        width >= 3840 -> ResolutionBadge.ULTRA_4K
                        width >= 1920 -> ResolutionBadge.FULL_HD
                        width >= 1280 -> ResolutionBadge.HD
                        else -> ResolutionBadge.FULL_HD
                    }

                    val format = name.substringAfterLast('.', "MP4").uppercase()

                    videoList.add(
                        VideoItem(
                            id = "local_vid_$id",
                            title = name.substringBeforeLast('.'),
                            folderName = bucketName,
                            durationText = formatSeconds(durationSec),
                            durationSeconds = durationSec.coerceAtLeast(1),
                            resolution = resolution,
                            sizeText = formatBytes(sizeBytes),
                            progressPercent = 0f,
                            episodeBadge = format,
                            dateText = "On Device",
                            url = contentUri.toString(),
                            isLocal = true,
                            format = format
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore videos", e)
        }
        videoList
    }

    suspend fun scanLocalAudios(): List<AudioItem> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioItem>()
        if (!hasStoragePermission()) {
            Log.w(TAG, "Storage permission not granted, skipping MediaStore audio scan")
            return@withContext audioList
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "Audio_$id"
                    val title = it.getString(titleColumn) ?: name.substringBeforeLast('.')
                    val artist = it.getString(artistColumn) ?: "Local Artist"
                    val album = it.getString(albumColumn) ?: "Internal Storage"
                    val durationMs = it.getLong(durationColumn)
                    val sizeBytes = it.getLong(sizeColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val durationSec = (durationMs / 1000).toInt()
                    val format = name.substringAfterLast('.', "MP3").uppercase()

                    audioList.add(
                        AudioItem(
                            id = "local_aud_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationText = formatSeconds(durationSec),
                            durationSeconds = durationSec.coerceAtLeast(1),
                            sizeText = formatBytes(sizeBytes),
                            uri = contentUri.toString(),
                            dateText = "Device Audio",
                            format = format
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore audio", e)
        }
        audioList
    }

    suspend fun importVideoFromUri(uri: Uri): VideoItem? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Imported Video"
            var fileSize = 0L

            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        fileName = it.getString(nameIdx) ?: fileName
                    }
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1) {
                        fileSize = it.getLong(sizeIdx)
                    }
                }
            }

            var durationSec = 120
            var resolution = ResolutionBadge.FULL_HD

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (durStr != null) {
                    durationSec = (durStr.toLong() / 1000).toInt()
                }
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val width = widthStr?.toIntOrNull() ?: 1920
                resolution = when {
                    width >= 3840 -> ResolutionBadge.ULTRA_4K
                    width >= 1920 -> ResolutionBadge.FULL_HD
                    width >= 1280 -> ResolutionBadge.HD
                    else -> ResolutionBadge.FULL_HD
                }
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract metadata with MediaMetadataRetriever", e)
            }

            val format = fileName.substringAfterLast('.', "MP4").uppercase()
            val uniqueId = "imported_vid_${System.currentTimeMillis()}_${(100..999).random()}"
            VideoItem(
                id = uniqueId,
                title = fileName.substringBeforeLast('.'),
                folderName = "Imported",
                durationText = formatSeconds(durationSec),
                durationSeconds = durationSec.coerceAtLeast(1),
                resolution = resolution,
                sizeText = if (fileSize > 0) formatBytes(fileSize) else "Local File",
                progressPercent = 0f,
                episodeBadge = format,
                dateText = "Imported",
                url = uri.toString(),
                isLocal = true,
                format = format
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed importing video from Uri: $uri", e)
            null
        }
    }

    suspend fun importAudioFromUri(uri: Uri): AudioItem? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Imported Audio"
            var fileSize = 0L

            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        fileName = it.getString(nameIdx) ?: fileName
                    }
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1) {
                        fileSize = it.getLong(sizeIdx)
                    }
                }
            }

            var durationSec = 180
            var artist = "Local Artist"
            var album = "Imported Music"

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (durStr != null) {
                    durationSec = (durStr.toLong() / 1000).toInt()
                }
                val artStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                if (!artStr.isNullOrBlank()) artist = artStr
                val albStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                if (!albStr.isNullOrBlank()) album = albStr
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract audio metadata with MediaMetadataRetriever", e)
            }

            val format = fileName.substringAfterLast('.', "MP3").uppercase()
            val uniqueId = "imported_aud_${System.currentTimeMillis()}_${(100..999).random()}"
            AudioItem(
                id = uniqueId,
                title = fileName.substringBeforeLast('.'),
                artist = artist,
                album = album,
                durationText = formatSeconds(durationSec),
                durationSeconds = durationSec.coerceAtLeast(1),
                sizeText = if (fileSize > 0) formatBytes(fileSize) else "Local Audio",
                uri = uri.toString(),
                dateText = "Imported",
                format = format
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed importing audio from Uri: $uri", e)
            null
        }
    }

    suspend fun importMediaFromUri(uri: Uri): Pair<VideoItem?, AudioItem?> = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        if (mimeType.startsWith("audio/")) {
            val audio = importAudioFromUri(uri)
            Pair(null, audio)
        } else {
            val video = importVideoFromUri(uri)
            if (video != null) {
                Pair(video, null)
            } else {
                val audio = importAudioFromUri(uri)
                Pair(null, audio)
            }
        }
    }

    fun createMediaFromUrl(url: String, customTitle: String? = null): VideoItem {
        val cleanUrl = url.trim()
        val guessedName = cleanUrl.substringAfterLast('/').substringBefore('?').takeIf { it.isNotEmpty() } ?: "Network Stream"
        val title = customTitle?.takeIf { it.isNotBlank() } ?: guessedName.substringBeforeLast('.')
        val ext = guessedName.substringAfterLast('.', "MP4").uppercase()
        val isHls = cleanUrl.contains(".m3u8") || ext == "M3U8"
        val format = if (isHls) "HLS" else ext
        return VideoItem(
            id = "stream_${System.currentTimeMillis()}_${(100..999).random()}",
            title = title,
            folderName = "Network Streams",
            durationText = if (isHls) "LIVE" else "Online",
            durationSeconds = 600,
            resolution = ResolutionBadge.FULL_HD,
            sizeText = "Online Stream",
            progressPercent = 0f,
            episodeBadge = format,
            dateText = "Network",
            url = cleanUrl,
            isLocal = false,
            format = format
        )
    }

    private fun formatSeconds(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}
