package com.example.voicereaderapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereaderapp.R
import com.example.voicereaderapp.domain.model.TTSLanguage
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender

/**
 * PDF Reader Settings Sheet
 * Appears as a modal bottom sheet in PDFViewerScreen
 * Contains Speed, Languages, and Voices settings specific to reading
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFReaderSettingsSheet(
    speed: Float,
    selectedVoice: String,
    selectedLanguage: String = "en-US",
    onSpeedChange: (Float) -> Unit,
    onVoiceChange: (voiceId: String, language: String) -> Unit,
    onDismiss: () -> Unit,
    // Global enforcement settings
    isSpeedEnforced: Boolean = false,
    enforcedSpeed: Float = 1.0f,
    isVoiceEnforced: Boolean = false
) {
    var currentLanguage by remember {
        mutableStateOf(TTSLanguage.fromCode(selectedLanguage))
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 0.75f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reader_settings_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Divider(modifier = Modifier.padding(bottom = 24.dp))

            // ==================== SPEED SECTION ====================
            Text(
                text = stringResource(R.string.playback_speed),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Show warning if speed is enforced
            if (isSpeedEnforced) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Speed Set Globally",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This document uses the main speed (${String.format("%.1f", enforcedSpeed)}x) from Settings. Disable \"Use Main Speed for All Documents\" to change per document.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "0.5x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${String.format("%.1f", if (isSpeedEnforced) enforcedSpeed else speed)}x",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "2.0x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Slider(
                value = if (isSpeedEnforced) enforcedSpeed else speed,
                onValueChange = { if (!isSpeedEnforced) onSpeedChange(it) },
                valueRange = 0.5f..2.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                enabled = !isSpeedEnforced
            )

            // ==================== LANGUAGE SECTION ====================
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TTSLanguage.values().forEach { language ->
                    val languageName = when (language) {
                        TTSLanguage.KOREAN -> stringResource(R.string.language_korean)
                        TTSLanguage.ENGLISH -> stringResource(R.string.language_english)
                    }
                    FilterChip(
                        selected = currentLanguage == language,
                        onClick = { if (!isVoiceEnforced) currentLanguage = language },
                        label = { Text(languageName) },
                        modifier = Modifier.weight(1f),
                        enabled = !isVoiceEnforced
                    )
                }
            }

            // ==================== VOICE SECTION ====================
            // Show warning if voice is enforced
            if (isVoiceEnforced) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Voice Set Globally",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This document uses the main voice from Settings. Disable \"Use Main Voice for All Documents\" to change per document.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.voice),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val voices = TTSVoice.getVoicesForLanguage(currentLanguage)
            val femaleVoices = voices.filter { it.gender == VoiceGender.FEMALE }
            val maleVoices = voices.filter { it.gender == VoiceGender.MALE }

            // Female voices
            if (femaleVoices.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.voice_female),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                femaleVoices.forEach { voice ->
                    VoiceOptionItem(
                        voice = voice,
                        isSelected = voice.id == selectedVoice,
                        onClick = {
                            if (!isVoiceEnforced) {
                                onVoiceChange(voice.id, voice.language.code)
                            }
                        },
                        enabled = !isVoiceEnforced
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Male voices
            if (maleVoices.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.voice_male),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                maleVoices.forEach { voice ->
                    VoiceOptionItem(
                        voice = voice,
                        isSelected = voice.id == selectedVoice,
                        onClick = {
                            if (!isVoiceEnforced) {
                                onVoiceChange(voice.id, voice.language.code)
                            }
                        },
                        enabled = !isVoiceEnforced
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceOptionItem(
    voice: TTSVoice,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else if (enabled)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = voice.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected)
                        FontWeight.Bold
                    else
                        FontWeight.Normal
                ),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.weight(1f))

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
