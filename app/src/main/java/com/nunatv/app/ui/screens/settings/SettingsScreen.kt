package com.nunatv.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunatv.app.data.preferences.AppColorPalette
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.preferences.AppThemeMode
import com.nunatv.app.data.preferences.AudioBoostLevel
import com.nunatv.app.data.preferences.VideoResolution
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.StorageStatusCard
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.util.DeviceUtils
import com.nunatv.app.util.StorageUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    onShowGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTv = remember { DeviceUtils.isTvDevice(context) }
    val currentResolution by appPreferences.selectedResolution.collectAsState(initial = VideoResolution.HIGH)
    val showDetailedResolution by appPreferences.showDetailedResolution.collectAsState(initial = false)
    var resolutionTapCount by remember { mutableStateOf(0) }
    var lastResolutionTapTime by remember { mutableStateOf(0L) }
    val currentBoostLevel by appPreferences.audioBoostLevel.collectAsState(initial = AudioBoostLevel.OFF)
    val backgroundPlayEnabled by appPreferences.backgroundPlaybackEnabled.collectAsState(initial = false)
    val pipEnabled by appPreferences.pipEnabled.collectAsState(initial = false)
    val hideShortsEnabled by appPreferences.hideShortsEnabled.collectAsState(initial = false)
    val currentPalette by appPreferences.colorPalette.collectAsState(initial = AppColorPalette.RED)
    val currentThemeMode by appPreferences.themeMode.collectAsState(initial = AppThemeMode.DARK)
    val downloadPath by appPreferences.downloadLocation.collectAsState(initial = "")
    val storageInfo = remember(downloadPath) {
        val dir = if (downloadPath.isNotBlank()) File(downloadPath) else StorageUtils.getDefaultDownloadDir(context)
        StorageUtils.getStorageInfo(dir)
    }

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Pengaturan Nuna TV",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tampilan & Palet Warna (Material 3 Expressive)
        Text(
            text = "Tampilan & Tema",
            style = MaterialTheme.typography.titleMedium.copy(
                color = colorScheme.primary
            )
        )
        Text(
            text = "Pilih mode gelap/terang dan palet warna utama aplikasi.",
            style = MaterialTheme.typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = currentThemeMode == mode
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .dpadFocusable(
                            shape = RoundedCornerShape(20.dp),
                            style = FocusStyle.PILL,
                            onClick = {
                                coroutineScope.launch { appPreferences.setThemeMode(mode) }
                            }
                        )
                        .clickable {
                            coroutineScope.launch { appPreferences.setThemeMode(mode) }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (mode == AppThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mode.label,
                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppColorPalette.entries.forEach { palette ->
                val isSelected = currentPalette == palette
                val swatchColor = Color(palette.swatchHex)
                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .dpadFocusable(
                            shape = RoundedCornerShape(50),
                            style = FocusStyle.PILL,
                            onClick = {
                                coroutineScope.launch { appPreferences.setColorPalette(palette) }
                            }
                        )
                        .clickable {
                            coroutineScope.launch { appPreferences.setColorPalette(palette) }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(swatchColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = palette.label,
                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Resolusi Default Unduhan",
            style = MaterialTheme.typography.titleMedium.copy(
                color = colorScheme.primary
            )
        )
        Text(
            text = if (showDetailedResolution) {
                "Mode Detail: Menentukan resolusi spesifik angka untuk unduhan video."
            } else {
                "Kualitas video yang akan otomatis digunakan saat menekan opsi Download."
            },
            style = MaterialTheme.typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val onResolutionClicked: (VideoResolution) -> Unit = { selectedRes ->
            val now = System.currentTimeMillis()
            if (now - lastResolutionTapTime > 1500L) {
                resolutionTapCount = 1
            } else {
                resolutionTapCount++
            }
            lastResolutionTapTime = now

            if (resolutionTapCount >= 5) {
                resolutionTapCount = 0
                val targetMode = !showDetailedResolution
                coroutineScope.launch {
                    appPreferences.setShowDetailedResolution(targetMode)
                    val mappedRes = if (targetMode) {
                        when (selectedRes) {
                            VideoResolution.AUDIO -> VideoResolution.AUDIO_ONLY
                            VideoResolution.LOW -> VideoResolution.RES_240P
                            VideoResolution.MEDIUM -> VideoResolution.RES_720P
                            VideoResolution.HIGH -> VideoResolution.RES_1080P
                            else -> selectedRes
                        }
                    } else {
                        when (selectedRes) {
                            VideoResolution.AUDIO_ONLY -> VideoResolution.AUDIO
                            VideoResolution.RES_240P, VideoResolution.RES_360P -> VideoResolution.LOW
                            VideoResolution.RES_480P, VideoResolution.RES_720P -> VideoResolution.MEDIUM
                            VideoResolution.RES_1080P -> VideoResolution.HIGH
                            else -> selectedRes
                        }
                    }
                    appPreferences.setResolution(mappedRes)
                }
                android.widget.Toast.makeText(
                    context,
                    if (targetMode) "Mode Resolusi Detail Diaktifkan" else "Kembali ke Mode Resolusi Bawaan",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                coroutineScope.launch {
                    appPreferences.setResolution(selectedRes)
                }
            }
        }

        val displayedResolutions = if (showDetailedResolution) {
            VideoResolution.DETAILED_TIERS
        } else {
            VideoResolution.DEFAULT_TIERS
        }

        // Pilihan Resolusi
        displayedResolutions.forEach { resolution ->
            val isSelected = if (showDetailedResolution) {
                currentResolution == resolution
            } else {
                currentResolution == resolution ||
                    (resolution == VideoResolution.HIGH && currentResolution == VideoResolution.RES_1080P) ||
                    (resolution == VideoResolution.MEDIUM && (currentResolution == VideoResolution.RES_720P || currentResolution == VideoResolution.RES_480P)) ||
                    (resolution == VideoResolution.LOW && (currentResolution == VideoResolution.RES_360P || currentResolution == VideoResolution.RES_240P)) ||
                    (resolution == VideoResolution.AUDIO && currentResolution == VideoResolution.AUDIO_ONLY)
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .dpadFocusable(
                        shape = RoundedCornerShape(16.dp),
                        style = FocusStyle.PILL,
                        onClick = { onResolutionClicked(resolution) }
                    )
                    .clickable { onResolutionClicked(resolution) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorScheme.primary,
                            unselectedColor = colorScheme.onSurfaceVariant
                        )
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = resolution.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        if (resolution.description.isNotBlank()) {
                            Text(
                                text = resolution.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Penguat Suara (Audio Boost)
        Text(
            text = "Penguat Suara (Audio Boost)",
            style = MaterialTheme.typography.titleMedium.copy(
                color = colorScheme.primary
            )
        )
        Text(
            text = "Meningkatkan kenyaringan suara video saat pemutaran jika volume bawaan terasa pelan.",
            style = MaterialTheme.typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AudioBoostLevel.entries.forEach { level ->
            val isSelected = currentBoostLevel == level
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .dpadFocusable(
                        shape = RoundedCornerShape(16.dp),
                        style = FocusStyle.PILL,
                        onClick = {
                            coroutineScope.launch {
                                appPreferences.setAudioBoostLevel(level)
                            }
                        }
                    )
                    .clickable {
                        coroutineScope.launch {
                            appPreferences.setAudioBoostLevel(level)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorScheme.primary,
                            unselectedColor = colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Pemutaran, Konten & Jendela
        Text(
            text = "Konten & Pemutaran",
            style = MaterialTheme.typography.titleMedium.copy(
                color = colorScheme.primary
            )
        )
        Text(
            text = "Atur filter YouTube Shorts, pemutaran latar belakang, dan jendela mengambang.",
            style = MaterialTheme.typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SettingsToggleCard(
            title = "Sembunyikan Video Shorts",
            subtitle = "Hilangkan tab Shorts dan sembunyikan video pendek vertikal dari feed serta hasil pencarian YouTube.",
            checked = hideShortsEnabled,
            onToggle = { enabled ->
                coroutineScope.launch {
                    appPreferences.setHideShortsEnabled(enabled)
                }
            }
        )

        SettingsToggleCard(
            title = "Putar di Belakang Layar (Audio)",
            subtitle = "Audio terus berjalan seperti YouTube Music saat pindah aplikasi atau layar mati.",
            checked = backgroundPlayEnabled,
            onToggle = { enabled ->
                coroutineScope.launch {
                    appPreferences.setBackgroundPlaybackEnabled(enabled)
                }
            }
        )

        if (!isTv) {
            SettingsToggleCard(
                title = "Popup Jendela (Picture-in-Picture)",
                subtitle = "Video otomatis mengambang di layar HP saat menekan Home. Menutup jendela menghentikan pemutaran.",
                checked = pipEnabled,
                onToggle = { enabled ->
                    coroutineScope.launch {
                        appPreferences.setPipEnabled(enabled)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Status & Lokasi Penyimpanan
        Text(
            text = "Status & Lokasi Penyimpanan",
            style = MaterialTheme.typography.titleMedium.copy(
                color = colorScheme.primary
            )
        )
        Text(
            text = "Sisa ruang bebas dan kapasitas video yang tersimpan di perangkat.",
            style = MaterialTheme.typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        StorageStatusCard(
            storageInfo = storageInfo,
            downloadPath = downloadPath
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Panduan & Bantuan",
            style = MaterialTheme.typography.titleMedium.copy(
                color = colorScheme.primary
            )
        )
        Text(
            text = "Lihat ringkasan cara pakai Home offline, Tab YouTube, dan kontrol remote atau HP.",
            style = MaterialTheme.typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onShowGuide,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceContainerHigh,
                contentColor = colorScheme.onSurface
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .dpadFocusable(
                    shape = RoundedCornerShape(50),
                    style = FocusStyle.PILL,
                    onClick = onShowGuide
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Buka Panduan Penggunaan Nuna TV",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) colorScheme.primaryContainer.copy(alpha = 0.72f) else colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .dpadFocusable(
                shape = RoundedCornerShape(16.dp),
                style = FocusStyle.PILL,
                onClick = { onToggle(!checked) }
            )
            .clickable { onToggle(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (checked) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                        fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (checked) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorScheme.onPrimary,
                    checkedTrackColor = colorScheme.primary,
                    uncheckedThumbColor = colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = colorScheme.surfaceContainerHigh
                )
            )
        }
    }
}
