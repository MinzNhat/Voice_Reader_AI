package com.example.voicereaderapp.utils

/**
 * Sealed class representing result states for async operations.
 * Provides type-safe handling of success, error, and loading states.
 *
 * @param T Type of data returned on success
 */
sealed class Result<out T> {
    /**
     * Success state with data.
     *
     * @property data The successful result data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Error state with exception.
     *
     * @property exception The error that occurred
     */
    data class Error(val exception: Throwable) : Result<Nothing>()

    /**
     * Loading state indicating ongoing operation.
     */
    object Loading : Result<Nothing>()

    /**
     * Returns data if success, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns true if result is success.
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Returns true if result is error.
     */
    fun isError(): Boolean = this is Error

    /**
     * Returns true if result is loading.
     */
    fun isLoading(): Boolean = this is Loading
}
