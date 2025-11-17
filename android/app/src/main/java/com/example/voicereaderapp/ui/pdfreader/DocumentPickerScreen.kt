package com.example.voicereaderapp.ui.pdfreader

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.voicereaderapp.utils.PDFHelper
import java.io.File

/**
 * Document Picker Screen
 * Shows uploaded documents and allows picking new PDFs/images
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPickerScreen(
    onDocumentSelected: (File) -> Unit
) {
    val context = LocalContext.current
    var selectedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // File picker launcher for PDFs
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            errorMessage = null

            try {
                // Copy URI to temporary file
                val tempFile = PDFHelper.copyUriToTempFile(context, it)

                if (tempFile != null) {
                    selectedFiles = selectedFiles + tempFile
                    isProcessing = false
                } else {
                    errorMessage = "Failed to load file"
                    isProcessing = false
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error loading file"
                isProcessing = false
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            errorMessage = null

            try {
                val tempFile = copyImageToTempFile(context, it)

                if (tempFile != null) {
                    selectedFiles = selectedFiles + tempFile
                    isProcessing = false
                } else {
                    errorMessage = "Failed to load image"
                    isProcessing = false
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error loading image"
                isProcessing = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,  // Dark background
        topBar = {
            TopAppBar(
                title = { Text("Voice Reader AI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,  // Dark surface
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pick PDF Button
                FloatingActionButton(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.PictureAsPdf, "Pick PDF")
                }

                // Pick Image Button
                FloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Image, "Pick Image")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Instructions Card
            if (selectedFiles.isEmpty() && !isProcessing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Welcome to Voice Reader AI",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Upload a PDF or image to get started:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "1. Tap the PDF or Image button below",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. Select a file from your device",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "3. Tap to open and start OCR + TTS",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Loading indicator
            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading file...")
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Document list
            if (selectedFiles.isNotEmpty()) {
                Text(
                    text = "Uploaded Documents",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedFiles) { file ->
                        DocumentCard(
                            file = file,
                            onClick = { onDocumentSelected(file) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Document card showing file info
 */
@Composable
fun DocumentCard(
    file: File,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on file type
            Icon(
                imageVector = if (file.extension.lowercase() == "pdf") {
                    Icons.Default.PictureAsPdf
                } else {
                    Icons.Default.Image
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.nameWithoutExtension,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${file.extension.uppercase()} • ${formatFileSize(file.length())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open"
            )
        }
    }
}

/**
 * Copy image URI to temp file
 */
private fun copyImageToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("image_temp", ".jpg", context.cacheDir)

        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Format file size for display
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
