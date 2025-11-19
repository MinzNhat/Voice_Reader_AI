package com.example.voicereaderapp.ui.pdfreader

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.voicereaderapp.data.remote.model.OCRWord
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.ui.common.ReaderMode
import com.example.voicereaderapp.ui.common.UnifiedReaderScreen
import com.example.voicereaderapp.ui.common.VerticalReaderPanel
import com.example.voicereaderapp.ui.index.Screen
import com.example.voicereaderapp.ui.settings.SettingsViewModel
import java.io.File


/**
 * PDF Viewer Screen with OCR overlay and TTS playback
 * Uses UnifiedReaderScreen for UI, PDFViewerViewModel for logic
 *
 * Supports two modes:
 * 1. New import: fileUri provided → perform OCR
 * 2. Saved document: documentId provided → load from database
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(
    fileUri: Uri? = null,
    documentId: String? = null,
    navController: NavController,
    viewModel: PDFViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var documentTitle by remember { mutableStateOf<String?>(null) }
    var showTakeNoteDialog by remember { mutableStateOf(false) }

    // Handle new import (fileUri provided)
    LaunchedEffect(fileUri) {
        if (fileUri != null) {
            // Get original filename from URI
            val originalFilename = getOriginalFilename(context, fileUri)
            documentTitle = originalFilename

            val file = uriToFile(context, fileUri)
            pdfFile = file
            if (file != null) {
                // Pass original filename to ViewModel for saving
                viewModel.performOCR(file, originalFilename)
            }
        }
    }

    // Handle saved document (documentId provided)
    LaunchedEffect(documentId) {
        if (documentId != null) {
            viewModel.loadSavedDocument(documentId)
        }
    }

    // Calculate progress for slider (time-based with word timings)
    val progress = remember(uiState.wordTimings, uiState.currentWordIndex) {
        if (uiState.wordTimings.isNotEmpty() && uiState.currentWordIndex >= 0) {
            uiState.currentWordIndex.toFloat() / uiState.wordTimings.size
        } else {
            0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        UnifiedReaderScreen(
            title = uiState.documentTitle ?: documentTitle?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "PDF Document",
            mode = ReaderMode.PDF,
            content = {
                // PDF content with OCR text display
                PDFReaderContent(
                    ocrText = uiState.ocrText ?: "Performing OCR...",
                    currentWordIndex = uiState.currentWordIndex
                )
            },
            progress = progress,
            isPlaying = uiState.isPlaying,
            isLoading = uiState.isOCRProcessing || uiState.isGeneratingAudio,
            playbackSpeed = uiState.playbackSpeed,
            selectedVoice = uiState.selectedSpeaker,
            selectedLanguage = uiState.selectedLanguage,
            onPlayPause = {
                if (uiState.audioBase64 == null) {
                    // No audio yet - generate it and auto-play (fixes double-press issue)
                    viewModel.generateSpeech(autoPlay = true)
                } else {
                    if (uiState.isPlaying) {
                        viewModel.pauseAudio()
                    } else {
                        // Always use playAudio() which will start from current position
                        viewModel.playAudio()
                    }
                }
            },
            onRewind = {
                viewModel.rewind()
            },
            onForward = {
                viewModel.forward()
            },
            onSeek = { fraction ->
                viewModel.seekToFraction(fraction)
            },
            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
            onVoiceChange = { voiceId, language ->
                viewModel.setVoiceAndLanguage(voiceId, language)
            },
            onTakeNote = { showTakeNoteDialog = true },
            onBack = { navController.popBackStack() }
        )

        // Take Note Dialog
        if (showTakeNoteDialog) {
            com.example.voicereaderapp.ui.common.TakeNoteDialog(
                documentTitle = uiState.documentTitle ?: documentTitle?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "PDF Document",
                onDismiss = { showTakeNoteDialog = false },
                onSaveNote = { note ->
                    // TODO: Save note to database
                    android.util.Log.d("PDFViewerScreen", "Note saved: $note")
                }
            )
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
 * PDF Reader content with OCR text display
 * Simplified version - controls are handled by UnifiedReaderScreen
 */
@Composable
private fun PDFReaderContent(
    ocrText: String,
    currentWordIndex: Int
) {
    val words = remember(ocrText) { ocrText.split(Regex("\\s+")) }
    val scrollState = rememberLazyListState()

    // Auto-scroll when reading
    LaunchedEffect(currentWordIndex) {
        if (currentWordIndex >= 0 && currentWordIndex < words.size) {
            val lineIndex = (currentWordIndex / 20).coerceAtLeast(0)
            scrollState.animateScrollToItem(lineIndex)
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            fontFamily = FontFamily.Serif,
                            fontSize = 20.sp,  // Larger for better readability
                            lineHeight = 32.sp,  // More spacing like the image
                            letterSpacing = 0.15.sp  // Better letter spacing
                        ),
                        color = if (highlight)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
                    )
                }
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

// Helper to get original filename from URI
private fun getOriginalFilename(context: Context, uri: Uri): String {
    var filename = "Document"

    try {
        // Try to get filename from ContentResolver
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        filename = cursor.getString(nameIndex)
                    }
                }
            }
        } else if (uri.scheme == "file") {
            // For file:// URIs, get the last path segment
            filename = uri.lastPathSegment ?: "Document"
        }
    } catch (e: Exception) {
        android.util.Log.e("PDFViewerScreen", "Failed to get original filename", e)
    }

    return filename
}

// Helper to convert Uri to File
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)

        // Detect file extension from MIME type or filename
        val mimeType = context.contentResolver.getType(uri)
        val extension = when {
            mimeType?.startsWith("image/") == true -> {
                when (mimeType) {
                    "image/png" -> ".png"
                    "image/jpeg" -> ".jpg"
                    "image/jpg" -> ".jpg"
                    else -> ".img"
                }
            }
            mimeType == "application/pdf" -> ".pdf"
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
            mimeType == "application/msword" -> ".doc"
            else -> {
                // Try to get extension from filename
                val filename = getOriginalFilename(context, uri)
                val dotIndex = filename.lastIndexOf('.')
                if (dotIndex > 0) {
                    val ext = filename.substring(dotIndex)
                    when (ext.lowercase()) {
                        ".pdf", ".docx", ".doc", ".png", ".jpg", ".jpeg" -> ext
                        else -> ".pdf" // Default fallback
                    }
                } else ".pdf"
            }
        }

        val tempFile = File.createTempFile("upload", extension, context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream?.copyTo(output)
        }
        inputStream?.close()
        tempFile
    } catch (e: Exception) {
        android.util.Log.e("PDFViewerScreen", "Failed to convert URI to file", e)
        null
    }
}
