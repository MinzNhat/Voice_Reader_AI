package com.example.voicereaderapp.utils

/**
 * Extension functions for String manipulation and validation.
 */

/**
 * Checks if string is not null and not blank.
 *
 * @return true if string is valid, false otherwise
 */
fun String?.isNotNullOrBlank(): Boolean {
    return this != null && this.isNotBlank()
}

/**
 * Truncates string to specified length and adds ellipsis.
 *
 * @param maxLength Maximum length of the string
 * @return Truncated string with ellipsis if needed
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        "${this.substring(0, maxLength)}..."
    } else {
        this
    }
}

/**
 * Capitalizes first letter of the string.
 *
 * @return String with first letter capitalized
 */
fun String.capitalizeFirst(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
}

/**
 * Removes all whitespace from string.
 *
 * @return String without whitespace
 */
fun String.removeWhitespace(): String {
    return this.replace("\\s+".toRegex(), "")
}

/**
 * Counts number of words in the string.
 *
 * @return Number of words
 */
fun String.wordCount(): Int {
    return this.trim().split("\\s+".toRegex()).size
}
