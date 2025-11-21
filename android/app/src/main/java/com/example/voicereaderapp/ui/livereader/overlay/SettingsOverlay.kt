package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.voicereaderapp.R
import com.example.voicereaderapp.domain.model.TTSLanguage
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender
import com.example.voicereaderapp.domain.model.VoiceSettings

/**
 * Settings Overlay for Live Scan
 * Shows voice and speed settings similar to PDFReaderSettingsSheet but in a smaller overlay
 */
@Composable
fun SettingsOverlay(
    currentSettings: VoiceSettings,
    onSpeedChange: (Float) -> Unit,
    onVoiceChange: (voiceId: String, language: String) -> Unit,
    onClose: () -> Unit,
    useMainVoiceForAll: Boolean = false,
    useMainSpeedForAll: Boolean = false
) {
    var currentLanguage by remember {
        mutableStateOf(TTSLanguage.fromCode(currentSettings.language))
    }
    var selectedVoiceId by remember { mutableStateOf(currentSettings.voiceId) }
    var selectedSpeed by remember { mutableStateOf(currentSettings.speed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .zIndex(1000f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reading_settings),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
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

                Divider(modifier = Modifier.padding(bottom = 16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ==================== SPEED SECTION ====================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.playback_speed),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        if (useMainSpeedForAll) {
                            Text(
                                text = stringResource(R.string.locked_by_global),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                            text = "${String.format("%.1f", selectedSpeed)}x",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
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
                        value = selectedSpeed,
                        onValueChange = {
                            if (!useMainSpeedForAll) {
                                selectedSpeed = it
                                onSpeedChange(it)
                            }
                        },
                        valueRange = 0.5f..2.0f,
                        enabled = !useMainSpeedForAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )

                    // ==================== LANGUAGE SECTION ====================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        if (useMainVoiceForAll) {
                            Text(
                                text = stringResource(R.string.locked),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TTSLanguage.values().forEach { language ->
                            FilterChip(
                                selected = currentLanguage == language,
                                onClick = {
                                    if (!useMainVoiceForAll) {
                                        currentLanguage = language
                                        // Auto-select default voice for the language
                                        val defaultVoice = TTSVoice.getDefaultVoiceForLanguage(language)
                                        selectedVoiceId = defaultVoice.id
                                        onVoiceChange(defaultVoice.id, language.code)
                                    }
                                },
                                label = { Text(language.displayName) },
                                enabled = !useMainVoiceForAll,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ==================== VOICE SECTION ====================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.voice),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        if (useMainVoiceForAll) {
                            Text(
                                text = stringResource(R.string.locked),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

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
                                isSelected = voice.id == selectedVoiceId,
                                onClick = {
                                    if (!useMainVoiceForAll) {
                                        selectedVoiceId = voice.id
                                        onVoiceChange(voice.id, voice.language.code)
                                    }
                                },
                                enabled = !useMainVoiceForAll
                            )
                        }
                        Spacer(Modifier.height(12.dp))
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
                                isSelected = voice.id == selectedVoiceId,
                                onClick = {
                                    if (!useMainVoiceForAll) {
                                        selectedVoiceId = voice.id
                                        onVoiceChange(voice.id, voice.language.code)
                                    }
                                },
                                enabled = !useMainVoiceForAll
                            )
                        }
                    }
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
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.15f else 0.08f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.3f),
        onClick = { if (enabled) onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = voice.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
            )

            Spacer(Modifier.weight(1f))

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
