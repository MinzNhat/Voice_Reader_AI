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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicereaderapp.domain.model.TTSLanguage
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender

/**
 * Global Settings Sheet
 * Appears as a modal bottom sheet in IndexScreen
 * Contains default Speed and Voice settings that apply to all documents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsSheet(
    speed: Float,
    selectedVoice: String,
    selectedLanguage: String = "en-US",
    selectedTheme: com.example.voicereaderapp.domain.model.ThemeMode = com.example.voicereaderapp.domain.model.ThemeMode.SYSTEM,
    onSpeedChange: (Float) -> Unit,
    onVoiceChange: (String) -> Unit,
    onVoiceAndLanguageChange: ((String, String) -> Unit)? = null,
    onThemeChange: ((com.example.voicereaderapp.domain.model.ThemeMode) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var currentLanguage by remember {
        mutableStateOf(TTSLanguage.fromCode(selectedLanguage))
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 0.75f  // 3/4 of screen height

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
                    text = "Default Settings",
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
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "These settings will be used as defaults for all documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Divider(modifier = Modifier.padding(bottom = 24.dp))

            // Speed Setting
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
                    text = "${String.format("%.1f", speed)}x",
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
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            // Theme Selection
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.voicereaderapp.domain.model.ThemeMode.values().forEach { theme ->
                    val themeName = when (theme) {
                        com.example.voicereaderapp.domain.model.ThemeMode.LIGHT -> "Light"
                        com.example.voicereaderapp.domain.model.ThemeMode.DARK -> "Dark"
                        com.example.voicereaderapp.domain.model.ThemeMode.SYSTEM -> "System"
                    }
                    FilterChip(
                        selected = selectedTheme == theme,
                        onClick = { onThemeChange?.invoke(theme) },
                        label = { Text(themeName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Language Selection
            Text(
                text = "Language",
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
                    FilterChip(
                        selected = currentLanguage == language,
                        onClick = { currentLanguage = language },
                        label = { Text(language.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Voice Setting
            Text(
                text = "Default Voice",
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
                    text = "Female",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                femaleVoices.forEach { voice ->
                    VoiceOptionItem(
                        voice = voice,
                        isSelected = voice.id == selectedVoice,
                        onClick = {
                            if (onVoiceAndLanguageChange != null) {
                                onVoiceAndLanguageChange(voice.id, voice.language.code)
                            } else {
                                onVoiceChange(voice.id)
                            }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Male voices
            if (maleVoices.isNotEmpty()) {
                Text(
                    text = "Male",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                maleVoices.forEach { voice ->
                    VoiceOptionItem(
                        voice = voice,
                        isSelected = voice.id == selectedVoice,
                        onClick = {
                            if (onVoiceAndLanguageChange != null) {
                                onVoiceAndLanguageChange(voice.id, voice.language.code)
                            } else {
                                onVoiceChange(voice.id)
                            }
                        }
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
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
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
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.weight(1f))

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
