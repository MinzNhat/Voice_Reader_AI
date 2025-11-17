package com.example.voicereaderapp.ui.common

/**
 * Enum representing different reading modes
 */
enum class ReaderMode {
    /**
     * Plain text reading mode (used by ReaderScreen)
     */
    TEXT,

    /**
     * PDF document reading mode (used by PDFViewerScreen)
     */
    PDF,

    /**
     * Image reading mode (future support for OCR from images)
     */
    IMAGE
}
