package com.example.voicereaderapp.ui.index

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector


/**
 * Định nghĩa các tuyến đường (routes) trong ứng dụng theo luồng mới:
 * Index -> Màn hình chính (Home Hub)
 * Scanner -> Màn hình quét ảnh
 * DocumentList -> Màn hình danh sách tài liệu (Continue Reading / Import PDF)
 * Reader -> Màn hình đọc/nghe tập trung (Live)
 */
sealed class Screen(val route: String) {
    object Index : Screen("index_screen")
    object Scanner : Screen("scanner_screen")
    object DocumentList : Screen("document_list_screen")
    object Reader : Screen("reader_screen/{documentId}") {
        fun createRoute(documentId: String) = "reader_screen/$documentId"
    }
}
