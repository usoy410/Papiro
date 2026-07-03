import sys

with open("app/src/main/java/com/usoy/papiro/data/GeminiService.kt", "r") as f:
    content = f.read()

old_ocr = """    suspend fun performOcr(imageBytes: ByteArray, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.customApiKey.trim()"""

new_ocr = """    suspend fun performOcr(imageBytes: ByteArray, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        if (settings.geminiApiKey.isNotEmpty() || settings.provider == SettingsStore.PROVIDER_GEMINI) {
            val apiKeyToUse = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
            val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(
                    Part(text = "Perform OCR and extract all text from this whiteboard or paper. Return ONLY the extracted text with proper layout/formatting."),
                    Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                )))
            )
            
            try {
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = apiKeyToUse,
                    request = request
                )
                return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "OCR Failed."
            } catch (e: Exception) {
                return@withContext "OCR Failed: ${e.message}"
            }
        }

        val apiKey = settings.customApiKey.trim()"""

content = content.replace(old_ocr, new_ocr)

with open("app/src/main/java/com/usoy/papiro/data/GeminiService.kt", "w") as f:
    f.write(content)
