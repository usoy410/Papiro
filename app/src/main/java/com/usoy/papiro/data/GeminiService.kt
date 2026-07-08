package com.usoy.papiro.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.usoy.papiro.data.SettingsStore
import com.usoy.papiro.BuildConfig
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.1-flash-lite"
    
    // Highly resilient timeouts for external APIs
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)  // 5 minutes
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
        
    private const val SYSTEM_INSTRUCTION = """
        You are Papiro, an expert multi-disciplinary academic companion and smart notebook guide for students in computer science, medicine/biology, mathematics, physical sciences, teaching/pedagogical studies, and humanities.
        Generate a highly structured Markdown note for the topic specified.
        Include:
        1. Clear title and conceptual overview.
        2. A structured Table of Contents (TOC) at the very beginning of the note to easily navigate to sections, utilizing standard markdown bullet links, e.g., `- [Creational Patterns](#creational-patterns)`. Ensure the link anchors are strictly lowercase, hyphenated, and alphanumeric (matching the section headers).
        3. Fully structured sections with detailed bullet points corresponding to the Table of Contents.
        4. Detailed code blocks with syntax for technical topics, or mathematical formulas (using standard markdown LaTeX blocks if helpful).
        5. Visualizations & Diagrams (OPTIONAL & ONLY WHEN TRULY BENEFICIAL):
           - ONLY if the topic is complex and a diagram/visual flowchart, sequence diagram, process flow, timeline, hierarchical tree, or structured chart would significantly aid comprehension, you may include a Mermaid diagram inside a ```mermaid code block.
           - IMPORTANT: ALWAYS enclose node text with parenthesis or special characters inside double quotes (e.g., `A["Text (with parens)"]`) to prevent Mermaid parse errors.
           - If a diagram is not highly relevant or feels forced, do NOT generate one. Keep simple topics simple.
        6. Web Images & Real-world Illustrations (OPTIONAL & ONLY WHEN TRULY BENEFICIAL):
           - ONLY if the topic is highly visual (e.g., human anatomy, biology/cells, famous historical landmarks, planetary/astronomical systems, physical machinery, or geographical maps) and would benefit immensely from a real-world picture or scientific illustration, you may include standard markdown image links.
           - CRITICAL RULE TO PREVENT 404 ERRORS: Do NOT try to guess or hallucinate specific Unsplash photo IDs (e.g., `images.unsplash.com/photo-1234...`) or Wikimedia Commons paths, as they are guaranteed to result in 404 Not Found errors.
           - Instead, ALWAYS use LoremFlickr, which is a stable, public, keyword-based image routing service that returns real, high-quality, live images matching your specified tag.
           - Format: `![Descriptive Image Title](https://loremflickr.com/800/600/TAG)` where `TAG` is a simple, accurate, lowercase keyword representing the subject (e.g., `skeleton`, `brain`, `cell`, `heart`, `castle`, `planet`, `map`, etc.).
           - Example: `![Human Skeleton](https://loremflickr.com/800/600/skeleton)`
           - If the topic is abstract, simple, or does not require a visual reference, do NOT include any image.
        7. Keep it educational, engaging, and rich in depth. CRITICAL: Output ONLY the Markdown note itself. Do NOT include ANY conversational filler, introductory text, or ending remarks (like 'Here is the note', 'Pro tip:', 'Let me know', etc.).
        8. CRITICAL MARKDOWN LIST FORMATTING: Never use asterisks (`*`) or plus signs (`+`) for markdown bullet lists/points. Always use a single hyphen followed by exactly one space.
        9. CUSTOM MARKDOWN TABLE STYLING AND COLORING: Whenever generating tables, you MUST utilize the app's advanced metadata comment tag in the VERY FIRST cell.
    """

    private fun getReadableErrorMessage(e: Throwable): String {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            return when (code) {
                429 -> "Gemini API rate limit exceeded (HTTP 429). Too many requests. Please wait a minute before trying again."
                400 -> "Invalid request (HTTP 400). Please check your prompt or model configuration."
                401, 403 -> "Invalid Gemini API Key (HTTP $code). Please verify your API key in Settings."
                404 -> "Model not found (HTTP 404). Please ensure you have selected a supported model in Settings."
                503 -> "Gemini API is temporarily overloaded or unavailable (HTTP 503). This is a temporary Google server-side issue. Please wait a moment and try again."
                500 -> "Gemini Internal Server Error (HTTP 500). Please try again later."
                else -> "Gemini API Error (HTTP $code): ${e.message()}"
            }
        }
        val msg = e.localizedMessage ?: e.message ?: "An unknown network error occurred."
        if (msg.contains("Unable to resolve host")) {
            return "Unable to connect to Gemini servers. Please check your internet connection."
        }
        return msg
    }

    suspend fun generateStructuredNote(context: Context, topic: String, existingContent: String = "", settings: SettingsStore): String = withContext(Dispatchers.IO) {
        return@withContext generateUsingGemini(topic, existingContent, settings)
    }

    private suspend fun generateUsingGemini(topic: String, existingContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        
        if (apiKey.isEmpty()) {
            return@withContext "Error: Gemini API Key is missing.\n\nPlease go to **Settings > Cloud Configuration** and enter your Google AI Studio Gemini API Key to use this feature."
        }
        
        val model = "gemini-3.1-flash-lite"

        val promptContent = if (existingContent.trim().isEmpty()) {
            "Generate a note about: $topic"
        } else {
            """
            You are editing/expanding an existing note.
            Existing Content of the note:
            ---
            $existingContent
            ---
            The user wants to expand on this note or add content regarding the topic/prompt: "$topic"
            Instructions:
            1. Review the existing content above. Do NOT duplicate or repeat any concepts, sections, explanations, code blocks, or diagrams that already exist.
            2. Only generate NEW, supplementary, or continuing sections that directly address "$topic" and add value.
            3. Do NOT output any conversational introductions, greetings, explanations of changes, or outros. Output ONLY the new/additional Markdown content ready to be appended below the existing note.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptContent)))),
            systemInstruction = Content(parts = listOf(Part(text = SYSTEM_INSTRUCTION.trimIndent())))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Error: Empty response from Gemini."
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error with model $model", e)
            val friendlyError = getReadableErrorMessage(e)
            return@withContext "Error: Gemini API connection failed. $friendlyError"
        }
    }

    private fun extractHeadersToToc(markdown: String): String {
        val lines = markdown.split("\n")
        val sb = StringBuilder()
        sb.append("# TABLE OF CONTENTS\n\n")
        var hasHeaders = false
        for (line in lines) {
            if (line.startsWith("#")) {
                hasHeaders = true
                val level = line.takeWhile { it == '#' }.length
                val name = line.substring(level).trim()
                if (name.equals("TABLE OF CONTENTS", ignoreCase = true)) continue
                val link = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                val indent = "  ".repeat(maxOf(0, level - 2))
                sb.append("$indent- [$name](#$link)\n")
            }
        }
        if (!hasHeaders) {
            return "# TABLE OF CONTENTS\n\n- (No headings found in the note to generate a table of contents.)"
        }
        return sb.toString()
    }

    suspend fun generateTableOfContents(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
           
        if (apiKey.isEmpty()) {
            return@withContext "# TABLE OF CONTENTS\n\n- Error: Gemini API Key is missing. Please configure it in Settings."
        }
           
        val model = "gemini-3.1-flash-lite"
        val systemInstruction = """
            You are a table of contents generator. 
            Analyze the provided markdown text and generate a structured table of contents.
            Use standard markdown bullet links, e.g. - [Section Name](#section-name).
            Keep the output concise, containing only the table of contents. No conversational intro or outro.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Generate a table of contents for this note:\n\n$noteContent")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: extractHeadersToToc(noteContent)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error TOC generation", e)
            val friendlyError = getReadableErrorMessage(e)
            return@withContext "# TABLE OF CONTENTS\n\n- Error: Table of Contents generation failed. $friendlyError"
        }
    }

    suspend fun generateCustomContent(context: Context, prompt: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
           
        if (apiKey.isEmpty()) {
            return@withContext "Error: Gemini API Key is missing. Please configure it in Settings."
        }
           
        val model = "gemini-3.1-flash-lite"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Error: No content generated."
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error for custom prompt with model $model", e)
            val friendlyError = getReadableErrorMessage(e)
            return@withContext "Error: Custom content generation failed. $friendlyError"
        }
    }

    suspend fun enhanceNoteContent(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        
        if (apiKey.isEmpty()) {
            return@withContext "Error: Gemini API Key is missing. Please configure it in Settings to enhance notes."
        }
        
        val model = "gemini-3.1-flash-lite"

         val systemInstruction = """
            You are an expert AI note assistant and enhancer.
            Your task is to analyze and enhance the note content.
            IMPORTANT: Output ONLY the enhanced content. Do NOT include ANY conversational filler, introductory text, or ending remarks (like 'Here is the enhanced note', 'Pro tip:', 'Let me know', etc.).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Please enhance this note content:\n\n$noteContent")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: enhanceLocalOffline(noteContent)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error enhancing note with model $model", e)
            val friendlyError = getReadableErrorMessage(e)
            return@withContext "Error: Enhancing note failed. $friendlyError"
        }
    }

    private fun enhanceLocalOffline(noteContent: String): String {
        val lines = noteContent.split("\n")
        val sb = StringBuilder()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                sb.append("\n")
            } else if (trimmed.startsWith("#") || trimmed.startsWith("-") || trimmed.startsWith("`")) {
                sb.append(line).append("\n")
            } else {
                var enhanced = trimmed
                if (enhanced.firstOrNull()?.isLowerCase() == true) {
                    enhanced = enhanced.replaceFirstChar { it.uppercase() }
                }
                if (!enhanced.endsWith(".") && !enhanced.endsWith("!") && !enhanced.endsWith("?")) {
                    enhanced += "."
                }
                sb.append(enhanced).append("\n")
            }
        }
        return sb.toString()
    }

    suspend fun extractTextFromBase64ImagesDirectly(context: Context, base64Images: List<String>, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKeyToUse = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        if (apiKeyToUse.isEmpty()) {
            return@withContext "Error: Gemini API Key is missing for direct image extraction."
        }
        val modelToUse = "gemini-3.1-flash-lite"
        
        val parts = mutableListOf<Part>()
        parts.add(Part(text = "Extract all text and structural content (tables, headings, lists) from these images and format it as structured Markdown. IMPORTANT: Output ONLY the extracted text. Do NOT include ANY conversational filler, introductory text, or ending remarks."))
        
        for (base64Str in base64Images) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Str)))
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts))
        )
        try {
            val response = RetrofitClient.service.generateContent(
                model = modelToUse,
                apiKey = apiKeyToUse,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No text found in images."
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Direct Extract error", e)
            val friendlyError = getReadableErrorMessage(e)
            return@withContext "Error: Direct extraction failed. $friendlyError"
        }
    }

    suspend fun refineOcrText(context: Context, rawOcrText: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKeyToUse = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        if (apiKeyToUse.isEmpty()) {
            return@withContext rawOcrText
        }
        
        val prompt = "Format and clean up the following extracted text from an image. Make it readable, fix obvious OCR typos, and format it nicely using Markdown. IMPORTANT: Output ONLY the formatted text. Do NOT include ANY conversational filler, introductory text (like 'Here is the cleaned-up...'), ending remarks, or explanations of the changes made:\n\n$rawOcrText"
        val modelToUse = "gemini-3.1-flash-lite"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )
        
        try {
            val response = RetrofitClient.service.generateContent(
                model = modelToUse,
                apiKey = apiKeyToUse,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: rawOcrText
        } catch (e: Exception) {
            Log.e(TAG, "Gemini OCR refine error", e)
            return@withContext rawOcrText
        }
    }

    suspend fun generateTitle(context: Context, content: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKeyToUse = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        if (apiKeyToUse.isEmpty()) {
            return@withContext "Untitled Note"
        }
        
        val prompt = "Generate a short, concise, and accurate title (maximum 6 words) for the following text. Respond ONLY with the title itself, no quotes, no formatting, and no conversational filler, introductory text, or ending remarks:\n\n${content.take(1500)}"
        val modelToUse = "gemini-3.1-flash-lite"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )
        
        try {
            val response = RetrofitClient.service.generateContent(
                model = modelToUse,
                apiKey = apiKeyToUse,
                request = request
            )
            val title = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "Untitled Note"
            return@withContext title.removeSurrounding("\"").take(50)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Title Gen error", e)
            return@withContext "Untitled Note"
        }
    }

    suspend fun testApiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Hello"))))
            )
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.1-flash-lite",
                apiKey = apiKey,
                request = request
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            if (text.isNotEmpty()) {
                Pair(true, "Verified successfully!")
            } else {
                Pair(false, "Failed to verify. Empty response received.")
            }
        } catch (e: Exception) {
            val friendlyError = getReadableErrorMessage(e)
            Pair(false, friendlyError)
        }
    }
}
