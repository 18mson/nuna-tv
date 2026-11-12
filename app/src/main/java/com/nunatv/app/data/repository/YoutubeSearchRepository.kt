package com.nunatv.app.data.repository

import android.content.Context
import android.util.Log
import com.nunatv.app.downloader.YoutubeDlManager
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class YoutubeSearchItem(
    val videoId: String,
    val title: String,
    val channel: String,
    val durationText: String,
    val viewCountText: String,
    val thumbnailUrl: String,
    val channelId: String = ""
) {
    val watchUrl: String
        get() = if (isShort) {
            "https://www.youtube.com/shorts/$videoId"
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }

    val isShort: Boolean
        get() = durationText.equals("SHORTS", ignoreCase = true) ||
            title.contains("#shorts", ignoreCase = true)
}

enum class YoutubeContentFilter(val label: String) {
    ALL("Semua"),
    LONG("Video (>= 4 mnt)"),
    SHORTS("Shorts / Pendek"),
    LIVE("Live")
}

enum class YoutubeSortOption(val label: String) {
    DEFAULT("Urutan: Relevan"),
    DURATION_DESC("Durasi Terpanjang"),
    DURATION_ASC("Durasi Terpendek")
}

object YoutubeSearchRepository {

    private const val TAG = "YoutubeSearchRepo"
    private const val INNERTUBE_SEARCH_URL = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val INNERTUBE_NEXT_URL = "https://www.youtube.com/youtubei/v1/next?prettyPrint=false"
    private const val INNERTUBE_BROWSE_URL = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val SUGGEST_URL = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=id&gl=ID&q="

    fun parseDurationSeconds(durationText: String): Long? {
        val clean = durationText.trim()
        if (clean.isBlank() || clean.equals("LIVE", ignoreCase = true) || clean.equals("SHORTS", ignoreCase = true)) {
            return null
        }
        val parts = clean.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    fun filterAndSortItems(
        items: List<YoutubeSearchItem>,
        filter: YoutubeContentFilter,
        sort: YoutubeSortOption
    ): List<YoutubeSearchItem> {
        val filtered = when (filter) {
            YoutubeContentFilter.ALL -> items
            YoutubeContentFilter.LIVE -> items.filter {
                it.durationText.equals("LIVE", ignoreCase = true)
            }
            YoutubeContentFilter.SHORTS -> items.filter {
                val sec = parseDurationSeconds(it.durationText)
                it.durationText.equals("SHORTS", ignoreCase = true) ||
                    it.title.contains("#shorts", ignoreCase = true) ||
                    (sec != null && sec in 1L..75L)
            }
            YoutubeContentFilter.LONG -> items.filter {
                val sec = parseDurationSeconds(it.durationText)
                sec != null && sec >= 240L
            }
        }
        return when (sort) {
            YoutubeSortOption.DEFAULT -> filtered
            YoutubeSortOption.DURATION_DESC -> filtered.sortedByDescending { parseDurationSeconds(it.durationText) ?: -1L }
            YoutubeSortOption.DURATION_ASC -> filtered.sortedBy { parseDurationSeconds(it.durationText) ?: Long.MAX_VALUE }
        }
    }

    /**
     * Memuat feed rekomendasi cerdas berdasarkan video yang terakhir ditonton dan riwayat pencarian.
     */
    suspend fun getRecommendedFeed(
        context: Context,
        lastVideoId: String?,
        lastVideoTitle: String?,
        recentSearches: List<String>
    ): Result<List<YoutubeSearchItem>> = withContext(Dispatchers.IO) {
        val combined = mutableListOf<YoutubeSearchItem>()
        val seenIds = mutableSetOf<String>()
        if (!lastVideoId.isNullOrBlank()) {
            seenIds.add(lastVideoId) // Jangan tampilkan ulang video yang barusan ditonton di urutan teratas
        }

        // 1. Ambil video terkait langsung dari video terakhir yang ditonton (/youtubei/v1/next)
        if (!lastVideoId.isNullOrBlank()) {
            try {
                val related = getRelatedVideos(lastVideoId)
                for (item in related) {
                    if (seenIds.add(item.videoId)) {
                        combined.add(item)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal ambil related videos: ${e.message}")
            }
        }

        // 2. Jika masih perlu tambahan atau belum pernah nonton, gabungkan hasil dari pencarian terakhir
        val fallbackQuery = recentSearches.firstOrNull()
            ?: lastVideoTitle?.takeIf { it.isNotBlank() }
            ?: if (combined.isEmpty()) "Trending Indonesia" else null

        if (fallbackQuery != null && combined.size < 16) {
            try {
                val searchRes = searchViaInnertube(fallbackQuery)
                for (item in searchRes) {
                    if (seenIds.add(item.videoId)) {
                        combined.add(item)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal ambil rekomendasi search: ${e.message}")
            }
        }

        if (combined.isNotEmpty()) {
            Result.success(combined.take(24))
        } else {
            search(context, fallbackQuery ?: "Trending Indonesia")
        }
    }

    /**
     * Mengambil daftar video terkait (Up Next) secara langsung untuk fitur Autoplay YouTube.
     */
    suspend fun fetchRelatedVideos(videoId: String): List<YoutubeSearchItem> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()
        try {
            getRelatedVideos(videoId).filter { it.videoId != videoId }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal ambil related videos untuk $videoId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Mengambil channelId dan nama channel pemilik video dari Innertube /next (berguna untuk video offline).
     */
    suspend fun fetchVideoChannelInfo(videoId: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        try {
            val root = postInnertubeJson(INNERTUBE_NEXT_URL, JSONObject().apply {
                put("context", buildWebContext())
                put("videoId", videoId)
            })
            findVideoOwnerChannel(root)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal ambil info channel untuk $videoId: ${e.message}")
            null
        }
    }

    /**
     * Memuat daftar video dari halaman utama suatu Channel YouTube.
     */
    suspend fun getChannelVideos(
        context: Context,
        channelId: String?,
        channelNameFallback: String?
    ): Result<List<YoutubeSearchItem>> = withContext(Dispatchers.IO) {
        val cleanChannelId = channelId?.trim().orEmpty()
        if (cleanChannelId.startsWith("UC")) {
            try {
                // Coba ambil tab Videos channel via /youtubei/v1/browse
                val browseWithTab = postInnertubeJson(INNERTUBE_BROWSE_URL, JSONObject().apply {
                    put("context", buildWebContext())
                    put("browseId", cleanChannelId)
                    put("params", "EgZ2aWRlb3PyBgQKAjoA")
                })
                val tabVideos = mutableListOf<YoutubeSearchItem>()
                val seen = mutableSetOf<String>()
                extractVideoRenderers(browseWithTab, tabVideos, seen)
                if (tabVideos.isNotEmpty()) {
                    val enriched = if (!channelNameFallback.isNullOrBlank()) {
                        tabVideos.map { item ->
                            if (item.channel == "YouTube" || item.channel.isBlank()) {
                                item.copy(channel = channelNameFallback, channelId = cleanChannelId)
                            } else {
                                item.copy(channelId = item.channelId.ifBlank { cleanChannelId })
                            }
                        }
                    } else {
                        tabVideos.map { it.copy(channelId = it.channelId.ifBlank { cleanChannelId }) }
                    }
                    return@withContext Result.success(enriched)
                }

                // Fallback ke halaman utama (Home) channel tanpa params
                val browseHome = postInnertubeJson(INNERTUBE_BROWSE_URL, JSONObject().apply {
                    put("context", buildWebContext())
                    put("browseId", cleanChannelId)
                })
                val homeVideos = mutableListOf<YoutubeSearchItem>()
                extractVideoRenderers(browseHome, homeVideos, mutableSetOf())
                if (homeVideos.isNotEmpty()) {
                    return@withContext Result.success(
                        homeVideos.map { it.copy(channelId = it.channelId.ifBlank { cleanChannelId }) }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal browse channel $cleanChannelId: ${e.message}")
            }
        }

        val fallbackQuery = channelNameFallback?.trim().orEmpty()
        if (fallbackQuery.isNotBlank()) {
            return@withContext search(context, fallbackQuery)
        }
        Result.failure(IllegalStateException("Informasi channel tidak tersedia."))
    }

    private fun buildWebContext(): JSONObject = JSONObject().apply {
        put("client", JSONObject().apply {
            put("clientName", "WEB")
            put("clientVersion", "2.20241101.00.00")
            put("hl", "id")
            put("gl", "ID")
        })
    }

    private fun postInnertubeJson(url: String, payload: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        }
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("HTTP ${connection.responseCode} dari YouTube")
        }
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(responseText)
    }

    private fun findVideoOwnerChannel(node: Any?): Pair<String, String>? {
        when (node) {
            is JSONObject -> {
                val ownerRenderer = node.optJSONObject("videoOwnerRenderer")
                if (ownerRenderer != null) {
                    val titleObj = ownerRenderer.optJSONObject("title")
                    val name = readTextRuns(titleObj)
                    val browseId = extractBrowseIdFromRuns(titleObj)
                        .ifBlank {
                            ownerRenderer.optJSONObject("navigationEndpoint")
                                ?.optJSONObject("browseEndpoint")
                                ?.optString("browseId", "") ?: ""
                        }
                    if (browseId.isNotBlank() || name.isNotBlank()) {
                        return browseId to name
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val found = findVideoOwnerChannel(node.opt(keys.next()))
                    if (found != null) return found
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    val found = findVideoOwnerChannel(node.opt(i))
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun getRelatedVideos(videoId: String): List<YoutubeSearchItem> {
        val connection = (URL(INNERTUBE_NEXT_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        }

        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20241101.00.00")
                    put("hl", "id")
                    put("gl", "ID")
                })
            })
            put("videoId", videoId)
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("HTTP ${connection.responseCode} dari YouTube next")
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)
        val collected = mutableListOf<YoutubeSearchItem>()
        val seenIds = mutableSetOf<String>()
        extractVideoRenderers(root, collected, seenIds)
        return collected
    }

    /**
     * Mengambil hint rekomendasi autocomplete YouTube saat user mengetik kata kunci.
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val connection = (URL("$SUGGEST_URL$encoded").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4_000
                readTimeout = 4_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
            }
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val rootArray = JSONArray(body)
            val suggestionArray = rootArray.optJSONArray(1) ?: return@withContext emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until minOf(suggestionArray.length(), 6)) {
                val item = suggestionArray.optString(i, "").trim()
                if (item.isNotBlank()) {
                    list.add(item)
                }
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengambil saran pencarian: ${e.message}")
            emptyList()
        }
    }

    suspend fun search(context: Context, query: String): Result<List<YoutubeSearchItem>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().ifEmpty { "trending indonesia" }
        try {
            val fastResults = searchViaInnertube(cleanQuery)
            if (fastResults.isNotEmpty()) {
                return@withContext Result.success(fastResults)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Innertube search fallback ke yt-dlp: ${e.message}")
        }

        // Fallback via yt-dlp jika Innertube gagal
        try {
            val ytDlpResults = searchViaYtDlp(context, cleanQuery)
            Result.success(ytDlpResults)
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal mencari video YouTube: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mengambil daftar video YouTube Shorts (vertikal) berdasarkan topik atau tren terkini.
     */
    suspend fun getShortsFeed(context: Context, topic: String = ""): Result<List<YoutubeSearchItem>> = withContext(Dispatchers.IO) {
        val query = topic.trim()
            .takeIf { it.isNotBlank() }
            ?.let { "$it #shorts" }
            ?: "shorts trending indonesia #shorts"

        try {
            val raw = searchViaInnertube(query)
            val normalized = raw
                .filter { item ->
                    val sec = parseDurationSeconds(item.durationText)
                    item.isShort || (sec != null && sec in 1L..75L)
                }
                .map { item ->
                    if (item.isShort) item else item.copy(durationText = "SHORTS")
                }
            if (normalized.isNotEmpty()) {
                return@withContext Result.success(normalized)
            }
            if (raw.isNotEmpty()) {
                return@withContext Result.success(raw.take(18).map { it.copy(durationText = "SHORTS") })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Innertube shorts fallback ke yt-dlp: ${e.message}")
        }

        try {
            val fallback = searchViaYtDlp(context, query)
                .map { it.copy(durationText = "SHORTS") }
            Result.success(fallback)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun searchViaInnertube(query: String): List<YoutubeSearchItem> {
        val connection = (URL(INNERTUBE_SEARCH_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        }

        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20241101.00.00")
                    put("hl", "id")
                    put("gl", "ID")
                })
            })
            put("query", query)
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val statusCode = connection.responseCode
        if (statusCode !in 200..299) {
            throw IllegalStateException("HTTP $statusCode dari YouTube search")
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)

        val collected = mutableListOf<YoutubeSearchItem>()
        val seenIds = mutableSetOf<String>()
        extractVideoRenderers(root, collected, seenIds)
        return collected
    }

    private fun extractVideoRenderers(
        node: Any?,
        out: MutableList<YoutubeSearchItem>,
        seenIds: MutableSet<String>
    ) {
        if (out.size >= 36) return
        when (node) {
            is JSONObject -> {
                val renderer = node.optJSONObject("videoRenderer")
                    ?: node.optJSONObject("compactVideoRenderer")
                    ?: node.optJSONObject("gridVideoRenderer")
                if (renderer != null) {
                    parseVideoRenderer(renderer)?.let { item ->
                        if (seenIds.add(item.videoId)) {
                            out.add(item)
                        }
                    }
                }

                val reelRenderer = node.optJSONObject("reelItemRenderer")
                if (reelRenderer != null) {
                    parseReelItemRenderer(reelRenderer)?.let { item ->
                        if (seenIds.add(item.videoId)) {
                            out.add(item)
                        }
                    }
                }

                val shortsLockup = node.optJSONObject("shortsLockupViewModel")
                if (shortsLockup != null) {
                    parseShortsLockupViewModel(shortsLockup)?.let { item ->
                        if (seenIds.add(item.videoId)) {
                            out.add(item)
                        }
                    }
                }

                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key != "videoRenderer" &&
                        key != "compactVideoRenderer" &&
                        key != "gridVideoRenderer" &&
                        key != "reelItemRenderer" &&
                        key != "shortsLockupViewModel"
                    ) {
                        extractVideoRenderers(node.opt(key), out, seenIds)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    extractVideoRenderers(node.opt(i), out, seenIds)
                }
            }
        }
    }

    private fun parseReelItemRenderer(renderer: JSONObject): YoutubeSearchItem? {
        val videoId = renderer.optString("videoId", "")
            .ifBlank {
                renderer.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("reelWatchEndpoint")
                    ?.optString("videoId", "") ?: ""
            }
        if (videoId.isBlank()) return null

        val title = readTextRuns(renderer.optJSONObject("headline"))
            .ifBlank {
                renderer.optJSONObject("accessibility")
                    ?.optJSONObject("accessibilityData")
                    ?.optString("label", "") ?: ""
            }
            .ifBlank { "YouTube Shorts ($videoId)" }

        val viewCount = readTextRuns(renderer.optJSONObject("viewCountText"))
        val thumbs = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "").orEmpty()
        } else {
            ""
        }.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

        return YoutubeSearchItem(
            videoId = videoId,
            title = title,
            channel = "Shorts",
            durationText = "SHORTS",
            viewCountText = viewCount,
            thumbnailUrl = thumbUrl
        )
    }

    private fun parseShortsLockupViewModel(vm: JSONObject): YoutubeSearchItem? {
        val watchEndpoint = vm.optJSONObject("onTap")
            ?.optJSONObject("innertubeCommand")
            ?.optJSONObject("reelWatchEndpoint")
        val videoId = watchEndpoint?.optString("videoId", "").orEmpty()
            .ifBlank {
                vm.optString("entityId", "")
                    .removePrefix("shorts-lockup-ui-model-")
                    .takeIf { it.length == 11 }
                    .orEmpty()
            }
        if (videoId.isBlank()) return null

        val overlay = vm.optJSONObject("overlayMetadata")
        val primaryTitle = overlay?.optJSONObject("primaryText")?.optString("content", "").orEmpty()
            .ifBlank { vm.optString("accessibilityText", "").substringBefore(", ") }
            .ifBlank { "YouTube Shorts ($videoId)" }
        val viewCount = overlay?.optJSONObject("secondaryText")?.optString("content", "").orEmpty()

        val sources = vm.optJSONObject("thumbnail")?.optJSONArray("sources")
        val endpointThumbs = watchEndpoint?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumbUrl = when {
            sources != null && sources.length() > 0 ->
                sources.optJSONObject(sources.length() - 1)?.optString("url", "").orEmpty()
            endpointThumbs != null && endpointThumbs.length() > 0 ->
                endpointThumbs.optJSONObject(endpointThumbs.length() - 1)?.optString("url", "").orEmpty()
            else -> ""
        }.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

        return YoutubeSearchItem(
            videoId = videoId,
            title = primaryTitle,
            channel = "Shorts",
            durationText = "SHORTS",
            viewCountText = viewCount,
            thumbnailUrl = thumbUrl
        )
    }

    private fun extractBrowseIdFromRuns(obj: JSONObject?): String {
        if (obj == null) return ""
        val runs = obj.optJSONArray("runs") ?: return ""
        for (i in 0 until runs.length()) {
            val browseId = runs.optJSONObject(i)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId", "") ?: ""
            if (browseId.isNotBlank()) return browseId
        }
        return ""
    }

    private fun parseVideoRenderer(renderer: JSONObject): YoutubeSearchItem? {
        val videoId = renderer.optString("videoId", "")
        if (videoId.isBlank()) return null

        val title = readTextRuns(renderer.optJSONObject("title"))
        if (title.isBlank()) return null

        val ownerObj = renderer.optJSONObject("ownerText")
        val longBylineObj = renderer.optJSONObject("longBylineText")
        val shortBylineObj = renderer.optJSONObject("shortBylineText")

        val channel = readTextRuns(ownerObj)
            .ifBlank { readTextRuns(longBylineObj) }
            .ifBlank { readTextRuns(shortBylineObj) }
            .ifBlank { "YouTube" }

        val channelId = extractBrowseIdFromRuns(ownerObj)
            .ifBlank { extractBrowseIdFromRuns(longBylineObj) }
            .ifBlank { extractBrowseIdFromRuns(shortBylineObj) }

        val isReelEndpoint = renderer.optJSONObject("navigationEndpoint")?.has("reelWatchEndpoint") == true
        val duration = if (isReelEndpoint) {
            "SHORTS"
        } else {
            readTextRuns(renderer.optJSONObject("lengthText"))
                .ifBlank { renderer.optJSONObject("lengthText")?.optString("simpleText", "") ?: "" }
                .ifBlank {
                    val overlays = renderer.optJSONArray("thumbnailOverlays")
                    var overlayDur = ""
                    if (overlays != null) {
                        for (i in 0 until overlays.length()) {
                            val overlayRenderer = overlays.optJSONObject(i)
                                ?.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                            val style = overlayRenderer?.optString("style", "") ?: ""
                            if (style.equals("SHORTS", ignoreCase = true)) {
                                overlayDur = "SHORTS"
                                break
                            }
                            val timeText = readTextRuns(overlayRenderer?.optJSONObject("text"))
                            if (timeText.isNotBlank()) {
                                overlayDur = timeText
                                break
                            }
                        }
                    }
                    overlayDur
                }
                .ifBlank { "LIVE" }
        }

        val viewCount = readTextRuns(renderer.optJSONObject("shortViewCountText"))
            .ifBlank { readTextRuns(renderer.optJSONObject("viewCountText")) }

        val thumbs = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val bestThumb = if (thumbs != null && thumbs.length() > 0) {
            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "")
        } else {
            ""
        }
        val thumbnailUrl = if (!bestThumb.isNullOrBlank()) {
            bestThumb
        } else {
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }

        return YoutubeSearchItem(
            videoId = videoId,
            title = title,
            channel = channel,
            durationText = duration,
            viewCountText = viewCount,
            thumbnailUrl = thumbnailUrl,
            channelId = channelId
        )
    }

    private fun readTextRuns(obj: JSONObject?): String {
        if (obj == null) return ""
        val simple = obj.optString("simpleText", "")
        if (simple.isNotBlank()) return simple
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
        }
        return sb.toString().trim()
    }

    private suspend fun searchViaYtDlp(context: Context, query: String): List<YoutubeSearchItem> {
        YoutubeDlManager.init(context)
        val request = YoutubeDLRequest("ytsearch15:$query").apply {
            addOption("--flat-playlist")
            addOption("--dump-single-json")
            addOption("--no-check-certificates")
            addOption("--socket-timeout", "30")
        }
        val response = YoutubeDL.getInstance().execute(request)
        val json = JSONObject(response.out)
        val entries = json.optJSONArray("entries") ?: return emptyList()
        val list = mutableListOf<YoutubeSearchItem>()
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val id = entry.optString("id", "")
            val title = entry.optString("title", "")
            if (id.isBlank() || title.isBlank()) continue
            val channel = entry.optString("uploader", "").ifBlank { entry.optString("channel", "YouTube") }
            val durSec = entry.optLong("duration", 0L)
            val durText = if (durSec > 0) {
                val m = durSec / 60
                val s = durSec % 60
                String.format("%d:%02d", m, s)
            } else ""
            list.add(
                YoutubeSearchItem(
                    videoId = id,
                    title = title,
                    channel = channel,
                    durationText = durText,
                    viewCountText = "",
                    thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                )
            )
        }
        return list
    }
}
