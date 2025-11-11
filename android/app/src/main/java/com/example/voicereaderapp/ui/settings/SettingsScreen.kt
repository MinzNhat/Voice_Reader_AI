package com.example.voicereaderapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicereaderapp.utils.VoiceFeedback
import com.example.voicereaderapp.utils.provideFeedback

/**
 * Settings screen for configuring voice parameters.
 * Allows users to adjust voice selection, reading speed, and pitch.
 *
 * @param viewModel ViewModel managing settings state
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics {
                contentDescription = "Màn hình cài đặt giọng đọc"
            }
    ) {
        Text(
            text = "Voice Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .semantics {
                    contentDescription = "Cài đặt giọng đọc"
                }
        )

        // Language selection
        Text(
            text = "Language: ${uiState.settings.language}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // TODO: Add language dropdown

        Spacer(modifier = Modifier.height(24.dp))

        // Speed slider
        Text(
            text = "Reading Speed: ${String.format("%.1f", uiState.settings.speed)}x",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                contentDescription = "Tốc độ đọc hiện tại là ${String.format("%.1f", uiState.settings.speed)} lần"
            }
        )
        Slider(
            value = uiState.settings.speed,
            onValueChange = { 
                viewModel.updateSpeed(it)
                context.provideFeedback(
                    VoiceFeedback.FeedbackType.SELECTION,
                    "Tốc độ ${String.format("%.1f", it)} lần"
                )
            },
            valueRange = 0.5f..2.0f,
            steps = 14,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .semantics {
                    contentDescription = "Thanh trượt điều chỉnh tốc độ đọc. Vuốt lên để tăng, xuống để giảm. Giá trị từ 0.5 đến 2.0"
                }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.5x", style = MaterialTheme.typography.bodySmall)
            Text("2.0x", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pitch slider
        Text(
            text = "Voice Pitch: ${String.format("%.1f", uiState.settings.pitch)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                contentDescription = "Độ cao giọng hiện tại là ${String.format("%.1f", uiState.settings.pitch)}"
            }
        )
        Slider(
            value = uiState.settings.pitch,
            onValueChange = { 
                viewModel.updatePitch(it)
                context.provideFeedback(
                    VoiceFeedback.FeedbackType.SELECTION,
                    "Độ cao ${String.format("%.1f", it)}"
                )
            },
            valueRange = 0.5f..2.0f,
            steps = 14,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .semantics {
                    contentDescription = "Thanh trượt điều chỉnh độ cao giọng. Vuốt lên để tăng, xuống để giảm. Giá trị từ 0.5 đến 2.0"
                }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.5", style = MaterialTheme.typography.bodySmall)
            Text("2.0", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save button
        Button(
            onClick = { 
                viewModel.saveSettings()
                context.provideFeedback(
                    VoiceFeedback.FeedbackType.SUCCESS,
                    "Đã lưu cài đặt"
                )
            },
            modifier = Modifier
                .align(Alignment.End)
                .semantics {
                    contentDescription = "Nút lưu cài đặt. Chạm hai lần để lưu các thay đổi"
                }
        ) {
            Text("Save Settings")
        }
    }
}
