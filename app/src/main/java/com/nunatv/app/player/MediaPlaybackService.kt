package com.nunatv.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nunatv.app.R
import com.nunatv.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MediaPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat

    private var cachedArtworkVideoId: String? = null
    private var cachedArtworkBitmap: Bitmap? = null
    private var lastNotificationKey: String = ""

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel()

        mediaSession = MediaSessionCompat(this, "NunaTvMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    MediaPlaybackManager.resumePlayback()
                }

                override fun onPause() {
                    MediaPlaybackManager.pausePlayback()
                }

                override fun onSkipToNext() {
                    MediaPlaybackManager.skipNext()
                }

                override fun onSkipToPrevious() {
                    MediaPlaybackManager.skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    MediaPlaybackManager.seekTo(pos)
                }

                override fun onStop() {
                    MediaPlaybackManager.stopAndClear(this@MediaPlaybackService)
                }
            })
            isActive = true
        }

        serviceScope.launch {
            MediaPlaybackManager.state.collect { state ->
                val video = state.currentVideo
                if (video == null) {
                    ServiceCompat.stopForeground(this@MediaPlaybackService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collect
                }

                if (video.videoId != cachedArtworkVideoId && video.thumbnailUrl.isNotBlank()) {
                    loadArtworkAsync(video.videoId, video.thumbnailUrl)
                }

                updateMediaSession(state)

                val key = "${video.videoId}|${state.isPlaying}|${state.isLoadingStream}|${state.durationMs}|${cachedArtworkVideoId == video.videoId}"
                if (key != lastNotificationKey) {
                    lastNotificationKey = key
                    val notification = buildNotification(state)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> MediaPlaybackManager.togglePlayPause()
            ACTION_NEXT -> MediaPlaybackManager.skipNext()
            ACTION_PREV -> MediaPlaybackManager.skipPrevious()
            ACTION_STOP -> {
                MediaPlaybackManager.stopAndClear(this)
                return START_NOT_STICKY
            }
        }

        val initialNotification = buildNotification(MediaPlaybackManager.state.value)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            initialNotification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.isActive = false
        mediaSession.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateMediaSession(state: MediaPlaybackState) {
        val video = state.currentVideo ?: return
        val pbState = when {
            state.isLoadingStream -> PlaybackStateCompat.STATE_BUFFERING
            state.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }

        val actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(pbState, state.currentPositionMs, if (state.isPlaying) 1.0f else 0f)
                .build()
        )

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, video.title)
            .putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                if (state.isLiveStream) "LIVE • ${video.author.ifBlank { "Nuna TV" }}" else video.author.ifBlank { "Nuna TV" }
            )
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                if (!state.isLiveStream && state.durationMs > 0L) state.durationMs else -1L
            )
        if (cachedArtworkVideoId == video.videoId && cachedArtworkBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cachedArtworkBitmap)
        }
        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun buildNotification(state: MediaPlaybackState): Notification {
        val video = state.currentVideo
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PLAYER, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPending = serviceActionPendingIntent(ACTION_PREV, 101)
        val playPausePending = serviceActionPendingIntent(ACTION_PLAY_PAUSE, 102)
        val nextPending = serviceActionPendingIntent(ACTION_NEXT, 103)
        val stopPending = serviceActionPendingIntent(ACTION_STOP, 104)

        val playPauseIcon = if (state.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseLabel = if (state.isPlaying) "Jeda" else "Putar"

        val subtitle = when {
            state.isLoadingStream -> "Menyiapkan stream..."
            video != null -> video.author.ifBlank { "Nuna TV" }
            else -> "Pemutar Latar Belakang"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(video?.title ?: "Nuna TV")
            .setContentText(subtitle)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPending)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Sebelumnya", prevPending)
            .addAction(playPauseIcon, playPauseLabel, playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Berikutnya", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tutup", stopPending)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (video != null && cachedArtworkVideoId == video.videoId && cachedArtworkBitmap != null) {
            builder.setLargeIcon(cachedArtworkBitmap)
        }

        return builder.build()
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadArtworkAsync(videoId: String, url: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .allowHardware(false)
                    .size(512, 288)
                    .build()
                val result = applicationContext.imageLoader.execute(request)
                val bmp = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    launch(Dispatchers.Main.immediate) {
                        cachedArtworkVideoId = videoId
                        cachedArtworkBitmap = bmp
                        val currentState = MediaPlaybackManager.state.value
                        if (currentState.currentVideo?.videoId == videoId) {
                            updateMediaSession(currentState)
                            notificationManager.notify(NOTIFICATION_ID, buildNotification(currentState))
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pemutar Media Latar Belakang",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kontrol pemutaran video dan audio Nuna TV"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "nuna_media_playback"
        const val NOTIFICATION_ID = 2002
        const val EXTRA_OPEN_PLAYER = "extra_open_player"

        const val ACTION_PLAY_PAUSE = "com.nunatv.app.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.nunatv.app.ACTION_NEXT"
        const val ACTION_PREV = "com.nunatv.app.ACTION_PREV"
        const val ACTION_STOP = "com.nunatv.app.ACTION_STOP"
    }
}
