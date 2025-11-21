package com.example.voicereaderapp.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Helper class for managing app locale/language settings.
 * Provides utilities to change the app's language at runtime.
 */
object LocaleHelper {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "selected_language"
    /**
     * Apply locale to context and return a wrapped context.
     *
     * @param context Current context
     * @param languageCode Language code (e.g., "en-US", "ko-KR")
     * @return Context with applied locale
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = getLocaleFromCode(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return context.createConfigurationContext(configuration)
    }

    /**
     * Convert language code to Locale object.
     *
     * @param languageCode Language code (e.g., "en-US", "ko-KR")
     * @return Locale object
     */
    private fun getLocaleFromCode(languageCode: String): Locale {
        return when {
            languageCode.startsWith("ko") -> Locale.KOREAN
            languageCode.startsWith("en") -> Locale.ENGLISH
            else -> {
                val parts = languageCode.split("-")
                if (parts.size >= 2) {
                    Locale(parts[0], parts[1])
                } else {
                    Locale(parts[0])
                }
            }
        }
    }

    /**
     * Get current app locale from settings.
     *
     * @param context Current context
     * @return Current language code
     */
    fun getCurrentLocale(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return "${locale.language}-${locale.country}"
    }

    /**
     * Save language preference to SharedPreferences
     *
     * @param context Current context
     * @param languageCode Language code to save
     */
    fun saveLanguagePreference(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Get saved language preference from SharedPreferences
     *
     * @param context Current context
     * @return Saved language code or "en-US" as default
     */
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en-US") ?: "en-US"
    }

    /**
     * Apply saved locale to context
     *
     * @param context Current context
     * @return Context with applied locale
     */
    fun applyLocale(context: Context): Context {
        val savedLanguage = getSavedLanguage(context)
        return setLocale(context, savedLanguage)
    }

    /**
     * Change language and restart activity
     *
     * @param activity Current activity
     * @param languageCode New language code
     */
    fun changeLanguage(activity: Activity, languageCode: String) {
        saveLanguagePreference(activity, languageCode)
        setLocale(activity, languageCode)
        activity.recreate()
    }
}
