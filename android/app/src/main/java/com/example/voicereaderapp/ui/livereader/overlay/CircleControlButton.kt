package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Floating circle button for live scan control
 * Similar to iOS AssistiveTouch - can be dragged and snaps to edges
 */
@Composable
fun CircleControlButton(
    viewModel: LiveOverlayViewModel
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Screen dimensions
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val buttonSizePx = with(density) { 60.dp.toPx() }

    // Position state
    var offsetX by remember { mutableStateOf(screenWidthPx - buttonSizePx - 16f) } // Start on right
    var offsetY by remember { mutableStateOf(screenHeightPx / 2) } // Center vertically

    // Animated position for smooth snapping
    val animatedOffsetX = remember { Animatable(offsetX) }
    val animatedOffsetY = remember { Animatable(offsetY) }

    // Update animated position when offset changes
    LaunchedEffect(offsetX, offsetY) {
        animatedOffsetX.animateTo(
            offsetX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        animatedOffsetY.animateTo(
            offsetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    animatedOffsetX.value.roundToInt(),
                    animatedOffsetY.value.roundToInt()
                )
            }
            .size(60.dp)
            .shadow(8.dp, CircleShape)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragStartX = down.position.x
                        var dragStartY = down.position.y

                        // Long press job for voice listening
                        val longPressJob = launch {
                            down.consume()
                            kotlinx.coroutines.delay(500L)
                            viewModel.onVoiceListeningStart()
                        }

                        var isDragging = false
                        var dragConsumed = false

                        // Handle drag
                        drag(down.id) { change ->
                            val dragAmount = change.positionChange()

                            // Check if significant drag occurred
                            if (!isDragging && (abs(dragAmount.x) > 10 || abs(dragAmount.y) > 10)) {
                                isDragging = true
                                longPressJob.cancel()
                                if (viewModel.isListening.value) {
                                    viewModel.onVoiceListeningEnd()
                                }
                            }

                            if (isDragging) {
                                // Update position during drag
                                offsetX = (offsetX + dragAmount.x).coerceIn(
                                    0f,
                                    screenWidthPx - buttonSizePx
                                )
                                offsetY = (offsetY + dragAmount.y).coerceIn(
                                    0f,
                                    screenHeightPx - buttonSizePx
                                )
                                change.consume()
                            }

                            // Check for horizontal swipe to expand overlay
                            val totalDragX = change.position.x - dragStartX
                            if (abs(totalDragX) > 50 && !dragConsumed) {
                                longPressJob.cancel()
                                if (viewModel.isListening.value) {
                                    viewModel.onVoiceListeningEnd()
                                }
                                viewModel.expandOverlay()
                                dragConsumed = true
                            }
                        }

                        longPressJob.cancel()
                        if (viewModel.isListening.value) {
                            viewModel.onVoiceListeningEnd()
                        }

                        // Snap to nearest edge (left or right) after drag
                        if (isDragging) {
                            val snapToLeft = offsetX < screenWidthPx / 2
                            offsetX = if (snapToLeft) {
                                16f // Left edge with padding
                            } else {
                                screenWidthPx - buttonSizePx - 16f // Right edge with padding
                            }

                            // Keep Y within bounds
                            offsetY = offsetY.coerceIn(
                                50f, // Top padding
                                screenHeightPx - buttonSizePx - 50f // Bottom padding
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Control",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Simplified Circle Button - position managed by WindowManager
 * Now includes edge snapping after drag ends
 */
@Composable
fun CircleControlButtonSimple(
    viewModel: LiveOverlayViewModel,
    onPositionChange: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .shadow(8.dp, CircleShape)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragStartX = down.position.x
                        var dragStartY = down.position.y

                        // Long press job for voice listening
                        val longPressJob = launch {
                            down.consume()
                            kotlinx.coroutines.delay(500L)
                            viewModel.onVoiceListeningStart()
                        }

                        var isDragging = false
                        var hasMoved = false

                        // Handle drag
                        drag(down.id) { change ->
                            val dragAmount = change.positionChange()

                            // Check if significant drag occurred
                            if (!isDragging && (abs(dragAmount.x) > 10 || abs(dragAmount.y) > 10)) {
                                isDragging = true
                                hasMoved = true
                                longPressJob.cancel()
                                if (viewModel.isListening.value) {
                                    viewModel.onVoiceListeningEnd()
                                }
                            }

                            if (isDragging) {
                                // Report delta position change to move the window
                                onPositionChange(dragAmount.x, dragAmount.y)
                                change.consume()
                            }
                        }

                        longPressJob.cancel()
                        if (viewModel.isListening.value) {
                            viewModel.onVoiceListeningEnd()
                        }

                        // If user tapped (didn't drag), open the overlay panel
                        if (!hasMoved && !isDragging) {
                            viewModel.expandOverlay()
                        }

                        // Notify service to snap to edge after drag ends
                        if (isDragging) {
                            onDragEnd()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Control",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
