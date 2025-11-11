package com.example.voicereaderapp.utils

import android.content.Context
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Voice feedback system for accessibility.
 * Provides audio and haptic feedback for user interactions.
 */
object VoiceFeedback {
    /**
     * Feedback types for different interactions.
     */
    enum class FeedbackType {
        SUCCESS,        // Successful action
        ERROR,          // Error occurred
        WARNING,        // Warning message
        INFO,           // Informational
        NAVIGATION,     // Navigation action
        SELECTION,      // Item selected
        START_READING,  // Started reading
        STOP_READING    // Stopped reading
    }

    /**
     * Provides haptic feedback based on feedback type.
     *
     * @param context Application context
     * @param type Type of feedback
     */
    fun provideHapticFeedback(context: Context, type: FeedbackType) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService<VibratorManager>()
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }

        val duration = when (type) {
            FeedbackType.SUCCESS -> 50L
            FeedbackType.ERROR -> 100L
            FeedbackType.WARNING -> 75L
            FeedbackType.NAVIGATION -> 30L
            FeedbackType.SELECTION -> 40L
            FeedbackType.START_READING -> 60L
            FeedbackType.STOP_READING -> 60L
            FeedbackType.INFO -> 40L
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    /**
     * Provides audio feedback based on feedback type.
     *
     * @param context Application context
     * @param type Type of feedback
     */
    fun provideAudioFeedback(context: Context, type: FeedbackType) {
        val audioManager = context.getSystemService<AudioManager>()
        
        val effectType = when (type) {
            FeedbackType.SUCCESS -> AudioManager.FX_KEY_CLICK
            FeedbackType.ERROR -> AudioManager.FX_KEYPRESS_INVALID
            FeedbackType.SELECTION -> AudioManager.FX_KEY_CLICK
            else -> AudioManager.FX_KEY_CLICK
        }

        audioManager?.playSoundEffect(effectType)
    }

    /**
     * Gets voice message for feedback type.
     *
     * @param type Type of feedback
     * @return Voice message in Vietnamese
     */
    fun getVoiceMessage(type: FeedbackType): String {
        return when (type) {
            FeedbackType.SUCCESS -> "Thành công"
            FeedbackType.ERROR -> "Lỗi xảy ra"
            FeedbackType.WARNING -> "Cảnh báo"
            FeedbackType.INFO -> "Thông tin"
            FeedbackType.NAVIGATION -> "Điều hướng"
            FeedbackType.SELECTION -> "Đã chọn"
            FeedbackType.START_READING -> "Bắt đầu đọc"
            FeedbackType.STOP_READING -> "Dừng đọc"
        }
    }

    /**
     * Provides complete feedback (haptic, audio, and voice).
     *
     * @param context Application context
     * @param type Type of feedback
     * @param additionalMessage Optional additional message
     */
    fun provideFeedback(
        context: Context, 
        type: FeedbackType,
        additionalMessage: String? = null
    ) {
        provideHapticFeedback(context, type)
        
        if (AccessibilityHelper.isAccessibilityEnabled(context)) {
            val message = if (additionalMessage != null) {
                "${getVoiceMessage(type)}. $additionalMessage"
            } else {
                getVoiceMessage(type)
            }
            context.announceForAccessibility(message)
        }
    }
}

/**
 * Extension function for easy feedback provision.
 *
 * @param type Type of feedback
 * @param message Optional message
 */
fun Context.provideFeedback(
    type: VoiceFeedback.FeedbackType,
    message: String? = null
) {
    VoiceFeedback.provideFeedback(this, type, message)
}
