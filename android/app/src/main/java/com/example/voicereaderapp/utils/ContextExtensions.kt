package com.example.voicereaderapp.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Utility functions for common Android operations.
 */

/**
 * Shows a short toast message.
 *
 * @param message Message to display
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Shows a short toast message from string resource.
 *
 * @param messageRes String resource ID
 */
fun Context.showToast(@StringRes messageRes: Int) {
    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
}

/**
 * Shows a long toast message.
 *
 * @param message Message to display
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
