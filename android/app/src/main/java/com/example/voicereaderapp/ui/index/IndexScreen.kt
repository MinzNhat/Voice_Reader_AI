package com.example.voicereaderapp.ui.index

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicereaderapp.ui.pdfreader.PdfReaderScreen
import com.example.voicereaderapp.ui.scanner.ScannerScreen
import com.example.voicereaderapp.ui.livereader.LiveReaderScreen
import com.example.voicereaderapp.ui.settings.SettingsScreen
import com.example.voicereaderapp.utils.NavigationHelper
import com.example.voicereaderapp.utils.VoiceFeedback
import com.example.voicereaderapp.utils.provideFeedback

/**
 * Main screen of the application.
 * Contains bottom navigation with tabs for different features:
 * - PDF Reader
 * - Scanner
 * - Live Reader
 * - Settings
 *
 * Fully accessible for visually impaired users with:
 * - Screen reader support (TalkBack)
 * - Voice feedback
 * - Haptic feedback
 * - Proper content descriptions
 *
 * @param viewModel ViewModel for managing index screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexScreen(
    viewModel: IndexViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val context = LocalContext.current

    // Announce screen when first loaded
    LaunchedEffect(Unit) {
        context.provideFeedback(
            VoiceFeedback.FeedbackType.INFO,
            NavigationHelper.getNavigationAnnouncement(NavigationHelper.Screen.INDEX)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Voice Reader AI",
                        modifier = Modifier.semantics {
                            contentDescription = "Voice Reader AI, ứng dụng đọc tài liệu cho người khiếm thị"
                        }
                    ) 
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.semantics {
                    contentDescription = "Thanh điều hướng với 4 tab: PDF, Quét ảnh, Đọc trực tiếp, và Cài đặt"
                }
            ) {
                NavigationBarItem(
                    icon = { /* Icon */ },
                    label = { Text("PDF") },
                    selected = selectedTab == 0,
                    onClick = { 
                        viewModel.selectTab(0)
                        context.provideFeedback(
                            VoiceFeedback.FeedbackType.NAVIGATION,
                            NavigationHelper.getScreenTitle(NavigationHelper.Screen.PDF_READER)
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (selectedTab == 0) {
                            "Tab đọc PDF, đang được chọn"
                        } else {
                            "Tab đọc PDF, chạm hai lần để chọn"
                        }
                    }
                )
                NavigationBarItem(
                    icon = { /* Icon */ },
                    label = { Text("Scanner") },
                    selected = selectedTab == 1,
                    onClick = { 
                        viewModel.selectTab(1)
                        context.provideFeedback(
                            VoiceFeedback.FeedbackType.NAVIGATION,
                            NavigationHelper.getScreenTitle(NavigationHelper.Screen.SCANNER)
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (selectedTab == 1) {
                            "Tab quét ảnh, đang được chọn"
                        } else {
                            "Tab quét ảnh, chạm hai lần để chọn"
                        }
                    }
                )
                NavigationBarItem(
                    icon = { /* Icon */ },
                    label = { Text("Live") },
                    selected = selectedTab == 2,
                    onClick = { 
                        viewModel.selectTab(2)
                        context.provideFeedback(
                            VoiceFeedback.FeedbackType.NAVIGATION,
                            NavigationHelper.getScreenTitle(NavigationHelper.Screen.LIVE_READER)
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (selectedTab == 2) {
                            "Tab đọc trực tiếp, đang được chọn"
                        } else {
                            "Tab đọc trực tiếp, chạm hai lần để chọn"
                        }
                    }
                )
                NavigationBarItem(
                    icon = { /* Icon */ },
                    label = { Text("Settings") },
                    selected = selectedTab == 3,
                    onClick = { 
                        viewModel.selectTab(3)
                        context.provideFeedback(
                            VoiceFeedback.FeedbackType.NAVIGATION,
                            NavigationHelper.getScreenTitle(NavigationHelper.Screen.SETTINGS)
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (selectedTab == 3) {
                            "Tab cài đặt, đang được chọn"
                        } else {
                            "Tab cài đặt, chạm hai lần để chọn"
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> PdfReaderScreen()
                1 -> ScannerScreen()
                2 -> LiveReaderScreen()
                3 -> SettingsScreen()
            }
        }
    }
}
