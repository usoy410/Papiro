package com.usoy.papiro.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.usoy.papiro.util.OcrHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentExtractionRepository(private val settingsStore: SettingsStore) {
    companion object {
        private const val TAG = "DocumentExtractionRepo"
    }

    suspend fun extractTextFromDocument(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

            if (mimeType.contains("pdf")) {
                val rawTextBuilder = java.lang.StringBuilder()
                com.usoy.papiro.util.PdfHelper.processPdfPages(context, uri) { bitmap ->
                    val rawText = OcrHelper.extractTextFromBitmap(bitmap)
                    if (rawText.isNotEmpty() && !rawText.startsWith("Error")) {
                        rawTextBuilder.append(rawText).append("\n\n")
                    }
                }
                val localText = rawTextBuilder.toString()
                if (localText.trim().isEmpty()) {
                    return@withContext "No text found in PDF."
                }

                val refined = GeminiService.refineOcrText(context, localText, settingsStore)
                return@withContext if (refined.isEmpty()) localText else refined
            } else {
                return@withContext extractTextFromImageUri(context, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Document extraction failed", e)
            return@withContext "Failed to extract text from document: ${e.localizedMessage}"
        }
    }
    
    private suspend fun extractTextFromImageUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        var scale = 1
        while (options.outWidth / scale > 2048 || options.outHeight / scale > 2048) { scale *= 2 }

        val inputStream2 = context.contentResolver.openInputStream(uri)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
        inputStream2?.close()

        if (bitmap != null) {
            val rawText = OcrHelper.extractTextFromBitmap(bitmap)
            bitmap.recycle()
            if (rawText.isNotEmpty() && !rawText.startsWith("Error")) {
                val refined = GeminiService.refineOcrText(context, rawText, settingsStore)
                return if (refined.isEmpty()) rawText else refined
            }
        }
        return "No text found in image document."
    }

    suspend fun extractTextFromImage(context: Context, imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        try {
            val ocrStrategy = settingsStore.ocrStrategy
            if (ocrStrategy == "GEMINI_VISION" && settingsStore.provider == SettingsStore.PROVIDER_GEMINI) {
                val base64Str = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                return@withContext GeminiService.extractTextFromBase64ImagesDirectly(context, listOf(base64Str), settingsStore)
            } else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                var scale = 1
                while (options.outWidth / scale > 2048 || options.outHeight / scale > 2048) { scale *= 2 }
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
                
                if (bitmap == null) {
                    return@withContext "Failed to decode image."
                }
                
                val rawText = OcrHelper.extractTextFromBitmap(bitmap)
                bitmap.recycle()
                if (rawText.isEmpty() || rawText.startsWith("Error")) {
                    return@withContext if (rawText.isEmpty()) "No text found in image." else rawText
                }
                
                val refined = GeminiService.refineOcrText(context, rawText, settingsStore)
                return@withContext if (refined.isEmpty()) rawText else refined
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed", e)
            return@withContext "Failed to extract text from image: ${e.localizedMessage}"
        }
    }
}
