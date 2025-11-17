@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.voicereaderapp.ui.index

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.ui.pdfreader.PdfReaderScreen
import com.example.voicereaderapp.ui.reader.ReaderScreen
import com.example.voicereaderapp.ui.scanner.ScannerScreen
import com.example.voicereaderapp.R

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

    // File picker để import PDF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            val bytes = inputStream?.readBytes() ?: ByteArray(0)
            inputStream?.close()

            val extractedText = "File imported from device.\nSize: ${bytes.size} bytes"

            val newDoc = ReadingDocument(
                id = "doc_${System.currentTimeMillis()}",
                title = "Imported File",
                content = extractedText,
                type = DocumentType.PDF,
                createdAt = System.currentTimeMillis(),
                lastReadPosition = 0
            )
            viewModel.saveImportedDocument(newDoc)
            navController.navigate(Screen.Reader.createRoute(newDoc.id))
        }
    }

    val importSources = listOf(
        ImportSource("Drive", Icons.Default.Cloud, Color(0xFF3B82F6)),
        ImportSource("Files", Icons.Default.Folder, Color(0xFF6B7280)),
        ImportSource("Gmail", Icons.Default.Email, Color(0xFFEF4444)),
        ImportSource("Messenger", Icons.Default.Message, Color(0xFF0EA5E9))
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
                HomeTopBar()
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
                        onItemClick = { docId ->
                            navController.navigate(Screen.Reader.createRoute(docId))
                        }
                    )
                }

                item {
                    ImportSectionCard(
                        sources = importSources,
                        onImportClick = { name ->
                            when (name) {
                                "Drive", "Files" -> filePickerLauncher.launch("application/pdf")
                                else -> { /* future: open other apps*/
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(80.dp)) // chừa chỗ cho FAB ở dưới
                }
            }
        }
    }
}

// --------------------------------------------------------
//  TOP BAR + HEADER
// --------------------------------------------------------

@Composable
fun HomeTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(75.dp)
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
            IconButton(onClick = { /* TODO: user profile */ }) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile"
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
        color = Color(0xFFEEF5FC)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = greeting,   // ⬅ dùng greeting động
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ready to keep listening to your documents?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563)
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
    onItemClick: (String) -> Unit
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
                "Continue Listening",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(12.dp))

            if (documents.isEmpty()) {
                Text(
                    text = "No documents yet. Try importing a file!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9CA3AF)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(documents) { doc ->
                        RecentDocCard(
                            document = doc,
                            onClick = { onItemClick(doc.id) }
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
    onClick: () -> Unit
) {
    val words = remember(document.content) {
        if (document.content.isBlank()) emptyList()
        else document.content.split(" ")
    }

    val progress = if (words.isEmpty()) 0f
    else (document.lastReadPosition.toFloat() / words.size.toFloat())
        .coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFE5EDFF),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8)
                    )
                }

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(Color(0xFFEF4444))
                        )
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "Progress ${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280)
        )
    }
}

// --------------------------------------------------------
//  IMPORT & LISTEN
// --------------------------------------------------------

@Composable
fun ImportSectionCard(
    sources: List<ImportSource>,
    onImportClick: (String) -> Unit
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
                "Import & Listen",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
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
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
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

fun getGreetingMessage(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 5..11 -> "Good morning ☀️"
        in 12..17 -> "Good afternoon 🌤"
        else -> "Good evening 🌙"
    }
}
