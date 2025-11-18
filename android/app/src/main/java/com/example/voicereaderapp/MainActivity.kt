package com.example.voicereaderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicereaderapp.domain.model.ThemeMode
import com.example.voicereaderapp.ui.index.IndexScreen
import com.example.voicereaderapp.ui.index.IndexWrapper
import com.example.voicereaderapp.ui.settings.SettingsViewModel
import com.example.voicereaderapp.ui.theme.VoiceReaderAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity of the Voice Reader AI application.
 * Serves as the entry point and hosts the main navigation.
 * Annotated with @AndroidEntryPoint to enable Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()

            // Determine if dark theme should be used
            val useDarkTheme = when (settingsState.settings.theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            VoiceReaderAppTheme(darkTheme = useDarkTheme) {
                IndexWrapper()
            }
        }
    }
}
