package com.example.voicereaderapp.utils

/**
 * Constants used throughout the application.
 * Contains configuration values, API keys, and shared constants.
 */
object Constants {
    // Database
    const val DATABASE_VERSION = 1
    const val DATABASE_NAME = "voice_reader_db"

    // DataStore
    const val PREFERENCES_NAME = "voice_settings"

    // Voice Settings
    const val DEFAULT_SPEED = 1.0f
    const val DEFAULT_PITCH = 1.0f
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 2.0f
    const val MIN_PITCH = 0.5f
    const val MAX_PITCH = 2.0f
    const val DEFAULT_LANGUAGE = "vi-VN"

    // Document Types
    const val TYPE_PDF = "PDF"
    const val TYPE_IMAGE = "IMAGE"
    const val TYPE_LIVE_SCREEN = "LIVE_SCREEN"

    // Permissions
    const val PERMISSION_CAMERA = android.Manifest.permission.CAMERA
    const val PERMISSION_READ_EXTERNAL = android.Manifest.permission.READ_EXTERNAL_STORAGE
    
    // Request Codes
    const val REQUEST_CODE_CAMERA = 1001
    const val REQUEST_CODE_GALLERY = 1002
    const val REQUEST_CODE_SCREEN_CAPTURE = 1003
}
