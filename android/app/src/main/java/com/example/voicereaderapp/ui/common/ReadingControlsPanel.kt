package com.example.voicereaderapp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.voicereaderapp.ui.settings.SettingsViewModel // Sử dụng lại ViewModel của cài đặt
import com.example.voicereaderapp.utils.VoiceFeedback
import com.example.voicereaderapp.utils.provideFeedback
import androidx.compose.material.icons.filled.ChevronRight // Tạm thời dùng icon này
import androidx.compose.ui.text.font.FontWeight

/**
 * ReadingControlsPanel là một Composable chứa các thanh trượt và lựa chọn
 * để điều chỉnh tốc độ đọc, cao độ và giọng nói.
 * Nó được thiết kế để nhúng (embed) vào màn hình Reader chính.
 *
 * @param viewModel ViewModel quản lý trạng thái cài đặt giọng nói (thường là SettingsViewModel)
 * @param onClose Callback khi người dùng muốn đóng bảng điều khiển
 */
@Composable
fun ReadingControlsPanel(
    viewModel: SettingsViewModel, // Giả định dùng lại SettingsViewModel cho logic cài đặt
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settings = uiState.settings

    // Sử dụng Surface hoặc Card để tạo background rõ ràng cho panel
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics {
                    contentDescription = "Bảng điều khiển cài đặt giọng đọc"
                }
        ) {
            // Header và nút Đóng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cài đặt Giọng đọc",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "Cài đặt giọng đọc"
                    }
                )
                // Nút đóng
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.semantics {
                        contentDescription = "Đóng bảng điều khiển"
                    }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 1. Lựa chọn Ngôn ngữ (Giả lập) ---
            Text("Ngôn ngữ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = settings.language,
                onValueChange = {}, // Chỉ là ví dụ, logic thay đổi cần được thực hiện trong ViewModel
                label = { Text("Ngôn ngữ đang dùng") },
                readOnly = true,
                trailingIcon = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // TODO: Implement Language Picker Dialog
                    },
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. Tốc độ Đọc (Reading Speed / Rate) ---
            Text(
                "Tốc độ Đọc: ${String.format("%.1f", settings.speed)}x",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = settings.speed,
                onValueChange = {
                    viewModel.updateSpeed(it)
                    context.provideFeedback(
                        VoiceFeedback.FeedbackType.SELECTION,
                        "Tốc độ đọc ${String.format("%.1f", it)}"
                    )
                },
                valueRange = 0.5f..2.0f,
                steps = 14,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics {
                        contentDescription = "Thanh trượt điều chỉnh tốc độ đọc. Giá trị từ 0.5 đến 2.0"
                    }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Chậm (0.5x)", style = MaterialTheme.typography.bodySmall)
                Text("Nhanh (2.0x)", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. Độ cao Giọng (Pitch) ---
            Text(
                "Độ cao Giọng: ${String.format("%.1f", settings.pitch)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = settings.pitch,
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
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics {
                        contentDescription = "Thanh trượt điều chỉnh độ cao giọng. Giá trị từ 0.5 đến 2.0"
                    }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Thấp (0.5)", style = MaterialTheme.typography.bodySmall)
                Text("Cao (2.0)", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nút Lưu cài đặt (Không bắt buộc nếu logic cập nhật tức thì)
            Button(
                onClick = {
                    viewModel.saveSettings()
                    context.provideFeedback(
                        VoiceFeedback.FeedbackType.SUCCESS,
                        "Đã lưu cài đặt"
                    )
                    onClose() // Tự động đóng sau khi lưu
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Nút lưu cài đặt và đóng. Chạm hai lần để lưu các thay đổi"
                    }
            ) {
                Text("Lưu Cài đặt và Đóng")
            }
        }
    }
}
