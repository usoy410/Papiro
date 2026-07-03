package com.usoy.papiro.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {
    fun getPath(context: Context, uri: Uri): String? {
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var name = "model.bin"
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        name = it.getString(displayNameIndex)
                    }
                }
            }
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val file = java.io.File(context.cacheDir, name)
                val outputStream = java.io.FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                return file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return uri.toString()
    }
}
