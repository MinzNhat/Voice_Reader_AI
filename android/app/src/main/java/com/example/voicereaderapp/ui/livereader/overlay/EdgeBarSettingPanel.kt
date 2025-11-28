package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereaderapp.R
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
    val useMainSpeedForAll by viewModel.useMainSpeedForAll.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls (Rewind, Play/Pause, Forward)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // REWIND
            IconButton(
                onClick = { viewModel.rewind() },
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.FastRewind,
                    contentDescription = stringResource(R.string.rewind_5_seconds),
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
                    .clickable { viewModel.toggleReading() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isReading) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play_pause),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.width(30.dp))

            // FORWARD
            IconButton(
                onClick = { viewModel.forward() },
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.FastForward,
                    contentDescription = stringResource(R.string.forward_5_seconds),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Khoảng cách với nút Play

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { viewModel.resetText() },
                modifier = Modifier
                    .size(48.dp) // Nhỏ hơn nút Play một chút
                    .background(
                        MaterialTheme.colorScheme.error, // Màu đỏ để cảnh báo nút xóa/reset
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Text",
                    tint = MaterialTheme.colorScheme.onError, // Màu trắng trên nền đỏ
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                            contentDescription = stringResource(R.string.summarize),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.summarize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                            contentDescription = stringResource(R.string.take_note),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.take_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }
            }

            // Second Row: Back & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Back Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            // Navigate to IndexScreen by launching MainActivity
                            val intent = android.content.Intent(context, com.example.voicereaderapp.MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                // Add extra to indicate we want to go to IndexScreen
                                putExtra("navigate_to", "index")
                            }
                            context.startActivity(intent)

                            // Stop the live overlay service after launching the activity
//                            com.example.voicereaderapp.ui.livereader.overlay.LiveOverlayService.stop(context)
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.back),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Settings Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                    ) {
                    IconButton(
                        onClick = { viewModel.showSettingsOverlay() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
