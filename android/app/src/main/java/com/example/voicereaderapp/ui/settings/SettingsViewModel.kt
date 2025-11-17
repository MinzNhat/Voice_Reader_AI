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
     * Updates the selected voice.
     *
     * @param voiceId Voice identifier (e.g., "matt", "sarah", "emma")
     */
    fun updateVoice(voiceId: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(voiceId = voiceId)
        )
        // Auto-save when voice changes
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
