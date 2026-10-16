package com.pdfmaster.app.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.pdfmaster.app.domain.model.ImageCompression
import com.pdfmaster.app.domain.model.ImageItem
import com.pdfmaster.app.domain.model.PageOrientation
import com.pdfmaster.app.domain.model.PageSize
import com.pdfmaster.app.domain.model.PdfSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

/**
 * Відповідає за створення PDF з зображень з використанням нативного android.graphics.pdf.PdfDocument
 */
class NativePdfDocumentGenerator(
    private val context: Context
) {

    suspend fun generatePdf(
        images: List<ImageItem>,
        settings: PdfSettings,
        outputFile: File,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val totalImages = images.size

        try {
            images.forEachIndexed { index, imageItem ->
                val progress = ((index + 1).toFloat() / totalImages * 85).toInt()
                onProgress(progress, "Обробка сторінки ${index + 1} з $totalImages...")

                // 1. Декодуємо та оптимізуємо Bitmap з урахуванням ротації та компресії
                val bitmap = decodeAndProcessBitmap(imageItem, settings.compression)

                // 2. Визначаємо розміри сторінки PDF
                val (pageWidth, pageHeight) = calculatePageDimensions(bitmap, settings)

                // 3. Створюємо опис сторінки PdfDocument.PageInfo
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // 4. Малюємо зображення з урахуванням полів (Margins)
                drawImageOnCanvas(canvas, bitmap, pageWidth, pageHeight, settings.margin.points.toInt())

                // 5. Нумерація сторінок (якщо увімкнено)
                if (settings.includePageNumbers) {
                    drawPageNumber(canvas, index + 1, totalImages, pageWidth, pageHeight)
                }

                pdfDocument.finishPage(page)
                bitmap.recycle() // Звільняємо пам'ять
            }

            onProgress(90, "Збереження PDF документа...")
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
                fos.flush()
            }
            onProgress(100, "PDF успішно створено!")
            outputFile
        } finally {
            pdfDocument.close()
        }
    }

    private fun decodeAndProcessBitmap(item: ImageItem, compression: ImageCompression): Bitmap {
        val uri = item.displayUri
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Обчислення inSampleSize для оптимізації споживання RAM
            var sampleSize = 1
            while (options.outWidth / sampleSize > compression.maxDimension ||
                options.outHeight / sampleSize > compression.maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                ?: throw IllegalStateException("Не вдалося декодувати зображення: $uri")

            // Застосування повороту, якщо він встановлений
            return if (item.rotation != 0f) {
                val matrix = Matrix().apply { postRotate(item.rotation) }
                Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
            } else {
                decodedBitmap
            }
        } finally {
            inputStream?.close()
        }
    }

    private fun calculatePageDimensions(bitmap: Bitmap, settings: PdfSettings): Pair<Int, Int> {
        if (settings.pageSize == PageSize.FIT_IMAGE) {
            val margin = (settings.margin.points * 2).toInt()
            return Pair(bitmap.width + margin, bitmap.height + margin)
        }

        val baseWidth = settings.pageSize.widthPt.toInt()
        val baseHeight = settings.pageSize.heightPt.toInt()

        val isLandscape = when (settings.orientation) {
            PageOrientation.PORTRAIT -> false
            PageOrientation.LANDSCAPE -> true
            PageOrientation.AUTO -> bitmap.width > bitmap.height
        }

        return if (isLandscape) {
            Pair(maxOf(baseWidth, baseHeight), minOf(baseWidth, baseHeight))
        } else {
            Pair(minOf(baseWidth, baseHeight), maxOf(baseWidth, baseHeight))
        }
    }

    private fun drawImageOnCanvas(canvas: Canvas, bitmap: Bitmap, pageWidth: Int, pageHeight: Int, margin: Int) {
        val availWidth = (pageWidth - margin * 2).toFloat()
        val availHeight = (pageHeight - margin * 2).toFloat()

        val scale = min(availWidth / bitmap.width, availHeight / bitmap.height)
        val targetWidth = bitmap.width * scale
        val targetHeight = bitmap.height * scale

        val left = margin + (availWidth - targetWidth) / 2f
        val top = margin + (availHeight - targetHeight) / 2f

        val destRect = RectF(left, top, left + targetWidth, top + targetHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, destRect, paint)
    }

    private fun drawPageNumber(canvas: Canvas, current: Int, total: Int, width: Int, height: Int) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$current / $total", width / 2f, height - 12f, textPaint)
    }
}
