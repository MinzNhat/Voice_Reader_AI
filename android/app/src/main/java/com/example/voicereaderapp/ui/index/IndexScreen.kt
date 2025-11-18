@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.voicereaderapp.ui.index

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.ui.pdfreader.PdfReaderScreen
import com.example.voicereaderapp.ui.reader.ReaderScreen
import com.example.voicereaderapp.ui.scanner.ScannerScreen
import com.example.voicereaderapp.R
import com.example.voicereaderapp.ui.livereader.overlay.LiveOverlayService
import com.example.voicereaderapp.ui.pdfreader.DocumentPickerScreen
import com.example.voicereaderapp.ui.pdfreader.PDFViewerScreen

// --------------------------------------------------------
//  ROUTES
// --------------------------------------------------------

/**
 * NavHost bao toàn bộ flow của tab Home.
 */
@Composable
fun IndexWrapper() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Index.route
    ) {
        composable(Screen.Index.route) {
            IndexScreen(navController = navController)
        }
        composable(Screen.Scanner.route) {
            ScannerScreen(navController = navController)
        }
        composable(Screen.DocumentList.route) {
            PdfReaderScreen(navController = navController)
        }
        composable(Screen.Reader.route) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")
            if (documentId != null) {
                ReaderScreen(navController = navController, documentId = documentId)
            } else {
                navController.popBackStack()
            }
        }
        // Document picker for OCR flow
        composable(Screen.DocumentPicker.route) {
            DocumentPickerScreen(
                onDocumentSelected = { uri ->
                    navController.navigate(Screen.PDFViewer.createRoute(uri.toString()))
                }
            )
        }

        // PDF Viewer with OCR (Speechify-style) - for new imports
        composable(
            route = Screen.PDFViewer.route,
            arguments = listOf(navArgument("fileUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileUri = backStackEntry.arguments?.getString("fileUri")
            if (fileUri != null) {
                val uri = Uri.parse(fileUri)
                PDFViewerScreen(
                    fileUri = uri,
                    navController = navController
                )
            } else {
                navController.popBackStack()
            }
        }

        // PDF Viewer for saved documents (from Continue Listening)
        composable(
            route = "pdf_saved/{documentId}",
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")
            if (documentId != null) {
                PDFViewerScreen(
                    documentId = documentId,
                    navController = navController
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}

// --------------------------------------------------------
//  DATA UI
// --------------------------------------------------------

data class ImportSource(
    val name: String,
    val icon: ImageVector,
    val tint: Color
)

// --------------------------------------------------------
//  HOME SCREEN (NEW DESIGN)
// --------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexScreen(
    navController: NavController,
    viewModel: IndexViewModel = hiltViewModel()
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current

    // ---------------- Bắt đầu khởi tạo các biến cho livereader -----------------------------
    var isLiveScanEnabled by remember { mutableStateOf(false) }
    var wasPermissionRequested by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {  }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (wasPermissionRequested) {
                    wasPermissionRequested = false
                    val hasPermission = Settings.canDrawOverlays(context)
                    if (hasPermission) {
                        LiveOverlayService.start(context, "Live Reader đã được kích hoạt.")
                        isLiveScanEnabled = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // ---------------- Kết thúc khởi tạo các biến cho livereader ----------------------------


    // State for rename dialog
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedDocumentId by remember { mutableStateOf<String?>(null) }
    var selectedDocumentTitle by remember { mutableStateOf("") }

    // State for global settings sheet
    var showGlobalSettings by remember { mutableStateOf(false) }
    val settingsViewModel: com.example.voicereaderapp.ui.settings.SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    // File picker for all document types (PDF, DOCX, Images)
    val allFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Navigate to PDF Viewer with OCR processing (works for all file types)
            navController.navigate(Screen.PDFViewer.createRoute(selectedUri.toString()))
        }
    }

    // File picker for Images only (for Drive/Albums)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Navigate to PDF Viewer with OCR processing
            navController.navigate(Screen.PDFViewer.createRoute(selectedUri.toString()))
        }
    }

    val importSources = listOf(
        ImportSource(name = "Albums", Icons.Default.PhotoLibrary, tint = Color(0xFF3B82F6)),
        ImportSource(name ="Files", Icons.Default.Folder, tint = Color(0xFFFFDB33)),
//        ImportSource("Gmail", Icons.Default.Email, Color(0xFFEF4444)),
//        ImportSource("Messenger", Icons.Default.Message, Color(0xFF0EA5E9))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            containerColor = Color.Transparent,
            topBar = {
                HomeTopBar(
                    onSettingsClick = { showGlobalSettings = true }
                )
            },
            floatingActionButton = {
                ScanFab {
                    navController.navigate(Screen.Scanner.route)
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    GreetingCard()
                }

                //            item {
                //                QuickActionsCard(
                //                    onScanClick = { navController.navigate(Screen.Scanner.route) },
                //                    onImportClick = { filePickerLauncher.launch("application/pdf") },
                //                    onLibraryClick = { navController.navigate(Screen.DocumentList.route) }
                //                )
                //            }

                item {
                    ContinueListeningCard(
                        documents = documents,
                        onItemClick = { document ->
                            // Route to appropriate screen based on document type
                            when (document.type) {
                                DocumentType.PDF -> {
                                    // PDF documents use PDFViewerScreen with backend TTS
                                    navController.navigate("pdf_saved/${document.id}")
                                }
                                else -> {
                                    // Text/Live Screen use ReaderScreen with local TTS
                                    navController.navigate(Screen.Reader.createRoute(document.id))
                                }
                            }
                        },
                        onDelete = { documentId ->
                            viewModel.deleteDocument(documentId)
                        },
                        onRename = { documentId, currentTitle ->
                            selectedDocumentId = documentId
                            selectedDocumentTitle = currentTitle
                            showRenameDialog = true
                        }
                    )
                }

                item {
                    ImportSectionCard(
                        sources = importSources,
                        onImportClick = { name ->
                            when (name) {
                                "Files" -> {
                                    // Files: Accept PDF, DOCX, and Images
                                    allFilesPickerLauncher.launch("*/*")
                                }
                                "Albums" -> {
                                    // Albums: Accept Images only
                                    imagePickerLauncher.launch("image/*")
                                }
                                else -> { /* future: open other apps*/
                                }
                            }
                        },
                        isLiveScanEnabled = isLiveScanEnabled,
                        onLiveScanToggle = { isChecked ->
                            if (isChecked) {
                                // Người dùng muốn BẬT
                                val hasPermission = Settings.canDrawOverlays(context)
                                if (hasPermission) {
                                    LiveOverlayService.start(context, "Live Reader đã được kích hoạt.")
                                    isLiveScanEnabled = true
                                } else {
                                    wasPermissionRequested = true
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                }
                            } else {
                                // Người dùng muốn TẮT
                                LiveOverlayService.stop(context)
                                isLiveScanEnabled = false
                            }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(80.dp)) // chừa chỗ cho FAB ở dưới
                }
            }
        }

        // Rename dialog
        if (showRenameDialog) {
            RenameDocumentDialog(
                currentTitle = selectedDocumentTitle,
                onDismiss = {
                    showRenameDialog = false
                    selectedDocumentId = null
                    selectedDocumentTitle = ""
                },
                onConfirm = { newTitle ->
                    selectedDocumentId?.let { documentId ->
                        viewModel.updateDocumentTitle(documentId, newTitle)
                    }
                    showRenameDialog = false
                    selectedDocumentId = null
                    selectedDocumentTitle = ""
                }
            )
        }

        // Global Settings Sheet
        if (showGlobalSettings) {
            com.example.voicereaderapp.ui.settings.GlobalSettingsSheet(
                speed = settingsState.settings.speed,
                selectedVoice = settingsState.settings.voiceId.ifEmpty { "matt" },
                selectedLanguage = settingsState.settings.language,
                selectedTheme = settingsState.settings.theme,
                onSpeedChange = {
                    settingsViewModel.updateSpeed(it)
                    settingsViewModel.saveSettings()
                },
                onVoiceChange = { settingsViewModel.updateVoice(it) },
                onVoiceAndLanguageChange = { voiceId, language ->
                    settingsViewModel.updateVoiceAndLanguage(voiceId, language)
                },
                onThemeChange = { theme ->
                    settingsViewModel.updateTheme(theme)
                },
                onDismiss = { showGlobalSettings = false }
            )
        }
    }
}

// --------------------------------------------------------
//  TOP BAR + HEADER
// --------------------------------------------------------

@Composable
fun HomeTopBar(
    onSettingsClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_2),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.Transparent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VoiceReader",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun GreetingCard() {

    val greeting = remember { getGreetingMessage() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = greeting,   // ⬅ dùng greeting động
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ready to keep listening to your documents?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --------------------------------------------------------
//  QUICK ACTIONS
// --------------------------------------------------------

@Composable
fun QuickActionsCard(
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onLibraryClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF8FAFF)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionChip(
                    icon = Icons.Default.PhotoCamera,
                    label = "Scan",
                    accent = Color(0xFF2563EB),
                    onClick = onScanClick
                )
                QuickActionChip(
                    icon = Icons.Default.UploadFile,
                    label = "Import",
                    accent = Color(0xFF10B981),
                    onClick = onImportClick
                )
                QuickActionChip(
                    icon = Icons.Default.LibraryBooks,
                    label = "Library",
                    accent = Color(0xFFF97316),
                    onClick = onLibraryClick
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.12f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// --------------------------------------------------------
//  CONTINUE LISTENING
// --------------------------------------------------------

@Composable
fun ContinueListeningCard(
    documents: List<ReadingDocument>,
    onItemClick: (ReadingDocument) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Continue Listening",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            if (documents.isEmpty()) {
                Text(
                    text = "No documents yet. Try importing a file!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(documents) { doc ->
                        RecentDocCard(
                            document = doc,
                            onClick = { onItemClick(doc) },
                            onDelete = { onDelete(doc.id) },
                            onRename = { onRename(doc.id, doc.title) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentDocCard(
    document: ReadingDocument,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null
) {
    val words = remember(document.content) {
        if (document.content.isBlank()) emptyList()
        else document.content.split(" ")
    }

    val progress = if (words.isEmpty()) 0f
    else (document.lastReadPosition.toFloat() / words.size.toFloat())
        .coerceIn(0f, 1f)

    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(160.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.onPrimary
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (document.type === DocumentType.IMAGE)
                                        Color(0xFFFFE5E5)
                                    else
                                        Color(0xFFE5EDFF),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (document.type === DocumentType.IMAGE)
                                    Icons.Outlined.Image
                                else
                                    Icons.Outlined.Description,
                                contentDescription = null,
                                tint = if (document.type === DocumentType.IMAGE)
                                    Color(0xFFE53935)
                                else
                                    Color(0xFF1D4ED8)
                            )
                        }

                        // Options menu button
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        showMenu = false
                                        onRename?.invoke()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, "Rename")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showMenu = false
                                        onDelete?.invoke()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, "Delete")
                                    }
                                )
                            }
                        }
                    }

                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(color = Color.Red)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = document.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 2000,
                    initialDelayMillis = 2000,
                    velocity = 30.dp
                )
        )

        Text(
            text = "Progress ${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --------------------------------------------------------
//  IMPORT & LISTEN
// --------------------------------------------------------

@Composable
fun ImportSectionCard(
    sources: List<ImportSource>,
    onImportClick: (String) -> Unit,
    isLiveScanEnabled: Boolean,
    onLiveScanToggle: (Boolean) -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Import & Listen",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sources.forEach { source ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onImportClick(source.name) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    source.tint.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = source.icon,
                                contentDescription = source.name,
                                tint = source.tint
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Live Scan Toggle
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLiveScanToggle(!isLiveScanEnabled) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isLiveScanEnabled)
                                    Color(0xFF10B981).copy(alpha = 0.12f)
                                else
                                    Color(0xFF6B7280).copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Live Scan",
                            tint = if (isLiveScanEnabled)
                                Color(0xFF10B981)
                            else
                                Color(0xFF6B7280)
                        )
                    }

                    Column {
                        Text(
                            text = "Live Scan",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Scan text in other apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isLiveScanEnabled,
                    onCheckedChange = onLiveScanToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF9CA3AF)
                    )
                )
            }
        }
    }
}

// --------------------------------------------------------
//  FLOATING SCAN BUTTON
// --------------------------------------------------------

@Composable
fun ScanFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFF2563EB),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
            .size(72.dp)
            .shadow(10.dp, CircleShape, clip = false)
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = "Scan",
            modifier = Modifier.size(30.dp)
        )
    }
}

// --------------------------------------------------------
//  RENAME DIALOG
// --------------------------------------------------------

@Composable
fun RenameDocumentDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Document",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a new name for this document:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedIndicatorColor = Color(0xFF2563EB),
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(newTitle)
                    }
                },
                enabled = newTitle.isNotBlank()
            ) {
                Text(
                    "Rename",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

fun getGreetingMessage(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 5..11 -> "Good morning ☀️"
        in 12..17 -> "Good afternoon 🌤"
        else -> "Good evening 🌙"
    }
}
