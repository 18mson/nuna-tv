package com.nunatv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunatv.app.util.StorageInfo
import com.nunatv.app.util.StorageUtils

@Composable
fun StorageStatusCard(
    storageInfo: StorageInfo,
    downloadPath: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        storageInfo.availableBytes < StorageUtils.LOW_STORAGE_THRESHOLD_BYTES -> Color(0xFFE50914)
        storageInfo.availableBytes < 800L * 1024L * 1024L -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        modifier = modifier
            .fillMaxWidth()
            .dpadFocusable(shape = RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kapasitas Penyimpanan",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "Sisa ${StorageUtils.formatFileSize(storageInfo.availableBytes)}",
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { storageInfo.usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terpakai: ${StorageUtils.formatFileSize(storageInfo.usedBytes)} / ${StorageUtils.formatFileSize(storageInfo.totalBytes)}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Video Nuna TV: ${StorageUtils.formatFileSize(storageInfo.appVideosBytes)}",
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (downloadPath.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Lokasi: $downloadPath",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CompactStorageBar(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        storageInfo.availableBytes < StorageUtils.LOW_STORAGE_THRESHOLD_BYTES -> Color(0xFFE50914)
        storageInfo.availableBytes < 800L * 1024L * 1024L -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1A1A1A),
        border = BorderStroke(1.dp, if (storageInfo.isLowStorage) statusColor else Color(0xFF2D2D2D)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = if (storageInfo.isLowStorage) {
                    "Penyimpanan hampir penuh! Sisa: ${StorageUtils.formatFileSize(storageInfo.availableBytes)}"
                } else {
                    "Penyimpanan Tersedia: ${StorageUtils.formatFileSize(storageInfo.availableBytes)} / ${StorageUtils.formatFileSize(storageInfo.totalBytes)}"
                },
                color = if (storageInfo.isLowStorage) Color(0xFFFF8A80) else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = if (storageInfo.isLowStorage) FontWeight.Bold else FontWeight.Medium
            )

            LinearProgressIndicator(
                progress = { storageInfo.usedFraction },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = Color(0xFF333333)
            )
        }
    }
}
