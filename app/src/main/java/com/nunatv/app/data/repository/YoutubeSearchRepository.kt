package com.nunatv.app.data.repository

import android.content.Context
import android.util.Log
import com.nunatv.app.downloader.YoutubeDlManager
import com.nunatv.app.ui.screens.youtube.ChannelDestination
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

data class ChannelDataResult(
    val channel: ChannelDestination,
    val videos: List<YoutubeSearchItem>
)

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
     * Memuat daftar video dan detail metadata profil/banner dari suatu Channel YouTube.
     */
    suspend fun getChannelData(
        context: Context,
        channelId: String?,
        channelNameFallback: String?,
        sourceVideoId: String = ""
    ): Result<ChannelDataResult> = withContext(Dispatchers.IO) {
        val cleanChannelId = channelId?.trim().orEmpty()
        var destination = ChannelDestination(
            channelId = cleanChannelId,
            channelName = channelNameFallback?.trim().orEmpty().ifBlank { "Channel YouTube" },
            sourceVideoId = sourceVideoId
        )

        if (cleanChannelId.startsWith("UC")) {
            try {
                // 1. Ambil tab Videos channel via /youtubei/v1/browse dengan params video
                val browseWithTab = postInnertubeJson(INNERTUBE_BROWSE_URL, JSONObject().apply {
                    put("context", buildWebContext())
                    put("browseId", cleanChannelId)
                    put("params", "EgZ2aWRlb3PyBgQKAjoA")
                })

                destination = parseChannelMetadata(browseWithTab, destination)

                val tabVideos = mutableListOf<YoutubeSearchItem>()
                val seen = mutableSetOf<String>()
                extractVideoRenderers(browseWithTab, tabVideos, seen)

                if (tabVideos.isNotEmpty()) {
                    val actualChannelName = destination.channelName.ifBlank { channelNameFallback.orEmpty() }
                    val enriched = tabVideos.map { item ->
                        if (item.channel == "YouTube" || item.channel.isBlank()) {
                            item.copy(channel = actualChannelName, channelId = cleanChannelId)
                        } else {
                            item.copy(channelId = item.channelId.ifBlank { cleanChannelId })
                        }
                    }
                    return@withContext Result.success(ChannelDataResult(destination, enriched))
                }

                // 2. Fallback ke halaman utama (Home) channel tanpa params jika tab videos kosong
                val browseHome = postInnertubeJson(INNERTUBE_BROWSE_URL, JSONObject().apply {
                    put("context", buildWebContext())
                    put("browseId", cleanChannelId)
                })

                destination = parseChannelMetadata(browseHome, destination)

                val homeVideos = mutableListOf<YoutubeSearchItem>()
                extractVideoRenderers(browseHome, homeVideos, mutableSetOf())
                if (homeVideos.isNotEmpty()) {
                    val actualChannelName = destination.channelName.ifBlank { channelNameFallback.orEmpty() }
                    val enriched = homeVideos.map { item ->
                        if (item.channel == "YouTube" || item.channel.isBlank()) {
                            item.copy(channel = actualChannelName, channelId = cleanChannelId)
                        } else {
                            item.copy(channelId = item.channelId.ifBlank { cleanChannelId })
                        }
                    }
                    return@withContext Result.success(ChannelDataResult(destination, enriched))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal browse channel $cleanChannelId: ${e.message}")
            }
        }

        val fallbackQuery = channelNameFallback?.trim().orEmpty()
        if (fallbackQuery.isNotBlank()) {
            val searchRes = search(context, fallbackQuery)
            return@withContext searchRes.map { videos ->
                ChannelDataResult(destination, videos)
            }
        }
        Result.failure(IllegalStateException("Informasi channel tidak tersedia."))
    }

    suspend fun getChannelVideos(
        context: Context,
        channelId: String?,
        channelNameFallback: String?
    ): Result<List<YoutubeSearchItem>> = getChannelData(context, channelId, channelNameFallback).map { it.videos }

    private fun parseChannelMetadata(root: JSONObject, fallback: ChannelDestination): ChannelDestination {
        var name = fallback.channelName
        var avatarUrl = fallback.avatarUrl
        var bannerUrl = fallback.bannerUrl
        var handle = fallback.handle
        var subCount = fallback.subscriberCount
        var vidCount = fallback.videoCount
        var description = fallback.description

        val header = root.optJSONObject("header")
        if (header != null) {
            val c4 = header.optJSONObject("c4TabbedHeaderRenderer")
            if (c4 != null) {
                val c4Title = c4.optString("title", "").trim()
                if (c4Title.isNotBlank()) name = c4Title

                val avThumbs = c4.optJSONObject("avatar")?.optJSONArray("thumbnails")
                val av = getBestThumbnailUrl(avThumbs)
                if (av.isNotBlank()) avatarUrl = av

                val banThumbs = (c4.optJSONObject("tvBanner")
                    ?: c4.optJSONObject("banner")
                    ?: c4.optJSONObject("mobileBanner"))?.optJSONArray("thumbnails")
                val ban = getBestThumbnailUrl(banThumbs)
                if (ban.isNotBlank()) bannerUrl = ban

                val sc = readTextRuns(c4.optJSONObject("subscriberCountText")).ifBlank {
                    c4.optJSONObject("subscriberCountText")?.optString("simpleText", "") ?: ""
                }
                if (sc.isNotBlank()) subCount = sc

                val vc = readTextRuns(c4.optJSONObject("videosCountText"))
                if (vc.isNotBlank()) vidCount = vc
            }

            val phr = header.optJSONObject("pageHeaderRenderer")?.optJSONObject("content")?.optJSONObject("pageHeaderViewModel")
            if (phr != null) {
                val phrTitle = phr.optJSONObject("title")?.optJSONObject("dynamicTextViewModel")
                    ?.optJSONObject("text")?.optString("content", "")?.trim().orEmpty()
                if (phrTitle.isNotBlank()) name = phrTitle

                val avSources = phr.optJSONObject("image")?.optJSONObject("decoratedAvatarViewModel")
                    ?.optJSONObject("avatar")?.optJSONObject("avatarViewModel")?.optJSONObject("image")?.optJSONArray("sources")
                val av = getBestThumbnailUrl(avSources)
                if (av.isNotBlank()) avatarUrl = av

                val banSources = phr.optJSONObject("banner")?.optJSONObject("imageBannerViewModel")
                    ?.optJSONObject("image")?.optJSONArray("sources")
                val ban = getBestThumbnailUrl(banSources)
                if (ban.isNotBlank()) bannerUrl = ban

                val metadataRows = phr.optJSONObject("metadata")?.optJSONObject("contentMetadataViewModel")?.optJSONArray("metadataRows")
                if (metadataRows != null) {
                    for (r in 0 until metadataRows.length()) {
                        val parts = metadataRows.optJSONObject(r)?.optJSONArray("metadataParts") ?: continue
                        for (p in 0 until parts.length()) {
                            val contentText = parts.optJSONObject(p)?.optJSONObject("text")?.optString("content", "")?.trim().orEmpty()
                            if (contentText.startsWith("@")) {
                                handle = contentText
                            } else if (contentText.contains("sub", ignoreCase = true) || contentText.contains("pelanggan", ignoreCase = true)) {
                                subCount = contentText
                            } else if (contentText.contains("video", ignoreCase = true)) {
                                vidCount = contentText
                            }
                        }
                    }
                }
            }
        }

        val cmr = root.optJSONObject("metadata")?.optJSONObject("channelMetadataRenderer")
        if (cmr != null) {
            val cmrTitle = cmr.optString("title", "").trim()
            if (name.isBlank() || name == "Channel YouTube") {
                if (cmrTitle.isNotBlank()) name = cmrTitle
            }
            val vanity = cmr.optString("vanityChannelUrl", "").trim()
            if (vanity.contains("/@") && handle.isBlank()) {
                handle = "@" + vanity.substringAfter("/@")
            }
            val desc = cmr.optString("description", "").trim()
            if (desc.isNotBlank()) description = desc

            if (avatarUrl.isBlank()) {
                avatarUrl = getBestThumbnailUrl(cmr.optJSONObject("avatar")?.optJSONArray("thumbnails"))
            }
        }

        val micro = root.optJSONObject("microformat")?.optJSONObject("microformatDataRenderer")
        if (micro != null) {
            val microTitle = micro.optString("title", "").trim()
            if (name.isBlank() || name == "Channel YouTube") {
                if (microTitle.isNotBlank()) name = microTitle
            }
            if (description.isBlank()) {
                description = micro.optString("description", "").trim()
            }
            if (avatarUrl.isBlank()) {
                avatarUrl = getBestThumbnailUrl(micro.optJSONObject("thumbnail")?.optJSONArray("thumbnails"))
            }
        }

        return fallback.copy(
            channelName = name,
            avatarUrl = avatarUrl,
            bannerUrl = bannerUrl,
            handle = handle,
            subscriberCount = subCount,
            videoCount = vidCount,
            description = description
        )
    }

    private fun getBestThumbnailUrl(arr: JSONArray?): String {
        if (arr == null || arr.length() == 0) return ""
        var best = ""
        for (i in 0 until arr.length()) {
            val u = arr.optJSONObject(i)?.optString("url", "") ?: ""
            if (u.isNotBlank()) best = u
        }
        if (best.startsWith("//")) best = "https:$best"
        return best
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
