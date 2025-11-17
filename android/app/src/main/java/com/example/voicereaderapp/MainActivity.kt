package com.example.voicereaderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.voicereaderapp.ui.index.IndexScreen
import com.example.voicereaderapp.ui.index.IndexWrapper
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
            VoiceReaderAppTheme {
                IndexWrapper()
            }
        }
    }
}
