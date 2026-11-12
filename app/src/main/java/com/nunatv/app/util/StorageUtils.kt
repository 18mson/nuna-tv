package com.nunatv.app.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
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
