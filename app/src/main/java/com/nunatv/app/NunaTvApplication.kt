package com.nunatv.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.nunatv.app.data.local.NunaDatabase
import com.nunatv.app.downloader.DownloadService
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NunaTvApplication : Application() {

    lateinit var database: NunaDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Prioritaskan IPv4 untuk mempercepat koneksi network dan mencegah timeout IPv6 di Android
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")

        // 1. Inisialisasi Room Database
        database = NunaDatabase.getInstance(this)

        // 2. Inisialisasi Notification Channel untuk Foreground Service
        createNotificationChannel()

        // 3. Inisialisasi youtubedl-android & FFmpeg secara asynchronous di background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Bersihkan sisa record gagal sebelumnya secara otomatis
                database.videoDao().deleteAllFailed()

                com.nunatv.app.downloader.YoutubeDlManager.init(applicationContext)
                Log.d(TAG, "YoutubeDL & FFmpeg berhasil diinisialisasi")
                // Update binary yt-dlp ke versi stable terbaru agar extractor YouTube selalu up-to-date
                com.nunatv.app.downloader.YoutubeDlManager.updateYtDlp(applicationContext)
            } catch (e: Throwable) {
                Log.e(TAG, "Gagal menginisialisasi YoutubeDL/FFmpeg: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DownloadService.CHANNEL_ID,
                "Nuna TV Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progress unduhan video Nuna TV"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "NunaTvApplication"
        lateinit var instance: NunaTvApplication
            private set
    }
}
