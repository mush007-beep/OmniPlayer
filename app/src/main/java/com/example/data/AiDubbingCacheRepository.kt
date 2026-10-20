package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.SubtitleCue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "AiDubbingCache"

data class CachedDubbingEntry(
    val videoId: String,
    val videoTitle: String,
    val languageCode: String,
    val cachedAtMillis: Long,
    val cues: List<SubtitleCue>
)

class AiDubbingCacheRepository(private val context: Context) {

    private val memoryCache = ConcurrentHashMap<String, CachedDubbingEntry>()
    private val cacheDir: File by lazy {
        File(context.filesDir, "ai_dub_cache").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    init {
        loadDiskCacheIntoMemory()
    }

    private fun buildKey(videoId: String, languageCode: String): String {
        val sanitizedId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "${sanitizedId}_$languageCode"
    }

    private fun loadDiskCacheIntoMemory() {
        try {
            val files = cacheDir.listFiles { file -> file.isFile && file.extension == "json" } ?: return
            for (file in files) {
                try {
                    val content = file.readText()
                    val json = JSONObject(content)
                    val entry = parseJsonToEntry(json)
                    if (entry != null) {
                        val key = buildKey(entry.videoId, entry.languageCode)
                        memoryCache[key] = entry
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed reading cache file ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing disk cache", e)
        }
    }

    fun isCached(videoId: String, languageCode: String, videoTitle: String = ""): Boolean {
        val key = buildKey(videoId, languageCode)
        if (memoryCache.containsKey(key)) return true
        if (videoTitle.isNotBlank()) {
            val titleKey = buildKey(videoTitle, languageCode)
            if (memoryCache.containsKey(titleKey)) return true
        }
        val file = File(cacheDir, "$key.json")
        return file.exists() && file.length() > 0
    }

    fun getCachedCues(videoId: String, languageCode: String, videoTitle: String = ""): List<SubtitleCue>? {
        val key = buildKey(videoId, languageCode)
        val inMemory = memoryCache[key]
        if (inMemory != null) {
            return inMemory.cues
        }

        if (videoTitle.isNotBlank()) {
            val titleKey = buildKey(videoTitle, languageCode)
            val titleInMemory = memoryCache[titleKey]
            if (titleInMemory != null) {
                return titleInMemory.cues
            }
        }

        // Try reading from disk
        val file = File(cacheDir, "$key.json")
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText())
                val entry = parseJsonToEntry(json)
                if (entry != null) {
                    memoryCache[key] = entry
                    return entry.cues
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing disk cache file", e)
            }
        }
        return null
    }

    fun putCues(
        videoId: String,
        videoTitle: String,
        languageCode: String,
        cues: List<SubtitleCue>
    ) {
        val key = buildKey(videoId, languageCode)
        val entry = CachedDubbingEntry(
            videoId = videoId,
            videoTitle = videoTitle,
            languageCode = languageCode,
            cachedAtMillis = System.currentTimeMillis(),
            cues = cues
        )
        memoryCache[key] = entry

        try {
            val file = File(cacheDir, "$key.json")
            val json = entryToJson(entry)
            file.writeText(json.toString())
            Log.d(TAG, "Cached AI dubbing for $videoId ($languageCode) with ${cues.size} cues.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist AI dubbing cache to disk", e)
        }
    }

    fun invalidate(videoId: String, languageCode: String) {
        val key = buildKey(videoId, languageCode)
        memoryCache.remove(key)
        try {
            val file = File(cacheDir, "$key.json")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting cache file for $key", e)
        }
    }

    fun clearAll() {
        memoryCache.clear()
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed clearing cache directory", e)
        }
    }

    fun getCachedEntriesCount(): Int = memoryCache.size

    fun getCachedEntry(videoId: String, languageCode: String): CachedDubbingEntry? {
        val key = buildKey(videoId, languageCode)
        return memoryCache[key]
    }

    private fun entryToJson(entry: CachedDubbingEntry): JSONObject {
        val obj = JSONObject()
        obj.put("videoId", entry.videoId)
        obj.put("videoTitle", entry.videoTitle)
        obj.put("languageCode", entry.languageCode)
        obj.put("cachedAtMillis", entry.cachedAtMillis)

        val array = JSONArray()
        for (cue in entry.cues) {
            val cueObj = JSONObject().apply {
                put("startSec", cue.startSec)
                put("endSec", cue.endSec)
                put("originalText", cue.originalText)
                put("translatedText", cue.translatedText)
                put("speaker", cue.speaker)
            }
            array.put(cueObj)
        }
        obj.put("cues", array)
        return obj
    }

    private fun parseJsonToEntry(obj: JSONObject): CachedDubbingEntry? {
        val videoId = obj.optString("videoId", "")
        val videoTitle = obj.optString("videoTitle", "")
        val languageCode = obj.optString("languageCode", "")
        val cachedAtMillis = obj.optLong("cachedAtMillis", System.currentTimeMillis())
        val cuesArray = obj.optJSONArray("cues") ?: return null

        val cues = mutableListOf<SubtitleCue>()
        for (i in 0 until cuesArray.length()) {
            val cueObj = cuesArray.getJSONObject(i)
            cues.add(
                SubtitleCue(
                    startSec = cueObj.getInt("startSec"),
                    endSec = cueObj.getInt("endSec"),
                    originalText = cueObj.getString("originalText"),
                    translatedText = cueObj.getString("translatedText"),
                    speaker = cueObj.optString("speaker", "Narrator")
                )
            )
        }

        return CachedDubbingEntry(
            videoId = videoId,
            videoTitle = videoTitle,
            languageCode = languageCode,
            cachedAtMillis = cachedAtMillis,
            cues = cues
        )
    }
}
