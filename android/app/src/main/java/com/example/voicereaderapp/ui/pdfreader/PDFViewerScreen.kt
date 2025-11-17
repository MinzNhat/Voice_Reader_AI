package com.example.voicereaderapp.ui.pdfreader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.voicereaderapp.data.remote.model.OCRWord
import java.io.File


/**
 * PDF Viewer Screen with OCR overlay and TTS playback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(
    pdfFile: File,
    viewModel: PDFViewerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Reader") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // OCR Button
                    IconButton(
                        onClick = { viewModel.performOCR(pdfFile) },
                        enabled = !uiState.isOCRProcessing
                    ) {
                        if (uiState.isOCRProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, "OCR")
                        }
                    }

                    // TTS Generate Button
                    IconButton(
                        onClick = { viewModel.generateSpeech() },
                        enabled = uiState.ocrText != null && !uiState.isGeneratingAudio
                    ) {
                        if (uiState.isGeneratingAudio) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.VolumeUp, "Generate Speech")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.audioBase64 != null) {
                AudioControlBar(
                    isPlaying = uiState.isPlaying,
                    onPlayPause = {
                        if (uiState.isPlaying) {
                            viewModel.pauseAudio()
                        } else {
                            if (uiState.currentPlaybackPosition == 0L) {
                                viewModel.playAudio()
                            } else {
                                viewModel.resumeAudio()
                            }
                        }
                    },
                    onStop = { viewModel.stopAudio() }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // PDF with OCR Overlay
            PDFWithOCROverlay(
                pdfFile = pdfFile,
                ocrWords = uiState.ocrWords,
                currentWordIndex = uiState.currentWordIndex
            )

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
}

/**
 * Audio control bar for TTS playback
 */
@Composable
fun AudioControlBar(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop Button
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, "Stop")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Play/Pause Button
            FloatingActionButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}

/**
 * PDF renderer with OCR word overlay and real-time highlighting
 *
 * COORDINATE SYSTEM:
 * 1. PDF Space: Original PDF coordinates (OCR bounding boxes are in this space)
 * 2. Canvas Space: Screen pixels where we draw
 *
 * TRANSFORMATION PIPELINE:
 * PDF Space → [Base Fit Scale] → [User Zoom] → [Pan Offset] → Canvas Space
 */
@Composable
fun PDFWithOCROverlay(
    pdfFile: File,
    ocrWords: List<OCRWord>,
    currentWordIndex: Int
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userZoom by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Render PDF to bitmap
    LaunchedEffect(pdfFile) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val page = pdfRenderer.openPage(0)

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

                // STEP 7: Draw OCR bounding boxes with SAME transformation
                ocrWords.forEachIndexed { index, word ->
                    val bbox = word.bbox.toRectF()

                    // Transform PDF coordinates → Canvas coordinates
                    // Apply: scale * totalScale + finalOffset
                    val canvasLeft = bbox.left * totalScale + finalOffsetX
                    val canvasTop = bbox.top * totalScale + finalOffsetY
                    val canvasRight = bbox.right * totalScale + finalOffsetX
                    val canvasBottom = bbox.bottom * totalScale + finalOffsetY

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
