package com.pdfmaster.app.data.pdf

import android.content.Context
import android.net.Uri
import com.pdfmaster.app.domain.model.PdfPageItem
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Використовує PdfBox-Android для складних операцій редагування:
 * - Перестановка сторінок
 * - Поворот сторінок на 90°, 180°, 270°
 * - Видалення сторінок
 * - Об'єднання (Merge) декількох PDF документів
 */
class PdfBoxEditor(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Створює новий відредагований PDF відповідно до нового порядку, кутів повороту та метаданих
     */
    suspend fun saveEditedPdf(
        originalPdfUri: Uri,
        modifiedPages: List<PdfPageItem>,
        additionalPdfUris: List<Uri>,
        outputFile: File,
        metadataTitle: String? = null,
        metadataAuthor: String? = null,
        metadataSubject: String? = null,
        metadataKeywords: String? = null,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        onProgress(15, "Завантаження вихідного документа...")
        val inputStream = context.contentResolver.openInputStream(originalPdfUri)
            ?: throw IllegalStateException("Не вдалося прочитати оригінальний PDF")

        val sourceDoc = PDDocument.load(inputStream)
        val newDoc = PDDocument()

        try {
            // Запис оновлених метаданих (Title, Author, Subject, Keywords)
            val docInfo = newDoc.documentInformation
            metadataTitle?.let { docInfo.title = it }
            metadataAuthor?.let { docInfo.author = it }
            metadataSubject?.let { docInfo.subject = it }
            metadataKeywords?.let { docInfo.keywords = it }
            docInfo.creator = "PDF Master Android (Jetpack Compose)"
            docInfo.producer = "Apache PDFBox Android"

            val total = modifiedPages.size
            modifiedPages.forEachIndexed { index, pageItem ->
                val progress = 20 + ((index + 1).toFloat() / total * 60).toInt()
                onProgress(progress, "Збирання сторінки ${index + 1} з $total...")

                if (pageItem.originalPageIndex < sourceDoc.numberOfPages) {
                    val originalPage = sourceDoc.getPage(pageItem.originalPageIndex)
                    // Додаємо сторінку до нового документа
                    val importedPage = newDoc.importPage(originalPage)

                    // Встановлюємо кут повороту (наприклад, +90, +180)
                    val currentRotation = importedPage.rotation
                    importedPage.rotation = (currentRotation + pageItem.rotationAngle) % 360
                }
            }

            // Додаємо додаткові PDF, якщо виконується Merge
            if (additionalPdfUris.isNotEmpty()) {
                onProgress(85, "Об'єднання з додатковими документами...")
                additionalPdfUris.forEach { addUri ->
                    context.contentResolver.openInputStream(addUri)?.use { addStream ->
                        val addDoc = PDDocument.load(addStream)
                        for (p in 0 until addDoc.numberOfPages) {
                            newDoc.importPage(addDoc.getPage(p))
                        }
                        addDoc.close()
                    }
                }
            }

            onProgress(95, "Запис відредагованого файлу з метаданими...")
            FileOutputStream(outputFile).use { fos ->
                newDoc.save(fos)
            }
            onProgress(100, "Збереження завершено!")
            outputFile
        } finally {
            sourceDoc.close()
            newDoc.close()
            inputStream.close()
        }
    }
}
