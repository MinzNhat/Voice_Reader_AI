package com.example.voicereaderapp.ui.pdfreader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereaderapp.data.remote.model.OCRWord
import java.io.File
import kotlin.math.roundToInt

// Speechify-inspired color scheme
object SpeechifyColors {
    val Background = Color(0xFF0A0A0A)          // Deep black
    val Surface = Color(0xFF1A1A1A)             // Dark surface
    val SurfaceVariant = Color(0xFF2A2A2A)      // Lighter surface
    val Primary = Color(0xFF4A9EFF)             // Speechify blue
    val PrimaryDim = Color(0xFF3A7ED9)          // Dimmer blue
    val HighlightActive = Color(0xFF4A9EFF)     // Blue pill for active word
    val HighlightInactive = Color(0x33FFFFFF)   // Subtle white for other words
    val TextPrimary = Color(0xFFFFFFFF)         // White text
    val TextSecondary = Color(0xFFB0B0B0)       // Gray text
}

/**
 * Speechify-style PDF Viewer with OCR overlay
 * DARK THEME | PREMIUM UI | FLOATING CONTROLS
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechifyStylePDFViewer(
    pdfFile: File,
    viewModel: PDFViewerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSpeedSelector by remember { mutableStateOf(false) }
    var showVoiceSelector by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpeechifyColors.Background)
    ) {
        // PDF Content with OCR Overlay
        SpeechifyPDFCanvas(
            pdfFile = pdfFile,
            ocrWords = uiState.ocrWords,
            currentWordIndex = uiState.currentWordIndex,
            ocrImageWidth = uiState.ocrImageWidth,
            ocrImageHeight = uiState.ocrImageHeight
        )

        // Top Bar
        SpeechifyTopBar(
            onBack = onBack,
            onOCR = { viewModel.performOCR(pdfFile) },
            onGenerateTTS = { viewModel.generateSpeech() },
            onVoiceSelect = { showVoiceSelector = true },
            isOCRProcessing = uiState.isOCRProcessing,
            isGeneratingAudio = uiState.isGeneratingAudio,
            currentVoice = uiState.selectedSpeaker
        )

        // Floating Control Bar (Speechify-style)
        if (uiState.audioBase64 != null) {
            SpeechifyFloatingControlBar(
                isPlaying = uiState.isPlaying,
                playbackSpeed = uiState.playbackSpeed,
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
                onStop = { viewModel.stopAudio() },
                onSpeedClick = { showSpeedSelector = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }

        // Speed Selector Dialog
        if (showSpeedSelector) {
            SpeedSelectorDialog(
                currentSpeed = uiState.playbackSpeed,
                onSpeedSelected = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    showSpeedSelector = false
                },
                onDismiss = { showSpeedSelector = false }
            )
        }

        // Voice Selector Dialog
        if (showVoiceSelector) {
            VoiceSelectorDialog(
                currentVoice = uiState.selectedSpeaker,
                onVoiceSelected = { voice ->
                    viewModel.setSpeaker(voice)
                    showVoiceSelector = false
                },
                onDismiss = { showVoiceSelector = false }
            )
        }

        // Error Snackbar
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                containerColor = Color(0xFFFF4444),
                contentColor = Color.White,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                }
            ) {
                Text(uiState.error!!)
            }
        }
    }
}

/**
 * Speechify-style top bar (minimal, dark)
 */
@Composable
fun SpeechifyTopBar(
    onBack: () -> Unit,
    onOCR: () -> Unit,
    onGenerateTTS: () -> Unit,
    onVoiceSelect: () -> Unit,
    isOCRProcessing: Boolean,
    isGeneratingAudio: Boolean,
    currentVoice: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(SpeechifyColors.Surface, CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = SpeechifyColors.TextPrimary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Voice Selector Button
            TextButton(
                onClick = onVoiceSelect,
                modifier = Modifier
                    .height(44.dp)
                    .background(SpeechifyColors.Surface, RoundedCornerShape(22.dp))
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Voice",
                    tint = SpeechifyColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getVoiceDisplayName(currentVoice),
                    color = SpeechifyColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // OCR Button
            IconButton(
                onClick = onOCR,
                enabled = !isOCRProcessing,
                modifier = Modifier
                    .size(44.dp)
                    .background(SpeechifyColors.Surface, CircleShape)
            ) {
                if (isOCRProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SpeechifyColors.Primary
                    )
                } else {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "OCR",
                        tint = SpeechifyColors.TextPrimary
                    )
                }
            }

            // TTS Button
            IconButton(
                onClick = onGenerateTTS,
                enabled = !isGeneratingAudio,
                modifier = Modifier
                    .size(44.dp)
                    .background(SpeechifyColors.Surface, CircleShape)
            ) {
                if (isGeneratingAudio) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SpeechifyColors.Primary
                    )
                } else {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "TTS",
                        tint = SpeechifyColors.TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Speechify-style floating control bar with glassmorphism
 */
@Composable
fun SpeechifyFloatingControlBar(
    isPlaying: Boolean,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(320.dp)
            .height(80.dp)
            .shadow(16.dp, RoundedCornerShape(40.dp)),
        shape = RoundedCornerShape(40.dp),
        color = SpeechifyColors.Surface.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Avatar (left)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SpeechifyColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Voice",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Stop Button
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = SpeechifyColors.TextPrimary
                )
            }

            // Play/Pause Button (center, large)
            FloatingActionButton(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp),
                containerColor = SpeechifyColors.Primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Speed Selector (right)
            TextButton(
                onClick = onSpeedClick,
                modifier = Modifier
                    .height(40.dp)
                    .background(
                        SpeechifyColors.SurfaceVariant,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    color = SpeechifyColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * PDF Canvas with OCR overlay
 * CRITICAL: Proper coordinate mapping with OCR image dimensions
 */
@Composable
fun SpeechifyPDFCanvas(
    pdfFile: File,
    ocrWords: List<OCRWord>,
    currentWordIndex: Int,
    ocrImageWidth: Int,
    ocrImageHeight: Int
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pdfPageWidth by remember { mutableStateOf(0) }
    var pdfPageHeight by remember { mutableStateOf(0) }
    var userZoom by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }

    // Render PDF to bitmap
    LaunchedEffect(pdfFile, currentPage) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val pdfRenderer = PdfRenderer(fileDescriptor)

            // Store total pages
            totalPages = pdfRenderer.pageCount

            // Clamp current page to valid range
            val pageIndex = currentPage.coerceIn(0, totalPages - 1)
            val page = pdfRenderer.openPage(pageIndex)

            // CRITICAL: Store original PDF dimensions
            pdfPageWidth = page.width
            pdfPageHeight = page.height

            // DEBUG: Log PDF dimensions
            android.util.Log.d("PDF_DEBUG", "PDF Page ${pageIndex + 1}/$totalPages Size: $pdfPageWidth x $pdfPageHeight")

            // Render at native resolution
            val pageBitmap = Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            )

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

                    if (newZoom != oldZoom) {
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

                // Original PDF dimensions (in PDF coordinate space)
                val pdfWidth = pdfPageWidth.toFloat()
                val pdfHeight = pdfPageHeight.toFloat()

                // Calculate base scale (fit PDF to canvas)
                val baseScaleX = canvasWidth / pdfWidth
                val baseScaleY = canvasHeight / pdfHeight
                val baseFitScale = minOf(baseScaleX, baseScaleY)

                // Total scale = base fit + user zoom
                val totalScale = baseFitScale * userZoom

                // PDF size on screen
                val displayedWidth = pdfWidth * totalScale
                val displayedHeight = pdfHeight * totalScale

                // Center the PDF
                val baseCenterX = (canvasWidth - displayedWidth) / 2f
                val baseCenterY = (canvasHeight - displayedHeight) / 2f

                // Apply pan
                val finalOffsetX = baseCenterX + panOffset.x
                val finalOffsetY = baseCenterY + panOffset.y

                // Draw drop shadow (Speechify-style)
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(finalOffsetX + 8f, finalOffsetY + 8f),
                    size = Size(displayedWidth, displayedHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )

                // Draw white background for PDF (so text is visible)
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(finalOffsetX, finalOffsetY),
                    size = Size(displayedWidth, displayedHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )

                // Draw PDF with rounded corners using clipRect
                drawContext.canvas.save()

                // Create clipping path for rounded corners
                val clipPath = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = finalOffsetX,
                            top = finalOffsetY,
                            right = finalOffsetX + displayedWidth,
                            bottom = finalOffsetY + displayedHeight,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                    )
                }

                drawContext.canvas.clipPath(clipPath)

                drawImage(
                    image = bmp.asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bmp.width, bmp.height),
                    dstOffset = IntOffset(

                        finalOffsetX.roundToInt(),
                        finalOffsetY.roundToInt()
                    ),
                    dstSize = IntSize(
                        displayedWidth.roundToInt(),
                        displayedHeight.roundToInt()
                    )
                )

                drawContext.canvas.restore()

                // Draw OCR highlights (Speechify-style pills)
                // CRITICAL: Scale OCR coordinates to PDF coordinates first
                ocrWords.forEachIndexed { index, word ->
                    val bbox = word.bbox.toRectF()

                    // DEBUG: Log first word's coordinates
                    if (index == 0) {
                        android.util.Log.d("OCR_DEBUG", "===== COORDINATE DEBUG =====")
                        android.util.Log.d("OCR_DEBUG", "Canvas size: $canvasWidth x $canvasHeight")
                        android.util.Log.d("OCR_DEBUG", "PDF dimensions: $pdfWidth x $pdfHeight")
                        android.util.Log.d("OCR_DEBUG", "OCR image dimensions: $ocrImageWidth x $ocrImageHeight")
                        android.util.Log.d("OCR_DEBUG", "Base scale X: $baseScaleX, Y: $baseScaleY")
                        android.util.Log.d("OCR_DEBUG", "Base fit scale: $baseFitScale")
                        android.util.Log.d("OCR_DEBUG", "User zoom: $userZoom")
                        android.util.Log.d("OCR_DEBUG", "Total scale: $totalScale")
                        android.util.Log.d("OCR_DEBUG", "Displayed size: $displayedWidth x $displayedHeight")
                        android.util.Log.d("OCR_DEBUG", "Base center: ($baseCenterX, $baseCenterY)")
                        android.util.Log.d("OCR_DEBUG", "Pan offset: (${panOffset.x}, ${panOffset.y})")
                        android.util.Log.d("OCR_DEBUG", "Final offset: ($finalOffsetX, $finalOffsetY)")
                        android.util.Log.d("OCR_DEBUG", "---")
                        android.util.Log.d("OCR_DEBUG", "Word: ${word.text}")
                        android.util.Log.d("OCR_DEBUG", "BBox in OCR space: (${bbox.left}, ${bbox.top}) to (${bbox.right}, ${bbox.bottom})")
                    }

                    // CRITICAL FIX: Transform coordinates
                    // Step 1: Scale from OCR image space → PDF space
                    val ocrToPdfScaleX = if (ocrImageWidth > 0) pdfWidth / ocrImageWidth else 1f
                    val ocrToPdfScaleY = if (ocrImageHeight > 0) pdfHeight / ocrImageHeight else 1f

                    // Note: Both OCR and PDF use top-left origin, so no Y-inversion needed
                    val pdfLeft = bbox.left * ocrToPdfScaleX
                    val pdfTop = bbox.top * ocrToPdfScaleY
                    val pdfRight = bbox.right * ocrToPdfScaleX
                    val pdfBottom = bbox.bottom * ocrToPdfScaleY

                    if (index == 0) {
                        android.util.Log.d("OCR_DEBUG", "OCR→PDF scale: ($ocrToPdfScaleX, $ocrToPdfScaleY)")
                        android.util.Log.d("OCR_DEBUG", "BBox in PDF space: ($pdfLeft, $pdfTop) to ($pdfRight, $pdfBottom)")

                        // Calculate what percentage of PDF this represents
                        val percentX = (pdfLeft / pdfWidth) * 100
                        val percentY = (pdfTop / pdfHeight) * 100
                        android.util.Log.d("OCR_DEBUG", "Position in PDF: ${percentX.toInt()}% from left, ${percentY.toInt()}% from top")
                    }

                    // Step 2: Transform from PDF space → Canvas space
                    val canvasLeft = pdfLeft * totalScale + finalOffsetX
                    val canvasTop = pdfTop * totalScale + finalOffsetY
                    val canvasRight = pdfRight * totalScale + finalOffsetX
                    val canvasBottom = pdfBottom * totalScale + finalOffsetY

                    if (index == 0) {
                        android.util.Log.d("OCR_DEBUG", "Transformed to canvas: ($canvasLeft, $canvasTop) to ($canvasRight, $canvasBottom)")
                    }

                    val width = canvasRight - canvasLeft
                    val height = canvasBottom - canvasTop

                    val isActive = index == currentWordIndex

                    // Speechify-style pill highlight
                    val fillColor = if (isActive) {
                        SpeechifyColors.HighlightActive.copy(alpha = 0.5f)
                    } else {
                        SpeechifyColors.HighlightInactive
                    }

                    val cornerRadius = height / 2f  // Full pill shape

                    // Draw pill background
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(canvasLeft - 4f, canvasTop - 2f),
                        size = Size(width + 8f, height + 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            cornerRadius + 2f,
                            cornerRadius + 2f
                        ),
                        style = Fill
                    )

                    // Glow effect for active word
                    if (isActive) {
                        drawRoundRect(
                            color = SpeechifyColors.HighlightActive.copy(alpha = 0.3f),
                            topLeft = Offset(canvasLeft - 8f, canvasTop - 4f),
                            size = Size(width + 16f, height + 8f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                cornerRadius + 4f,
                                cornerRadius + 4f
                            ),
                            style = Fill
                        )
                    }

                    // DEBUG: Draw crosshair at first word center for visual check
                    if (index == 0) {
                        val centerX = canvasLeft + width / 2
                        val centerY = canvasTop + height / 2

                        // Red crosshair
                        drawLine(
                            color = Color.Red,
                            start = Offset(centerX - 20, centerY),
                            end = Offset(centerX + 20, centerY),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color.Red,
                            start = Offset(centerX, centerY - 20),
                            end = Offset(centerX, centerY + 20),
                            strokeWidth = 3f
                        )
                    }
                }
            }
        } ?: run {
            // Loading
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SpeechifyColors.Primary
            )
        }

        // Page navigation controls (if multiple pages)
        if (totalPages > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = SpeechifyColors.Surface.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous page
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous",
                            tint = if (currentPage > 0) SpeechifyColors.TextPrimary else SpeechifyColors.TextSecondary
                        )
                    }

                    // Page indicator
                    Text(
                        text = "${currentPage + 1} / $totalPages",
                        color = SpeechifyColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Next page
                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = if (currentPage < totalPages - 1) SpeechifyColors.TextPrimary else SpeechifyColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Speed selector dialog with slider
 */
@Composable
fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentSpeed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpeechifyColors.Surface,
        title = {
            Text(
                "Playback Speed",
                color = SpeechifyColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Speed display
                Text(
                    text = "${"%.2f".format(sliderValue)}x",
                    color = SpeechifyColors.Primary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Slider
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0.5f..2.0f,
                        steps = 29, // 0.05 increments
                        colors = SliderDefaults.colors(
                            thumbColor = SpeechifyColors.Primary,
                            activeTrackColor = SpeechifyColors.Primary,
                            inactiveTrackColor = SpeechifyColors.SurfaceVariant
                        )
                    )

                    // Min/Max labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "0.5x",
                            color = SpeechifyColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            "2.0x",
                            color = SpeechifyColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { preset ->
                        OutlinedButton(
                            onClick = { sliderValue = preset },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (sliderValue == preset) {
                                    SpeechifyColors.Primary.copy(alpha = 0.2f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (sliderValue == preset) SpeechifyColors.Primary else SpeechifyColors.SurfaceVariant
                            )
                        ) {
                            Text(
                                "${preset}x",
                                color = SpeechifyColors.TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSpeedSelected(sliderValue)
                onDismiss()
            }) {
                Text("Apply", color = SpeechifyColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpeechifyColors.TextSecondary)
            }
        }
    )
}

/**
 * Voice selector dialog
 */
data class VoiceOption(
    val id: String,
    val name: String,
    val language: String,
    val gender: String
)

val availableVoices = listOf(
    VoiceOption("clara", "Clara", "English", "Female"),
    VoiceOption("danna", "Anna", "English", "Female"),
    VoiceOption("djoey", "Joey", "English", "Female"),
    VoiceOption("matt", "Matt", "English", "Male")
)

fun getVoiceDisplayName(voiceId: String): String {
    return availableVoices.find { it.id == voiceId }?.name ?: "Matt"
}

@Composable
fun VoiceSelectorDialog(
    currentVoice: String,
    onVoiceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpeechifyColors.Surface,
        title = {
            Text(
                "Select Voice",
                color = SpeechifyColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableVoices.forEach { voice ->
                    Surface(
                        onClick = { onVoiceSelected(voice.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (voice.id == currentVoice) {
                            SpeechifyColors.Primary.copy(alpha = 0.2f)
                        } else {
                            SpeechifyColors.SurfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Gender icon
                                Icon(
                                    imageVector = if (voice.gender == "Male") {
                                        Icons.Default.Person
                                    } else {
                                        Icons.Default.Person
                                    },
                                    contentDescription = voice.gender,
                                    tint = if (voice.id == currentVoice) {
                                        SpeechifyColors.Primary
                                    } else {
                                        SpeechifyColors.TextSecondary
                                    }
                                )

                                Column {
                                    Text(
                                        text = voice.name,
                                        color = SpeechifyColors.TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = if (voice.id == currentVoice) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                    Text(
                                        text = "${voice.language} • ${voice.gender}",
                                        color = SpeechifyColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (voice.id == currentVoice) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = SpeechifyColors.Primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SpeechifyColors.Primary)
            }
        }
    )
}
