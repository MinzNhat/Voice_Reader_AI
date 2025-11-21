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
import androidx.compose.material.icons.filled.NoteAdd
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
    onClickSpeed: () -> Unit,
    onClickVoice: () -> Unit,
    onTakeNote: () -> Unit = {},  // <-- Added for Take Note feature
    onClose: () -> Unit,
    // Global settings enforcement
    isSpeedEnforced: Boolean = false,
    enforcedSpeed: Float = 1.0f,
    isVoiceEnforced: Boolean = false,
    enforcedVoiceName: String = ""
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
                    .clickable { onClose() },
                tint = MaterialTheme.colorScheme.onSurface
            )

            // Speed label (1.0x) - Shows enforced speed if enabled
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${String.format("%.1f", if (isSpeedEnforced) enforcedSpeed else speed)}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSpeedEnforced)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .onGloballyPositioned { speedIconY = it.positionInRoot().y }
                        .clickable(enabled = !isSpeedEnforced) { onClickSpeed() }
                )
                if (isSpeedEnforced) {
                    Text(
                        "Global",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Voice icon - Shows if enforced
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Voices",
                    modifier = Modifier
                        .size(26.dp)
                        .onGloballyPositioned { voiceIconY = it.positionInRoot().y }
                        .clickable(enabled = !isVoiceEnforced) { onClickVoice()},
                    tint = if (isVoiceEnforced)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (isVoiceEnforced) {
                    Text(
                        "Global",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Take Note icon
            Icon(
                Icons.Default.NoteAdd,
                contentDescription = "Take Note",
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onTakeNote() },
                tint = MaterialTheme.colorScheme.primary
            )

            // Other icons (can be used for future features)
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Default.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
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
                    Text(
                        "Speed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Show warning if speed is enforced
                    if (isSpeedEnforced) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Speed is set globally (${String.format("%.1f", enforcedSpeed)}x). Change in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Slider(
                        value = if (isSpeedEnforced) enforcedSpeed else speed,
                        valueRange = 0.5f..2f,
                        onValueChange = { if (!isSpeedEnforced) onSpeedChange(it) },
                        modifier = Modifier.height(32.dp),
                        enabled = !isSpeedEnforced
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
                    Text(
                        "Voices",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))

                    // Show warning if voice is enforced
                    if (isVoiceEnforced) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Voice is set globally ($enforcedVoiceName). Change in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    listOf("Emma", "Michael", "Anna").forEach { voice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVoice(voice) }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                voice,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

