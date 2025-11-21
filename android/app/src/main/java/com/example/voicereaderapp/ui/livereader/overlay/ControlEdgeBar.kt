package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs


@Composable
fun BoxScope.ControlEdgeBar(
    viewModel: LiveOverlayViewModel
) {
    val isReading by viewModel.isReading.collectAsState()

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(y = 200.dp) // Position from center
            .size(width = 40.dp, height = 90.dp) // Vùng chạm
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val longPressJob = launch {
                            down.consume()
                            kotlinx.coroutines.delay(500L)
                            viewModel.onVoiceListeningStart()
                        }
                        var dragConsumed = false
                        do {
                            val event = awaitPointerEvent()
                            val dragDistanceX = event.changes.first().position.x - down.position.x
                            if (abs(dragDistanceX) > 15) {
                                if (!dragConsumed) {
                                    longPressJob.cancel()
                                    if (viewModel.isListening.value) {
                                        viewModel.onVoiceListeningEnd()
                                    }
                                    // ✅ Gọi hàm trong ViewModel để mở rộng
                                    viewModel.expandOverlay()
                                    dragConsumed = true
                                }
                            }
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })

                        longPressJob.cancel()
                        if (viewModel.isListening.value) {
                            viewModel.onVoiceListeningEnd()
                        }
                    }
                }
            }
    ) {
        // Edge bar - changes color when reading (lighter grey)
        Box(
            modifier = Modifier
                .size(width = 7.dp, height = 80.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}

/**
 * Simplified EdgeBar without BoxScope - used when window is positioned by WindowManager
 * Now supports vertical dragging to reposition the bar
 */
@Composable
fun ControlEdgeBarSimple(
    viewModel: LiveOverlayViewModel,
    onPositionChange: (Float, Float) -> Unit
) {
    val isReading by viewModel.isReading.collectAsState()

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 90.dp) // Vùng chạm
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragStartX = down.position.x
                        var dragStartY = down.position.y

                        val longPressJob = launch {
                            down.consume()
                            kotlinx.coroutines.delay(500L)
                            viewModel.onVoiceListeningStart()
                        }

                        var isDragging = false
                        var dragConsumed = false

                        do {
                            val event = awaitPointerEvent()
                            val currentChange = event.changes.first()
                            val dragDistanceX = currentChange.position.x - dragStartX
                            val dragDistanceY = currentChange.position.y - dragStartY

                            // Check if this is a vertical drag (for repositioning)
                            if (!isDragging && abs(dragDistanceY) > 10) {
                                isDragging = true
                                longPressJob.cancel()
                                if (viewModel.isListening.value) {
                                    viewModel.onVoiceListeningEnd()
                                }
                            }

                            // Check if this is a horizontal swipe (for expanding)
                            if (abs(dragDistanceX) > 50 && !dragConsumed) {
                                if (!dragConsumed) {
                                    longPressJob.cancel()
                                    if (viewModel.isListening.value) {
                                        viewModel.onVoiceListeningEnd()
                                    }
                                    viewModel.expandOverlay()
                                    dragConsumed = true
                                }
                            }

                            // If dragging vertically, update position
                            if (isDragging && !dragConsumed) {
                                val deltaY = currentChange.position.y - currentChange.previousPosition.y
                                onPositionChange(0f, deltaY)
                            }

                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })

                        longPressJob.cancel()
                        if (viewModel.isListening.value) {
                            viewModel.onVoiceListeningEnd()
                        }
                    }
                }
            }
    ) {
        // Edge bar - changes color when reading (lighter grey)
        Box(
            modifier = Modifier
                .size(width = 7.dp, height = 80.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}
