package com.pdfmaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pdfmaster.app.ui.screens.home.HomeScreen
import com.pdfmaster.app.ui.screens.imagetopdf.ImageToPdfScreen
import com.pdfmaster.app.ui.screens.imagetopdf.ImageToPdfViewModel
import com.pdfmaster.app.ui.screens.pdfeditor.PdfEditorScreen
import com.pdfmaster.app.ui.screens.pdfeditor.PdfEditorViewModel
import com.pdfmaster.app.ui.theme.PdfMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PdfMasterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToImageToPdf = { navController.navigate("image_to_pdf") },
                                onNavigateToPdfEditor = { navController.navigate("pdf_editor") },
                                onNavigateToPdfTranslate = { navController.navigate("pdf_translate") }
                            )
                        }

                        composable("image_to_pdf") {
                            val viewModel: ImageToPdfViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            ImageToPdfScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenCrop = { /* Запуск uCrop Activity за необхідності */ }
                            )
                        }

                        composable("pdf_editor") {
                            val viewModel: PdfEditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            PdfEditorScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("pdf_translate") {
                            PdfTranslateScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
