package com.pdfmaster.app.data.scanner

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Менеджер сканування документів на базі Google ML Kit Document Scanner API.
 * Забезпечує надійне On-Device визначення контурів, виправлення спотворень перспективи (dewarping),
 * видалення тіней/пальців та застосування оптимізованих фільтрів документів (B&W, Grayscale, Color).
 */
class DocumentScannerManager(private val activity: Activity) {

    private val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(25)
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .setScannerMode(SCANNER_MODE_FULL) // Включає автоматичне захоплення, ручне редагування кутів та фільтри
        .build()

    private val scanner: GmsDocumentScanner = GmsDocumentScanning.getClient(scannerOptions)

    /**
     * Запуск нативного сканера ML Kit через ActivityResultLauncher
     */
    fun startScan(launcher: ActivityResultLauncher<IntentSenderRequest>, onFailure: (Exception) -> Unit) {
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender: IntentSender ->
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Обробка отриманого результату сканування
     */
    fun parseResult(result: GmsDocumentScanningResult?): ScannedDocumentResult? {
        if (result == null) return null

        val pagesUris = result.pages?.map { it.imageUri } ?: emptyList()
        val pdfUri = result.pdf?.uri
        val pageCount = result.pdf?.pageCount ?: pagesUris.size

        return ScannedDocumentResult(
            pagesUris = pagesUris,
            pdfUri = pdfUri,
            pageCount = pageCount
        )
    }
}

data class ScannedDocumentResult(
    val pagesUris: List<android.net.Uri>,
    val pdfUri: android.net.Uri?,
    val pageCount: Int
)
