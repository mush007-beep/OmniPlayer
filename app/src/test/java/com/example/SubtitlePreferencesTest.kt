package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.data.SubtitlePreferencesRepository
import com.example.data.subtitleDataStore
import com.example.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubtitlePreferencesTest {

    private lateinit var context: Context
    private lateinit var repository: SubtitlePreferencesRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.subtitleDataStore.edit { it.clear() }
        repository = SubtitlePreferencesRepository(context)
    }

    @Test
    fun testDefaultSubtitlePreferences() = runBlocking {
        val settings = repository.subtitleSettingsFlow.first()
        assertEquals("ur", settings.languageCode)
        assertTrue(settings.isSubtitlesVisible)
        assertTrue(settings.isAutoDubbingVoiceEnabled)
        assertEquals(14, settings.fontSizeSp)
        assertEquals("Yellow", settings.textColorName)
    }

    @Test
    fun testSaveAndRetrieveDubLanguage() = runBlocking {
        repository.saveDubLanguage("hi")
        val settings = repository.subtitleSettingsFlow.first()
        assertEquals("hi", settings.languageCode)

        repository.saveDubLanguage("es")
        val updatedSettings = repository.subtitleSettingsFlow.first()
        assertEquals("es", updatedSettings.languageCode)
    }

    @Test
    fun testSaveAndRetrieveSubtitleSettings() = runBlocking {
        repository.saveSubtitlesVisible(false)
        repository.saveAutoDubVoiceEnabled(false)
        repository.saveSubtitleFontSize(18)
        repository.saveSubtitleTextColor("Cyan")

        val settings = repository.subtitleSettingsFlow.first()
        assertFalse(settings.isSubtitlesVisible)
        assertFalse(settings.isAutoDubbingVoiceEnabled)
        assertEquals(18, settings.fontSizeSp)
        assertEquals("Cyan", settings.textColorName)
    }

    @Test
    fun testPlayerViewModelInitializesWithPersistentSettings() = runBlocking {
        repository.saveDubLanguage("ar")
        repository.saveSubtitleFontSize(18)

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = PlayerViewModel(app)

        var retries = 40
        while (retries > 0 && (viewModel.selectedDubLanguage.value.code != "ar" || viewModel.subtitleFontSize.value != 18)) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(50)
            retries--
        }
        ShadowLooper.idleMainLooper()

        assertEquals("ar", viewModel.selectedDubLanguage.value.code)
        assertEquals(18, viewModel.subtitleFontSize.value)
    }
}
