package com.example.voicereaderapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.voicereaderapp.ui.pdfreader.SpeakerSelectionDialog

/**
 * Unified Reader Screen Component
 *
 * Provides a consistent reading interface for TEXT, PDF, and IMAGE modes.
 * This component handles the UI layer only - all business logic remains in the respective ViewModels.
 *
 * @param title Document title (supports marquee scrolling for long names)
 * @param mode Reading mode (TEXT, PDF, or IMAGE)
 * @param content Composable content area (provided by parent screen based on mode)
 * @param progress Playback progress (0.0 to 1.0)
 * @param isPlaying Whether audio is currently playing
 * @param isLoading Whether content is loading
 * @param playbackSpeed Current playback speed
 * @param selectedVoice Currently selected voice ID
 * @param selectedLanguage Currently selected language code
 * @param onPlayPause Play/pause button callback
 * @param onRewind Rewind button callback
 * @param onForward Forward button callback
 * @param onSeek Seek slider callback (0.0 to 1.0)
 * @param onSpeedChange Speed change callback
 * @param onVoiceChange Voice and language selection callback
 * @param onBack Back button callback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedReaderScreen(
    title: String,
    mode: ReaderMode,
    content: @Composable () -> Unit,
    progress: Float,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    playbackSpeed: Float,
    selectedVoice: String,
    selectedLanguage: String = "en-US",
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVoiceChange: (voiceId: String, language: String) -> Unit,
    onTakeNote: () -> Unit = {},  // Added for Take Note feature
    onShowNotes: () -> Unit = {},  // Added for Notes button
    onChat: () -> Unit,
    onBack: () -> Unit,
    // Global settings enforcement
    isSpeedEnforced: Boolean = false,
    enforcedSpeed: Float = 1.0f,
    isVoiceEnforced: Boolean = false
) {
    var showReaderSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ==========================
        // BACKGROUND
        // ==========================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        )

        // =============================
        // SPEECHIFY-STYLE TOP BAR
        // =============================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp)
        ) {

            // BACK BUTTON (left)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onChat,
                modifier = Modifier
                    .align(Alignment.CenterStart) // Align left
                    .padding(start = 68.dp)       // 16dp (start) + 44dp (back btn size) + 8dp (spacing) = 68dp
                    .size(44.dp)
                    .zIndex(10f)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome, // Icon ✨
                    contentDescription = "Chat AI",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // TITLE (center) with Marquee
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 2000,
                        initialDelayMillis = 2000,
                        velocity = 30.dp
                    )
                )
            }

            // NOTES BUTTON (right, before settings)
            IconButton(
                onClick = onShowNotes,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 68.dp)  // Leave space for settings button
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = "Notes",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 1.0f)
                )
            }

            // SETTINGS BUTTON (right) - Opens PDFReaderSettingsSheet
            IconButton(
                onClick = {
                    showReaderSettings = true
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ==========================
        // CONTENT BOX (rounded corners)
        // ==========================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp)
            ) {

                // Content area (provided by parent)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        content()
                    }
                }

                // ==========================
                // SEEK SLIDER
                // ==========================
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0xFF9CA3AF)
                    )
                )

                Spacer(Modifier.height(2.dp))

                // ==========================
                // PLAYBACK CONTROLS
                // ==========================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // REWIND
                    IconButton(
                        onClick = onRewind,
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Rewind",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(30.dp))

                    // PLAY / PAUSE
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(Modifier.width(30.dp))

                    // FORWARD
                    IconButton(
                        onClick = onForward,
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Forward",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // ==========================
        // PDF READER SETTINGS SHEET
        // ==========================
        if (showReaderSettings) {
            com.example.voicereaderapp.ui.settings.PDFReaderSettingsSheet(
                speed = playbackSpeed,
                selectedVoice = selectedVoice,
                selectedLanguage = selectedLanguage,
                onSpeedChange = onSpeedChange,
                onVoiceChange = onVoiceChange,
                onDismiss = { showReaderSettings = false },
                // Global settings enforcement
                isSpeedEnforced = isSpeedEnforced,
                enforcedSpeed = enforcedSpeed,
                isVoiceEnforced = isVoiceEnforced
            )
        }
    }
}
