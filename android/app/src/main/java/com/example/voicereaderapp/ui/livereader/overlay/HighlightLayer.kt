package com.example.voicereaderapp.ui.livereader.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@Composable
fun HighlightLayer(viewModel: LiveOverlayViewModel) {
    val lines = viewModel.getLines() // lấy danh sách chữ thành các dòng
    val listState = rememberLazyListState()
    val currentIdx by viewModel.currentIndex.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Layer phủ full màn hình, trong suốt
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Nếu drag đủ lớn (> 20px) thì update highlight
                    if (dragAmount.getDistance() > 20f) {
                        val firstVisible = listState.firstVisibleItemIndex
                        viewModel.updateHighlightIdx(firstVisible)
                    }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(16.dp)
        ){
            itemsIndexed(lines) { index, line ->
                val isCurrent = index == currentIdx
                val isPrev = index == currentIdx - 1
                val isNext = index == currentIdx + 1

                // màu highlight
                val bgColor = when {
                    isCurrent -> Color.Yellow.copy(alpha = 0.4f)
                    isPrev || isNext -> Color.Yellow.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ){
                    // tạm thời print ra
                    Text(text = line, color = Color.Black)
                }
            }
        }

        LaunchedEffect(currentIdx) {
            coroutineScope.launch {
                listState.animateScrollToItem(currentIdx)
            }
        }
    }
}
