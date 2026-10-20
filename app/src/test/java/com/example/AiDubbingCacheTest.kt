package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AiDubbingCacheRepository
import com.example.model.AutoDubLanguage
import com.example.model.SubtitleCue
import com.example.model.VideoItem
import com.example.viewmodel.PlayerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiDubbingCacheTest {

    private lateinit var context: Context
    private lateinit var cacheRepository: AiDubbingCacheRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cacheRepository = AiDubbingCacheRepository(context)
        cacheRepository.clearAll()
    }

    @Test
    fun testCachePutAndRetrieveCues() {
        assertFalse(cacheRepository.isCached("vid_101", "ur"))

        val mockCues = listOf(
            SubtitleCue(0, 5, "Hello world", "ہیلو ورلڈ", "Narrator"),
            SubtitleCue(6, 12, "AI Dubbing test", "اے آئی ڈبنگ ٹیسٹ", "Gemini")
        )

        cacheRepository.putCues("vid_101", "4K Ocean Wildlife", "ur", mockCues)

        assertTrue(cacheRepository.isCached("vid_101", "ur"))

        val retrieved = cacheRepository.getCachedCues("vid_101", "ur")
        assertNotNull(retrieved)
        assertEquals(2, retrieved!!.size)
        assertEquals("ہیلو ورلڈ", retrieved[0].translatedText)
        assertEquals("اے آئی ڈبنگ ٹیسٹ", retrieved[1].translatedText)
    }

    @Test
    fun testCachePersistenceAcrossRepositoryInstances() {
        val testCues = listOf(
            SubtitleCue(0, 8, "Persist test", "پرسیست ٹیسٹ", "Speaker 1")
        )
        cacheRepository.putCues("vid_persistent", "Dolphin 4K", "es", testCues)

        // Instantiate new repository to simulate app restart
        val newRepoInstance = AiDubbingCacheRepository(context)
        assertTrue(newRepoInstance.isCached("vid_persistent", "es"))

        val loadedCues = newRepoInstance.getCachedCues("vid_persistent", "es")
        assertNotNull(loadedCues)
        assertEquals(1, loadedCues!!.size)
        assertEquals("پرسیست ٹیسٹ", loadedCues[0].translatedText)
    }

    @Test
    fun testCacheInvalidateAndClearAll() {
        val cues = listOf(SubtitleCue(0, 4, "A", "الف", "S1"))
        cacheRepository.putCues("vid_delete", "Title", "hi", cues)
        assertTrue(cacheRepository.isCached("vid_delete", "hi"))

        cacheRepository.invalidate("vid_delete", "hi")
        assertFalse(cacheRepository.isCached("vid_delete", "hi"))
        assertNull(cacheRepository.getCachedCues("vid_delete", "hi"))
    }

    @Test
    fun testPlayerViewModelUsesCacheToPreventRedundantApiCalls() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = PlayerViewModel(app)
        val testVideo = VideoItem(
            id = "vid_coral_reef",
            title = "Coral Reef 4K",
            folderName = "Nature",
            durationText = "03:15",
            durationSeconds = 195,
            sizeText = "450 MB"
        )

        val targetLang = AutoDubLanguage("ur", "Urdu (اردو)", "🇵🇰")

        // Pre-populate cache for this video
        val cachedCues = listOf(
            SubtitleCue(0, 10, "Original voice", "پیشگی محفوظ شدہ ڈبنگ آواز", "اے آئی راوی")
        )
        viewModel.aiDubbingCacheRepo.putCues(testVideo.id, testVideo.title, targetLang.code, cachedCues)

        // Open video in player
        viewModel.openVideo(testVideo)
        ShadowLooper.idleMainLooper()

        // Verify that the ViewModel detected the cached dubbing
        assertTrue("Dubbing should be marked as cached", viewModel.isDubbingCached.value)
        val active = viewModel.activeSubtitles.value
        assertEquals("Should load cached cues without making API calls", 1, active.size)
        assertEquals("پیشگی محفوظ شدہ ڈبنگ آواز", active[0].translatedText)

        // Test clearing cache for current video
        viewModel.clearCacheForCurrentVideo()
        assertFalse("Dubbing should no longer be cached after clearing", viewModel.isDubbingCached.value)
    }
}
