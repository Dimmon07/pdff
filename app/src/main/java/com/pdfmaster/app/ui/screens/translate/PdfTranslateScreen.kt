package com.pdfmaster.app.ui.screens.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfTranslateScreen(
    onNavigateBack: () -> Unit
) {
    var sourceLang by remember { mutableStateOf("en") }
    var targetLang by remember { mutableStateOf("uk") }
    var isBilingual by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Переклад PDF (ML Kit)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Вибір мов
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("З мови", style = MaterialTheme.typography.labelSmall)
                        Text("Англійська (EN)", fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = {
                        val tmp = sourceLang
                        sourceLang = targetLang
                        targetLang = tmp
                    }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Поміняти")
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("На мову", style = MaterialTheme.typography.labelSmall)
                        Text("Українська (UK)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Інфо про On-Device ML Kit
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    text = "Google ML Kit виконує переклад локально на пристрої користувача без надсилання конфіденційних документів на сервери.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
