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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

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
    val qualityOptions: List<StreamQualityOption>,
    val isLive: Boolean = false,
    val userAgent: String? = null
)

object YoutubeDlManager {

    private const val TAG = "YoutubeDlManager"

    // Gunakan player client android lebih dulu agar lebih cepat, lalu fallback web bila perlu
    private const val YOUTUBE_FAST_STREAM_ARGS =
        "youtube:player_client=android"
    private const val YOUTUBE_EXTRACTOR_ARGS =
        "youtube:player_client=android,web"
    private const val STREAM_CACHE_TTL_MS = 10 * 60 * 1000L

    private val streamCache = ConcurrentHashMap<String, Pair<Long, ResolvedYoutubeVideo>>()

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
     * Mengambil URL stream video+audio siap putar:
     * 1) Cek cache memori 10 menit
     * 2) Coba API Innertube /player langsung (~200-500ms, tanpa Python)
     * 3) Fallback ke yt-dlp bila diperlukan
     */
    suspend fun resolvePlayableVideo(
        context: Context,
        videoUrl: String,
        preferredHeight: Int = 720
    ): ResolvedYoutubeVideo? = withContext(Dispatchers.IO) {
        try {
            val targetUrl = cleanUrl(videoUrl)
            val fallbackId = extractVideoId(targetUrl) ?: ""
            val cacheKey = if (fallbackId.isNotBlank()) "$fallbackId:$preferredHeight" else "$targetUrl:$preferredHeight"
            val now = System.currentTimeMillis()
            streamCache[cacheKey]?.let { (cachedAt, cachedResult) ->
                if (now - cachedAt < STREAM_CACHE_TTL_MS) {
                    return@withContext cachedResult
                }
                streamCache.remove(cacheKey)
            }

            // Jalur cepat: Innertube /youtubei/v1/player tanpa overhead Python/yt-dlp
            if (fallbackId.isNotBlank()) {
                val fastResolved = resolveViaInnertubePlayerFast(fallbackId, preferredHeight)
                if (fastResolved != null) {
                    streamCache[cacheKey] = System.currentTimeMillis() to fastResolved
                    return@withContext fastResolved
                }
            }

            init(context)
            val cookiesFile = getCookiesFile(context)

            fun fetchInfoWithArgs(extractorArgs: String): VideoInfo? {
                val request = YoutubeDLRequest(targetUrl).apply {
                    addOption("-f", "best[ext=mp4][acodec!=none][vcodec!=none]/best[acodec!=none][vcodec!=none]/best")
                    addOption("--extractor-args", extractorArgs)
                    addOption("--no-playlist")
                    addOption("--no-warnings")
                    addOption("--no-check-certificates")
                    addOption("--socket-timeout", "20")
                    if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
                        addOption("--cookies", cookiesFile.absolutePath)
                    }
                }
                return YoutubeDL.getInstance().getInfo(request)
            }

            val info = try {
                fetchInfoWithArgs(YOUTUBE_FAST_STREAM_ARGS)
            } catch (_: Throwable) {
                null
            } ?: fetchInfoWithArgs(YOUTUBE_EXTRACTOR_ARGS)
                ?: return@withContext null

            val formats = info.formats.orEmpty()
            val isHlsUrl: (String?) -> Boolean = { url ->
                !url.isNullOrBlank() && (
                    url.contains(".m3u8", ignoreCase = true) ||
                        url.contains("manifest/hls", ignoreCase = true)
                    )
            }
            val isDashUrl: (String?) -> Boolean = { url ->
                !url.isNullOrBlank() && (
                    url.contains(".mpd", ignoreCase = true) ||
                        url.contains("manifest/dash", ignoreCase = true)
                    )
            }

            // Pilih audio terbaik (MP4/M4A non-manifest untuk merge adaptif)
            val bestProgressiveAudio = formats
                .filter { f ->
                    !f.url.isNullOrBlank() &&
                        !isHlsUrl(f.url) &&
                        !isDashUrl(f.url) &&
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
            val bestProgressiveAudioUrl = bestProgressiveAudio?.url

            val bestHlsAudioUrl = formats
                .filter { f ->
                    isHlsUrl(f.url) &&
                        !f.acodec.isNullOrBlank() && f.acodec != "none" &&
                        (f.vcodec.isNullOrBlank() || f.vcodec == "none")
                }
                .maxByOrNull { it.tbr }
                ?.url

            // Kumpulkan opsi resolusi per tinggi piksel (muxed mp4, muxed HLS live, atau adaptive video+audio)
            val candidatesByHeight = formats
                .filter { f ->
                    val stream = f.url?.takeIf { it.isNotBlank() }
                    val hasVideo = !f.vcodec.isNullOrBlank() && f.vcodec != "none"
                    val hasAudio = !f.acodec.isNullOrBlank() && f.acodec != "none"
                    val hlsStream = isHlsUrl(stream)
                    stream != null &&
                        hasVideo &&
                        f.height >= 144 &&
                        f.ext != "mhtml" &&
                        (hasAudio || (hlsStream && !bestHlsAudioUrl.isNullOrBlank()) || (!hlsStream && !bestProgressiveAudioUrl.isNullOrBlank()) || hlsStream)
                }
                .groupBy { it.height }

            val qualityOptions = candidatesByHeight.entries
                .sortedByDescending { it.key }
                .mapNotNull { (height, list) ->
                    val bestForHeight = list.sortedWith(
                        compareByDescending<VideoFormat> { !it.acodec.isNullOrBlank() && it.acodec != "none" && !isHlsUrl(it.url) && it.ext == "mp4" }
                            .thenByDescending { !it.acodec.isNullOrBlank() && it.acodec != "none" && isHlsUrl(it.url) }
                            .thenByDescending { it.vcodec?.startsWith("avc") == true }
                            .thenByDescending { it.ext == "mp4" }
                            .thenByDescending { !(it.vcodec?.startsWith("av01") == true) }
                            .thenByDescending { it.tbr }
                    ).firstOrNull() ?: return@mapNotNull null

                    val isMuxed = !bestForHeight.acodec.isNullOrBlank() && bestForHeight.acodec != "none"
                    val resolvedVideoUrl = bestForHeight.url ?: return@mapNotNull null
                    val resolvedAudioUrl = when {
                        isMuxed -> null
                        isHlsUrl(resolvedVideoUrl) -> bestHlsAudioUrl
                        else -> bestProgressiveAudioUrl
                    }
                    StreamQualityOption(
                        label = "${height}p",
                        height = height,
                        videoUrl = resolvedVideoUrl,
                        audioUrl = resolvedAudioUrl
                    )
                }

            val fallbackUrl = info.url?.takeIf { it.isNotBlank() }
                ?: info.manifestUrl?.takeIf { it.isNotBlank() }
                ?: formats.lastOrNull {
                    !it.url.isNullOrBlank() &&
                        it.acodec != "none" &&
                        it.vcodec != "none"
                }?.url
                ?: formats.lastOrNull { !it.url.isNullOrBlank() && it.ext != "mhtml" }?.url

            val resolvedOptions = if (qualityOptions.isNotEmpty()) {
                qualityOptions
            } else if (!fallbackUrl.isNullOrBlank()) {
                val h = info.height.takeIf { it >= 144 } ?: 720
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
            val isLive = durationSec == 0L && (
                isHlsUrl(activeQuality.videoUrl) ||
                    isDashUrl(activeQuality.videoUrl) ||
                    !info.manifestUrl.isNullOrBlank()
                )
            val formattedDuration = if (isLive) {
                "LIVE"
            } else if (durationSec > 0) {
                val h = durationSec / 3600
                val m = (durationSec % 3600) / 60
                val s = durationSec % 60
                if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
            } else {
                ""
            }

            val resolved = ResolvedYoutubeVideo(
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
                qualityOptions = resolvedOptions,
                isLive = isLive
            )
            streamCache[cacheKey] = System.currentTimeMillis() to resolved
            resolved
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal mengambil stream URL: ${e.message}", e)
            null
        }
    }

    /**
     * Memuat URL stream ke cache lebih awal di background agar perpindahan video berikutnya instan.
     */
    suspend fun prefetchPlayableVideo(
        context: Context,
        videoUrl: String,
        preferredHeight: Int = 720
    ) {
        try {
            resolvePlayableVideo(context, videoUrl, preferredHeight)
        } catch (_: Throwable) {
        }
    }

    private fun resolveViaInnertubePlayerFast(
        videoId: String,
        preferredHeight: Int
    ): ResolvedYoutubeVideo? {
        val clients = listOf(
            Triple(
                "ANDROID_VR",
                "1.60.19",
                "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
            ),
            Triple(
                "IOS",
                "19.45.4",
                "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)"
            )
        )

        for ((clientName, clientVersion, userAgent) in clients) {
            try {
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", clientName)
                            put("clientVersion", clientVersion)
                            put("hl", "id")
                            put("gl", "ID")
                            if (clientName == "ANDROID_VR") {
                                put("androidSdkVersion", 32)
                            } else if (clientName == "IOS") {
                                put("deviceMake", "Apple")
                                put("deviceModel", "iPhone16,2")
                                put("osName", "iPhone")
                                put("osVersion", "18.1.0.22B83")
                            }
                        })
                    })
                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                }

                val conn = (URL("https://www.youtube.com/youtubei/v1/player?prettyPrint=false").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 4500
                    readTimeout = 4500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", userAgent)
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (conn.responseCode !in 200..299) continue
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)

                val playability = root.optJSONObject("playabilityStatus")?.optString("status", "")
                if (playability != "OK") continue

                val videoDetails = root.optJSONObject("videoDetails")
                val streamingData = root.optJSONObject("streamingData") ?: continue
                val isLive = videoDetails?.optBoolean("isLiveContent", false) == true &&
                    (videoDetails.optString("lengthSeconds", "0").toLongOrNull() ?: 0L) == 0L

                val hlsManifestUrl = streamingData.optString("hlsManifestUrl", "").takeIf { it.isNotBlank() }
                if (isLive && hlsManifestUrl != null) {
                    val liveOpt = StreamQualityOption(label = "Auto", height = 720, videoUrl = hlsManifestUrl, audioUrl = null)
                    return ResolvedYoutubeVideo(
                        videoId = videoId,
                        title = videoDetails?.optString("title", "").orEmpty().ifBlank { "Live YouTube ($videoId)" },
                        channel = videoDetails?.optString("author", "").orEmpty().ifBlank { "YouTube" },
                        channelId = videoDetails?.optString("channelId", "").orEmpty(),
                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        durationText = "LIVE",
                        streamUrl = hlsManifestUrl,
                        audioStreamUrl = null,
                        activeQuality = liveOpt,
                        qualityOptions = listOf(liveOpt),
                        isLive = true,
                        userAgent = userAgent
                    )
                }

                val muxed = streamingData.optJSONArray("formats") ?: JSONArray()
                val adaptive = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()

                // Cari audio terbaik (MP4/AAC diutamakan)
                var bestAudioUrl: String? = null
                var bestAudioBitrate = -1L
                for (i in 0 until adaptive.length()) {
                    val f = adaptive.optJSONObject(i) ?: continue
                    val url = f.optString("url", "")
                    val mime = f.optString("mimeType", "")
                    if (url.isBlank() || !mime.startsWith("audio/")) continue
                    val isM4a = mime.contains("mp4a", ignoreCase = true)
                    val bitrate = f.optLong("bitrate", 0L) + if (isM4a) 1_000_000L else 0L
                    if (bitrate > bestAudioBitrate) {
                        bestAudioBitrate = bitrate
                        bestAudioUrl = url
                    }
                }

                val optionsByHeight = mutableMapOf<Int, Pair<Int, StreamQualityOption>>()

                fun considerFormat(f: JSONObject, isMuxed: Boolean) {
                    val url = f.optString("url", "")
                    val mime = f.optString("mimeType", "")
                    if (url.isBlank() || !mime.startsWith("video/")) return
                    val height = f.optInt("height", 0)
                    if (height < 144) return
                    if (!isMuxed && bestAudioUrl.isNullOrBlank()) return

                    val isMp4 = mime.contains("video/mp4", ignoreCase = true)
                    val isAvc = mime.contains("avc1", ignoreCase = true)
                    val isAv1 = mime.contains("av01", ignoreCase = true)
                    val score = (if (isMuxed && isMp4) 10000 else 0) +
                        (if (isAvc) 1000 else 0) +
                        (if (isMp4) 500 else 0) +
                        (if (!isAv1) 200 else 0)

                    val existing = optionsByHeight[height]
                    if (existing == null || score > existing.first) {
                        optionsByHeight[height] = score to StreamQualityOption(
                            label = "${height}p",
                            height = height,
                            videoUrl = url,
                            audioUrl = if (isMuxed) null else bestAudioUrl
                        )
                    }
                }

                for (i in 0 until muxed.length()) {
                    muxed.optJSONObject(i)?.let { considerFormat(it, isMuxed = true) }
                }
                for (i in 0 until adaptive.length()) {
                    adaptive.optJSONObject(i)?.let { considerFormat(it, isMuxed = false) }
                }

                val qualityOptions = optionsByHeight.entries
                    .sortedByDescending { it.key }
                    .map { it.value.second }

                val finalOptions = if (qualityOptions.isNotEmpty()) {
                    qualityOptions
                } else if (hlsManifestUrl != null) {
                    listOf(StreamQualityOption(label = "Auto", height = 720, videoUrl = hlsManifestUrl, audioUrl = null))
                } else {
                    emptyList()
                }

                if (finalOptions.isEmpty()) continue

                val activeQuality = finalOptions.firstOrNull { it.height <= preferredHeight }
                    ?: finalOptions.last()

                val durationSec = videoDetails?.optString("lengthSeconds", "0")?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val formattedDuration = if (isLive) {
                    "LIVE"
                } else if (durationSec > 0L) {
                    val h = durationSec / 3600
                    val m = (durationSec % 3600) / 60
                    val s = durationSec % 60
                    if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
                } else {
                    ""
                }

                return ResolvedYoutubeVideo(
                    videoId = videoId,
                    title = videoDetails?.optString("title", "").orEmpty().ifBlank { "Video YouTube ($videoId)" },
                    channel = videoDetails?.optString("author", "").orEmpty().ifBlank { "YouTube" },
                    channelId = videoDetails?.optString("channelId", "").orEmpty(),
                    thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                    durationText = formattedDuration,
                    streamUrl = activeQuality.videoUrl,
                    audioStreamUrl = activeQuality.audioUrl,
                    activeQuality = activeQuality,
                    qualityOptions = finalOptions,
                    isLive = isLive,
                    userAgent = userAgent
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Fast Innertube /player ($clientName) gagal untuk $videoId: ${e.message}")
            }
        }
        return null
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
