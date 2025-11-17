package com.example.voicereaderapp.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicereaderapp.ui.settings.SettingsViewModel

@Composable
fun VerticalReaderPanel(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    selectedVoice: String,
    onSelectVoice: (String) -> Unit,
    onClickSpeed: () -> Unit,     // <-- thêm
    onClickVoice: () -> Unit,
    onClose: () -> Unit
) {
    var showSpeed by remember { mutableStateOf(false) }
    var showVoices by remember { mutableStateOf(false) }

    var speedIconY by remember { mutableStateOf(0f) }
    var voiceIconY by remember { mutableStateOf(0f) }

    Box(Modifier.fillMaxSize()) {

        // ===== Toolbar =====
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    RoundedCornerShape(26.dp)
                )
                .padding(vertical = 18.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Close button
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onClose() }
            )

            // Speed label (1.0x)
            Text(
                "${String.format("%.1f", speed)}x",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .onGloballyPositioned { speedIconY = it.positionInRoot().y }
                    .clickable { onClickSpeed() }
            )

            // Voice icon
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Voices",
                modifier = Modifier
                    .size(26.dp)
                    .onGloballyPositioned { voiceIconY = it.positionInRoot().y }
                    .clickable { onClickVoice()}
            )

            // Other icons
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(26.dp))
            Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(26.dp))
            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(26.dp))
        }

        // ============ SPEED POPUP ============
        if (showSpeed) {
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .offset(x = (-140).dp, y = speedIconY.dp - 35.dp)
                    .width(140.dp)
                    .height(65.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Speed", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Slider(
                        value = speed,
                        valueRange = 0.5f..2f,
                        onValueChange = onSpeedChange,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        // ============ VOICES POPUP ============
        if (showVoices) {
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .offset(x = (-160).dp, y = voiceIconY.dp - 40.dp)
                    .width(150.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Voices", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))

                    listOf("Emma", "Michael", "Anna").forEach { voice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVoice(voice) }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(voice)
                        }
                    }
                }
            }
        }
    }
}

