package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun LiveHighlightOverlay(viewModel: LiveOverlayViewModel) {
    // Lắng nghe 2 luồng dữ liệu từ ViewModel
    val words by viewModel.currentPageWords.collectAsState()
    val currentIndex by viewModel.currentLocalIndex.collectAsState()

    // Màu Highlight: Vàng nhạt, trong suốt 40%
    val highlightColor = Color(0xFFFFEB3B).copy(alpha = 0.4f)
    // Màu Viền: Vàng đậm hơn
    val borderColor = Color(0xFFFFC107)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Chỉ vẽ khi index hợp lệ
        if (words.isNotEmpty() && currentIndex >= 0 && currentIndex < words.size) {
            val currentWord = words[currentIndex]
            val bbox = currentWord.bbox

            // Tính toán tọa độ Rect từ 4 điểm của BBox
            // Lấy Min/Max để đảm bảo hình chữ nhật bao trọn chữ dù chữ có hơi nghiêng
            val left = minOf(bbox.x1, bbox.x4)
            val top = minOf(bbox.y1, bbox.y2)
            val right = maxOf(bbox.x2, bbox.x3)
            val bottom = maxOf(bbox.y3, bbox.y4)

            val rectWidth = right - left
            val rectHeight = bottom - top

            // 1. Vẽ nền (Fill)
            drawRect(
                color = highlightColor,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight)
            )

            // 2. Vẽ viền (Stroke) - Dày 3 pixel
            drawRect(
                color = borderColor,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 4f)
            )
        }
    }
}
