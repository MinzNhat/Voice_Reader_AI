package com.example.voicereaderapp.utils

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Gesture helper for accessibility navigation.
 * Provides custom gestures optimized for visually impaired users.
 */
object GestureHelper {
    /**
     * Gesture types for navigation.
     */
    enum class AccessibilityGesture {
        SINGLE_TAP,         // Single tap - select/activate
        DOUBLE_TAP,         // Double tap - confirm action
        LONG_PRESS,         // Long press - open context menu
        SWIPE_RIGHT,        // Swipe right - next item
        SWIPE_LEFT,         // Swipe left - previous item
        SWIPE_UP,           // Swipe up - scroll up/increase
        SWIPE_DOWN,         // Swipe down - scroll down/decrease
        TWO_FINGER_TAP,     // Two finger tap - pause/play
        THREE_FINGER_SWIPE  // Three finger swipe - special action
    }

    /**
     * Describes gesture for voice feedback.
     *
     * @param gesture Gesture type
     * @return Vietnamese description
     */
    fun getGestureDescription(gesture: AccessibilityGesture): String {
        return when (gesture) {
            AccessibilityGesture.SINGLE_TAP -> "Chạm một lần để chọn"
            AccessibilityGesture.DOUBLE_TAP -> "Chạm hai lần để xác nhận"
            AccessibilityGesture.LONG_PRESS -> "Giữ lâu để mở menu"
            AccessibilityGesture.SWIPE_RIGHT -> "Vuốt phải để chuyển mục tiếp theo"
            AccessibilityGesture.SWIPE_LEFT -> "Vuốt trái để quay lại mục trước"
            AccessibilityGesture.SWIPE_UP -> "Vuốt lên để cuộn lên hoặc tăng"
            AccessibilityGesture.SWIPE_DOWN -> "Vuốt xuống để cuộn xuống hoặc giảm"
            AccessibilityGesture.TWO_FINGER_TAP -> "Chạm hai ngón để tạm dừng hoặc phát"
            AccessibilityGesture.THREE_FINGER_SWIPE -> "Vuốt ba ngón để thực hiện hành động đặc biệt"
        }
    }

    /**
     * Standard gesture instructions for screen readers.
     */
    object Instructions {
        const val BUTTON = "Chạm hai lần để kích hoạt"
        const val SLIDER = "Vuốt lên để tăng, vuốt xuống để giảm"
        const val LIST_ITEM = "Chạm hai lần để mở, vuốt phải để xem mục tiếp theo"
        const val BACK = "Chạm hai lần để quay lại"
        const val PLAY_PAUSE = "Chạm hai lần để phát hoặc tạm dừng"
        const val SETTINGS = "Chạm hai lần để mở cài đặt"
    }
}

/**
 * Modifier extension for accessibility tap handling.
 * Provides longer press delay for accessibility users.
 *
 * @param onTap Single tap callback
 * @param onDoubleTap Double tap callback
 * @param onLongPress Long press callback
 */
fun Modifier.accessibleTapGesture(
    onTap: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onTap = { onTap?.invoke() },
        onDoubleTap = { onDoubleTap?.invoke() },
        onLongPress = { onLongPress?.invoke() }
    )
}
