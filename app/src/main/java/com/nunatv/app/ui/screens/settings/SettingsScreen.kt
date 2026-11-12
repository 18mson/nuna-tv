package com.nunatv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.preferences.AudioBoostLevel
import com.nunatv.app.data.preferences.VideoResolution
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.StorageStatusCard
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.util.StorageUtils
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentResolution by appPreferences.selectedResolution.collectAsState(initial = VideoResolution.RES_1080P)
    val currentBoostLevel by appPreferences.audioBoostLevel.collectAsState(initial = AudioBoostLevel.OFF)
    val downloadPath by appPreferences.downloadLocation.collectAsState(initial = "")
    val storageInfo = remember(downloadPath) {
        val dir = if (downloadPath.isNotBlank()) File(downloadPath) else StorageUtils.getDefaultDownloadDir(context)
        StorageUtils.getStorageInfo(dir)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Pengaturan Nuna TV",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Resolusi Default Unduhan",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE50914)
            )
        )
        Text(
            text = "Kualitas video yang akan otomatis digunakan saat menekan opsi Download.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Pilihan Resolusi
        VideoResolution.entries.forEach { resolution ->
            val isSelected = currentResolution == resolution
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF262626) else Color(0xFF1A1A1A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .dpadFocusable(
                        shape = RoundedCornerShape(10.dp),
                        style = FocusStyle.PILL,
                        onClick = {
                            coroutineScope.launch {
                                appPreferences.setResolution(resolution)
                            }
                        }
                    )
                    .clickable {
                        coroutineScope.launch {
                            appPreferences.setResolution(resolution)
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
                            selectedColor = Color(0xFFE50914),
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = resolution.label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Penguat Suara (Audio Boost)
        Text(
            text = "Penguat Suara (Audio Boost)",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE50914)
            )
        )
        Text(
            text = "Meningkatkan kenyaringan suara video saat pemutaran jika volume bawaan terasa pelan.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AudioBoostLevel.entries.forEach { level ->
            val isSelected = currentBoostLevel == level
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF262626) else Color(0xFF1A1A1A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .dpadFocusable(
                        shape = RoundedCornerShape(10.dp),
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
                            selectedColor = Color(0xFFE50914),
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Status & Lokasi Penyimpanan
        Text(
            text = "Status & Lokasi Penyimpanan",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE50914)
            )
        )
        Text(
            text = "Sisa ruang bebas dan kapasitas video yang tersimpan di perangkat.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        StorageStatusCard(
            storageInfo = storageInfo,
            downloadPath = downloadPath
        )
    }
}
