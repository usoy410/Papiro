package com.usoy.papiro.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TitleGenerator {
    private const val TAG = "TitleGenerator"
        
    suspend fun generateTitle(context: Context, content: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        return@withContext GeminiService.generateTitle(context, content, settings)
    }
}
