package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs


@Composable
fun ControlEdgeBar( // Không cần BoxScope nữa
    viewModel: LiveOverlayViewModel
) {
    Box(
        modifier = Modifier
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
        // Thanh màu xám trực quan
        Box(
            modifier = Modifier
                .size(width = 7.dp, height = 80.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp)
                .background(Color.Gray, shape = RoundedCornerShape(3.dp))
        )
    }
}
