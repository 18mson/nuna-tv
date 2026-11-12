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

enum class VideoResolution(val label: String, val formatSpec: String, val preferredHeight: Int) {
    RES_1080P("1080p (FHD)", "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best", 1080),
    RES_720P("720p (HD)", "bestvideo[height<=720]+bestaudio/best[height<=720]/best", 720),
    RES_480P("480p (SD)", "bestvideo[height<=480]+bestaudio/best[height<=480]/best", 480),
    RES_360P("360p", "bestvideo[height<=360]+bestaudio/best[height<=360]/best", 360),
    RES_240P("240p", "bestvideo[height<=240]+bestaudio/best[height<=240]/best", 240),
    AUDIO_ONLY("Audio Saja (M4A/MP3)", "bestaudio/best", 360);

    companion object {
        fun fromLabel(label: String): VideoResolution {
            return entries.find {
                it.label.equals(label, ignoreCase = true) ||
                    it.name.equals(label, ignoreCase = true) ||
                    it.label.startsWith(label, ignoreCase = true)
            } ?: RES_1080P
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

class AppPreferences(private val context: Context) {

    private val defaultDownloadDir = StorageUtils.getDefaultDownloadDir(context).absolutePath

    val selectedResolution: Flow<VideoResolution> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_RESOLUTION] ?: VideoResolution.RES_1080P.name
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
    }
}
