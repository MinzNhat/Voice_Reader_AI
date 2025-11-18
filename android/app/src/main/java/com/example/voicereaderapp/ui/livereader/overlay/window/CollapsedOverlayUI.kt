package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereaderapp.data.local.database.VoiceReaderDatabase
import com.example.voicereaderapp.data.local.entity.NoteRepository
import com.example.voicereaderapp.ui.livereader.overlay.note.NoteTakingOverlay
import com.example.voicereaderapp.ui.livereader.overlay.note.NoteViewModelFactory

@Composable
fun CollapsedOverlayUI(viewModel: LiveOverlayViewModel) {
    val isNoteOverlayVisible by viewModel.isNoteOverlayVisible.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Thanh điều khiển bên cạnh
        ControlEdgeBar(viewModel = viewModel)

        // 2. Vùng hiển thị Mic khi nhấn giữ
        VoiceInteractionPad(viewModel = viewModel)

        // 3. Lớp phủ Ghi chú (chỉ hiện khi state là true)
        if (isNoteOverlayVisible) {
            val context = LocalContext.current
            val noteDao = VoiceReaderDatabase.getDatabase(context).noteDao()
            val noteRepository = NoteRepository(noteDao)
            val noteViewModel: NoteViewModel = viewModel(factory = NoteViewModelFactory(noteRepository))

            NoteTakingOverlay(
                noteViewModel = noteViewModel,
                onClose = { viewModel.hideNoteOverlay() }
            )
        }
    }
}
