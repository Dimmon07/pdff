package com.pdfmaster.app.ui.screens.imagetopdf

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pdfmaster.app.domain.model.ImageItem
import com.pdfmaster.app.domain.model.PageMargin
import com.pdfmaster.app.domain.model.PageOrientation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    viewModel: ImageToPdfViewModel,
    onNavigateBack: () -> Unit,
    onOpenCrop: (ImageItem) -> Unit
) {
    val images by viewModel.images.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }

    // Використання нативного Android Photo Picker (Android 13+ та сумісність з Android 4.4+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesPicked(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Зображення в PDF (${images.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Параметри PDF")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                text = { Text("Додати фото") }
            )
        },
        bottomBar = {
            if (images.isNotEmpty()) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = { viewModel.generatePdf() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Конвертувати в PDF", fontWeight = FontWeight.Bold)
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
            if (images.isEmpty()) {
                // Порожній стан
                EmptyImagesPlaceholder(
                    onPickClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(images, key = { _, item -> item.id }) { index, item ->
                        ImageCardItem(
                            item = item,
                            index = index,
                            onRotate = { viewModel.rotateImage(item.id) },
                            onDelete = { viewModel.removeImage(item.id) },
                            onCrop = { onOpenCrop(item) },
                            onMoveLeft = { if (index > 0) viewModel.moveImage(index, index - 1) },
                            onMoveRight = { if (index < images.size - 1) viewModel.moveImage(index, index + 1) }
                        )
                    }
                }
            }

            // Індикатор прогресу генерації
            when (val state = uiState) {
                is ImageToPdfUiState.Generating -> {
                    GeneratingProgressDialog(progress = state.progress, message = state.message)
                }
                is ImageToPdfUiState.Success -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetState() },
                        icon = { Icon(Icons.Default.CheckCircle, tint = MaterialTheme.colorScheme.primary, contentDescription = null) },
                        title = { Text("PDF успішно створено!") },
                        text = {
                            Text("Файл '${state.result.fileName}' збережено у папку Завантаження (Downloads).")
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.resetState() }) {
                                Text("Зрозуміло")
                            }
                        }
                    )
                }
                is ImageToPdfUiState.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetState() },
                        title = { Text("Помилка") },
                        text = { Text(state.message) },
                        confirmButton = {
                            Button(onClick = { viewModel.resetState() }) {
                                Text("ОК")
                            }
                        }
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun ImageCardItem(
    item: ImageItem,
    index: Int,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    onCrop: () -> Unit,
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
                    .height(170.dp)
            ) {
                AsyncImage(
                    model = item.displayUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(item.rotation),
                    contentScale = ContentScale.Crop
                )

                // Номер сторінки
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Кнопка видалення
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Видалити", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Нижня панель дій над фото
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRotate, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Повернути", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onCrop, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Crop, contentDescription = "Обрізати", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onMoveLeft, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Вліво", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveRight, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Вправо", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyImagesPlaceholder(onPickClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Зображень ще не додано",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Виберіть фотографії з галереї для створення багатосторінкового PDF",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onPickClick) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Обрати фото")
        }
    }
}

@Composable
fun GeneratingProgressDialog(progress: Int, message: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Створення PDF...") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                Text(text = "$progress%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    )
}
