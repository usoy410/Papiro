package com.usoy.papiro.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("papiro_settings", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var paperDesign: String
        get() = prefs.getString(KEY_PAPER_DESIGN, "GRID") ?: "GRID"
        set(value) = prefs.edit().putString(KEY_PAPER_DESIGN, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "INDIGO") ?: "INDIGO"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var appThemeMode: String
        get() = prefs.getString(KEY_APP_THEME_MODE, "LIGHT") ?: "LIGHT"
        set(value) = prefs.edit().putString(KEY_APP_THEME_MODE, value).apply()

    var ocrStrategy: String
        get() = prefs.getString(KEY_OCR_STRATEGY, "ML_KIT") ?: "ML_KIT"
        set(value) = prefs.edit().putString(KEY_OCR_STRATEGY, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_PAPER_DESIGN = "paper_design"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_OCR_STRATEGY = "ocr_strategy"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
