package com.example.voicereaderapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Voice Reader AI.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * This triggers Hilt's code generation including base class for the application.
 */
@HiltAndroidApp
class VoiceReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization code here
    }
}
