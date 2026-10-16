package com.pdfmaster.app.ui.screens.pdfeditor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaster.app.data.pdf.AndroidPdfRendererManager
import com.pdfmaster.app.data.pdf.PdfBoxEditor
import com.pdfmaster.app.data.storage.StorageManager
import com.pdfmaster.app.domain.model.PdfExportResult
import com.pdfmaster.app.domain.model.PdfPageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PdfEditorUiState {
    data object NoFileLoaded : PdfEditorUiState
    data object Loading : PdfEditorUiState
    data class Ready(val pages: List<PdfPageItem>) : PdfEditorUiState
    data class Saving(val progress: Int, val message: String) : PdfEditorUiState
    data class Success(val result: PdfExportResult) : PdfEditorUiState
    data class Error(val message: String) : PdfEditorUiState
}

class PdfEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val rendererManager = AndroidPdfRendererManager(application)
    private val pdfEditor = PdfBoxEditor(application)
    private val storageManager = StorageManager(application)

    private val _uiState = MutableStateFlow<PdfEditorUiState>(PdfEditorUiState.NoFileLoaded)
    val uiState: StateFlow<PdfEditorUiState> = _uiState.asStateFlow()

    private var currentPdfUri: Uri? = null
    private val additionalMergeUris = mutableListOf<Uri>()

    fun loadPdf(uri: Uri) {
        currentPdfUri = uri
        viewModelScope.launch {
            _uiState.value = PdfEditorUiState.Loading
            try {
                val pages = rendererManager.extractPagesInfo(uri)
                _uiState.value = PdfEditorUiState.Ready(pages)
            } catch (e: Exception) {
                _uiState.value = PdfEditorUiState.Error("Не вдалося відкрити PDF: ${e.localizedMessage}")
            }
        }
    }

    fun rotatePage(pageIndex: Int) {
        val currentState = _uiState.value as? PdfEditorUiState.Ready ?: return
        val updated = currentState.pages.mapIndexed { index, page ->
            if (index == pageIndex) {
                page.copy(rotationAngle = (page.rotationAngle + 90) % 360)
            } else page
        }
        _uiState.value = PdfEditorUiState.Ready(updated)
    }

    fun deletePage(pageIndex: Int) {
        val currentState = _uiState.value as? PdfEditorUiState.Ready ?: return
        if (currentState.pages.size <= 1) {
            _uiState.value = PdfEditorUiState.Error("Документ повинен містити хоча б 1 сторінку")
            return
        }
        val mutable = currentState.pages.toMutableList()
        mutable.removeAt(pageIndex)
        _uiState.value = PdfEditorUiState.Ready(mutable)
    }

    fun reorderPages(fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value as? PdfEditorUiState.Ready ?: return
        val mutable = currentState.pages.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        _uiState.value = PdfEditorUiState.Ready(mutable)
    }

    fun mergeAdditionalPdf(extraUri: Uri) {
        additionalMergeUris.add(extraUri)
        // Також оновлюємо прев'ю з новими сторінками
        viewModelScope.launch {
            try {
                val extraPages = rendererManager.extractPagesInfo(extraUri)
                val currentState = _uiState.value as? PdfEditorUiState.Ready
                val currentPages = currentState?.pages ?: emptyList()
                _uiState.value = PdfEditorUiState.Ready(currentPages + extraPages)
            } catch (e: Exception) {
                _uiState.value = PdfEditorUiState.Error("Помилка додавання PDF: ${e.localizedMessage}")
            }
        }
    }

    fun saveModifiedPdf(fileName: String = "Edited_Document") {
        val originalUri = currentPdfUri ?: return
        val currentState = _uiState.value as? PdfEditorUiState.Ready ?: return

        viewModelScope.launch {
            try {
                _uiState.value = PdfEditorUiState.Saving(10, "Початок збереження...")
                val tempFile = storageManager.createTempPdfFile("edited_")

                pdfEditor.saveEditedPdf(
                    originalPdfUri = originalUri,
                    modifiedPages = currentState.pages,
                    additionalPdfUris = additionalMergeUris,
                    outputFile = tempFile,
                    onProgress = { progress, msg ->
                        _uiState.value = PdfEditorUiState.Saving(progress, msg)
                    }
                )

                val publicUri = storageManager.savePdfToPublicStorage(tempFile, fileName)
                _uiState.value = PdfEditorUiState.Success(
                    PdfExportResult(
                        uri = publicUri,
                        fileName = "$fileName.pdf",
                        fileSizeBytes = tempFile.length(),
                        pageCount = currentState.pages.size
                    )
                )
            } catch (e: Exception) {
                _uiState.value = PdfEditorUiState.Error("Помилка збереження: ${e.localizedMessage}")
            }
        }
    }
}
