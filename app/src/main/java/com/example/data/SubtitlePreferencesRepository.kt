package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.subtitleDataStore: DataStore<Preferences> by preferencesDataStore(name = "subtitle_dubbing_settings")

data class SubtitleSettings(
    val languageCode: String = "ur",
    val isSubtitlesVisible: Boolean = true,
    val isAutoDubbingVoiceEnabled: Boolean = true,
    val fontSizeSp: Int = 14,
    val textColorName: String = "Yellow"
)

class SubtitlePreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DUB_LANGUAGE_CODE = stringPreferencesKey("dub_language_code")
        val SUBTITLES_VISIBLE = booleanPreferencesKey("subtitles_visible")
        val AUTO_DUB_VOICE_ENABLED = booleanPreferencesKey("auto_dub_voice_enabled")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val TEXT_COLOR_NAME = stringPreferencesKey("text_color_name")
    }

    val subtitleSettingsFlow: Flow<SubtitleSettings> = context.subtitleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val langCode = preferences[PreferencesKeys.DUB_LANGUAGE_CODE] ?: "ur"
            val subsVisible = preferences[PreferencesKeys.SUBTITLES_VISIBLE] ?: true
            val dubVoice = preferences[PreferencesKeys.AUTO_DUB_VOICE_ENABLED] ?: true
            val fontSize = preferences[PreferencesKeys.FONT_SIZE_SP] ?: 14
            val textColor = preferences[PreferencesKeys.TEXT_COLOR_NAME] ?: "Yellow"
            SubtitleSettings(
                languageCode = langCode,
                isSubtitlesVisible = subsVisible,
                isAutoDubbingVoiceEnabled = dubVoice,
                fontSizeSp = fontSize,
                textColorName = textColor
            )
        }

    suspend fun saveDubLanguage(code: String) {
        context.subtitleDataStore.edit { preferences ->
            preferences[PreferencesKeys.DUB_LANGUAGE_CODE] = code
        }
    }

    suspend fun saveSubtitlesVisible(visible: Boolean) {
        context.subtitleDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLES_VISIBLE] = visible
        }
    }

    suspend fun saveAutoDubVoiceEnabled(enabled: Boolean) {
        context.subtitleDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DUB_VOICE_ENABLED] = enabled
        }
    }

    suspend fun saveSubtitleFontSize(fontSizeSp: Int) {
        context.subtitleDataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE_SP] = fontSizeSp
        }
    }

    suspend fun saveSubtitleTextColor(colorName: String) {
        context.subtitleDataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_COLOR_NAME] = colorName
        }
    }
}
