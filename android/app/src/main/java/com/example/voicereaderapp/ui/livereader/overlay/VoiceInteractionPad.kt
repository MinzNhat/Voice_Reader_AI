package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.BoxScope
import kotlinx.coroutines.delay


@Composable
fun BoxScope.VoiceInteractionPad(viewModel: LiveOverlayViewModel) {
    val isListening by viewModel.isListening.collectAsState()

    // Mic nhấp nháy
    if (isListening) {
        val infiniteTransition = rememberInfiniteTransition(label = "micBlinkTransition")
        val micAlpha by infiniteTransition.animateFloat(
            initialValue = 1f, // Bắt đầu từ mờ đục
            targetValue = 0.2f, // Đi đến gần trong suốt
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse // Lặp lại bằng cách đảo ngược (0.2 -> 1)
            ),
            label = "micAlpha"
        )


        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Listening",
            tint = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter) // .align() chỉ hoạt động trong một scope như Box
                .padding(bottom = 100.dp)
                .size(72.dp)
                .alpha(micAlpha)
        )

    }

}



