package com.example.voicereaderapp.ui.scanner

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
