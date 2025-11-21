package com.example.voicereaderapp.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.voicereaderapp.domain.model.LiveScanBarStyle
import com.example.voicereaderapp.domain.model.ThemeMode
import com.example.voicereaderapp.domain.model.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property to create DataStore instance for preferences.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_settings")

/**
 * Manager class for handling voice settings persistence using DataStore.
 * Provides methods to read and write voice configuration preferences.
 *
 * @property context Application context
 */
@Singleton
class VoiceSettingsPreferences @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val VOICE_ID_KEY = stringPreferencesKey("voice_id")
        private val SPEED_KEY = floatPreferencesKey("speed")
        private val PITCH_KEY = floatPreferencesKey("pitch")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val USE_MAIN_VOICE_FOR_ALL_KEY = booleanPreferencesKey("use_main_voice_for_all")
        private val MAIN_VOICE_ID_KEY = stringPreferencesKey("main_voice_id")
        private val USE_MAIN_SPEED_FOR_ALL_KEY = booleanPreferencesKey("use_main_speed_for_all")
        private val MAIN_SPEED_KEY = floatPreferencesKey("main_speed")
        private val LIVE_SCAN_BAR_STYLE_KEY = stringPreferencesKey("live_scan_bar_style")
    }

    /**
     * Retrieves voice settings as a Flow.
     * Emits default values if no preferences are saved.
     *
     * @return Flow emitting VoiceSettings
     */
    fun getVoiceSettings(): Flow<VoiceSettings> {
        return dataStore.data.map { preferences ->
            val themeString = preferences[THEME_KEY] ?: "SYSTEM"
            val theme = try {
                ThemeMode.valueOf(themeString)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }

            val liveScanBarStyleString = preferences[LIVE_SCAN_BAR_STYLE_KEY] ?: "EDGE_BAR"
            val liveScanBarStyle = try {
                LiveScanBarStyle.valueOf(liveScanBarStyleString)
            } catch (e: IllegalArgumentException) {
                LiveScanBarStyle.EDGE_BAR
            }

            VoiceSettings(
                voiceId = preferences[VOICE_ID_KEY] ?: "matt",
                speed = preferences[SPEED_KEY] ?: 1.0f,
                pitch = preferences[PITCH_KEY] ?: 1.0f,
                language = preferences[LANGUAGE_KEY] ?: "en-US",
                theme = theme,
                useMainVoiceForAll = preferences[USE_MAIN_VOICE_FOR_ALL_KEY] ?: false,
                mainVoiceId = preferences[MAIN_VOICE_ID_KEY] ?: "matt",
                useMainSpeedForAll = preferences[USE_MAIN_SPEED_FOR_ALL_KEY] ?: false,
                mainSpeed = preferences[MAIN_SPEED_KEY] ?: 1.0f,
                liveScanBarStyle = liveScanBarStyle
            )
        }
    }

    /**
     * Saves voice settings to DataStore.
     *
     * @param settings VoiceSettings to be persisted
     */
    suspend fun saveVoiceSettings(settings: VoiceSettings) {
        dataStore.edit { preferences ->
            preferences[VOICE_ID_KEY] = settings.voiceId
            preferences[SPEED_KEY] = settings.speed
            preferences[PITCH_KEY] = settings.pitch
            preferences[LANGUAGE_KEY] = settings.language
            preferences[THEME_KEY] = settings.theme.name
            preferences[USE_MAIN_VOICE_FOR_ALL_KEY] = settings.useMainVoiceForAll
            preferences[MAIN_VOICE_ID_KEY] = settings.mainVoiceId
            preferences[USE_MAIN_SPEED_FOR_ALL_KEY] = settings.useMainSpeedForAll
            preferences[MAIN_SPEED_KEY] = settings.mainSpeed
            preferences[LIVE_SCAN_BAR_STYLE_KEY] = settings.liveScanBarStyle.name
        }
    }

    /**
     * Updates only the reading speed.
     *
     * @param speed New speed value
     */
    suspend fun updateSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[SPEED_KEY] = speed
        }
    }

    /**
     * Updates only the voice pitch.
     *
     * @param pitch New pitch value
     */
    suspend fun updatePitch(pitch: Float) {
        dataStore.edit { preferences ->
            preferences[PITCH_KEY] = pitch
        }
    }
}
