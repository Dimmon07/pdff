package com.pdfmaster.app.ui.screens.pdfeditor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pdfmaster.app.domain.model.PdfPageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    viewModel: PdfEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Лаунчер вибору PDF файлу
    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadPdf(it) }
    }

    // Лаунчер для додавання другого PDF (Merge)
    val mergePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.mergeAdditionalPdf(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор PDF") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (uiState is PdfEditorUiState.Ready) {
                        IconButton(onClick = { mergePdfLauncher.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.Add, contentDescription = "Додати PDF для об'єднання")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState is PdfEditorUiState.Ready) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = { viewModel.saveModifiedPdf() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Зберегти відредагований PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is PdfEditorUiState.NoFileLoaded -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Оберіть PDF для редагування",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ви зможете змінювати порядок сторінок, повертати їх, видаляти непотрібні або об'єднувати документи.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Відкрити PDF файл")
                        }
                    }
                }
                is PdfEditorUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PdfEditorUiState.Ready -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(state.pages, key = { _, item -> "${item.originalPageIndex}_${item.pageIndex}" }) { index, item ->
                            PdfPageGridItem(
                                item = item,
                                index = index,
                                onRotate = { viewModel.rotatePage(index) },
                                onDelete = { viewModel.deletePage(index) },
                                onMoveLeft = { if (index > 0) viewModel.reorderPages(index, index - 1) },
                                onMoveRight = { if (index < state.pages.size - 1) viewModel.reorderPages(index, index + 1) }
                            )
                        }
                    }
                }
                is PdfEditorUiState.Saving -> {
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        title = { Text("Збереження...") },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message)
                            }
                        }
                    )
                }
                is PdfEditorUiState.Success -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.loadPdf(state.result.uri) },
                        title = { Text("Готово!") },
                        text = { Text("Файл успішно збережено: ${state.result.fileName}") },
                        confirmButton = {
                            Button(onClick = { viewModel.loadPdf(state.result.uri) }) {
                                Text("Продовжити")
                            }
                        }
                    )
                }
                is PdfEditorUiState.Error -> {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Помилка") },
                        text = { Text(state.message) },
                        confirmButton = {
                            Button(onClick = { /* dismiss */ }) {
                                Text("ОК")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PdfPageGridItem(
    item: PdfPageItem,
    index: Int,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White)
            ) {
                AsyncImage(
                    model = item.thumbnailUri,
                    contentDescription = "Прев'ю сторінки",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(item.rotationAngle.toFloat()),
                    contentScale = ContentScale.Fit
                )

                // Номер сторінки
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Стор. ${index + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Видалити
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Дії над сторінкою
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRotate, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Повернути на 90°", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onMoveLeft, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Перемістити назад", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveRight, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Перемістити вперед", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
