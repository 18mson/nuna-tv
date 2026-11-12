package com.nunatv.app.downloader

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nunatv.app.data.local.DownloadStatus
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.local.NunaDatabase
import com.nunatv.app.data.preferences.VideoResolution
import com.nunatv.app.util.StorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoUrl = intent?.getStringExtra(EXTRA_URL)
        val resolutionName = intent?.getStringExtra(EXTRA_RESOLUTION) ?: VideoResolution.RES_1080P.name
        val destDirPath = intent?.getStringExtra(EXTRA_DEST_DIR)

        if (videoUrl.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resolution = VideoResolution.fromLabel(resolutionName)
        val destDir = if (destDirPath != null) File(destDirPath) else StorageUtils.getDefaultDownloadDir(this)

        // Mulai Foreground Service dengan initial notification
        startForegroundWithNotification("Menyiapkan unduhan...", 0)

        // Jalankan proses download
        serviceScope.launch {
            processDownload(videoUrl, resolution, destDir)
        }

        return START_NOT_STICKY
    }

    private suspend fun processDownload(
        videoUrl: String,
        resolution: VideoResolution,
        destDir: File
    ) {
        val targetUrl = YoutubeDlManager.cleanUrl(videoUrl)
        val extractedId = com.nunatv.app.ui.screens.browser.YoutubeWebViewClient.extractVideoId(targetUrl)
        val database = NunaDatabase.getInstance(applicationContext)
        val dao = database.videoDao()

        // Cegah duplikasi unduhan jika video sudah tersimpan atau sedang diunduh
        if (!extractedId.isNullOrBlank()) {
            val existing = dao.getVideoByVideoId(extractedId)
            val alreadyExists = existing != null && (
                existing.status == DownloadStatus.DOWNLOADING ||
                    existing.status == DownloadStatus.QUEUED ||
                    (existing.status == DownloadStatus.COMPLETED && existing.localFilePath.isNotEmpty() && File(existing.localFilePath).exists())
                )
            if (alreadyExists) {
                Log.d(TAG, "Video $extractedId sudah ada atau sedang diunduh, melewati duplikat.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
        }

        // 0. Cek kapasitas penyimpanan sebelum memulai download
        val initialStorage = StorageUtils.getStorageInfo(destDir)
        if (initialStorage.availableBytes > 0L &&
            initialStorage.availableBytes < StorageUtils.MIN_REQUIRED_FOR_DOWNLOAD_BYTES
        ) {
            val availStr = StorageUtils.formatFileSize(initialStorage.availableBytes)
            val minStr = StorageUtils.formatFileSize(StorageUtils.MIN_REQUIRED_FOR_DOWNLOAD_BYTES)
            val msg = "Penyimpanan tidak cukup untuk mengunduh video. Sisa ruang tersedia: $availStr (minimal butuh $minStr). Hapus beberapa video di Home terlebih dahulu."
            showFailedNotification("Penyimpanan Tidak Cukup", "Tersisa $availStr")
            DownloadEventBus.emitError(
                DownloadErrorEvent(
                    videoTitle = "Video YouTube",
                    message = msg,
                    isStorageFull = true
                )
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // 1. Ekstrak Video Info
        updateNotification("Mengambil info video...", 0, indeterminate = true)
        val info = YoutubeDlManager.getVideoInfo(applicationContext, targetUrl)
        val videoId = info?.id ?: extractedId ?: targetUrl.substringAfter("v=").substringBefore("&")
        val title = info?.title ?: "YouTube Video ($videoId)"
        val thumbnailUrl = info?.thumbnail ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        val duration = info?.duration?.toLong() ?: 0L

        // 2. Simpan ke Room Database
        val entity = DownloadedVideoEntity(
            videoId = videoId,
            originalUrl = targetUrl,
            title = title,
            author = info?.uploader ?: "",
            thumbnailUrl = thumbnailUrl,
            resolution = resolution.label,
            durationSeconds = duration,
            status = DownloadStatus.DOWNLOADING,
            progress = 0
        )
        val recordId = dao.insertVideo(entity)

        // 3. Mulai Download
        val result = YoutubeDlManager.downloadVideo(
            context = applicationContext,
            videoUrl = targetUrl,
            formatSpec = resolution.formatSpec,
            destDirectory = destDir
        ) { progress, etaInSeconds, _ ->
            val progressInt = progress.toInt().coerceIn(0, 100)
            val stageDesc = when {
                progressInt <= 50 -> "Video ($progressInt%)"
                progressInt <= 90 -> "Audio ($progressInt%)"
                else -> "Menggabungkan ($progressInt%)..."
            }
            updateNotification("$title • $stageDesc", progressInt, indeterminate = progressInt >= 95)
            serviceScope.launch {
                dao.updateProgress(recordId, progressInt, DownloadStatus.DOWNLOADING)
            }
        }

        // 4. Update Database Sesuai Hasil
        result.fold(
            onSuccess = { downloadedFile ->
                dao.markCompleted(
                    id = recordId,
                    filePath = downloadedFile.absolutePath,
                    fileSize = downloadedFile.length()
                )
                showCompletedNotification(title)
            },
            onFailure = { error ->
                Log.e(TAG, "Download gagal: ${error.message}", error)
                // Hapus otomatis record dari Room Database dan bersihkan file parsial/temp jika gagal
                cleanupPartialFiles(destDir, videoId)
                dao.deleteVideoById(recordId)

                val errMsg = error.message ?: ""
                val availAfter = StorageUtils.getStorageInfo(destDir).availableBytes
                val isStorageFull = errMsg.contains("No space", ignoreCase = true) ||
                    errMsg.contains("ENOSPC", ignoreCase = true) ||
                    errMsg.contains("not enough space", ignoreCase = true) ||
                    (availAfter in 1 until StorageUtils.MIN_REQUIRED_FOR_DOWNLOAD_BYTES)

                val displayMessage = if (isStorageFull) {
                    "Download \"$title\" gagal karena ruang penyimpanan tidak cukup (sisa ruang: ${StorageUtils.formatFileSize(availAfter)}). Silakan hapus beberapa video di Home."
                } else {
                    "Download \"$title\" gagal: ${errMsg.ifBlank { "Kesalahan jaringan atau video tidak tersedia" }}"
                }
                showFailedNotification(
                    if (isStorageFull) "Penyimpanan Tidak Cukup" else "Download Gagal",
                    displayMessage
                )
                DownloadEventBus.emitError(
                    DownloadErrorEvent(
                        videoTitle = title,
                        message = displayMessage,
                        isStorageFull = isStorageFull
                    )
                )
            }
        )

        // Selesai
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun startForegroundWithNotification(title: String, progress: Int) {
        val notification = buildNotification(title, progress, indeterminate = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(content: String, progress: Int, indeterminate: Boolean) {
        val notification = buildNotification(content, progress, indeterminate)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String, progress: Int, indeterminate: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nuna TV Downloader")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun showCompletedNotification(title: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Selesai")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun cleanupPartialFiles(destDir: File, videoId: String) {
        try {
            if (!destDir.exists()) return
            destDir.listFiles()?.forEach { file ->
                val name = file.name
                val matchesVideo = videoId.isNotEmpty() && name.contains("[$videoId]")
                val isTempFile = name.endsWith(".part") || name.endsWith(".ytdl") || name.endsWith(".temp")
                if (file.isFile && matchesVideo && (isTempFile || file.length() == 0L)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membersihkan file parsial: ${e.message}")
        }
    }

    private fun showFailedNotification(title: String, reason: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Gagal (Dihapus Otomatis)")
            .setContentText("$title: $reason")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "DownloadService"
        const val CHANNEL_ID = "nuna_tv_downloads_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_URL = "extra_video_url"
        const val EXTRA_RESOLUTION = "extra_resolution"
        const val EXTRA_DEST_DIR = "extra_dest_dir"

        fun start(context: Context, videoUrl: String, resolutionName: String, destDir: String? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, videoUrl)
                putExtra(EXTRA_RESOLUTION, resolutionName)
                putExtra(EXTRA_DEST_DIR, destDir)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
