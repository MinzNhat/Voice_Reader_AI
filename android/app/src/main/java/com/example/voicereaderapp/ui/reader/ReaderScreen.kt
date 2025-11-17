package com.example.voicereaderapp.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.voicereaderapp.ui.common.ReaderMode
import com.example.voicereaderapp.ui.common.UnifiedReaderScreen
import com.example.voicereaderapp.ui.settings.SettingsViewModel

/**
 * ReaderScreen - Text reading mode
 * Uses UnifiedReaderScreen for UI, ReaderViewModel for logic
 */
@Composable
fun ReaderScreen(
    navController: NavController,
    documentId: String,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()
    var showTakeNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    // Calculate progress for slider (word-based)
    val words = remember(uiState.documentContent) {
        uiState.documentContent.split(" ")
    }
    val progress = if (words.isNotEmpty() && uiState.currentWordIndex >= 0) {
        uiState.currentWordIndex.toFloat() / words.size
    } else {
        0f
    }

    // Determine mode based on document type
    val readerMode = when (uiState.documentType) {
        com.example.voicereaderapp.domain.model.DocumentType.PDF -> ReaderMode.PDF
        com.example.voicereaderapp.domain.model.DocumentType.IMAGE -> ReaderMode.IMAGE
        com.example.voicereaderapp.domain.model.DocumentType.LIVE_SCREEN -> ReaderMode.TEXT
    }

    UnifiedReaderScreen(
        title = uiState.documentTitle.takeIf { it.isNotBlank() } ?: "Document",
        mode = readerMode,
        content = {
            // Text content with word highlighting
            TextReaderContent(
                content = uiState.documentContent,
                currentWordIndex = uiState.currentWordIndex
            )
        },
        progress = progress,
        isPlaying = uiState.isPlaying,
        isLoading = uiState.isLoading,
        playbackSpeed = settingsState.settings.speed,
        selectedVoice = "Emma", // TODO: Get from settings
        onPlayPause = { viewModel.togglePlayPause() },
        onRewind = { viewModel.rewind() },
        onForward = { viewModel.forward() },
        onSeek = { fraction ->
            val targetIndex = (fraction * words.size).toInt().coerceIn(0, words.size - 1)
            viewModel.jumpTo(targetIndex)
        },
        onSpeedChange = { settingsViewModel.updateSpeed(it) },
        onVoiceChange = { /* TODO: Implement voice change */ },
        onTakeNote = { showTakeNoteDialog = true },
        onBack = { navController.popBackStack() }
    )

    // Take Note Dialog
    if (showTakeNoteDialog) {
        com.example.voicereaderapp.ui.common.TakeNoteDialog(
            documentTitle = uiState.documentTitle.takeIf { it.isNotBlank() } ?: "Document",
            onDismiss = { showTakeNoteDialog = false },
            onSaveNote = { note ->
                // TODO: Save note to database
                android.util.Log.d("ReaderScreen", "Note saved: $note")
            }
        )
    }
}

/**
 * Text content with word-by-word highlighting
 */
@Composable
private fun TextReaderContent(
    content: String,
    currentWordIndex: Int
) {
    val words = remember(content) { content.split(" ") }
    val scrollState = rememberLazyListState()

    // Auto-scroll to current word
    LaunchedEffect(currentWordIndex) {
        if (currentWordIndex >= 0) {
            val lineIndex = (currentWordIndex / 20).coerceAtLeast(0)
            scrollState.animateScrollToItem(lineIndex)
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp)
    ) {
        itemsIndexed(words.chunked(20)) { chunkIndex, chunk ->
            val startIndex = chunkIndex * 20

            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                chunk.forEachIndexed { i, word ->
                    val globalIndex = startIndex + i
                    val highlight = globalIndex == currentWordIndex

                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp
                        ),
                        color = if (highlight)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onBackground,
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

