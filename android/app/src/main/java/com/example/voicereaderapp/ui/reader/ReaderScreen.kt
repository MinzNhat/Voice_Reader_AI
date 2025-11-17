package com.example.voicereaderapp.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.voicereaderapp.ui.common.ReadingControlsPanel
import com.example.voicereaderapp.ui.common.VerticalReaderPanel
import com.example.voicereaderapp.ui.settings.SettingsViewModel

// Giả sử ReaderViewModel đã được tạo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    navController: NavController,
    documentId: String,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    var showControls by remember { mutableStateOf(false) }
    var showSpeedSlider by remember { mutableStateOf(false) }
    var showVoicePicker by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    // Gradient nền phía trên
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ==========================
        // BACKGROUND GRADIENT
        // ==========================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
//                    Brush.verticalGradient(
//                        listOf(
//                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
//                            MaterialTheme.colorScheme.background
//                        )
//                    )
//                )
        )

        // =============================
        // SPEECHIFY TOP BAR
        // =============================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp)
        ) {

            // BACK BUTTON (bên trái)
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // TITLE (luôn ở chính giữa màn hình)
            Text(
                text = uiState.documentTitle.takeIf { it.isNotBlank() } ?: "Document",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center)
            )

            // SETTINGS BUTTON (bên phải)
            IconButton(
                onClick = {
                    showControls = true
                    showSpeedSlider = false
                    showVoicePicker = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ==========================
        // CONTENT BOX (bo góc lớn)
        // ==========================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            ReaderContent(
                title = uiState.documentTitle,
                content = uiState.documentContent,
                currentWordIndex = uiState.currentWordIndex,
                isPlaying = uiState.isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onRewind = { viewModel.rewind() },
                onForward = { viewModel.forward() },
                onScrub = { newIndex -> viewModel.jumpTo(newIndex) }
            )
        }

        // ==========================
        // SETTINGS BOTTOM SHEET
        // ==========================
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (-5).dp, y = (-200).dp)// << đặt sát bên phải
            ) {
                VerticalReaderPanel(
                    speed = settingsViewModel.uiState.collectAsState().value.settings.speed,
                    onSpeedChange = { settingsViewModel.updateSpeed(it) },
                    selectedVoice = "Emma",
                    onSelectVoice = { showVoicePicker = true },
                    onClickSpeed = {
                        showSpeedSlider = true
                        showVoicePicker = false
                    },
                    onClickVoice = {
                        showVoicePicker = true
                        showSpeedSlider = false
                    },
                    onClose = {
                        showControls = false
                        showSpeedSlider = false
                        showVoicePicker = false
                    }
                )
            }
        }

        if (showSpeedSlider) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 80.dp)
                    .offset(y = (-250).dp)
                    .width(160.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Speed", fontWeight = FontWeight.Bold)
                    Slider(
                        value = settingsViewModel.uiState.value.settings.speed,
                        onValueChange = {
                            settingsViewModel.updateSpeed(it)
                        },
                        valueRange = 0.5f..2.0f
                    )
                }
            }
        }

        if (showVoicePicker) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 100.dp)
                    .offset(y = (-170).dp)
                    .width(180.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp
            ) {
                Column(Modifier.padding(16.dp)) {

                    Text("Voices", fontWeight = FontWeight.Bold)

                    listOf(
                        "https://randomuser.me/api/portraits/women/2.jpg",
                        "https://randomuser.me/api/portraits/men/3.jpg",
                        "https://randomuser.me/api/portraits/women/4.jpg"
                    ).forEach { url ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // settingsViewModel.updateVoice(url)
                                    showVoicePicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Voice",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Text(" Voice", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderContent(
    title: String,
    content: String,
    currentWordIndex: Int,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onScrub: (Int) -> Unit
) {
    val words = remember(content) { content.split(" ") }
    val scrollState = rememberLazyListState()

    // ⭐ Auto-scroll khi đọc
    LaunchedEffect(currentWordIndex) {
        val lineIndex = (currentWordIndex / 6).coerceAtLeast(0)
        scrollState.animateScrollToItem(lineIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {

        // ⭐ Vùng text auto-scroll + highlight
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 36.dp)
        ) {

            itemsIndexed(words.chunked(6)) { chunkIndex, chunk ->
                val startIndex = chunkIndex * 6

                Row(Modifier.padding(vertical = 4.dp)) {
                    chunk.forEachIndexed { i, word ->
                        val globalIndex = startIndex + i

                        val highlight = globalIndex == currentWordIndex
                        Text(
                            text = "$word ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (highlight)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // ⭐ Scrubber (Progress Bar có thể kéo)
        Slider(
            value = if (words.isNotEmpty()) currentWordIndex.toFloat() / words.size else 0f,
            onValueChange = { fraction ->
                val newIndex = (fraction * words.size).toInt()
                onScrub(newIndex)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )

        Spacer(Modifier.height(2.dp))

        // ⭐ Controls: Rewind  |  Play/Pause  |  Forward
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ==== REWIND
            IconButton(
                onClick = onRewind,
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.FastRewind,
                    contentDescription = "Rewind",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(30.dp))

            // ==== PLAY / PAUSE
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.width(30.dp))

            // ==== FORWARD
            IconButton(
                onClick = onForward,
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.FastForward,
                    contentDescription = "Forward",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

