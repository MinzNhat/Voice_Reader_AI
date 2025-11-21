package com.example.voicereaderapp.ui.scanner

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.voicereaderapp.ui.index.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScannerScreen(
    navController: NavController,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    // Request permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }


    // Navigate to PDFViewer after successful processing
    // Watch all three states so navigation triggers when processing completes
    LaunchedEffect(uiState.documentId, uiState.isProcessing, uiState.isGeneratingAudio) {
        Log.d("ScannerScreen", "State changed - docId: ${uiState.documentId}, processing: ${uiState.isProcessing}, generating: ${uiState.isGeneratingAudio}")

        if (uiState.documentId != null && !uiState.isProcessing && !uiState.isGeneratingAudio) {
            // Small delay to show success state
            kotlinx.coroutines.delay(500)
            // Navigate to the PDF viewer screen, removing scanner from back stack
            Log.d("ScannerScreen", "✅ Navigating to viewer with documentId: ${uiState.documentId}")
            navController.navigate("pdf_saved/${uiState.documentId}?source=scanner") {
                // Remove scanner from back stack, so back button goes to IndexScreen
                popUpTo(Screen.Index.route) {
                    inclusive = false
                }
            }
        }
    }

    // Reset state and ask for camera permission on first load
    LaunchedEffect(true) {
        viewModel.resetState()
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please grant camera permission to use this feature.")
        }
        return
    }

    // Camera capture use case
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: androidx.camera.core.Camera? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var isCaptureReady by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var frozenFrame: Bitmap? by remember { mutableStateOf(null) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var lastAppliedZoom by remember { mutableStateOf(1f) }

    // Apply zoom to camera only when it changes
    LaunchedEffect(zoomRatio) {
        if (camera != null && zoomRatio != lastAppliedZoom) {
            camera?.let { cam ->
                try {
                    val cameraControl = cam.cameraControl
                    val cameraInfo = cam.cameraInfo

                    val minZoom = cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                    val maxZoom = cameraInfo.zoomState.value?.maxZoomRatio ?: 10f

                    // Clamp zoom ratio to camera's supported range
                    val clampedZoom = zoomRatio.coerceIn(minZoom, maxZoom)

                    cameraControl.setZoomRatio(clampedZoom)
                    lastAppliedZoom = clampedZoom
                    Log.d("ScannerScreen", "Applied zoom: $clampedZoom (was: $lastAppliedZoom)")
                } catch (e: Exception) {
                    Log.e("ScannerScreen", "Failed to apply zoom", e)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Camera preview with zoom (hide when frozen frame is shown)
        if (frozenFrame == null) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                currentZoom = zoomRatio,
                onZoomChange = { newZoom -> zoomRatio = newZoom },
                onCaptureReady = { capture, cam, preview ->
                    imageCapture = capture
                    camera = cam
                    previewView = preview
                    isCaptureReady = true
                    Log.d("ScannerScreen", "Camera ready for capture")
                }
            )
        } else {
            // Show frozen frame
            Image(
                bitmap = frozenFrame!!.asImageBitmap(),
                contentDescription = "Captured frame",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // ====== BACK BUTTON ======
        IconButton(
            onClick = {
                Log.d("ScannerScreen", "Back button clicked")
                navController.navigateUp()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 48.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // ====== NÚT CHỤP ẢNH ======
        IconButton(
            onClick = {
                if (imageCapture == null || previewView == null) {
                    Log.e("ScannerScreen", "ImageCapture or PreviewView is null, camera not ready")
                    return@IconButton
                }

                if (uiState.isProcessing || uiState.isGeneratingAudio || isCapturing) {
                    Log.d("ScannerScreen", "Already processing, ignoring click")
                    return@IconButton
                }

                // Capture the current preview frame to freeze the screen
                try {
                    val bitmap = previewView?.bitmap
                    if (bitmap != null) {
                        frozenFrame = bitmap
                        Log.d("ScannerScreen", "Captured frozen frame: ${bitmap.width}x${bitmap.height}")
                    }
                } catch (e: Exception) {
                    Log.e("ScannerScreen", "Failed to capture preview frame", e)
                }

                // Show capturing state immediately
                isCapturing = true

                Log.d("ScannerScreen", "Starting image capture...")
                val photoFile = createImageFile(context)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("ScannerScreen", "Capture failed: ${exc.message}", exc)
                            isCapturing = false
                            frozenFrame = null // Restore camera preview
                            viewModel.resetState()
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("ScannerScreen", "Image saved to: ${photoFile.absolutePath}")
                            // Keep frozen frame until processing starts
                            if (photoFile.exists()) {
                                Log.d("ScannerScreen", "File exists, size: ${photoFile.length()} bytes")
                                viewModel.processImage(photoFile.absolutePath)
                            } else {
                                Log.e("ScannerScreen", "Photo file doesn't exist!")
                                isCapturing = false
                                frozenFrame = null // Restore camera preview
                                viewModel.resetState()
                            }
                            // Navigation is handled by LaunchedEffect after processing completes
                        }
                    }
                )
            },
            enabled = isCaptureReady && !uiState.isProcessing && !uiState.isGeneratingAudio && !isCapturing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(80.dp)
                .background(
                    if (isCaptureReady && !uiState.isProcessing && !isCapturing)
                        Color.White.copy(alpha = 0.9f)
                    else
                        Color.Gray.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {}

        // Processing overlay (show over frozen frame)
        if (uiState.isProcessing || uiState.isGeneratingAudio) {
            // Hide capturing state once processing starts
            LaunchedEffect(Unit) {
                isCapturing = false
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = when {
                            uiState.isProcessing -> "Processing image..."
                            uiState.isGeneratingAudio -> "Generating audio..."
                            else -> "Processing..."
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    // Cancel button
                    TextButton(
                        onClick = {
                            Log.d("ScannerScreen", "User cancelled processing")
                            frozenFrame = null // Restore camera preview
                            viewModel.resetState()
                        }
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            }
        }

        // Error display
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            Log.d("ScannerScreen", "Error dismissed")
                            frozenFrame = null // Restore camera preview
                            viewModel.resetState()
                        }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

private fun createImageFile(context: android.content.Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.cacheDir
    return File(storageDir, "IMG_$timestamp.jpg")
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    onCaptureReady: (ImageCapture, androidx.camera.core.Camera, PreviewView) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isZooming by remember { mutableStateOf(false) }

    // Use a mutable ref to track accumulated zoom - this won't be captured by AndroidView closure
    val accumulatedZoom = remember { mutableStateOf(1f) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({

                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // ImageCapture use case
                    val imageCapture = ImageCapture.Builder().build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )

                        // Pass imageCapture, camera, and previewView
                        onCaptureReady(imageCapture, camera, previewView)

                    } catch (e: Exception) {
                        Log.e("ScannerScreen", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { _, _, zoomFactor, _ ->
                            if (zoomFactor != 1f) {
                                isZooming = true
                                // Use the mutable ref instead of currentZoom param
                                val newZoom = (accumulatedZoom.value * zoomFactor).coerceIn(0.5f, 10f)
                                accumulatedZoom.value = newZoom
                                Log.d("ScannerScreen", "Zoom gesture: accumulated=${accumulatedZoom.value}, factor=$zoomFactor, new=$newZoom")
                                onZoomChange(newZoom)
                            }
                        }
                    )
                }
        )

        // Display zoom level
        if (currentZoom > 1.1f || currentZoom < 0.9f) {
            Text(
                text = "${String.format("%.1f", currentZoom)}x",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        // Hide zoom indicator after zooming stops
        LaunchedEffect(isZooming) {
            if (isZooming) {
                kotlinx.coroutines.delay(2000)
                isZooming = false
            }
        }
    }
}
