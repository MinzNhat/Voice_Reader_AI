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

/**
 * Available NAVER TTS Premium speakers
 */
data class Speaker(
    val id: String,
    val name: String,
    val language: String,
    val gender: String
)

val AVAILABLE_SPEAKERS = listOf(
    // English speakers
    Speaker("matt", "Matt", "English (US)", "Male"),
    Speaker("clara", "Clara", "English (US)", "Female"),

    // Korean speakers (for reference)
    Speaker("nara", "Nara", "Korean", "Female"),
    Speaker("jinho", "Jinho", "Korean", "Male"),

    // Other languages
    Speaker("shinji", "Shinji", "Japanese", "Male"),
    Speaker("meimei", "Meimei", "Chinese", "Female"),
    Speaker("liangliang", "Liangliang", "Chinese", "Male"),
    Speaker("jose", "Jose", "Spanish", "Male"),
    Speaker("carmen", "Carmen", "Spanish", "Female")
)

/**
 * Speaker selection dialog
 */
@Composable
fun SpeakerSelectionDialog(
    currentSpeaker: String,
    onDismiss: () -> Unit,
    onSpeakerSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Voice")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(AVAILABLE_SPEAKERS) { speaker ->
                    SpeakerItem(
                        speaker = speaker,
                        isSelected = speaker.id == currentSpeaker,
                        onClick = {
                            onSpeakerSelected(speaker.id)
                            onDismiss()
                        }
                    )
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
 * Individual speaker item
 */
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
