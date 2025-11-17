package com.example.voicereaderapp.ui.pdfreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.voicereaderapp.data.remote.model.OCRWord
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.ui.index.Screen
import java.io.File


/**
 * PDF Viewer Screen with OCR overlay and TTS playback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(
    fileUri: Uri,
    navController: NavController,
    viewModel: PDFViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }

    // Convert URI to File and perform OCR
    LaunchedEffect(fileUri) {
        val file = uriToFile(context, fileUri)
        pdfFile = file
        if (file != null) {
            viewModel.performOCR(file)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Reader") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // OCR Button
                    IconButton(
                        onClick = {
                            pdfFile?.let { viewModel.performOCR(it) }
                        },
                        enabled = !uiState.isOCRProcessing && pdfFile != null
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
                currentWordIndex = uiState.currentWordIndex,
                ocrImageWidth = uiState.ocrImageWidth,
                ocrImageHeight = uiState.ocrImageHeight
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

    Button(
        onClick = {
            uiState.ocrText?.let { text ->
                val newDoc = ReadingDocument(
                    id = "doc_${System.currentTimeMillis()}",
                    title = "PDF Document",
                    content = text,
                    type = DocumentType.PDF,
                    createdAt = System.currentTimeMillis(),
                    lastReadPosition = 0
                )
                // Inject DocumentRepository and save
                // Then navigate to Reader
                navController.navigate(Screen.Reader.createRoute(newDoc.id))
            }
        }
    ) {
        Text("Start Reading")
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
