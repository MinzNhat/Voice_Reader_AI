package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereaderapp.data.local.database.VoiceReaderDatabase
import com.example.voicereaderapp.data.local.entity.NoteRepository
import com.example.voicereaderapp.ui.livereader.overlay.note.NoteTakingOverlay
import com.example.voicereaderapp.ui.livereader.overlay.note.NoteViewModelFactory
import androidx.compose.material3.MaterialTheme

@Composable
fun ExpandedOverlayUI(viewModel: LiveOverlayViewModel) {
    // isExpanded sẽ luôn là true khi Composable này được hiển thị
    val scale by animateFloatAsState(targetValue = 1f, label = "scale")
    val panelAlpha by animateFloatAsState(targetValue = 1f, label = "alpha")
    val panelCorner by animateDpAsState(targetValue = 16.dp, label = "corner")

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Nền mờ, khi chạm vào sẽ đóng lại
            // .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                // ✅ Gọi hàm trong ViewModel để thu gọn
                detectTapGestures { viewModel.collapseOverlay() }
            }
    ) {
        // Panel Cài đặt
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                //.offset(y = (-100).dp)
                .size(width = 300.dp, height = 450.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = panelAlpha
                    transformOrigin = TransformOrigin(1f, 0.4f)
                }
                // Ngăn việc chạm vào panel đóng overlay
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(panelCorner))
                .padding(16.dp)
        ) {
            // Nội dung của panel cài đặt
            EdgeBarSettingsPanel(viewModel)
        }

        val isNoteOverlayVisible by viewModel.isNoteOverlayVisible.collectAsState()
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

        val isSettingsOverlayVisible by viewModel.isSettingsOverlayVisible.collectAsState()
        if (isSettingsOverlayVisible) {
            val context = LocalContext.current
            // Get current settings from ViewModel
            val currentSpeed by viewModel.speed.collectAsState()
            val currentVoiceId by viewModel.selectedVoiceId.collectAsState()
            val currentLanguage by viewModel.selectedLanguage.collectAsState()
            val useMainVoiceForAll by viewModel.useMainVoiceForAll.collectAsState()
            val useMainSpeedForAll by viewModel.useMainSpeedForAll.collectAsState()

            val currentSettings = remember(currentSpeed, currentVoiceId, currentLanguage) {
                com.example.voicereaderapp.domain.model.VoiceSettings(
                    speed = currentSpeed,
                    voiceId = currentVoiceId,
                    language = currentLanguage
                )
            }

            SettingsOverlay(
                currentSettings = currentSettings,
                onSpeedChange = { newSpeed ->
                    viewModel.setSpeed(newSpeed)
                },
                onVoiceChange = { voiceId, language ->
                    viewModel.setVoiceById(voiceId, language)
                },
                onClose = { viewModel.hideSettingsOverlay() },
                useMainVoiceForAll = useMainVoiceForAll,
                useMainSpeedForAll = useMainSpeedForAll
            )
        }
    }
}
