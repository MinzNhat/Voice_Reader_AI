package com.example.voicereaderapp.ui.pdfreader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voicereaderapp.domain.model.TTSLanguage
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender

/**
 * Legacy Speaker data class - kept for backward compatibility
 * Use TTSVoice enum for new code
 */
@Deprecated("Use TTSVoice enum instead")
data class Speaker(
    val id: String,
    val name: String,
    val language: String,
    val gender: String
)

/**
 * Speaker selection dialog with language selection
 * Allows users to choose voice with automatic language detection
 */
@Composable
fun SpeakerSelectionDialog(
    currentSpeaker: String,
    currentLanguage: String = "en-US",
    onDismiss: () -> Unit,
    onSpeakerSelected: (voiceId: String, language: String) -> Unit
) {
    var selectedLanguage by remember {
        mutableStateOf(TTSLanguage.fromCode(currentLanguage))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Voice")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language selection chips
                Text(
                    text = "Language",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TTSLanguage.values().forEach { language ->
                        FilterChip(
                            selected = selectedLanguage == language,
                            onClick = { selectedLanguage = language },
                            label = { Text(language.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Divider()

                // Voice selection
                Text(
                    text = "Voice",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val voices = TTSVoice.getVoicesForLanguage(selectedLanguage)
                val femaleVoices = voices.filter { it.gender == VoiceGender.FEMALE }
                val maleVoices = voices.filter { it.gender == VoiceGender.MALE }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Female voices
                    if (femaleVoices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Female",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(femaleVoices) { voice ->
                            VoiceItem(
                                voice = voice,
                                isSelected = voice.id == currentSpeaker,
                                onClick = {
                                    onSpeakerSelected(voice.id, voice.language.code)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // Male voices
                    if (maleVoices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Male",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }
                        items(maleVoices) { voice ->
                            VoiceItem(
                                voice = voice,
                                isSelected = voice.id == currentSpeaker,
                                onClick = {
                                    onSpeakerSelected(voice.id, voice.language.code)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Voice item in the selection list
 */
@Composable
private fun VoiceItem(
    voice: TTSVoice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = voice.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Legacy speaker item - kept for backward compatibility
 */
@Deprecated("Use VoiceItem with TTSVoice instead")
@Composable
fun SpeakerItem(
    speaker: Speaker,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = speaker.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${speaker.language} • ${speaker.gender}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
