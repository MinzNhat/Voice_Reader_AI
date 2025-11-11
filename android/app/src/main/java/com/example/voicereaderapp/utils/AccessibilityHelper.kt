package com.example.voicereaderapp.utils

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Helper class for accessibility features.
 * Provides utilities for supporting visually impaired users.
 */
object AccessibilityHelper {
    /**
     * Checks if TalkBack or other screen reader is enabled.
     *
     * @param context Application context
     * @return true if screen reader is active
     */
    fun isScreenReaderEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as? AccessibilityManager
        return accessibilityManager?.isEnabled == true && 
               accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Checks if any accessibility service is enabled.
     *
     * @param context Application context
     * @return true if accessibility is enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as? AccessibilityManager
        return accessibilityManager?.isEnabled == true
    }

    /**
     * Gets recommended timeout for accessibility users.
     * Returns longer timeout if screen reader is enabled.
     *
     * @param context Application context
     * @param defaultTimeout Default timeout in milliseconds
     * @return Adjusted timeout for accessibility
     */
    fun getAccessibilityTimeout(context: Context, defaultTimeout: Long): Long {
        return if (isScreenReaderEnabled(context)) {
            defaultTimeout * 2 // Double timeout for screen reader users
        } else {
            defaultTimeout
        }
    }

    /**
     * Formats time duration for screen reader announcement.
     *
     * @param milliseconds Duration in milliseconds
     * @return Human-readable time description
     */
    fun formatTimeForAccessibility(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "$hours giờ ${minutes % 60} phút"
            minutes > 0 -> "$minutes phút ${seconds % 60} giây"
            else -> "$seconds giây"
        }
    }

    /**
     * Formats reading progress for screen reader.
     *
     * @param current Current position
     * @param total Total length
     * @return Progress description
     */
    fun formatProgressForAccessibility(current: Int, total: Int): String {
        val percentage = if (total > 0) (current * 100 / total) else 0
        return "Đã đọc $percentage phần trăm, vị trí $current trong tổng số $total"
    }

    /**
     * Creates accessibility-friendly button description.
     *
     * @param label Button label
     * @param action Action description
     * @return Complete accessibility description
     */
    fun createButtonDescription(label: String, action: String): String {
        return "$label, nút, $action"
    }

    /**
     * Creates accessibility description for document.
     *
     * @param title Document title
     * @param type Document type
     * @param wordCount Number of words
     * @return Accessibility description
     */
    fun createDocumentDescription(title: String, type: String, wordCount: Int): String {
        return "$title, tài liệu loại $type, $wordCount từ"
    }
}

/**
 * Extension function to add accessibility content description to Composable.
 *
 * @param description Content description for screen readers
 */
fun SemanticsPropertyReceiver.accessibilityDescription(description: String) {
    contentDescription = description
}

/**
 * Extension function for context to announce text via accessibility service.
 * Works with TalkBack and other screen readers.
 *
 * @param message Message to announce
 */
fun Context.announceForAccessibility(message: String) {
    val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) 
        as? AccessibilityManager
    if (accessibilityManager?.isEnabled == true) {
        // Create accessibility event
        val event = android.view.accessibility.AccessibilityEvent.obtain()
        event.eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
        event.text.add(message)
        accessibilityManager.sendAccessibilityEvent(event)
    }
}
