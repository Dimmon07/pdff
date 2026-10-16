package com.pdfmaster.app.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfmaster.app.domain.model.PdfPageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Генерує прев'ю сторінок існуючого PDF за допомогою нативного PdfRenderer
 */
class AndroidPdfRendererManager(private val context: Context) {

    suspend fun extractPagesInfo(pdfUri: Uri): List<PdfPageItem> = withContext(Dispatchers.IO) {
        val tempFile = copyUriToTempFile(pdfUri)
        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val pages = mutableListOf<PdfPageItem>()

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            // Рендеримо легке прев'ю сторінки в кеш для Coil
            val thumbnailUri = renderPageToCache(page, i)
            page.close()

            pages.add(
                PdfPageItem(
                    pageIndex = i,
                    originalPageIndex = i,
                    rotationAngle = 0,
                    thumbnailUri = thumbnailUri,
                    sourceDocumentUri = pdfUri
                )
            )
        }

        renderer.close()
        pfd.close()
        pages
    }

    private fun renderPageToCache(page: PdfRenderer.Page, pageIndex: Int): Uri {
        // Оптимальна ширина прев'ю для сітки на мобільному
        val width = 450
        val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE) // Біле тло для прозорих PDF
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        val cacheFile = File(context.cacheDir, "preview_page_${pageIndex}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        return Uri.fromFile(cacheFile)
    }

    private fun copyUriToTempFile(uri: Uri): File {
        val tempFile = File.createTempFile("temp_render_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Неможливо відкрити InputStream для: $uri")
        return tempFile
    }
}
