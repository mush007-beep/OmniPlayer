package com.example

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioItem
import com.example.model.VideoItem
import com.example.service.LocalMediaScanner
import com.example.viewmodel.PlayerViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalMediaImportTest {

    private lateinit var app: Application
    private lateinit var scanner: LocalMediaScanner
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        scanner = LocalMediaScanner(app)
        viewModel = PlayerViewModel(app)
    }

    @Test
    fun testRequiredPermissionsIncludeModernAndLegacyStorage() {
        val permissions = scanner.getRequiredPermissions()
        assertTrue("Permissions should not be empty", permissions.isNotEmpty())
        // On SDK 34, should request READ_MEDIA_VIDEO and READ_MEDIA_AUDIO
        val containsMediaVideo = permissions.any { it.contains("READ_MEDIA_VIDEO") }
        val containsMediaAudio = permissions.any { it.contains("READ_MEDIA_AUDIO") }
        assertTrue("Should request READ_MEDIA_VIDEO on API 34", containsMediaVideo)
        assertTrue("Should request READ_MEDIA_AUDIO on API 34", containsMediaAudio)
    }

    @Test
    fun testStoragePermissionGrantedTriggersScan() {
        viewModel.onStoragePermissionResult(true)
        assertTrue(viewModel.hasStoragePermission.value)
    }

    @Test
    fun testStoragePermissionDeniedSetsMessage() {
        viewModel.onStoragePermissionResult(false)
        assertFalse(viewModel.hasStoragePermission.value)
        assertNotNull(viewModel.importStatusMessage.value)
        assertTrue(viewModel.importStatusMessage.value!!.contains("permission"))
    }

    @Test
    fun testAudioPlaybackSelection() {
        val testAudio = AudioItem(
            id = "aud_custom_1",
            title = "Summer Breeze Acoustic",
            artist = "Local Artist",
            album = "My Music",
            durationText = "04:15",
            durationSeconds = 255,
            sizeText = "6.1 MB"
        )
        viewModel.playAudio(testAudio)
        assertEquals("aud_custom_1", viewModel.currentAudio.value?.id)
        assertTrue(viewModel.isAudioPlaying.value)

        viewModel.toggleAudioPlayback()
        assertFalse(viewModel.isAudioPlaying.value)
    }
}
