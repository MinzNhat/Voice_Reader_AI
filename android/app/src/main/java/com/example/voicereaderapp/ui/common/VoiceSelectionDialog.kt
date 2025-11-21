package com.example.voicereaderapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.voicereaderapp.domain.model.TTSLanguage
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender

/**
 * Dialog for selecting TTS voice.
 * Allows users to choose language and voice.
 *
 * @param currentLanguage Currently selected language
 * @param currentVoiceId Currently selected voice ID
 * @param isMainVoiceEnforced Whether main voice for all documents is enabled
 * @param onDismiss Callback when dialog is dismissed
 * @param onVoiceSelected Callback when a voice is selected with language and voice
 */
@Composable
fun VoiceSelectionDialog(
    currentLanguage: String,
    currentVoiceId: String,
    isMainVoiceEnforced: Boolean = false,
    onDismiss: () -> Unit,
    onVoiceSelected: (language: String, voiceId: String) -> Unit
) {
    var selectedLanguage by remember {
        mutableStateOf(TTSLanguage.fromCode(currentLanguage))
    }
    var selectedVoiceId by remember { mutableStateOf(currentVoiceId) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Title
                Text(
                    text = "Select Voice",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Show warning if main voice is enforced
                if (isMainVoiceEnforced) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Main Voice Enforced",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Voice selection is disabled because \"Use Main Voice for All Documents\" is enabled in Settings. Disable it to change voice per document.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Language Selection
                Text(
                    text = "Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TTSLanguage.values().forEach { language ->
                        FilterChip(
                            selected = selectedLanguage == language,
                            onClick = {
                                if (!isMainVoiceEnforced) {
                                    selectedLanguage = language
                                    // Auto-select default voice for the language
                                    selectedVoiceId = TTSVoice.getDefaultVoiceForLanguage(language).id
                                }
                            },
                            label = { Text(language.displayName) },
                            modifier = Modifier.weight(1f),
                            enabled = !isMainVoiceEnforced
                        )
                    }
                }

                // Voice Selection
                Text(
                    text = "Voice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Group voices by gender
                val voices = TTSVoice.getVoicesForLanguage(selectedLanguage)
                val femaleVoices = voices.filter { it.gender == VoiceGender.FEMALE }
                val maleVoices = voices.filter { it.gender == VoiceGender.MALE }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Female voices
                    if (femaleVoices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Female",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(femaleVoices) { voice ->
                            VoiceItem(
                                voice = voice,
                                isSelected = selectedVoiceId == voice.id,
                                onClick = {
                                    if (!isMainVoiceEnforced) {
                                        selectedVoiceId = voice.id
                                    }
                                },
                                enabled = !isMainVoiceEnforced
                            )
                        }
                    }

                    // Male voices
                    if (maleVoices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Male",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }
                        items(maleVoices) { voice ->
                            VoiceItem(
                                voice = voice,
                                isSelected = selectedVoiceId == voice.id,
                                onClick = {
                                    if (!isMainVoiceEnforced) {
                                        selectedVoiceId = voice.id
                                    }
                                },
                                enabled = !isMainVoiceEnforced
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onVoiceSelected(selectedLanguage.code, selectedVoiceId)
                            onDismiss()
                        },
                        enabled = !isMainVoiceEnforced
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

/**
 * Individual voice item in the selection list.
 */
@Composable
private fun VoiceItem(
    voice: TTSVoice,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, enabled = enabled),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else if (enabled)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = voice.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else if (enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
