package com.example.voicereaderapp.ui.livereader.overlay.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voicereaderapp.data.local.entity.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    notes: List<NoteEntity>,
    onNoteClick: (Long) -> Unit,
    onAddNewClick: () -> Unit,
    onClose: () -> Unit,
    onDeleteClick: (Long) -> Unit // ✅ Thêm hàm callback để xử lý việc xóa
) {
    var deleteTargetId by remember { mutableStateOf<Long?>(null) }
    if (deleteTargetId != null) {
        ConfirmDeleteDialog(
            onCancel = { deleteTargetId = null },
            onConfirm = {
                deleteTargetId?.let { onDeleteClick(it) }
                deleteTargetId = null
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Notes") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Notes")
                    }
                },
                // ✅ Xóa nút '+' khỏi đây
                actions = { /* Trống */ },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        // ✅ Thêm nút hành động nổi (FAB)
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewClick,
                shape = RoundedCornerShape(16.dp), // Hình vuông bo góc
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Note")
            }
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No notes yet. Tap '+' to create one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = note.title,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        },
                        // ✅ Thêm nút Xóa vào cuối mỗi mục
                        trailingContent = {
                            IconButton(onClick = { deleteTargetId = note.id }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            // Làm cho toàn bộ mục (trừ nút xóa) có thể click để mở chi tiết
                            .clickable { onNoteClick(note.id) }
                            .padding(horizontal = 8.dp)
                    )
                    Divider()
                }
            }
        }
    }
}
