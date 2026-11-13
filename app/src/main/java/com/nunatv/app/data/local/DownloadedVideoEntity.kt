package com.nunatv.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

@Entity(tableName = "downloaded_videos")
data class DownloadedVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val originalUrl: String,
    val title: String,
    val author: String = "",
    val thumbnailUrl: String,
    val localFilePath: String = "",
    val resolution: String,
    val fileSize: Long = 0L,
    val durationSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val downloadTimestamp: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val category: String = "Umum"
) {
    fun categoriesList(): List<String> {
        return category.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Umum") }
    }

    fun isIn(targetCategory: String): Boolean {
        if (targetCategory.equals("Semua", ignoreCase = true)) return true
        return categoriesList().any { it.equals(targetCategory, ignoreCase = true) }
    }
}
