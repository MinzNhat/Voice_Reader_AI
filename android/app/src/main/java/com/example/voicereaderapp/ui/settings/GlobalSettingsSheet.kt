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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicereaderapp.R
import com.example.voicereaderapp.domain.model.TTSLanguage
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender
import com.example.voicereaderapp.utils.LocaleHelper
import android.app.Activity

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
    useMainVoiceForAll: Boolean = false,
    mainVoiceId: String = "matt",
    useMainSpeedForAll: Boolean = false,
    mainSpeed: Float = 1.0f,
    liveScanBarStyle: com.example.voicereaderapp.domain.model.LiveScanBarStyle = com.example.voicereaderapp.domain.model.LiveScanBarStyle.EDGE_BAR,
    onLanguageChange: ((String) -> Unit)? = null,
    onThemeChange: ((com.example.voicereaderapp.domain.model.ThemeMode) -> Unit)? = null,
    onMainVoiceToggle: ((Boolean) -> Unit)? = null,
    onMainVoiceChange: ((String) -> Unit)? = null,
    onMainSpeedToggle: ((Boolean) -> Unit)? = null,
    onMainSpeedChange: ((Float) -> Unit)? = null,
    onLiveScanBarStyleChange: ((com.example.voicereaderapp.domain.model.LiveScanBarStyle) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var currentLanguage by remember {
        mutableStateOf(TTSLanguage.fromCode(selectedLanguage))
    }

    val context = LocalContext.current
    val activity = context as? Activity

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

                            // Change app locale immediately
                            activity?.let {
                                LocaleHelper.changeLanguage(it, language.code)
                            }
                        },
                        label = { Text(languageName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Divider(modifier = Modifier.padding(bottom = 24.dp))

            // Main Speed for All Documents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.use_main_speed_for_all),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = stringResource(R.string.override_speed_settings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = useMainSpeedForAll,
                    onCheckedChange = { onMainSpeedToggle?.invoke(it) }
                )
            }

            // Speed Slider (visible when main speed is enabled)
            if (useMainSpeedForAll) {
                Text(
                    text = stringResource(R.string.main_speed),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "0.5x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Slider(
                        value = mainSpeed,
                        onValueChange = { onMainSpeedChange?.invoke(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14, // 0.1 increments
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "2.0x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = stringResource(R.string.current_speed, mainSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }

            Divider(modifier = Modifier.padding(bottom = 24.dp))

            // Main Voice for All Documents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.use_main_voice_for_all),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = stringResource(R.string.override_voice_settings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = useMainVoiceForAll,
                    onCheckedChange = { onMainVoiceToggle?.invoke(it) }
                )
            }

            // Voice Selection (visible when main voice is enabled)
            if (useMainVoiceForAll) {
                Text(
                    text = stringResource(R.string.main_voice),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Korean Voices
                Text(
                    text = stringResource(R.string.korean),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("nminseo" to "Minseo", "nshasha" to "Shasha", "danna" to "Movie Choi", "vmaum" to "Mammom").forEach { (id, name) ->
                        FilterChip(
                            selected = mainVoiceId == id,
                            onClick = { onMainVoiceChange?.invoke(id) },
                            label = { Text(name, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // English Voices
                Text(
                    text = stringResource(R.string.english),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("nanna" to "Anna", "nclara" to "Clara", "matt" to "Matt").forEach { (id, name) ->
                        FilterChip(
                            selected = mainVoiceId == id,
                            onClick = { onMainVoiceChange?.invoke(id) },
                            label = { Text(name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(bottom = 24.dp))

            // Live Scan Bar Style
            Text(
                text = stringResource(R.string.live_scan_bar_style),
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
                FilterChip(
                    selected = liveScanBarStyle == com.example.voicereaderapp.domain.model.LiveScanBarStyle.EDGE_BAR,
                    onClick = { onLiveScanBarStyleChange?.invoke(com.example.voicereaderapp.domain.model.LiveScanBarStyle.EDGE_BAR) },
                    label = { Text(stringResource(R.string.edge_bar)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = liveScanBarStyle == com.example.voicereaderapp.domain.model.LiveScanBarStyle.CIRCLE_BUTTON,
                    onClick = { onLiveScanBarStyleChange?.invoke(com.example.voicereaderapp.domain.model.LiveScanBarStyle.CIRCLE_BUTTON) },
                    label = { Text(stringResource(R.string.circle_button)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
