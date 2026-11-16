package com.nunatv.app.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.nunatv.app.data.local.DownloadStatus
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.local.VideoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

data class StorageInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val appVideosBytes: Long
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val isLowStorage: Boolean
        get() = availableBytes < StorageUtils.LOW_STORAGE_THRESHOLD_BYTES
}

object StorageUtils {

    // Ambang minimum ruang aman agar download tidak memacetkan Android TV (150 MB)
    const val MIN_REQUIRED_FOR_DOWNLOAD_BYTES = 150L * 1024L * 1024L

    // Ambang peringatan warna merah/kuning pada bar penyimpanan (400 MB)
    const val LOW_STORAGE_THRESHOLD_BYTES = 400L * 1024L * 1024L

    /**
     * Mengambil direktori penyimpanan default untuk video Nuna TV.
     * Menggunakan direktori Scoped Storage Downloads aplikasi.
     */
    fun getDefaultDownloadDir(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Folder publik Download/NunaTV di memori penyimpanan bersama.
     * Video di folder ini TIDAK AKAN terhapus oleh Android saat aplikasi di-uninstall!
     */
    fun getPublicDownloadDir(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val nunaFolder = File(publicDownloads, "NunaTV")
        if (!nunaFolder.exists()) {
            nunaFolder.mkdirs()
        }
        return nunaFolder
    }

    /**
     * Memindahkan video dari penyimpanan aplikasi ke folder publik agar aman dari uninstall.
     */
    suspend fun exportVideosToPublic(
        dao: VideoDao,
        videos: List<DownloadedVideoEntity>
    ): Int {
        val targetDir = getPublicDownloadDir()
        var movedCount = 0
        withContext(Dispatchers.IO) {
            for (video in videos) {
                if (video.localFilePath.isNotBlank()) {
                    val currentFile = File(video.localFilePath)
                    if (currentFile.exists() && currentFile.parentFile?.absolutePath != targetDir.absolutePath) {
                        val newFile = File(targetDir, currentFile.name)
                        try {
                            if (currentFile.renameTo(newFile)) {
                                dao.updateFilePath(video.id, newFile.absolutePath)
                                movedCount++
                            } else {
                                currentFile.copyTo(newFile, overwrite = true)
                                currentFile.delete()
                                dao.updateFilePath(video.id, newFile.absolutePath)
                                movedCount++
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        return movedCount
    }

    /**
     * Memindai folder publik (Download/NunaTV) dan mengimpor video yang belum ada di database.
     */
    suspend fun scanAndImportVideos(
        dao: VideoDao,
        targetDir: File = getPublicDownloadDir()
    ): Int {
        var importedCount = 0
        withContext(Dispatchers.IO) {
            if (!targetDir.exists()) return@withContext
            val existing = dao.getAllVideosList().map { File(it.localFilePath).name }.toSet()
            val videoExtensions = setOf("mp4", "mkv", "webm", "m4a", "mp3")
            targetDir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() > 0 && file.extension.lowercase() in videoExtensions) {
                    if (file.name !in existing) {
                        val name = file.nameWithoutExtension
                        val extractedId = if (name.contains("[") && name.contains("]")) {
                            name.substringAfterLast("[").substringBeforeLast("]")
                        } else {
                            "local_${System.currentTimeMillis()}_${file.name.hashCode()}"
                        }
                        val title = if (name.contains("[")) {
                            name.substringBeforeLast("[").trim().removeSuffix("-").trim()
                        } else {
                            name
                        }
                        val isAudio = file.extension.lowercase() in setOf("m4a", "mp3")
                        val entity = DownloadedVideoEntity(
                            videoId = extractedId,
                            originalUrl = "https://youtube.com/watch?v=$extractedId",
                            title = title.ifBlank { file.nameWithoutExtension },
                            author = "Offline Video",
                            thumbnailUrl = "https://img.youtube.com/vi/$extractedId/hqdefault.jpg",
                            localFilePath = file.absolutePath,
                            fileSize = file.length(),
                            durationSeconds = 0L,
                            resolution = if (isAudio) "Audio" else "720p",
                            category = if (isAudio) "Musik" else "Umum",
                            status = DownloadStatus.COMPLETED,
                            progress = 100
                        )
                        dao.insertVideo(entity)
                        importedCount++
                    }
                }
            }
        }
        return importedCount
    }

    /**
     * Menghitung statistik penyimpanan (Total, Free Space, Terpakai, dan ukuran video Nuna TV).
     */
    fun getStorageInfo(targetDir: File): StorageInfo {
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val stat = StatFs(targetDir.absolutePath)
            val total = stat.totalBytes
            val avail = stat.availableBytes
            val used = (total - avail).coerceAtLeast(0L)
            val appBytes = targetDir.listFiles()
                ?.filter { it.isFile }
                ?.sumOf { it.length() } ?: 0L
            StorageInfo(
                totalBytes = total,
                availableBytes = avail,
                usedBytes = used,
                appVideosBytes = appBytes
            )
        } catch (_: Exception) {
            StorageInfo(0L, 0L, 0L, 0L)
        }
    }

    /**
     * Format byte size menjadi format human-readable (KB, MB, GB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val decimalFormat = DecimalFormat("#,##0.#")
        return "${decimalFormat.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    /**
     * Format durasi detik ke hh:mm:ss atau mm:ss.
     */
    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
