package com.example.voicereaderapp.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions for Date and time formatting.
 */

/**
 * Formats timestamp to readable date string.
 * Format: dd/MM/yyyy HH:mm
 *
 * @return Formatted date string
 */
fun Long.toFormattedDate(): String {
    val date = Date(this)
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Formats timestamp to short date string.
 * Format: dd/MM/yyyy
 *
 * @return Formatted date string
 */
fun Long.toShortDate(): String {
    val date = Date(this)
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Formats timestamp to time string.
 * Format: HH:mm
 *
 * @return Formatted time string
 */
fun Long.toTimeString(): String {
    val date = Date(this)
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Gets current timestamp in milliseconds.
 *
 * @return Current timestamp
 */
fun currentTimestamp(): Long = System.currentTimeMillis()
