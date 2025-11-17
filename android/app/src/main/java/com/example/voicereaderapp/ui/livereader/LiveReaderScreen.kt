package com.example.voicereaderapp.ui.livereader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Screen for live screen reading.
 * Captures and reads text from the current screen in real-time.
 * Requires screen capture permissions.
 *
 * @param viewModel ViewModel managing live reader state
 */
@Composable
fun LiveReaderScreen(
    viewModel: LiveReaderViewModel = hiltViewModel()
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
            text = "Live Screen Reader",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Status: ${if (uiState.isReading) "Reading..." else "Stopped"}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Start/Stop button
        Button(
            onClick = {
                if (uiState.isReading) {
                    viewModel.stopReading()
                } else {
                    viewModel.startReading()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isReading) "Stop Reading" else "Start Reading")
        }

        // Display captured text
        uiState.capturedText?.let { text ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Captured Text:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(text = text)
                }
            }
        }

        // Error message
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
