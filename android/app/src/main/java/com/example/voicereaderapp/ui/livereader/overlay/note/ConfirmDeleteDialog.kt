package com.example.voicereaderapp.ui.livereader.overlay.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.voicereaderapp.R

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp), // Tăng độ bo góc cho mềm mại hơn
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Căn giữa tất cả nội dung
            ) {
                // ✅ THÊM BIỂU TƯỢNG CẢNH BÁO
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = stringResource(R.string.delete_note),
                    tint = MaterialTheme.colorScheme.error, // Dùng màu báo lỗi
                    modifier = Modifier.size(48.dp) // Kích thước lớn, nổi bật
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ NHẤN MẠNH TIÊU ĐỀ
                Text(
                    text = stringResource(R.string.delete_note),
                    style = MaterialTheme.typography.headlineSmall, // Kiểu chữ to, rõ ràng hơn
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp)) // Giảm khoảng cách để nhóm với tiêu đề

                Text(
                    text = stringResource(R.string.delete_note_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hàng chứa các nút bấm không thay đổi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.note_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp)) // Thêm khoảng cách giữa 2 nút
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.note_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
