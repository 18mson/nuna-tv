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
        storageInfo.availableBytes < StorageUtils.LOW_STORAGE_THRESHOLD_BYTES -> MaterialTheme.colorScheme.error
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
    modifier: Modifier = Modifier,
    compactHeader: Boolean = false
) {
    val statusColor = when {
        storageInfo.availableBytes < StorageUtils.LOW_STORAGE_THRESHOLD_BYTES -> MaterialTheme.colorScheme.error
        storageInfo.availableBytes < 800L * 1024L * 1024L -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }

    Surface(
        shape = if (compactHeader) RoundedCornerShape(50) else RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (storageInfo.isLowStorage) statusColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .then(if (compactHeader) Modifier else Modifier.fillMaxWidth())
                .padding(
                    horizontal = if (compactHeader) 10.dp else 14.dp,
                    vertical = if (compactHeader) 5.dp else 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compactHeader) 8.dp else 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(if (compactHeader) 14.dp else 16.dp)
            )

            Text(
                text = when {
                    compactHeader -> "Sisa ${StorageUtils.formatFileSize(storageInfo.availableBytes)}"
                    storageInfo.isLowStorage -> "Penyimpanan hampir penuh! Sisa: ${StorageUtils.formatFileSize(storageInfo.availableBytes)}"
                    else -> "Penyimpanan Tersedia: ${StorageUtils.formatFileSize(storageInfo.availableBytes)} / ${StorageUtils.formatFileSize(storageInfo.totalBytes)}"
                },
                color = if (storageInfo.isLowStorage) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compactHeader) 11.sp else 12.sp,
                fontWeight = if (storageInfo.isLowStorage) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )

            LinearProgressIndicator(
                progress = { storageInfo.usedFraction },
                modifier = Modifier
                    .then(if (compactHeader) Modifier.width(56.dp) else Modifier.weight(1f))
                    .height(if (compactHeader) 5.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        }
    }
}
