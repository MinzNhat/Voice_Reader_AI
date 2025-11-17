package com.example.voicereaderapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Speechify-inspired Dark Color Scheme
private val SpeechifyDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4A9EFF),      // Speechify blue
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF3A7ED9),
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,

    secondary = androidx.compose.ui.graphics.Color(0xFF4A9EFF),
    onSecondary = androidx.compose.ui.graphics.Color.White,

    tertiary = androidx.compose.ui.graphics.Color(0xFF6B9FFF),
    onTertiary = androidx.compose.ui.graphics.Color.White,

    background = androidx.compose.ui.graphics.Color(0xFF0A0A0A),   // Deep black
    onBackground = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

    surface = androidx.compose.ui.graphics.Color(0xFF1A1A1A),      // Dark surface
    onSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2A),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB0B0B0),

    error = androidx.compose.ui.graphics.Color(0xFFFF4444),
    onError = androidx.compose.ui.graphics.Color.White
)

private val DarkColorScheme = SpeechifyDarkColorScheme

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun VoiceReaderAppTheme(
    darkTheme: Boolean = true,  // Always use dark theme (Speechify-style)
    // Dynamic color disabled for consistent branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use Speechify dark theme
    val colorScheme = SpeechifyDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}