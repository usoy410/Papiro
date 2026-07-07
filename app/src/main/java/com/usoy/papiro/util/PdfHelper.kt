package com.usoy.papiro.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfHelper {
    suspend fun processPdfPages(
        context: Context,
        uri: Uri,
        onPageBitmap: suspend (Bitmap) -> Unit
    ) = withContext(Dispatchers.IO) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor)
                val pageCount = pdfRenderer.pageCount
                // Since we process pages sequentially and recycle the bitmap, we don't need a strict page limit.
                for (i in 0 until pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val densityMultiplier = 1.5f // increased density for better OCR, but not too high
                    val width = (page.width * densityMultiplier).toInt()
                    val height = (page.height * densityMultiplier).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // white background
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    onPageBitmap(bitmap)
                    
                    bitmap.recycle()
                    page.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }
}
