package com.nunatv.app.downloader

import android.content.Context
import android.util.Log
import com.nunatv.app.util.StorageUtils
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class StreamQualityOption(
    val label: String,
    val height: Int,
    val videoUrl: String,
    val audioUrl: String? = null
)

data class ResolvedYoutubeVideo(
    val videoId: String,
    val title: String,
    val channel: String,
    val channelId: String,
    val thumbnailUrl: String,
    val durationText: String,
    val streamUrl: String,
    val audioStreamUrl: String?,
    val activeQuality: StreamQualityOption,
    val qualityOptions: List<StreamQualityOption>
)

object YoutubeDlManager {

    private const val TAG = "YoutubeDlManager"

    // Gunakan player client android & web fallback yang kompatibel di Android tanpa JS runtime eksternal (Deno/Node)
    private const val YOUTUBE_EXTRACTOR_ARGS =
        "youtube:player_client=android,web"

    @Volatile
    private var isInitialized = false
    private val initMutex = Mutex()

    /**
     * Mengekstrak cookies dari WebView (Android CookieManager) ke file format Netscape
     * agar yt-dlp diotentikasi layaknya browser sungguhan dan menghindari HTTP 403 Forbidden.
     */
    fun getCookiesFile(context: Context): File? {
        return try {
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookieString = cookieManager.getCookie("https://www.youtube.com")
                ?: cookieManager.getCookie("https://m.youtube.com")
            if (cookieString.isNullOrBlank()) return null

            val file = File(context.cacheDir, "youtube_cookies.txt")
            val sb = StringBuilder()
            sb.append("# Netscape HTTP Cookie File\n")
            val pairs = cookieString.split(";").map { it.trim() }
            for (pair in pairs) {
                val idx = pair.indexOf('=')
                if (idx > 0) {
                    val name = pair.substring(0, idx).trim()
                    val value = pair.substring(idx + 1).trim()
                    sb.append(".youtube.com\tTRUE\t/\tTRUE\t2147483647\t$name\t$value\n")
                }
            }
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengekstrak cookies WebView: ${e.message}")
            null
        }
    }

    /**
     * Memastikan YoutubeDL & FFmpeg telah diinisialisasi sebelum digunakan.
     */
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        initMutex.withLock {
            if (!isInitialized) {
                try {
                    YoutubeDL.getInstance().init(context.applicationContext)
                    FFmpeg.getInstance().init(context.applicationContext)
                    isInitialized = true
                    Log.d(TAG, "YoutubeDL & FFmpeg berhasil diinisialisasi")
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal menginisialisasi YoutubeDL/FFmpeg: ${e.message}", e)
                    throw e
                }
            }
        }
    }

    /**
     * Memperbarui binary yt-dlp ke versi stabil terbaru dari repository resmi.
     */
    suspend fun updateYtDlp(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            init(context)
            val status = YoutubeDL.getInstance().updateYoutubeDL(
                context.applicationContext,
                YoutubeDL.UpdateChannel.STABLE
            )
            Log.d(TAG, "Update yt-dlp selesai dengan status: $status")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Gagal update yt-dlp: ${e.message}")
            false
        }
    }

    /**
     * Mengekstrak ID video YouTube (11 karakter) dari berbagai format URL (watch, youtu.be, shorts, live, embed).
     */
    fun extractVideoId(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        val patterns = listOf(
            Regex("""(?:v=|/v/|youtu\.be/|/shorts/|/live/|/embed/)([A-Za-z0-9_-]{11})"""),
            Regex("""^([A-Za-z0-9_-]{11})$""")
        )
        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Membersihkan URL YouTube dari parameter playlist, mix, atau tracking agar unduhan fokus pada video tunggal.
     */
    fun cleanUrl(url: String): String {
        val trimmed = url.trim()
        val id = extractVideoId(trimmed)
        return if (!id.isNullOrBlank()) {
            "https://www.youtube.com/watch?v=$id"
        } else {
            trimmed
        }
    }

    /**
     * Mengambil URL stream video+audio siap putar, daftar resolusi yang tersedia, dan metadata dalam satu panggilan yt-dlp.
     */
    suspend fun resolvePlayableVideo(
        context: Context,
        videoUrl: String,
        preferredHeight: Int = 720
    ): ResolvedYoutubeVideo? = withContext(Dispatchers.IO) {
        try {
            init(context)
            val targetUrl = cleanUrl(videoUrl)
            val fallbackId = extractVideoId(targetUrl) ?: ""
            val request = YoutubeDLRequest(targetUrl).apply {
                addOption("-f", "best[ext=mp4][acodec!=none][vcodec!=none]/best[acodec!=none][vcodec!=none]/best")
                addOption("--extractor-args", YOUTUBE_EXTRACTOR_ARGS)
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("--socket-timeout", "30")
                val cookiesFile = getCookiesFile(context)
                if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
                    addOption("--cookies", cookiesFile.absolutePath)
                }
            }
            val info = YoutubeDL.getInstance().getInfo(request)
            val formats = info.formats.orEmpty()

            // Pilih audio terbaik (M4A/AAC diutamakan agar kompatibel dengan MergingMediaSource + ExoPlayer)
            val bestAudio = formats
                .filter { f ->
                    !f.url.isNullOrBlank() &&
                        !f.acodec.isNullOrBlank() && f.acodec != "none" &&
                        (f.vcodec.isNullOrBlank() || f.vcodec == "none") &&
                        f.ext != "mhtml"
                }
                .sortedWith(
                    compareByDescending<VideoFormat> { it.ext == "m4a" }
                        .thenByDescending { it.abr }
                        .thenByDescending { it.tbr }
                )
                .firstOrNull()
            val bestAudioUrl = bestAudio?.url

            // Kumpulkan opsi resolusi per tinggi piksel (muxed atau adaptive video+audio)
            val candidatesByHeight = formats
                .filter { f ->
                    val stream = f.url?.takeIf { it.isNotBlank() }
                    val hasVideo = !f.vcodec.isNullOrBlank() && f.vcodec != "none"
                    val hasAudio = !f.acodec.isNullOrBlank() && f.acodec != "none"
                    stream != null &&
                        hasVideo &&
                        f.height >= 144 &&
                        f.ext != "mhtml" &&
                        (hasAudio || !bestAudioUrl.isNullOrBlank())
                }
                .groupBy { it.height }

            val qualityOptions = candidatesByHeight.entries
                .sortedByDescending { it.key }
                .mapNotNull { (height, list) ->
                    val bestForHeight = list.sortedWith(
                        compareByDescending<VideoFormat> { !it.acodec.isNullOrBlank() && it.acodec != "none" && it.ext == "mp4" }
                            .thenByDescending { it.vcodec?.startsWith("avc") == true }
                            .thenByDescending { it.ext == "mp4" }
                            .thenByDescending { !(it.vcodec?.startsWith("av01") == true) }
                            .thenByDescending { it.tbr }
                    ).firstOrNull() ?: return@mapNotNull null

                    val isMuxed = !bestForHeight.acodec.isNullOrBlank() && bestForHeight.acodec != "none"
                    val resolvedVideoUrl = bestForHeight.url ?: return@mapNotNull null
                    StreamQualityOption(
                        label = "${height}p",
                        height = height,
                        videoUrl = resolvedVideoUrl,
                        audioUrl = if (isMuxed) null else bestAudioUrl
                    )
                }

            val fallbackUrl = info.url?.takeIf { it.isNotBlank() }
                ?: formats.lastOrNull {
                    !it.url.isNullOrBlank() &&
                        it.acodec != "none" &&
                        it.vcodec != "none"
                }?.url
                ?: formats.lastOrNull { !it.url.isNullOrBlank() && it.ext != "mhtml" }?.url

            val resolvedOptions = if (qualityOptions.isNotEmpty()) {
                qualityOptions
            } else if (!fallbackUrl.isNullOrBlank()) {
                val h = info.height.takeIf { it >= 144 } ?: 360
                listOf(StreamQualityOption(label = "${h}p", height = h, videoUrl = fallbackUrl, audioUrl = null))
            } else {
                emptyList()
            }

            if (resolvedOptions.isEmpty()) return@withContext null

            val activeQuality = resolvedOptions.firstOrNull { it.height <= preferredHeight }
                ?: resolvedOptions.last()

            val resolvedId = info.id?.takeIf { it.isNotBlank() } ?: fallbackId
            val resolvedChannelId = info.uploaderId?.takeIf { it.startsWith("UC") } ?: ""
            val durationSec = info.duration.toLong().coerceAtLeast(0L)
            val formattedDuration = if (durationSec > 0) {
                val h = durationSec / 3600
                val m = (durationSec % 3600) / 60
                val s = durationSec % 60
                if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
            } else {
                ""
            }

            ResolvedYoutubeVideo(
                videoId = resolvedId,
                title = info.title?.takeIf { it.isNotBlank() } ?: "Video YouTube ($resolvedId)",
                channel = info.uploader?.takeIf { it.isNotBlank() } ?: "YouTube",
                channelId = resolvedChannelId,
                thumbnailUrl = info.thumbnail?.takeIf { it.isNotBlank() }
                    ?: if (resolvedId.isNotBlank()) "https://img.youtube.com/vi/$resolvedId/hqdefault.jpg" else "",
                durationText = formattedDuration,
                streamUrl = activeQuality.videoUrl,
                audioStreamUrl = activeQuality.audioUrl,
                activeQuality = activeQuality,
                qualityOptions = resolvedOptions
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal mengambil stream URL: ${e.message}", e)
            null
        }
    }

    /**
     * Mengambil URL stream video+audio siap putar untuk Media3 ExoPlayer.
     */
    suspend fun getPlayableStreamUrl(context: Context, videoUrl: String): String? {
        return resolvePlayableVideo(context, videoUrl)?.streamUrl
    }

    /**
     * Mengambil metadata video (Judul, Author, Thumbnail, Durasi) via yt-dlp tanpa mengunduh.
     */
    suspend fun getVideoInfo(context: Context, videoUrl: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            init(context)
            val targetUrl = cleanUrl(videoUrl)
            val request = YoutubeDLRequest(targetUrl).apply {
                addOption("--extractor-args", YOUTUBE_EXTRACTOR_ARGS)
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("--socket-timeout", "60")
                addOption("--no-mtime")
                val cookiesFile = getCookiesFile(context)
                if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
                    addOption("--cookies", cookiesFile.absolutePath)
                }
            }
            return@withContext YoutubeDL.getInstance().getInfo(request)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil video info: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Overload untuk kompatibilitas jika dipanggil tanpa context.
     */
    suspend fun getVideoInfo(videoUrl: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val targetUrl = cleanUrl(videoUrl)
            val request = YoutubeDLRequest(targetUrl).apply {
                addOption("--extractor-args", YOUTUBE_EXTRACTOR_ARGS)
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("--socket-timeout", "60")
                addOption("--no-mtime")
            }
            return@withContext YoutubeDL.getInstance().getInfo(request)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil video info: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Mengeksekusi unduhan video via youtubedl-android dengan progress callback.
     */
    suspend fun downloadVideo(
        context: Context,
        videoUrl: String,
        formatSpec: String,
        destDirectory: File = StorageUtils.getDefaultDownloadDir(context),
        onProgress: (progress: Float, etaInSeconds: Long, line: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            init(context)

            if (!destDirectory.exists()) {
                destDirectory.mkdirs()
            }

            val targetUrl = cleanUrl(videoUrl)
            // Template nama file unik berdasarkan judul dan id
            val outputTemplate = "${destDirectory.absolutePath}/%(title).80s-[%(id)s].%(ext)s"

            val request = YoutubeDLRequest(targetUrl).apply {
                addOption("-f", formatSpec)
                addOption("-o", outputTemplate)
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("--no-check-certificates")
                addOption("--extractor-args", YOUTUBE_EXTRACTOR_ARGS)
                val cookiesFile = getCookiesFile(context)
                if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
                    addOption("--cookies", cookiesFile.absolutePath)
                }
                if (!formatSpec.contains("bestaudio") || formatSpec.contains("bestvideo")) {
                    addOption("--merge-output-format", "mp4")
                }
                addOption("--socket-timeout", "60")
                addOption("--retries", "5")
            }

            var lastRawProgress = 0f
            var streamStage = 1 // 1: Video (0-50%), 2: Audio (50-90%), 3: Merge/Build (90-100%)
            var hasSeenVideoDownload = false

            val isSplitFormat = formatSpec.contains("+") || (!formatSpec.contains("bestaudio") && !formatSpec.contains("AUDIO_ONLY"))

            val response = YoutubeDL.getInstance().execute(request) { rawProgress, etaInSeconds, lineOrNull ->
                val line = lineOrNull ?: ""

                // Deteksi fase berdasarkan output log yt-dlp
                if (line.contains("[Merger]") || line.contains("Merging formats") || line.contains("Deleting original file")) {
                    streamStage = 3
                } else if (line.contains("[download] Destination:") || line.contains("Destination:")) {
                    if (hasSeenVideoDownload) {
                        streamStage = 2
                    }
                }

                // Deteksi perpindahan ke stream audio kedua (misal rawProgress reset dari >75% ke <25%)
                if (isSplitFormat && streamStage == 1 && lastRawProgress > 75f && rawProgress < 25f) {
                    streamStage = 2
                }

                if (rawProgress > 10f) {
                    hasSeenVideoDownload = true
                }
                lastRawProgress = rawProgress

                val compositeProgress: Float = if (!isSplitFormat) {
                    rawProgress.coerceIn(0f, 100f)
                } else {
                    when (streamStage) {
                        1 -> {
                            // Video berbobot 50% (0% - 50%)
                            (rawProgress * 0.50f).coerceIn(0f, 50f)
                        }
                        2 -> {
                            // Audio berbobot 40% (50% - 90%)
                            (50f + (rawProgress * 0.40f)).coerceIn(50f, 90f)
                        }
                        else -> {
                            // Proses penggabungan / build berbobot 10% (90% - 100%)
                            95f
                        }
                    }
                }

                onProgress(compositeProgress, etaInSeconds, line)
            }

            Log.d(TAG, "Download selesai: ${response.out}")

            // Cari file yang baru dibuat di direktori tujuan yang cocok
            val downloadedFiles = destDirectory.listFiles { file ->
                file.isFile && (file.extension == "mp4" || file.extension == "mkv" || file.extension == "m4a" || file.extension == "webm")
            }?.sortedByDescending { it.lastModified() }

            val targetFile = downloadedFiles?.firstOrNull()
            if (targetFile != null && targetFile.exists()) {
                Result.success(targetFile)
            } else {
                Result.failure(Exception("File hasil unduhan tidak ditemukan di ${destDirectory.absolutePath}"))
            }

        } catch (e: YoutubeDLException) {
            Log.e(TAG, "YoutubeDL Error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "General Download Error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
