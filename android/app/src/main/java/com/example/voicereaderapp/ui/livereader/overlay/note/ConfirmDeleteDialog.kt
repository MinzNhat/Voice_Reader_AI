//package com.example.voicereaderapp.ui.livereader.overlay.note
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.Warning
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import com.example.voicereaderapp.R
//
//@Composable
//fun ConfirmDeleteDialog(
//    onConfirm: () -> Unit,
//    onCancel: () -> Unit
//) {
//    Dialog(onDismissRequest = onCancel) {
//        Surface(
//            shape = RoundedCornerShape(16.dp), // Tăng độ bo góc cho mềm mại hơn
//            tonalElevation = 6.dp,
//            color = MaterialTheme.colorScheme.surface
//        ) {
//            Column(
//                modifier = Modifier.padding(24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally // Căn giữa tất cả nội dung
//            ) {
//                // ✅ THÊM BIỂU TƯỢNG CẢNH BÁO
//                Icon(
//                    imageVector = Icons.Outlined.Warning,
//                    contentDescription = stringResource(R.string.delete_note),
//                    tint = MaterialTheme.colorScheme.error, // Dùng màu báo lỗi
//                    modifier = Modifier.size(48.dp) // Kích thước lớn, nổi bật
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // ✅ NHẤN MẠNH TIÊU ĐỀ
//                Text(
//                    text = stringResource(R.string.delete_note),
//                    style = MaterialTheme.typography.headlineSmall, // Kiểu chữ to, rõ ràng hơn
//                    fontWeight = FontWeight.Bold
//                )
//
//                Spacer(modifier = Modifier.height(8.dp)) // Giảm khoảng cách để nhóm với tiêu đề
//
//                Text(
//                    text = stringResource(R.string.delete_note_confirm),
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Hàng chứa các nút bấm không thay đổi
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    TextButton(onClick = onCancel) {
//                        Text(stringResource(R.string.note_cancel))
//                    }
//                    Spacer(modifier = Modifier.width(8.dp)) // Thêm khoảng cách giữa 2 nút
//                    TextButton(onClick = onConfirm) {
//                        Text(stringResource(R.string.note_delete), color = MaterialTheme.colorScheme.error)
//                    }
//                }
//            }
//        }
//    }
//}

package com.example.voicereaderapp.ui.livereader.overlay.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex // Quan trọng để đè lên layer khác
import com.example.voicereaderapp.R

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    // 1. Thay Dialog bằng Box full màn hình để làm lớp phủ (Overlay)
    Box(
        modifier = Modifier
            .fillMaxSize() // Phủ kín toàn bộ diện tích Floating Window
            .zIndex(99f)   // Đảm bảo nó nằm trên cùng nhất
            .background(Color.Black.copy(alpha = 0.6f)) // Làm tối nền đằng sau (Scrim)
            .clickable(
                // Chặn click xuyên qua lớp nền xuống nội dung bên dưới
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Nếu muốn click ra ngoài thì tắt dialog -> gọi onCancel() ở đây
                onCancel()
            },
        contentAlignment = Alignment.Center // Căn hộp thoại ra giữa
    ) {
        // 2. Nội dung chính của Dialog (Giữ nguyên Surface như cũ)
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp) // Cách lề màn hình một chút
                .clickable(
                    // Chặn click từ Surface xuyên xuống Box nền (quan trọng)
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = stringResource(R.string.delete_note),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.delete_note),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.delete_note_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.note_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.note_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
