package com.example.voicereaderapp.ui.pdfreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.voicereaderapp.data.remote.model.OCRWord
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.ui.common.VerticalReaderPanel
import com.example.voicereaderapp.ui.index.Screen
import com.example.voicereaderapp.ui.settings.SettingsViewModel
import java.io.File


/**
 * PDF Viewer Screen with OCR overlay and TTS playback
 * UI styled to match ReaderScreen (Speechify-style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(
    fileUri: Uri,
    navController: NavController,
    viewModel: PDFViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var showControls by remember { mutableStateOf(false) }
    var showSpeedSlider by remember { mutableStateOf(false) }
    var showVoicePicker by remember { mutableStateOf(false) }

    // Convert URI to File and perform OCR
    LaunchedEffect(fileUri) {
        val file = uriToFile(context, fileUri)
        pdfFile = file
        if (file != null) {
            viewModel.performOCR(file)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ==========================
        // BACKGROUND
        // ==========================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        )

        // =============================
        // SPEECHIFY-STYLE TOP BAR
        // =============================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp)
        ) {

            // BACK BUTTON (left)
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

            // TITLE (center)
            Text(
                text = "PDF Reader",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center)
            )

            // SETTINGS BUTTON (right)
            IconButton(
                onClick = {
                    showControls = true
                    showSpeedSlider = false
                    showVoicePicker = false
                },
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
        // CONTENT BOX (rounded corners)
        // ==========================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            tonalElevation = 6.dp,
            color = Color.White
        ) {
            PDFReaderContent(
                ocrText = uiState.ocrText ?: "Performing OCR...",
                ocrWords = uiState.ocrWords,
                currentWordIndex = uiState.currentWordIndex,
                isPlaying = uiState.isPlaying,
                isLoading = uiState.isOCRProcessing || uiState.isGeneratingAudio,
                audioReady = uiState.audioBase64 != null,
                onPlayPause = {
                    if (uiState.audioBase64 == null) {
                        viewModel.generateSpeech()
                    } else {
                        if (uiState.isPlaying) {
                            viewModel.pauseAudio()
                        } else {
                            if (uiState.currentPlaybackPosition == 0L) {
                                viewModel.playAudio()
                            } else {
                                viewModel.resumeAudio()
                            }
                        }
                    }
                },
                onRewind = {
                    // TODO: Implement rewind by seeking in audio
                },
                onForward = {
                    // TODO: Implement forward by seeking in audio
                },
                onScrub = { fraction ->
                    // TODO: Implement scrubbing
                }
            )
        }

        // ==========================
        // SETTINGS PANEL (right side)
        // ==========================
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (-5).dp, y = (-200).dp)
            ) {
                VerticalReaderPanel(
                    speed = uiState.playbackSpeed,
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                    selectedVoice = uiState.selectedSpeaker,
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

        // Speed Slider Dialog
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
                        value = uiState.playbackSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.5f..2.0f
                    )
                    Text("${String.format("%.1f", uiState.playbackSpeed)}x")
                }
            }
        }

        // Voice Picker Dialog
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
                        "matt" to "https://randomuser.me/api/portraits/men/1.jpg",
                        "sarah" to "https://randomuser.me/api/portraits/women/2.jpg",
                        "emma" to "https://randomuser.me/api/portraits/women/4.jpg"
                    ).forEach { (speaker, url) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSpeaker(speaker)
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
                            Text(
                                " ${speaker.capitalize()}",
                                modifier = Modifier.padding(start = 8.dp),
                                color = if (speaker == uiState.selectedSpeaker)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Error Snackbar
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(uiState.error!!)
            }
        }
    }
}

/**
 * PDF Reader content with OCR text display (ReaderScreen-style)
 */
@Composable
fun PDFReaderContent(
    ocrText: String,
    ocrWords: List<OCRWord>,
    currentWordIndex: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    audioReady: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onScrub: (Float) -> Unit
) {
    val words = remember(ocrText) { ocrText.split(Regex("\\s+")) }
    val scrollState = rememberLazyListState()

    // Auto-scroll when reading
    LaunchedEffect(currentWordIndex) {
        if (currentWordIndex >= 0 && currentWordIndex < words.size) {
            val lineIndex = (currentWordIndex / 6).coerceAtLeast(0)
            scrollState.animateScrollToItem(lineIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {

        // Scrollable text area with word highlighting
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 36.dp)
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
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
                                    MaterialTheme.colorScheme.onBackground,
                                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Scrubber (Progress Bar)
        if (!isLoading && words.isNotEmpty()) {
            Slider(
                value = if (words.isNotEmpty() && currentWordIndex >= 0)
                    currentWordIndex.toFloat() / words.size
                else
                    0f,
                onValueChange = { fraction ->
                    onScrub(fraction)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(2.dp))

        // Controls: Rewind | Play/Pause | Forward
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // REWIND
            IconButton(
                onClick = onRewind,
                enabled = audioReady && !isLoading,
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

            // PLAY / PAUSE
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    .clickable(enabled = !isLoading) { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(Modifier.width(30.dp))

            // FORWARD
            IconButton(
                onClick = onForward,
                enabled = audioReady && !isLoading,
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

/**
 * PDF renderer with OCR word overlay and real-time highlighting
 *
 * COORDINATE SYSTEM:
 * 1. OCR Image Space: NAVER OCR processes images at its own resolution (e.g., 1275x1650)
 * 2. PDF Space: Original PDF coordinates (e.g., 612x792)
 * 3. Canvas Space: Screen pixels where we draw
 *
 * TRANSFORMATION PIPELINE (TWO STEPS):
 * OCR Image Space → [OCR→PDF Scale] → PDF Space → [Base Fit + Zoom + Pan] → Canvas Space
 */
@Composable
fun PDFWithOCROverlay(
    pdfFile: File?,
    ocrWords: List<OCRWord>,
    currentWordIndex: Int,
    ocrImageWidth: Int,
    ocrImageHeight: Int
) {
    android.util.Log.e("PDF_VIEWER", "🔥 PDFWithOCROverlay - OCR words: ${ocrWords.size}, dims: ${ocrImageWidth}x${ocrImageHeight}")

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pdfPageWidth by remember { mutableStateOf(0) }
    var pdfPageHeight by remember { mutableStateOf(0) }
    var userZoom by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Render PDF to bitmap
    LaunchedEffect(pdfFile) {
        if (pdfFile == null) return@LaunchedEffect
        try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val page = pdfRenderer.openPage(0)

            // CRITICAL: Store PDF page dimensions
            pdfPageWidth = page.width
            pdfPageHeight = page.height
            android.util.Log.d("PDF_DEBUG", "📄 PDF Page Size: ${page.width} x ${page.height}")

            // Create bitmap with page dimensions (this is our PDF space)
            val pageBitmap = Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            )

            // Render page to bitmap
            page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            bitmap = pageBitmap
            page.close()
            pdfRenderer.close()
            fileDescriptor.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldZoom = userZoom
                    val newZoom = (userZoom * zoom).coerceIn(0.5f, 4f)

                    // Apply zoom around the gesture centroid
                    if (newZoom != oldZoom) {
                        // Adjust pan to zoom around centroid
                        val zoomFactor = newZoom / oldZoom
                        panOffset = (panOffset - centroid) * zoomFactor + centroid
                    }

                    userZoom = newZoom
                    panOffset += pan
                }
            }
    ) {
        bitmap?.let { bmp ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val pdfWidth = bmp.width.toFloat()
                val pdfHeight = bmp.height.toFloat()

                // STEP 1: Calculate base scale to fit PDF in canvas (aspect-fit)
                val baseScaleX = canvasWidth / pdfWidth
                val baseScaleY = canvasHeight / pdfHeight
                val baseFitScale = minOf(baseScaleX, baseScaleY)

                // STEP 2: Apply user zoom on top of base scale
                val totalScale = baseFitScale * userZoom

                // STEP 3: Calculate PDF dimensions in canvas space
                val displayedWidth = pdfWidth * totalScale
                val displayedHeight = pdfHeight * totalScale

                // STEP 4: Calculate base centering offset (before pan)
                val baseCenterX = (canvasWidth - displayedWidth) / 2f
                val baseCenterY = (canvasHeight - displayedHeight) / 2f

                // STEP 5: Final position = base center + user pan
                val finalOffsetX = baseCenterX + panOffset.x
                val finalOffsetY = baseCenterY + panOffset.y

                // STEP 6: Draw PDF with drop shadow (Speechify-style)
                // Shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.1f),
                    topLeft = Offset(finalOffsetX + 4f, finalOffsetY + 4f),
                    size = Size(displayedWidth, displayedHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )

                // PDF Image
                drawImage(
                    image = bmp.asImageBitmap(),
                    srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                    srcSize = androidx.compose.ui.unit.IntSize(bmp.width, bmp.height),
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        finalOffsetX.toInt(),
                        finalOffsetY.toInt()
                    ),
                    dstSize = androidx.compose.ui.unit.IntSize(
                        displayedWidth.toInt(),
                        displayedHeight.toInt()
                    )
                )

                // STEP 7: Draw OCR bounding boxes with TWO-STEP transformation

                // Calculate OCR image dimensions if backend didn't provide them
                var actualOcrWidth = ocrImageWidth
                var actualOcrHeight = ocrImageHeight

                if (actualOcrWidth == 0 || actualOcrHeight == 0) {
                    // Fallback: Calculate from bounding box coordinates
                    val maxX = ocrWords.maxOfOrNull { word ->
                        val bbox = word.bbox.toRectF()
                        maxOf(bbox.left, bbox.right)
                    } ?: pdfPageWidth.toFloat()

                    val maxY = ocrWords.maxOfOrNull { word ->
                        val bbox = word.bbox.toRectF()
                        maxOf(bbox.top, bbox.bottom)
                    } ?: pdfPageHeight.toFloat()

                    actualOcrWidth = kotlin.math.ceil(maxX).toInt()
                    actualOcrHeight = kotlin.math.ceil(maxY).toInt()

                    android.util.Log.w("OCR_DEBUG", "⚠️ Backend didn't provide OCR dimensions!")
                    android.util.Log.w("OCR_DEBUG", "Calculated from bboxes: ${actualOcrWidth}x${actualOcrHeight}")
                }

                ocrWords.forEachIndexed { index, word ->
                    val bbox = word.bbox.toRectF()

                    // DEBUG: Log first word's coordinates
                    if (index == 0) {
                        android.util.Log.d("OCR_DEBUG", "===== COORDINATE DEBUG =====")
                        android.util.Log.d("OCR_DEBUG", "Canvas size: $canvasWidth x $canvasHeight")
                        android.util.Log.d("OCR_DEBUG", "PDF dimensions: $pdfWidth x $pdfHeight")
                        android.util.Log.d("OCR_DEBUG", "OCR image dimensions from backend: $ocrImageWidth x $ocrImageHeight")
                        android.util.Log.d("OCR_DEBUG", "OCR image dimensions (actual): $actualOcrWidth x $actualOcrHeight")
                        android.util.Log.d("OCR_DEBUG", "Total scale: $totalScale")
                        android.util.Log.d("OCR_DEBUG", "Final offset: ($finalOffsetX, $finalOffsetY)")
                        android.util.Log.d("OCR_DEBUG", "Word: ${word.text}")
                        android.util.Log.d("OCR_DEBUG", "BBox in OCR space: (${bbox.left}, ${bbox.top}) to (${bbox.right}, ${bbox.bottom})")
                    }

                    // ⭐ CRITICAL FIX: Two-step transformation
                    // STEP 1: Transform from OCR Image Space → PDF Space
                    val ocrToPdfScaleX = if (actualOcrWidth > 0) pdfWidth / actualOcrWidth else 1f
                    val ocrToPdfScaleY = if (actualOcrHeight > 0) pdfHeight / actualOcrHeight else 1f

                    val pdfLeft = bbox.left * ocrToPdfScaleX
                    val pdfTop = bbox.top * ocrToPdfScaleY
                    val pdfRight = bbox.right * ocrToPdfScaleX
                    val pdfBottom = bbox.bottom * ocrToPdfScaleY

                    if (index == 0) {
                        android.util.Log.d("OCR_DEBUG", "OCR→PDF scale: ($ocrToPdfScaleX, $ocrToPdfScaleY)")
                        android.util.Log.d("OCR_DEBUG", "BBox in PDF space: ($pdfLeft, $pdfTop) to ($pdfRight, $pdfBottom)")
                    }

                    // STEP 2: Transform from PDF Space → Canvas Space
                    val canvasLeft = pdfLeft * totalScale + finalOffsetX
                    val canvasTop = pdfTop * totalScale + finalOffsetY
                    val canvasRight = pdfRight * totalScale + finalOffsetX
                    val canvasBottom = pdfBottom * totalScale + finalOffsetY

                    if (index == 0) {
                        android.util.Log.d("OCR_DEBUG", "Transformed to canvas: ($canvasLeft, $canvasTop) to ($canvasRight, $canvasBottom)")
                        android.util.Log.d("OCR_DEBUG", "===============================")
                    }

                    val canvasWidth = canvasRight - canvasLeft
                    val canvasHeight = canvasBottom - canvasTop

                    // Speechify-style highlight colors
                    val isActive = index == currentWordIndex

                    // Background fill (pastel yellow for active, subtle green for others)
                    val fillColor = if (isActive) {
                        Color(0x88FFE082) // Pastel yellow, semi-transparent
                    } else {
                        Color(0x3300FF00) // Light green, very transparent
                    }

                    // Stroke color
                    val strokeColor = if (isActive) {
                        Color(0xFFFFD54F) // Bright yellow-gold
                    } else {
                        Color(0x6600AA00) // Medium green
                    }

                    val strokeWidth = if (isActive) 3f else 1.5f
                    val cornerRadius = 6f

                    // Draw fill
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(canvasLeft, canvasTop),
                        size = Size(canvasWidth, canvasHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                    )

                    // Draw stroke
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(canvasLeft, canvasTop),
                        size = Size(canvasWidth, canvasHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        } ?: run {
            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// Helper to convert Uri to File
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".pdf", context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream?.copyTo(output)
        }
        inputStream?.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
