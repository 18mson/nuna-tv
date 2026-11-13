package com.nunatv.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nunatv.app.data.repository.YoutubeSearchItem
import com.nunatv.app.util.StorageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nuna_settings")

enum class VideoResolution(
    val label: String,
    val description: String,
    val formatSpec: String,
    val preferredHeight: Int,
    val isDetail: Boolean = false
) {
    // Mode Default / Sederhana (Kategori)
    AUDIO(
        label = "Audio",
        description = "Format audio terbaik (M4A/MP3)",
        formatSpec = "bestaudio/best",
        preferredHeight = 0,
        isDetail = false
    ),
    LOW(
        label = "Rendah",
        description = "360p - 240p (Prioritas 240p)",
        formatSpec = "bestvideo[height=240]+bestaudio/best[height=240]/bestvideo[height<=360]+bestaudio/best[height<=360]/best",
        preferredHeight = 240,
        isDetail = false
    ),
    MEDIUM(
        label = "Sedang",
        description = "720p - 480p (Paling relevan)",
        formatSpec = "bestvideo[height<=720][height>=480]+bestaudio/bestvideo[height<=720]+bestaudio/best[height<=720]/best",
        preferredHeight = 720,
        isDetail = false
    ),
    HIGH(
        label = "Tinggi",
        description = "1080p - 2K (Prioritas tertinggi)",
        formatSpec = "bestvideo[height<=1440][height>=1080]+bestaudio/bestvideo[height<=1440]+bestaudio/best[height<=1440]/best",
        preferredHeight = 1080,
        isDetail = false
    ),

    // Mode Detail Angka Spesifik (Hidden Easter Egg Setting)
    RES_1080P(
        label = "1080p (FHD)",
        description = "Resolusi Full HD 1080p",
        formatSpec = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
        preferredHeight = 1080,
        isDetail = true
    ),
    RES_720P(
        label = "720p (HD)",
        description = "Resolusi HD 720p",
        formatSpec = "bestvideo[height<=720]+bestaudio/best[height<=720]/best",
        preferredHeight = 720,
        isDetail = true
    ),
    RES_480P(
        label = "480p (SD)",
        description = "Resolusi Standar 480p",
        formatSpec = "bestvideo[height<=480]+bestaudio/best[height<=480]/best",
        preferredHeight = 480,
        isDetail = true
    ),
    RES_360P(
        label = "360p",
        description = "Resolusi Standar 360p",
        formatSpec = "bestvideo[height<=360]+bestaudio/best[height<=360]/best",
        preferredHeight = 360,
        isDetail = true
    ),
    RES_240P(
        label = "240p",
        description = "Resolusi Ringan 240p",
        formatSpec = "bestvideo[height<=240]+bestaudio/best[height<=240]/best",
        preferredHeight = 240,
        isDetail = true
    ),
    AUDIO_ONLY(
        label = "Audio Saja (M4A/MP3)",
        description = "Hanya stream audio tanpa video",
        formatSpec = "bestaudio/best",
        preferredHeight = 0,
        isDetail = true
    );

    companion object {
        val DEFAULT_TIERS = listOf(HIGH, MEDIUM, LOW, AUDIO)
        val DETAILED_TIERS = listOf(RES_1080P, RES_720P, RES_480P, RES_360P, RES_240P, AUDIO_ONLY)

        fun fromLabel(label: String): VideoResolution {
            return entries.find {
                it.label.equals(label, ignoreCase = true) ||
                    it.name.equals(label, ignoreCase = true) ||
                    it.label.startsWith(label, ignoreCase = true)
            } ?: HIGH
        }
    }
}

enum class AudioBoostLevel(
    val label: String,
    val shortLabel: String,
    val targetGainMb: Int
) {
    OFF("Normal (0 dB)", "Normal", 0),
    MEDIUM("Sedang (+6 dB)", "+6 dB", 600),
    HIGH("Kuat (+12 dB)", "+12 dB", 1200),
    MAX("Ekstra (+18 dB)", "+18 dB", 1800);

    fun next(): AudioBoostLevel {
        val list = entries
        return list[(ordinal + 1) % list.size]
    }

    companion object {
        fun fromName(name: String?): AudioBoostLevel {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: OFF
        }
    }
}

enum class AppColorPalette(val label: String, val swatchHex: Long) {
    PURPLE("Ungu", 0xFF8E54E9),
    RED("Merah", 0xFFE50914),
    BLUE("Biru", 0xFF2979FF),
    BROWN("Coklat", 0xFF8D5B4C);

    companion object {
        fun fromName(name: String?): AppColorPalette {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: RED
        }
    }
}

enum class AppThemeMode(val label: String) {
    DARK("Mode Gelap"),
    LIGHT("Mode Terang");

    companion object {
        fun fromName(name: String?): AppThemeMode {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: DARK
        }
    }
}

class AppPreferences(private val context: Context) {

    private val defaultDownloadDir = StorageUtils.getDefaultDownloadDir(context).absolutePath

    val selectedResolution: Flow<VideoResolution> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_RESOLUTION] ?: VideoResolution.HIGH.name
        VideoResolution.fromLabel(saved)
    }

    val audioBoostLevel: Flow<AudioBoostLevel> = context.dataStore.data.map { prefs ->
        AudioBoostLevel.fromName(prefs[KEY_AUDIO_BOOST])
    }

    val downloadLocation: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DOWNLOAD_LOCATION] ?: defaultDownloadDir
    }

    val customCategories: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_CATEGORIES] ?: setOf("Umum", "Musik", "Gaming", "Edukasi")
    }

    val recentSearchQueries: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_SEARCHES] ?: return@map emptyList()
        try {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> arr.optString(i, "") }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val lastWatchedVideoId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_WATCHED_ID]?.takeIf { it.isNotBlank() }
    }

    val lastWatchedVideoTitle: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_WATCHED_TITLE]?.takeIf { it.isNotBlank() }
    }

    val watchHistory: Flow<List<YoutubeSearchItem>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_WATCH_HISTORY] ?: return@map emptyList()
        parseWatchHistoryJson(raw)
    }

    val backgroundPlaybackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKGROUND_PLAYBACK] ?: false
    }

    val pipEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIP_ENABLED] ?: false
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_ONBOARDING] ?: false
    }

    val hideShortsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HIDE_SHORTS] ?: false
    }

    val colorPalette: Flow<AppColorPalette> = context.dataStore.data.map { prefs ->
        AppColorPalette.fromName(prefs[KEY_COLOR_PALETTE])
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        AppThemeMode.fromName(prefs[KEY_THEME_MODE])
    }

    val showDetailedResolution: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_DETAILED_RESOLUTION] ?: false
    }

    private fun parseWatchHistoryJson(raw: String): MutableList<YoutubeSearchItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<YoutubeSearchItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val vid = obj.optString("videoId", "")
                val title = obj.optString("title", "")
                if (vid.isBlank() || title.isBlank()) continue
                list.add(
                    YoutubeSearchItem(
                        videoId = vid,
                        title = title,
                        channel = obj.optString("channel", "YouTube"),
                        durationText = obj.optString("durationText", ""),
                        viewCountText = obj.optString("viewCountText", ""),
                        thumbnailUrl = obj.optString("thumbnailUrl", "https://i.ytimg.com/vi/$vid/hqdefault.jpg"),
                        channelId = obj.optString("channelId", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    suspend fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = try {
                val arr = JSONArray(prefs[KEY_RECENT_SEARCHES] ?: "[]")
                MutableList(arr.length()) { i -> arr.optString(i, "") }.filter { it.isNotBlank() }.toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            current.removeAll { it.equals(trimmed, ignoreCase = true) }
            current.add(0, trimmed)
            val trimmedList = current.take(5)
            prefs[KEY_RECENT_SEARCHES] = JSONArray(trimmedList).toString()
        }
    }

    suspend fun recordWatchedItem(item: YoutubeSearchItem) {
        if (item.videoId.isBlank()) return
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_WATCHED_ID] = item.videoId
            if (item.title.isNotBlank()) {
                prefs[KEY_LAST_WATCHED_TITLE] = item.title
            }
            val current = parseWatchHistoryJson(prefs[KEY_WATCH_HISTORY] ?: "[]")
            current.removeAll { it.videoId == item.videoId }
            current.add(0, item)
            val arr = JSONArray()
            current.take(20).forEach { entry ->
                arr.put(
                    JSONObject().apply {
                        put("videoId", entry.videoId)
                        put("title", entry.title)
                        put("channel", entry.channel)
                        put("durationText", entry.durationText)
                        put("viewCountText", entry.viewCountText)
                        put("thumbnailUrl", entry.thumbnailUrl)
                        put("channelId", entry.channelId)
                    }
                )
            }
            prefs[KEY_WATCH_HISTORY] = arr.toString()
        }
    }

    suspend fun recordWatchedVideo(videoId: String, title: String) {
        if (videoId.isBlank()) return
        recordWatchedItem(
            YoutubeSearchItem(
                videoId = videoId,
                title = title.ifBlank { "Video YouTube ($videoId)" },
                channel = "YouTube",
                durationText = "",
                viewCountText = "",
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            )
        )
    }

    suspend fun clearWatchHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_WATCH_HISTORY)
        }
    }

    suspend fun setResolution(resolution: VideoResolution) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RESOLUTION] = resolution.name
        }
    }

    suspend fun setAudioBoostLevel(level: AudioBoostLevel) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUDIO_BOOST] = level.name
        }
    }

    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKGROUND_PLAYBACK] = enabled
        }
    }

    suspend fun setPipEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIP_ENABLED] = enabled
        }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_ONBOARDING] = seen
        }
    }

    suspend fun setHideShortsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HIDE_SHORTS] = enabled
        }
    }

    suspend fun setColorPalette(palette: AppColorPalette) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COLOR_PALETTE] = palette.name
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setShowDetailedResolution(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_DETAILED_RESOLUTION] = show
        }
    }

    suspend fun setDownloadLocation(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DOWNLOAD_LOCATION] = path
        }
    }

    suspend fun addCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_CATEGORIES] ?: setOf("Umum", "Musik", "Gaming", "Edukasi")
            prefs[KEY_CUSTOM_CATEGORIES] = current + trimmed
        }
    }

    suspend fun removeCategory(category: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_CATEGORIES] ?: setOf("Umum", "Musik", "Gaming", "Edukasi")
            prefs[KEY_CUSTOM_CATEGORIES] = current - category
        }
    }

    companion object {
        val KEY_RESOLUTION = stringPreferencesKey("pref_default_resolution")
        val KEY_AUDIO_BOOST = stringPreferencesKey("pref_audio_boost")
        val KEY_DOWNLOAD_LOCATION = stringPreferencesKey("pref_download_location")
        val KEY_CUSTOM_CATEGORIES = androidx.datastore.preferences.core.stringSetPreferencesKey("pref_custom_categories")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("pref_recent_searches")
        val KEY_WATCH_HISTORY = stringPreferencesKey("pref_watch_history")
        val KEY_LAST_WATCHED_ID = stringPreferencesKey("pref_last_watched_id")
        val KEY_LAST_WATCHED_TITLE = stringPreferencesKey("pref_last_watched_title")
        val KEY_BACKGROUND_PLAYBACK = booleanPreferencesKey("pref_background_playback_enabled")
        val KEY_PIP_ENABLED = booleanPreferencesKey("pref_pip_enabled")
        val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("pref_has_seen_onboarding")
        val KEY_HIDE_SHORTS = booleanPreferencesKey("pref_hide_shorts")
        val KEY_COLOR_PALETTE = stringPreferencesKey("pref_color_palette")
        val KEY_THEME_MODE = stringPreferencesKey("pref_theme_mode")
        val KEY_SHOW_DETAILED_RESOLUTION = booleanPreferencesKey("pref_show_detailed_resolution")
    }
}
