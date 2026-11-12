package com.nunatv.app.player

import android.content.Context
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil.imageLoader
import coil.request.ImageRequest
import com.nunatv.app.data.local.DownloadStatus
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.preferences.AudioBoostLevel
import com.nunatv.app.data.repository.VideoRepository
import com.nunatv.app.data.repository.YoutubeSearchItem
import com.nunatv.app.data.repository.YoutubeSearchRepository
import com.nunatv.app.downloader.StreamQualityOption
import com.nunatv.app.downloader.YoutubeDlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

data class MediaPlaybackState(
    val currentVideo: DownloadedVideoEntity? = null,
    val playlist: List<DownloadedVideoEntity> = emptyList(),
    val isRemoteStream: Boolean = false,
    val isLiveStream: Boolean = false,
    val isShortStream: Boolean = false,
    val isLoadingStream: Boolean = false,
    val streamLoadError: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeAudioStreamUrl: String? = null,
    val activeStreamQuality: StreamQualityOption? = null,
    val streamQualityOptions: List<StreamQualityOption> = emptyList(),
    val activeChannelId: String = "",
    val audioBoostLevel: AudioBoostLevel = AudioBoostLevel.OFF,
    val isAutoplayEnabled: Boolean = true,
    val isMinimizedInApp: Boolean = false,
    val videoWidth: Int = 16,
    val videoHeight: Int = 9
)

@OptIn(UnstableApi::class)
object MediaPlaybackManager {

    private const val TAG = "MediaPlaybackManager"
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appContext: Context? = null
    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var resolveJob: Job? = null
    private var relatedFetchJob: Job? = null
    private var prefetchJob: Job? = null
    private var positionLoopJob: Job? = null

    private var preferredHeightForResolve: Int = 720
    private var resolutionLabelForResolve: String = "720p (HD)"
    private var isBackgroundAllowed: Boolean = false
    private var isAppForeground: Boolean = true
    private var completedLocalVideosProvider: () -> Map<String, DownloadedVideoEntity> = { emptyMap() }
    private var channelIdCache = mutableMapOf<String, String>()
    private var upNextQueue = listOf<DownloadedVideoEntity>()
    private var lastRetryAction: (() -> Unit)? = null
    private var activeStreamUserAgent: String? = null

    private val _state = MutableStateFlow(MediaPlaybackState())
    val state: StateFlow<MediaPlaybackState> = _state.asStateFlow()

    private val httpDataSourceFactory by lazy {
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
    }

    fun configureEnvironment(
        context: Context,
        backgroundAllowed: Boolean,
        boostLevel: AudioBoostLevel,
        preferredHeight: Int,
        resolutionLabel: String,
        completedLocalProvider: () -> Map<String, DownloadedVideoEntity>
    ) {
        val appCtx = context.applicationContext
        appContext = appCtx
        isBackgroundAllowed = backgroundAllowed
        preferredHeightForResolve = preferredHeight
        resolutionLabelForResolve = resolutionLabel
        completedLocalVideosProvider = completedLocalProvider

        if (_state.value.audioBoostLevel != boostLevel) {
            _state.value = _state.value.copy(audioBoostLevel = boostLevel)
            applyAudioBoost(boostLevel)
        }

        if (_state.value.currentVideo != null) {
            syncBackgroundService(appCtx)
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        isAppForeground = inForeground
    }

    fun ensurePlayer(context: Context): ExoPlayer {
        val appCtx = context.applicationContext
        appContext = appCtx
        exoPlayer?.let { return it }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val dataSourceFactory = DefaultDataSource.Factory(appCtx, httpDataSourceFactory)
        val created = ExoPlayer.Builder(appCtx)
            .setAudioAttributes(audioAttributes, true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        created.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupLoudnessEnhancer(audioSessionId)
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _state.update { it.copy(isPlaying = playing) }
                if (playing) {
                    startPositionLoop()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    _state.update {
                        it.copy(
                            videoWidth = videoSize.width,
                            videoHeight = videoSize.height,
                            isShortStream = it.isShortStream || videoSize.height > videoSize.width
                        )
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val player = exoPlayer ?: return
                if (playbackState == Player.STATE_READY) {
                    val safeDuration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L } ?: 0L
                    _state.update {
                        it.copy(
                            durationMs = safeDuration,
                            isLiveStream = it.isLiveStream || player.isCurrentMediaItemLive,
                            streamLoadError = null
                        )
                    }
                    if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET && player.audioSessionId > 0) {
                        setupLoudnessEnhancer(player.audioSessionId)
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    val snapshot = _state.value
                    val handleDirectlyInBackground = snapshot.isMinimizedInApp || !isAppForeground
                    if (handleDirectlyInBackground &&
                        !snapshot.isLiveStream &&
                        snapshot.isAutoplayEnabled &&
                        hasNextVideo()
                    ) {
                        skipNext()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.message}", error)
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isLoadingStream = false,
                        streamLoadError = "Pemutaran terganggu (${error.errorCodeName}). Ketuk Coba Lagi."
                    )
                }
            }
        })

        exoPlayer = created
        return created
    }

    private fun startPositionLoop() {
        positionLoopJob?.cancel()
        positionLoopJob = managerScope.launch {
            while (true) {
                val player = exoPlayer ?: break
                if (!player.isPlaying) break
                val safeDuration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L } ?: 0L
                val safePosition = player.currentPosition.takeIf { it != C.TIME_UNSET && it >= 0L } ?: 0L
                _state.update {
                    it.copy(
                        currentPositionMs = safePosition,
                        durationMs = safeDuration
                    )
                }
                delay(500)
            }
        }
    }

    private fun setupLoudnessEnhancer(sessionId: Int) {
        try {
            loudnessEnhancer?.release()
        } catch (_: Throwable) {
        }
        loudnessEnhancer = if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId > 0) {
            try {
                LoudnessEnhancer(sessionId)
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
        applyAudioBoost(_state.value.audioBoostLevel)
    }

    private fun applyAudioBoost(level: AudioBoostLevel) {
        val enhancer = loudnessEnhancer ?: return
        try {
            if (level.targetGainMb > 0) {
                enhancer.setTargetGain(level.targetGainMb)
                enhancer.enabled = true
            } else {
                enhancer.setTargetGain(0)
                enhancer.enabled = false
            }
        } catch (_: Throwable) {
        }
    }

    fun openLocalVideo(
        context: Context,
        video: DownloadedVideoEntity,
        allLocalVideos: List<DownloadedVideoEntity>,
        minimized: Boolean = false
    ) {
        val appCtx = context.applicationContext
        resolveJob?.cancel()
        relatedFetchJob?.cancel()
        prefetchJob?.cancel()
        activeStreamUserAgent = null
        val player = ensurePlayer(appCtx)

        val channelId = channelIdCache[video.videoId].orEmpty()
        lastRetryAction = { openLocalVideo(appCtx, video, allLocalVideos, minimized) }

        _state.update {
            it.copy(
                currentVideo = video,
                playlist = allLocalVideos.ifEmpty { listOf(video) },
                isRemoteStream = false,
                isLiveStream = false,
                isShortStream = false,
                isLoadingStream = false,
                streamLoadError = null,
                activeAudioStreamUrl = null,
                activeStreamQuality = null,
                streamQualityOptions = emptyList(),
                activeChannelId = channelId,
                isMinimizedInApp = minimized,
                currentPositionMs = 0L,
                durationMs = 0L
            )
        }

        val localFile = File(video.localFilePath)
        if (!localFile.exists()) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isLoadingStream = false,
                    streamLoadError = "File video tidak ditemukan di penyimpanan."
                )
            }
            return
        }

        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(localFile)))
        player.prepare()
        player.playWhenReady = true
        syncBackgroundService(appCtx)
        preloadNotificationBitmap(appCtx, video.thumbnailUrl)
    }

    fun openYoutubeStream(
        context: Context,
        item: YoutubeSearchItem,
        currentList: List<YoutubeSearchItem>,
        minimized: Boolean = false
    ) {
        val appCtx = context.applicationContext
        val localMatch = completedLocalVideosProvider()[item.videoId]
        if (localMatch != null) {
            val localList = completedLocalVideosProvider().values.toList()
            recordYoutubeWatchedAsync(appCtx, item)
            openLocalVideo(appCtx, localMatch, localList, minimized = minimized)
            return
        }

        resolveJob?.cancel()
        relatedFetchJob?.cancel()
        prefetchJob?.cancel()
        activeStreamUserAgent = null
        val player = ensurePlayer(appCtx)
        player.pause()
        player.clearMediaItems()

        if (item.channelId.isNotBlank()) {
            channelIdCache[item.videoId] = item.channelId
        }
        currentList.forEach { listItem ->
            if (listItem.channelId.isNotBlank()) {
                channelIdCache[listItem.videoId] = listItem.channelId
            }
        }

        val initialEntity = item.toRemoteEntity(resolutionLabel = resolutionLabelForResolve)
        val tailFromList = currentList
            .filter { it.videoId != item.videoId }
            .map { it.toRemoteEntity(resolutionLabel = resolutionLabelForResolve) }
        val combinedPlaylist = listOf(initialEntity) + (upNextQueue + tailFromList)
            .distinctBy { it.videoId }
            .filter { it.videoId != initialEntity.videoId }

        lastRetryAction = { openYoutubeStream(appCtx, item, currentList, minimized) }

        _state.update {
            it.copy(
                currentVideo = initialEntity,
                playlist = combinedPlaylist,
                isRemoteStream = true,
                isLiveStream = item.durationText.equals("LIVE", ignoreCase = true),
                isShortStream = item.isShort,
                isLoadingStream = true,
                streamLoadError = null,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                activeAudioStreamUrl = null,
                activeStreamQuality = null,
                streamQualityOptions = emptyList(),
                activeChannelId = item.channelId.ifBlank { channelIdCache[item.videoId].orEmpty() },
                isMinimizedInApp = minimized
            )
        }
        syncBackgroundService(appCtx)
        preloadNotificationBitmap(appCtx, item.thumbnailUrl)

        // Ambil video terkait di background agar antrean Autoplay langsung siap
        relatedFetchJob = managerScope.launch {
            val related = YoutubeSearchRepository.fetchRelatedVideos(item.videoId)
            if (related.isNotEmpty()) {
                related.forEach { rel ->
                    if (rel.channelId.isNotBlank()) {
                        channelIdCache[rel.videoId] = rel.channelId
                    }
                }
                val activeId = _state.value.currentVideo?.videoId
                if (activeId == item.videoId) {
                    val relatedEntities = related.map { it.toRemoteEntity(resolutionLabel = resolutionLabelForResolve) }
                    _state.update { cur ->
                        val active = cur.currentVideo ?: return@update cur
                        val mergedTail = (upNextQueue + relatedEntities + cur.playlist)
                            .distinctBy { it.videoId }
                            .filter { it.videoId != active.videoId }
                        cur.copy(playlist = listOf(active) + mergedTail)
                    }
                }
            }
        }

        // Resolve stream URL langsung di background tanpa menahan navigasi ke PlayerScreen
        resolveJob = managerScope.launch {
            val resolved = YoutubeDlManager.resolvePlayableVideo(
                context = appCtx,
                videoUrl = item.watchUrl,
                preferredHeight = preferredHeightForResolve
            )
            if (_state.value.currentVideo?.videoId != item.videoId) return@launch

            if (resolved == null) {
                _state.update {
                    it.copy(
                        isLoadingStream = false,
                        streamLoadError = "Gagal memuat stream video. Periksa koneksi atau coba lagi."
                    )
                }
                return@launch
            }

            val finalChannelId = item.channelId
                .ifBlank { resolved.channelId }
                .ifBlank { channelIdCache[item.videoId].orEmpty() }
            if (finalChannelId.isNotBlank()) {
                channelIdCache[item.videoId] = finalChannelId
            }

            val enrichedItem = item.copy(
                title = item.title.ifBlank { resolved.title },
                channel = item.channel.takeIf { it.isNotBlank() && it != "YouTube" } ?: resolved.channel,
                channelId = finalChannelId,
                durationText = item.durationText.ifBlank { resolved.durationText },
                thumbnailUrl = item.thumbnailUrl.ifBlank { resolved.thumbnailUrl }
            )
            recordYoutubeWatchedAsync(appCtx, enrichedItem)

            val updatedEntity = initialEntity.copy(
                title = enrichedItem.title,
                author = enrichedItem.channel,
                thumbnailUrl = enrichedItem.thumbnailUrl,
                localFilePath = resolved.streamUrl,
                resolution = resolved.activeQuality.label
            )

            _state.update { cur ->
                val tail = cur.playlist.filter { it.videoId != updatedEntity.videoId }
                cur.copy(
                    currentVideo = updatedEntity,
                    playlist = listOf(updatedEntity) + tail,
                    isLiveStream = resolved.isLive || item.durationText.equals("LIVE", ignoreCase = true),
                    isLoadingStream = false,
                    streamLoadError = null,
                    activeAudioStreamUrl = resolved.audioStreamUrl,
                    activeStreamQuality = resolved.activeQuality,
                    streamQualityOptions = resolved.qualityOptions,
                    activeChannelId = finalChannelId
                )
            }

            activeStreamUserAgent = resolved.userAgent
            prepareAndPlayStream(resolved.streamUrl, resolved.audioStreamUrl, seekMs = 0L, userAgent = resolved.userAgent)
            syncBackgroundService(appCtx)
            prefetchNextInBackground(appCtx)
        }
    }

    fun switchToPlaylistVideo(context: Context, targetVideo: DownloadedVideoEntity) {
        val appCtx = context.applicationContext
        upNextQueue = upNextQueue.filterNot { it.videoId == targetVideo.videoId }
        val localMatch = completedLocalVideosProvider()[targetVideo.videoId]
        val currentSnapshot = _state.value

        if (!currentSnapshot.isRemoteStream) {
            recordLocalPlayAsync(appCtx, targetVideo)
            openLocalVideo(appCtx, targetVideo, currentSnapshot.playlist, minimized = currentSnapshot.isMinimizedInApp)
            return
        }

        if (localMatch != null) {
            recordLocalPlayAsync(appCtx, localMatch)
            openLocalVideo(appCtx, localMatch, completedLocalVideosProvider().values.toList(), minimized = currentSnapshot.isMinimizedInApp)
            return
        }

        val item = YoutubeSearchItem(
            videoId = targetVideo.videoId,
            title = targetVideo.title,
            channel = targetVideo.author.ifBlank { "YouTube" },
            durationText = "",
            viewCountText = "",
            thumbnailUrl = targetVideo.thumbnailUrl,
            channelId = channelIdCache[targetVideo.videoId].orEmpty()
        )
        val remaining = currentSnapshot.playlist
            .filter { it.videoId != targetVideo.videoId }
            .map {
                YoutubeSearchItem(
                    videoId = it.videoId,
                    title = it.title,
                    channel = it.author.ifBlank { "YouTube" },
                    durationText = "",
                    viewCountText = "",
                    thumbnailUrl = it.thumbnailUrl,
                    channelId = channelIdCache[it.videoId].orEmpty()
                )
            }
        openYoutubeStream(appCtx, item, remaining, minimized = currentSnapshot.isMinimizedInApp)
    }

    fun retryLoadStream() {
        lastRetryAction?.invoke()
    }

    fun selectStreamQuality(option: StreamQualityOption) {
        val cur = _state.value
        val playing = cur.currentVideo ?: return
        val player = exoPlayer ?: return
        val resumePosMs = player.currentPosition.coerceAtLeast(0L)

        _state.update {
            it.copy(
                activeAudioStreamUrl = option.audioUrl,
                activeStreamQuality = option,
                currentVideo = playing.copy(
                    localFilePath = option.videoUrl,
                    resolution = option.label
                )
            )
        }
        prepareAndPlayStream(option.videoUrl, option.audioUrl, seekMs = resumePosMs, userAgent = activeStreamUserAgent)
    }

    private fun buildHttpFactory(userAgent: String?): DefaultHttpDataSource.Factory {
        return if (userAgent.isNullOrBlank()) {
            httpDataSourceFactory
        } else {
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent(userAgent)
        }
    }

    private fun createStreamMediaSource(streamUrl: String, userAgent: String?): MediaSource {
        val uri = Uri.parse(streamUrl)
        val lower = streamUrl.lowercase()
        val item = MediaItem.fromUri(uri)
        val factory = buildHttpFactory(userAgent)
        return when {
            lower.contains(".m3u8") || lower.contains("manifest/hls") -> {
                HlsMediaSource.Factory(factory).createMediaSource(item)
            }
            lower.contains(".mpd") || lower.contains("manifest/dash") -> {
                DashMediaSource.Factory(factory).createMediaSource(item)
            }
            else -> {
                ProgressiveMediaSource.Factory(factory).createMediaSource(item)
            }
        }
    }

    private fun prepareAndPlayStream(videoUrl: String, audioUrl: String?, seekMs: Long, userAgent: String? = null) {
        val player = exoPlayer ?: return
        try {
            if (!audioUrl.isNullOrBlank() && videoUrl.startsWith("http")) {
                val videoSource = createStreamMediaSource(videoUrl, userAgent)
                val audioSource = createStreamMediaSource(audioUrl, userAgent)
                player.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else if (videoUrl.startsWith("http")) {
                player.setMediaSource(createStreamMediaSource(videoUrl, userAgent))
            } else {
                player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            }
            player.prepare()
            if (seekMs > 0L && !_state.value.isLiveStream) {
                player.seekTo(seekMs)
            }
            player.playWhenReady = true
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal menyiapkan MediaSource untuk $videoUrl: ${t.message}", t)
            _state.update {
                it.copy(
                    isPlaying = false,
                    isLoadingStream = false,
                    streamLoadError = "Format siaran gagal dimuat (${t.javaClass.simpleName}). Ketuk Coba Lagi."
                )
            }
        }
    }

    private fun prefetchNextInBackground(context: Context) {
        prefetchJob?.cancel()
        val next = getNextVideo() ?: return
        if (completedLocalVideosProvider().containsKey(next.videoId)) return
        if (next.originalUrl.isBlank()) return
        prefetchJob = managerScope.launch(Dispatchers.IO) {
            YoutubeDlManager.prefetchPlayableVideo(
                context = context,
                videoUrl = next.originalUrl,
                preferredHeight = preferredHeightForResolve
            )
        }
    }

    fun enqueueNext(item: YoutubeSearchItem) {
        if (item.channelId.isNotBlank()) {
            channelIdCache[item.videoId] = item.channelId
        }
        val entity = item.toRemoteEntity(resolutionLabel = resolutionLabelForResolve)
        upNextQueue = upNextQueue.filterNot { it.videoId == entity.videoId } + entity

        _state.update { cur ->
            val current = cur.currentVideo ?: return@update cur
            if (!cur.isRemoteStream) return@update cur
            val tail = (upNextQueue + cur.playlist)
                .distinctBy { it.videoId }
                .filter { it.videoId != current.videoId }
            cur.copy(playlist = listOf(current) + tail)
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun pausePlayback() {
        exoPlayer?.pause()
    }

    fun resumePlayback() {
        exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        if (_state.value.isLiveStream && !player.isCurrentMediaItemSeekable) return
        val rawDuration = player.duration
        val maxMs = if (rawDuration != C.TIME_UNSET && rawDuration > 0L) rawDuration else 0L
        val clamped = if (maxMs > 0L) positionMs.coerceIn(0L, maxMs) else positionMs.coerceAtLeast(0L)
        player.seekTo(clamped)
        _state.update { it.copy(currentPositionMs = clamped) }
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        seekTo(player.currentPosition + deltaMs)
    }

    fun hasNextVideo(): Boolean {
        val s = _state.value
        return s.playlist.size > 1
    }

    fun getNextVideo(): DownloadedVideoEntity? {
        val s = _state.value
        val cur = s.currentVideo ?: return null
        if (s.playlist.size <= 1) return null
        val idx = s.playlist.indexOfFirst { it.videoId == cur.videoId }
        return if (idx >= 0) s.playlist[(idx + 1) % s.playlist.size] else s.playlist.firstOrNull { it.videoId != cur.videoId }
    }

    fun getPrevVideo(): DownloadedVideoEntity? {
        val s = _state.value
        val cur = s.currentVideo ?: return null
        if (s.playlist.size <= 1) return null
        val idx = s.playlist.indexOfFirst { it.videoId == cur.videoId }
        return if (idx >= 0) s.playlist[(idx - 1 + s.playlist.size) % s.playlist.size] else s.playlist.lastOrNull { it.videoId != cur.videoId }
    }

    fun skipNext() {
        val ctx = appContext ?: return
        val next = getNextVideo() ?: return
        switchToPlaylistVideo(ctx, next)
    }

    fun skipPrevious() {
        val ctx = appContext ?: return
        val prev = getPrevVideo() ?: return
        switchToPlaylistVideo(ctx, prev)
    }

    fun setAudioBoostLevel(level: AudioBoostLevel) {
        _state.update { it.copy(audioBoostLevel = level) }
        applyAudioBoost(level)
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        _state.update { it.copy(isAutoplayEnabled = enabled) }
    }

    fun setMinimized(minimized: Boolean) {
        _state.update { it.copy(isMinimizedInApp = minimized) }
    }

    fun rememberChannelIds(items: List<YoutubeSearchItem>) {
        items.forEach {
            if (it.channelId.isNotBlank()) {
                channelIdCache[it.videoId] = it.channelId
            }
        }
    }

    fun getChannelIdForVideo(videoId: String): String = channelIdCache[videoId].orEmpty()

    fun stopAndClear(context: Context) {
        val appCtx = context.applicationContext
        resolveJob?.cancel()
        relatedFetchJob?.cancel()
        prefetchJob?.cancel()
        positionLoopJob?.cancel()
        activeStreamUserAgent = null
        lastRetryAction = null

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
        }
        _state.update { cur ->
            MediaPlaybackState(
                audioBoostLevel = cur.audioBoostLevel,
                isAutoplayEnabled = cur.isAutoplayEnabled
            )
        }
        try {
            appCtx.stopService(Intent(appCtx, MediaPlaybackService::class.java))
        } catch (_: Throwable) {
        }
    }

    fun onAppStoppedWhileNotInPip(context: Context) {
        val snapshot = _state.value
        if (snapshot.currentVideo == null) return
        if (isBackgroundAllowed) {
            syncBackgroundService(context.applicationContext)
        } else {
            pausePlayback()
        }
    }

    fun onPipClosed(context: Context) {
        val snapshot = _state.value
        if (snapshot.currentVideo == null) return
        if (isBackgroundAllowed) {
            syncBackgroundService(context.applicationContext)
        } else {
            stopAndClear(context)
        }
    }

    private fun syncBackgroundService(context: Context) {
        if (_state.value.currentVideo == null) return
        val intent = Intent(context, MediaPlaybackService::class.java)
        try {
            if (isBackgroundAllowed) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(intent)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal menyinkronkan MediaPlaybackService: ${e.message}")
        }
    }

    private fun preloadNotificationBitmap(context: Context, url: String) {
        if (url.isBlank()) return
        val req = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        context.imageLoader.enqueue(req)
    }

    private fun recordYoutubeWatchedAsync(context: Context, item: YoutubeSearchItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppPreferences(context).recordWatchedItem(item)
            } catch (_: Throwable) {
            }
        }
    }

    private fun recordLocalPlayAsync(context: Context, video: DownloadedVideoEntity) {
        if (video.id <= 0L) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = VideoRepository(com.nunatv.app.data.local.NunaDatabase.getInstance(context).videoDao())
                repo.recordPlay(video.id)
            } catch (_: Throwable) {
            }
        }
    }
}

fun YoutubeSearchItem.toRemoteEntity(
    streamUrl: String = "",
    resolutionLabel: String = "1080p"
): DownloadedVideoEntity {
    val stableId = -abs(videoId.hashCode().toLong()).let { if (it == 0L) -1L else it }
    return DownloadedVideoEntity(
        id = stableId,
        videoId = videoId,
        originalUrl = watchUrl,
        title = title,
        author = channel,
        thumbnailUrl = thumbnailUrl,
        localFilePath = streamUrl,
        resolution = resolutionLabel,
        status = DownloadStatus.COMPLETED
    )
}
