package com.example.voicereaderapp.ui.scanner

<<<<<<< HEAD
import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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

    // Request permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processImage(uri.toString())
            navController.navigate(Screen.DocumentList.route)
        }
    }

    // Ask for camera permission
    LaunchedEffect(true) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vui lòng cấp quyền camera để sử dụng tính năng này.")
        }
        return
    }

    // Camera capture use case
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Camera preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onCaptureReady = { capture -> imageCapture = capture }
        )

        // ====== NÚT CHỤP ẢNH ======
        IconButton(
            onClick = {
                val photoFile = createImageFile(context)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("ScannerScreen", "Capture failed: ${exc.message}")
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            viewModel.processImage(photoFile.path)
                            navController.navigate(Screen.DocumentList.route)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
        ) {}

        // ====== NÚT MỞ THƯ VIỆN ======
        IconButton(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
                .background(Color.White.copy(alpha = 0.85f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Photo,
                contentDescription = "Gallery",
                tint = Color.Black
            )
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
    onCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
                onCaptureReady(imageCapture)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("ScannerScreen", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}
=======
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Screen for scanning images and extracting text.
 * Allows users to capture images using camera or select from gallery,
 * then extracts text using OCR.
 *
 * @param viewModel ViewModel managing scanner state
 */
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Scanner",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.isProcessing) {
            CircularProgressIndicator()
            Text(
                text = "Processing image...",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            // Camera button
            Button(
                onClick = { viewModel.openCamera() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Take Photo")
            }

            // Gallery button
            Button(
                onClick = { viewModel.openGallery() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select from Gallery")
            }

            // Show extracted text if available
            uiState.extractedText?.let { text ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Extracted Text:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(text = text)
                    }
                }
            }
        }
    }
}
>>>>>>> origin/cd
