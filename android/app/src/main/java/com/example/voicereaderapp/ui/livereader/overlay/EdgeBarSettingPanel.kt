package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereaderapp.data.local.database.VoiceReaderDatabase
import com.example.voicereaderapp.data.local.entity.NoteRepository
import com.example.voicereaderapp.ui.livereader.overlay.note.NoteTakingOverlay
import com.example.voicereaderapp.ui.livereader.overlay.note.NoteViewModelFactory
import java.text.DecimalFormat

@Composable
fun EdgeBarSettingsPanel(viewModel: LiveOverlayViewModel) {
    val isReading by viewModel.isReading.collectAsState()
    val currentSpeed by viewModel.speed.collectAsState()
    val currentVoice by viewModel.voiceConfig.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        IconButton(
            onClick = { viewModel.toggleReading() },
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = if (isReading) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isReading) "Pause" else "Continue",
                modifier = Modifier.fillMaxSize(0.8f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val speedFormatter = remember { DecimalFormat("#.#") }
            Text(
                text = "Reading speed: ${speedFormatter.format(currentSpeed)}x",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = currentSpeed,
                onValueChange = { newSpeed -> viewModel.setSpeed(newSpeed) },
                valueRange = 0.5f..2.0f,  // API limit (NAVER Clova Voice)
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // === START OF 2x2 GRID LAYOUT ===
        // Parent Column to hold the two rows of buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Adds space between the two rows
        ) {
            // First Row: Summarize & Take Note
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Summarize Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { /* TODO: Call ViewModel to summarize */ },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = "Summarize",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(text = "Summarize", style = MaterialTheme.typography.labelSmall)
                }

                // Take Note Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.showNoteOverlay() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Take Note",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(text = "Take Note", style = MaterialTheme.typography.labelSmall)

                }
            }

            // Second Row: Change Voice & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Change Voice Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            val newVoice = if (currentVoice == VoiceConfig.Male) VoiceConfig.FeMale else VoiceConfig.Male
                            viewModel.setVoice(newVoice)
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (currentVoice == VoiceConfig.Male) Icons.Default.Male else Icons.Default.Female,
                            contentDescription = "Change Voice",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = if (currentVoice == VoiceConfig.Male) "Male Voice" else "Female Voice",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Settings Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                    ) {
                    IconButton(
                        onClick = { /* TODO: ... */ },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(text = "Settings", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
