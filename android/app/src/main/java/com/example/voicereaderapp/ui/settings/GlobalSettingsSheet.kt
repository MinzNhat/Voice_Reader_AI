package com.example.voicereaderapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicereaderapp.R
import com.example.voicereaderapp.domain.model.TTSLanguage
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender

/**
 * Global Settings Sheet
 * Appears as a modal bottom sheet in IndexScreen
 * Contains only Theme and Language settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsSheet(
    selectedLanguage: String = "en-US",
    selectedTheme: com.example.voicereaderapp.domain.model.ThemeMode = com.example.voicereaderapp.domain.model.ThemeMode.SYSTEM,
    onLanguageChange: ((String) -> Unit)? = null,
    onThemeChange: ((com.example.voicereaderapp.domain.model.ThemeMode) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var currentLanguage by remember {
        mutableStateOf(TTSLanguage.fromCode(selectedLanguage))
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 0.75f  // 3/4 of screen height

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = stringResource(R.string.settings_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Divider(modifier = Modifier.padding(bottom = 24.dp))

            // Theme Selection
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.voicereaderapp.domain.model.ThemeMode.values().forEach { theme ->
                    val themeName = when (theme) {
                        com.example.voicereaderapp.domain.model.ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        com.example.voicereaderapp.domain.model.ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        com.example.voicereaderapp.domain.model.ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    }
                    FilterChip(
                        selected = selectedTheme == theme,
                        onClick = { onThemeChange?.invoke(theme) },
                        label = { Text(themeName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Language Selection
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TTSLanguage.values().forEach { language ->
                    val languageName = when (language) {
                        TTSLanguage.KOREAN -> stringResource(R.string.language_korean)
                        TTSLanguage.ENGLISH -> stringResource(R.string.language_english)
                    }
                    FilterChip(
                        selected = currentLanguage == language,
                        onClick = {
                            currentLanguage = language
                            onLanguageChange?.invoke(language.code)
                        },
                        label = { Text(languageName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
