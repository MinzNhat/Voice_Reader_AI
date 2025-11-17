package com.example.voicereaderapp.ui.pdfreader

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicereaderapp.data.remote.NetworkModule
import com.example.voicereaderapp.data.repository.OCRRepositoryImpl
import com.example.voicereaderapp.data.repository.TTSRepositoryImpl
import java.io.File

/**
 * Navigation sealed class for PDF Reader flow
 */
sealed class PDFReaderRoute(val route: String) {
    object DocumentPicker : PDFReaderRoute("document_picker")
    object PDFViewer : PDFReaderRoute("pdf_viewer")
}

/**
 * Main navigation for PDF Reader feature
 * Handles document picking and PDF viewing
 */
//@Composable
//fun PDFReaderNavigation() {
//    val navController = rememberNavController()
//    var selectedFile by remember { mutableStateOf<File?>(null) }
//
//    // Initialize repositories
//    val ocrRepository = remember { OCRRepositoryImpl(NetworkModule.api) }
//    val ttsRepository = remember { TTSRepositoryImpl(NetworkModule.api) }
//
//    // Initialize ViewModel
//    val viewModel = remember {
//        PDFViewerViewModel(
//            ocrRepository = ocrRepository,
//            ttsRepository = ttsRepository
//        )
//    }
//
//    NavHost(
//        navController = navController,
//        startDestination = PDFReaderRoute.DocumentPicker.route
//    ) {
//        // Document picker screen
//        composable(PDFReaderRoute.DocumentPicker.route) {
//            DocumentPickerScreen(
//                onDocumentSelected = { file ->
//                    selectedFile = file
//                    navController.navigate(PDFReaderRoute.PDFViewer.route)
//                }
//            )
//        }
//
//        // PDF viewer screen (Speechify-style)
//        composable(PDFReaderRoute.PDFViewer.route) {
//            selectedFile?.let { file ->
//                SpeechifyStylePDFViewer(
//                    pdfFile = file,
//                    viewModel = viewModel,
//                    onBack = {
//                        navController.popBackStack()
//                    }
//                )
//            }
//        }
//    }
//}
