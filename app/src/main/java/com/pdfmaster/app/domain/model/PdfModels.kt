package com.pdfmaster.app.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Елемент зображення, обраного користувачем для створення PDF
 */
@Parcelize
data class ImageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val rotation: Float = 0f, // 0f, 90f, 180f, 270f
    val croppedUri: Uri? = null,
    val sizeBytes: Long = 0L
) : Parcelable {
    val displayUri: Uri
        get() = croppedUri ?: uri
}

/**
 * Сторінка існуючого або створюваного PDF для редактора
 */
data class PdfPageItem(
    val pageIndex: Int,
    val originalPageIndex: Int,
    val rotationAngle: Int = 0, // 0, 90, 180, 270
    val thumbnailUri: Uri? = null,
    val sourceDocumentUri: Uri? = null,
    val isSelected: Boolean = false
)

enum class PageOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO
}

enum class PageMargin(val points: Float) {
    NONE(0f),
    SMALL(18f), // ~0.25 inch
    LARGE(36f)  // ~0.5 inch
}

enum class ImageCompression(val quality: Int, val maxDimension: Int) {
    HIGH(95, 2400),
    MEDIUM(80, 1600),
    LOW(60, 1000)
}

enum class PageSize(val displayName: String, val widthPt: Float, val heightPt: Float) {
    A4("A4", 595f, 842f),
    A3("A3", 842f, 1191f),
    A5("A5", 420f, 595f),
    FIT_IMAGE("Самі фото", 0f, 0f)
}

data class RecentPdfEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val pageSize: PageSize = PageSize.A4
)

data class PdfSettings(
    val fileName: String = "Document_${System.currentTimeMillis()}",
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val margin: PageMargin = PageMargin.SMALL,
    val compression: ImageCompression = ImageCompression.MEDIUM,
    val pageSize: PageSize = PageSize.A4,
    val includePageNumbers: Boolean = true
)

data class PdfExportResult(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val pageCount: Int
)
