package com.nunatv.app.data.preferences

import android.content.Context
import org.json.JSONObject

/**
 * Penyimpan posisi terakhir pemutaran video yang sangat ringan.
 * Menggunakan satu JSON kecil di SharedPreferences (maksimal 40 video terbaru)
 * agar fitur "Lanjutkan Menonton" hampir tidak memakai RAM maupun storage.
 */
object PlaybackPositionStore {
    private const val PREF_NAME = "nuna_resume_positions"
    private const val KEY_JSON = "positions_json"
    private const val MAX_ENTRIES = 40
    private const val MIN_RESUME_MS = 5_000L
    private const val END_MARGIN_MS = 10_000L
    private const val MIN_DURATION_FOR_END_MARGIN_MS = 15_000L

    private val lock = Any()
    private var loaded = false
    private val cache = LinkedHashMap<String, Long>(MAX_ENTRIES + 2, 0.75f, false)

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        val raw = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null)
        if (!raw.isNullOrBlank()) {
            try {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val pos = obj.optLong(key, 0L)
                    if (key.isNotBlank() && pos >= MIN_RESUME_MS) {
                        cache[key] = pos
                    }
                }
            } catch (_: Exception) {
                cache.clear()
            }
        }
        loaded = true
    }

    private fun persistLocked(context: Context) {
        val obj = JSONObject()
        for ((k, v) in cache) {
            obj.put(k, v)
        }
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, obj.toString())
            .apply()
    }

    fun getPositionMs(context: Context, videoId: String): Long {
        if (videoId.isBlank()) return 0L
        synchronized(lock) {
            ensureLoaded(context)
            return cache[videoId] ?: 0L
        }
    }

    fun savePositionMs(
        context: Context,
        videoId: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (videoId.isBlank()) return
        val shouldClear = positionMs < MIN_RESUME_MS ||
            (durationMs > MIN_DURATION_FOR_END_MARGIN_MS && positionMs >= durationMs - END_MARGIN_MS)

        synchronized(lock) {
            ensureLoaded(context)
            if (shouldClear) {
                if (cache.remove(videoId) != null) {
                    persistLocked(context)
                }
                return
            }

            cache.remove(videoId)
            cache[videoId] = positionMs
            while (cache.size > MAX_ENTRIES) {
                val oldestKey = cache.entries.firstOrNull()?.key ?: break
                cache.remove(oldestKey)
            }
            persistLocked(context)
        }
    }

    fun clearPosition(context: Context, videoId: String) {
        if (videoId.isBlank()) return
        synchronized(lock) {
            ensureLoaded(context)
            if (cache.remove(videoId) != null) {
                persistLocked(context)
            }
        }
    }
}
