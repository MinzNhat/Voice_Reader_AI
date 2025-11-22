package com.example.voicereaderapp.ui.livereader.overlay.note

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereaderapp.ui.livereader.overlay.NoteViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long?,
    noteViewModel: NoteViewModel,
    onBack: () -> Unit
) {
    // State cục bộ để lưu trữ nội dung đang chỉnh sửa
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }


    // Chỉ chạy một lần khi noteId thay đổi để load dữ liệu
    LaunchedEffect(noteId) {
        if (noteId != null) {
            noteViewModel.getNoteById(noteId).collectLatest { note ->
                if (note != null) {
                    title = note.title
                    content = note.content
                }
            }
        } else {
            // Nếu là note mới, đặt tiêu đề là "Untitled"
            title = "Untitled"
        }
    }

    Scaffold(
        topBar = {
            // Thanh công cụ giống hệt mẫu
            TopAppBar(
                title = { /* Không có tiêu đề */ },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    // Nút Back
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Nút Xóa (chỉ hiển thị khi đang sửa note đã có)
                    if (noteId != null) {
                        Log.d("NoteViewModel", "Deleting note with ID: $noteId")
                        IconButton(onClick = {
                            showDeleteDialog = true
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Nút Done
                    TextButton(onClick = {
                        noteViewModel.saveNote(noteId, title, content)
                        onBack() // Quay về danh sách sau khi lưu
                    }) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // TextField cho Tiêu đề
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TextField cho Nội dung
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Chiếm hết không gian còn lại
            )
        }
    }
    if (showDeleteDialog && noteId != null) {
        ConfirmDeleteDialog(
            onConfirm = {
                noteViewModel.deleteNote(noteId)
                showDeleteDialog = false
                onBack()
            },
            onCancel = { showDeleteDialog = false }
        )
    }
}
