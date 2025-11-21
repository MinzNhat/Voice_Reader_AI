package com.example.voicereaderapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.domain.model.VoiceSettings
import com.example.voicereaderapp.domain.usecase.GetVoiceSettingsUseCase
import com.example.voicereaderapp.domain.usecase.UpdateVoiceSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Settings screen.
 *
 * @property settings Current voice settings
 * @property isSaving Whether settings are being saved
 * @property error Error message if any
 */
data class SettingsUiState(
    val settings: VoiceSettings = VoiceSettings(),
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Settings screen.
 * Manages voice configuration settings.
 *
 * @property getVoiceSettingsUseCase Use case for retrieving settings
 * @property updateVoiceSettingsUseCase Use case for updating settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase,
    private val updateVoiceSettingsUseCase: UpdateVoiceSettingsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())

    /**
     * Current UI state of the settings screen.
     */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Loads current voice settings from repository.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            getVoiceSettingsUseCase().collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    /**
     * Updates the reading speed.
     *
     * @param speed New speed value (0.5 to 2.0)
     */
    fun updateSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(speed = speed)
        )
    }

    /**
     * Updates the voice pitch.
     *
     * @param pitch New pitch value (0.5 to 2.0)
     */
    fun updatePitch(pitch: Float) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(pitch = pitch)
        )
    }

    /**
     * Updates the selected voice and language.
     *
     * @param voiceId Voice identifier (e.g., "matt", "minseo")
     * @param language Language code (e.g., "en-US", "ko-KR")
     */
    fun updateVoiceAndLanguage(voiceId: String, language: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(
                voiceId = voiceId,
                language = language
            )
        )
        // Auto-save when voice changes
        saveSettings()
    }

    /**
     * Updates the selected voice only.
     *
     * @param voiceId Voice identifier (e.g., "matt", "minseo")
     */
    fun updateVoice(voiceId: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(voiceId = voiceId)
        )
        // Auto-save when voice changes
        saveSettings()
    }

    /**
     * Updates the language.
     *
     * @param language Language code (e.g., "en-US", "ko-KR")
     */
    fun updateLanguage(language: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(language = language)
        )
    }

    /**
     * Updates the theme mode.
     *
     * @param theme Theme mode (LIGHT, DARK, SYSTEM)
     */
    fun updateTheme(theme: com.example.voicereaderapp.domain.model.ThemeMode) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(theme = theme)
        )
        // Auto-save when theme changes
        saveSettings()
    }

    /**
     * Updates the main voice for all documents setting.
     * When enabling, sets default voice to "matt" if not already set.
     *
     * @param useMainVoice Whether to use main voice for all documents
     */
    fun updateUseMainVoiceForAll(useMainVoice: Boolean) {
        val currentSettings = _uiState.value.settings

        // When turning ON the toggle, ensure mainVoiceId is set to "matt" as default
        val updatedSettings = if (useMainVoice && currentSettings.mainVoiceId != "matt") {
            currentSettings.copy(
                useMainVoiceForAll = useMainVoice,
                mainVoiceId = "matt"  // Set default to Matt when first enabled
            )
        } else {
            currentSettings.copy(useMainVoiceForAll = useMainVoice)
        }

        _uiState.value = _uiState.value.copy(settings = updatedSettings)
        saveSettings()
    }

    /**
     * Updates the main voice ID.
     *
     * @param mainVoiceId Main voice identifier
     */
    fun updateMainVoiceId(mainVoiceId: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(mainVoiceId = mainVoiceId)
        )
        saveSettings()
    }

    /**
     * Updates the live scan bar style.
     *
     * @param style Live scan bar style (EDGE_BAR, CIRCLE_BUTTON)
     */
    fun updateLiveScanBarStyle(style: com.example.voicereaderapp.domain.model.LiveScanBarStyle) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(liveScanBarStyle = style)
        )
        saveSettings()
    }

    /**
     * Updates the main speed for all documents setting.
     *
     * @param useMainSpeed Whether to use main speed for all documents
     */
    fun updateUseMainSpeedForAll(useMainSpeed: Boolean) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(useMainSpeedForAll = useMainSpeed)
        )
        saveSettings()
    }

    /**
     * Updates the main speed value.
     *
     * @param mainSpeed Main speed value
     */
    fun updateMainSpeed(mainSpeed: Float) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(mainSpeed = mainSpeed)
        )
        saveSettings()
    }

    /**
     * Saves the current settings to repository.
     */
    fun saveSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                updateVoiceSettingsUseCase(_uiState.value.settings)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message
                )
            }
        }
    }
}
