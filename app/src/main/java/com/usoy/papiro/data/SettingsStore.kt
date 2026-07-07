package com.usoy.papiro.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("papiro_settings", Context.MODE_PRIVATE)

    var provider: String
        get() {
            return prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
        }
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var selectedCloudModel: String
        get() {
            val saved = prefs.getString(KEY_SELECTED_CLOUD_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
            return if (saved.startsWith("gpt") || saved.startsWith("claude") || saved.contains("llama") || saved.contains("deepseek")) {
                "gemini-2.5-flash"
            } else {
                saved
            }
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_CLOUD_MODEL, value).apply()

    var cloudModels: List<String>
        get() {
            val saved = prefs.getString(KEY_CLOUD_MODELS, null)
            val defaultList = listOf("gemini-1.5-flash", "gemini-2.5-flash", "gemini-3.1-flash-lite")
            if (saved == null) return defaultList
            val list = saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val filtered = list.filter { !it.startsWith("gpt") && !it.startsWith("claude") && !it.contains("llama") && !it.contains("deepseek") }
            return filtered.ifEmpty { defaultList }
        }
        set(value) {
            prefs.edit().putString(KEY_CLOUD_MODELS, value.joinToString(",")).apply()
        }

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

    companion object {
        const val PROVIDER_GEMINI = "gemini"

        private const val KEY_PROVIDER = "provider"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_SELECTED_CLOUD_MODEL = "selected_cloud_model"
        private const val KEY_CLOUD_MODELS = "cloud_models_list"
        private const val KEY_PAPER_DESIGN = "paper_design"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_OCR_STRATEGY = "ocr_strategy"
    }
}
