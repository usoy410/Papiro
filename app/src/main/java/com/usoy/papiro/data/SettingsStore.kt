package com.usoy.papiro.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("papiro_settings", Context.MODE_PRIVATE)

    var provider: String
        get() {
            val p = prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
            return if (p == "custom") PROVIDER_GEMINI else p
        }
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    var localModelPath: String
        get() = prefs.getString(KEY_LOCAL_MODEL_PATH, "/sdcard/Download/llama-3.2-1b-instruct.task") ?: "/sdcard/Download/llama-3.2-1b-instruct.task"
        set(value) = prefs.edit().putString(KEY_LOCAL_MODEL_PATH, value).apply()

    var localMaxTokens: Int
        get() = prefs.getInt(KEY_LOCAL_MAX_TOKENS, 512)
        set(value) = prefs.edit().putInt(KEY_LOCAL_MAX_TOKENS, value).apply()

    var localTemperature: Float
        get() = prefs.getFloat(KEY_LOCAL_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_LOCAL_TEMPERATURE, value).apply()

    var ollamaBaseUrl: String
        get() = prefs.getString(KEY_OLLAMA_BASE_URL, "http://localhost:11434") ?: "http://localhost:11434"
        set(value) = prefs.edit().putString(KEY_OLLAMA_BASE_URL, value).apply()

    var ollamaModel: String
        get() = prefs.getString(KEY_OLLAMA_MODEL, "llama3") ?: "llama3"
        set(value) = prefs.edit().putString(KEY_OLLAMA_MODEL, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var selectedCloudModel: String
        get() {
            val saved = prefs.getString(KEY_SELECTED_CLOUD_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
            return if (saved.startsWith("gpt") || saved.startsWith("claude") || saved.contains("llama") || saved.contains("deepseek")) {
                "gemini-3.5-flash"
            } else {
                saved
            }
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_CLOUD_MODEL, value).apply()

    var selectedLocalModel: String
        get() = prefs.getString(KEY_SELECTED_LOCAL_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_LOCAL_MODEL, value).apply()

    var cloudModels: List<String>
        get() {
            val saved = prefs.getString(KEY_CLOUD_MODELS, null)
            val defaultList = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")
            if (saved == null) return defaultList
            val list = saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val filtered = list.filter { !it.startsWith("gpt") && !it.startsWith("claude") && !it.contains("llama") && !it.contains("deepseek") }
            return filtered.ifEmpty { defaultList }
        }
        set(value) {
            prefs.edit().putString(KEY_CLOUD_MODELS, value.joinToString(",")).apply()
        }

    var localModels: List<String>
        get() {
            val saved = prefs.getString(KEY_LOCAL_MODELS, null)
            return if (saved == null) {
                emptyList()
            } else {
                saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        set(value) {
            prefs.edit().putString(KEY_LOCAL_MODELS, value.joinToString(",")).apply()
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

    companion object {
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OLLAMA = "ollama"
        const val PROVIDER_LOCAL_ON_DEVICE = "local_on_device"

        private const val KEY_PROVIDER = "provider"
        private const val KEY_LOCAL_MODEL_PATH = "local_model_path"
        private const val KEY_LOCAL_MAX_TOKENS = "local_max_tokens"
        private const val KEY_LOCAL_TEMPERATURE = "local_temperature"
        private const val KEY_OLLAMA_BASE_URL = "ollama_base_url"
        private const val KEY_OLLAMA_MODEL = "ollama_model"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_SELECTED_CLOUD_MODEL = "selected_cloud_model"
        private const val KEY_SELECTED_LOCAL_MODEL = "selected_local_model"
        private const val KEY_CLOUD_MODELS = "cloud_models_list"
        private const val KEY_LOCAL_MODELS = "local_models_list"
        private const val KEY_PAPER_DESIGN = "paper_design"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
    }
}
