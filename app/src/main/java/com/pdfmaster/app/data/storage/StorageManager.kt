package com.pdfmaster.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Керує збереженням та експортом PDF у Scoped Storage (Android 10-15+)
 * Використовує MediaStore.Downloads для публічного доступу без застарілих дозволів
 */
class StorageManager(private val context: Context) {

    /**
     * Створює тимчасовий файл у внутрішньому кеші
     */
    fun createTempPdfFile(prefix: String = "pdf_master_"): File {
        val storageDir = File(context.cacheDir, "generated_pdfs").apply { mkdirs() }
        return File.createTempFile(prefix, ".pdf", storageDir)
    }

    /**
     * Зберігає згенерований або відредагований PDF у публічну папку "Завантаження" (Downloads)
     * через сучасний MediaStore API (Scoped Storage, не вимагає прав WRITE_EXTERNAL_STORAGE)
     */
    suspend fun savePdfToPublicStorage(tempFile: File, displayName: String): Uri = withContext(Dispatchers.IO) {
        val sanitizedName = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDFMaster")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = context.contentResolver.insert(collection, values)
            ?: throw IllegalStateException("Не вдалося створити запис у MediaStore")

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            FileInputStream(tempFile).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalStateException("Не вдалося відкрити OutputStream для: $uri")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        uri
    }
}
