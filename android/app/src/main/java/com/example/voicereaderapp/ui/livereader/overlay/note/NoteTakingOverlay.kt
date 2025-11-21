package com.example.voicereaderapp.ui.livereader.overlay.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.voicereaderapp.ui.livereader.overlay.NoteViewModel

@Composable
fun NoteTakingOverlay(
    noteViewModel: NoteViewModel,
    onClose: () -> Unit
) {
    var selectedNoteId by remember { mutableStateOf<Long?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }

    val allNotes by noteViewModel.notes.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .zIndex(1000f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.75f).fillMaxHeight(0.75f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            // Điều hướng dựa trên state
            if (isCreatingNewNote || selectedNoteId != null) {
                NoteDetailScreen(
                    noteId = selectedNoteId,
                    noteViewModel = noteViewModel,
                    onBack = {
                        // Reset state để quay về màn hình danh sách
                        selectedNoteId = null
                        isCreatingNewNote = false
                    }
                )
            } else {
                NoteListScreen(
                    notes = allNotes,
                    onNoteClick = { noteId -> selectedNoteId = noteId },
                    onAddNewClick = { isCreatingNewNote = true },
                    onClose = onClose,
                    onDeleteClick = { noteId -> noteViewModel.deleteNote(noteId) },
                    onRenameClick = { noteId, newTitle ->
                        noteViewModel.renameNote(noteId, newTitle)
                    }
                )
            }
        }
    }
}
