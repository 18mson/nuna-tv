package com.nunatv.app.ui

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.nunatv.app.NunaTvApplication
import com.nunatv.app.data.local.DownloadStatus
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.preferences.AudioBoostLevel
import com.nunatv.app.data.preferences.VideoResolution
import com.nunatv.app.data.repository.VideoRepository
import com.nunatv.app.downloader.DownloadErrorEvent
import com.nunatv.app.downloader.DownloadEventBus
import com.nunatv.app.downloader.DownloadService
import com.nunatv.app.player.MediaPlaybackManager
import com.nunatv.app.player.MediaPlaybackService
import com.nunatv.app.player.MediaPlaybackState
import com.nunatv.app.ui.components.AdaptiveScaffold
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.MiniPlayerBar
import com.nunatv.app.ui.components.OnboardingDialog
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.ui.navigation.Screen
import com.nunatv.app.ui.screens.home.HomeScreen
import com.nunatv.app.ui.screens.home.HomeViewModel
import com.nunatv.app.ui.screens.player.PlayerScreen
import com.nunatv.app.ui.screens.settings.SettingsScreen
import com.nunatv.app.ui.screens.youtube.ChannelDestination
import com.nunatv.app.ui.screens.youtube.TvYoutubeScreen
import com.nunatv.app.ui.screens.youtube.YoutubeViewModel
import com.nunatv.app.util.DeviceUtils
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_PIP_CONTROL = "com.nunatv.app.action.PIP_CONTROL"
        private const val EXTRA_PIP_COMMAND = "pip_command"
        private const val PIP_CMD_PREV = "prev"
        private const val PIP_CMD_TOGGLE = "toggle"
        private const val PIP_CMD_NEXT = "next"
    }

    private val isTvDevice by lazy { DeviceUtils.isTvDevice(this) }
    private var isInPipModeState by mutableStateOf(false)
    private var pipEnabledSetting by mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val pipControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_PIP_CONTROL) return
            when (intent.getStringExtra(EXTRA_PIP_COMMAND)) {
                PIP_CMD_PREV -> MediaPlaybackManager.skipPrevious()
                PIP_CMD_TOGGLE -> MediaPlaybackManager.togglePlayPause()
                PIP_CMD_NEXT -> MediaPlaybackManager.skipNext()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Minta izin notifikasi untuk Android 13+ agar download dan media kontrol tampil
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Mencegah layar TV/Proyektor mati atau blank saat aplikasi Nuna TV terbuka
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ContextCompat.registerReceiver(
            this,
            pipControlReceiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        handleIncomingIntent(intent)

        val app = application as NunaTvApplication
        val repository = VideoRepository(app.database.videoDao())
        val appPreferences = AppPreferences(this)
        val homeViewModel = HomeViewModel(repository, appPreferences)
        val youtubeViewModel = YoutubeViewModel(applicationContext, appPreferences)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val allVideos by homeViewModel.videos.collectAsState()
            val completedVideos = remember(allVideos) {
                allVideos.filter {
                    it.status == DownloadStatus.COMPLETED &&
                        it.localFilePath.isNotEmpty() &&
                        File(it.localFilePath).exists()
                }
            }
            val completedVideosByVideoId = remember(completedVideos) {
                completedVideos.associateBy { it.videoId }
            }
            val activeDownloadIds = remember(allVideos) {
                allVideos
                    .filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                    .map { it.videoId }
                    .toSet()
            }
            val savedOrActiveVideoIds = remember(completedVideosByVideoId, activeDownloadIds) {
                completedVideosByVideoId.keys + activeDownloadIds
            }

            val defaultResolution by appPreferences.selectedResolution.collectAsState(initial = VideoResolution.RES_1080P)
            val audioBoostLevel by appPreferences.audioBoostLevel.collectAsState(initial = AudioBoostLevel.OFF)
            val backgroundPlaybackEnabled by appPreferences.backgroundPlaybackEnabled.collectAsState(initial = false)
            val pipEnabled by appPreferences.pipEnabled.collectAsState(initial = false)
            val hasSeenOnboarding by appPreferences.hasSeenOnboarding.collectAsState(initial = null)
            val playbackState by MediaPlaybackManager.state.collectAsState()

            var currentTab by rememberSaveable { mutableStateOf(Screen.Home.route) }
            var pendingChannelToOpen by remember { mutableStateOf<ChannelDestination?>(null) }
            var downloadErrorDialog by remember { mutableStateOf<DownloadErrorEvent?>(null) }
            var showOnboardingDialog by remember { mutableStateOf(false) }

            LaunchedEffect(
                backgroundPlaybackEnabled,
                audioBoostLevel,
                defaultResolution,
                completedVideosByVideoId
            ) {
                MediaPlaybackManager.configureEnvironment(
                    context = applicationContext,
                    backgroundAllowed = backgroundPlaybackEnabled,
                    boostLevel = audioBoostLevel,
                    preferredHeight = defaultResolution.preferredHeight,
                    resolutionLabel = defaultResolution.label,
                    completedLocalProvider = { completedVideosByVideoId }
                )
            }

            LaunchedEffect(
                pipEnabled,
                playbackState.isPlaying,
                playbackState.currentVideo?.id,
                playbackState.isMinimizedInApp,
                playbackState.isLoadingStream,
                playbackState.streamLoadError,
                playbackState.videoWidth,
                playbackState.videoHeight,
                playbackState.playlist.size
            ) {
                pipEnabledSetting = pipEnabled
                updatePipParamsIfSupported(playbackState)
            }

            LaunchedEffect(Unit) {
                DownloadEventBus.errors.collect { event ->
                    downloadErrorDialog = event
                }
            }

            val showFullPlayer = playbackState.currentVideo != null &&
                (!playbackState.isMinimizedInApp || isInPipModeState)

            // Tangani tombol back untuk kembali ke tab Home jika di tab lain
            BackHandler(enabled = !showFullPlayer && currentTab != Screen.Home.route) {
                currentTab = Screen.Home.route
            }

            AdaptiveScaffold(
                isTv = isTvDevice,
                currentRoute = if (showFullPlayer) "player" else currentTab,
                onNavigate = { targetScreen ->
                    currentTab = targetScreen.route
                },
                miniPlayerContent = {
                    if (!showFullPlayer && playbackState.currentVideo != null) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            hasNext = MediaPlaybackManager.hasNextVideo(),
                            onExpand = { MediaPlaybackManager.setMinimized(false) },
                            onTogglePlayPause = { MediaPlaybackManager.togglePlayPause() },
                            onNext = { MediaPlaybackManager.skipNext() },
                            onClose = { MediaPlaybackManager.stopAndClear(this@MainActivity) }
                        )
                    }
                }
            ) {
                if (showFullPlayer && playbackState.currentVideo != null) {
                    val playing = playbackState.currentVideo!!
                    val shouldOfferDownload = playbackState.isRemoteStream &&
                        !playbackState.isLiveStream &&
                        !playbackState.isLoadingStream &&
                        playbackState.streamLoadError == null &&
                        (playing.videoId !in savedOrActiveVideoIds)

                    PlayerScreen(
                        playbackState = playbackState,
                        downloadResolutionLabel = defaultResolution.label,
                        isInPictureInPictureMode = isInPipModeState,
                        pipEnabled = !isTvDevice && pipEnabled && supportsPip(),
                        onEnterPip = if (!isTvDevice && pipEnabled && supportsPip()) {
                            { enterPipManually(playbackState) }
                        } else {
                            null
                        },
                        onRetryStream = { MediaPlaybackManager.retryLoadStream() },
                        onAudioBoostChange = { nextLevel ->
                            MediaPlaybackManager.setAudioBoostLevel(nextLevel)
                            coroutineScope.launch {
                                appPreferences.setAudioBoostLevel(nextLevel)
                            }
                        },
                        onSelectStreamQuality = { option ->
                            MediaPlaybackManager.selectStreamQuality(option)
                        },
                        onOpenChannel = { chId, chName, vidId ->
                            MediaPlaybackManager.stopAndClear(this@MainActivity)
                            pendingChannelToOpen = ChannelDestination(
                                channelId = chId,
                                channelName = chName,
                                sourceVideoId = vidId
                            )
                            currentTab = Screen.YoutubeTv.route
                        },
                        onTriggerDownload = if (shouldOfferDownload) {
                            {
                                DownloadService.start(
                                    context = this@MainActivity,
                                    videoUrl = playing.originalUrl,
                                    resolutionName = defaultResolution.name
                                )
                                Toast.makeText(
                                    this@MainActivity,
                                    "Memulai download (${defaultResolution.label})...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            null
                        },
                        onVideoChange = { nextVideo ->
                            MediaPlaybackManager.switchToPlaylistVideo(this@MainActivity, nextVideo)
                        },
                        onMinimizeToMiniPlayer = {
                            MediaPlaybackManager.setMinimized(true)
                        },
                        onBack = {
                            MediaPlaybackManager.stopAndClear(this@MainActivity)
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Tab Home (Pustaka Offline)
                        if (currentTab == Screen.Home.route) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onPlayVideo = { video ->
                                    MediaPlaybackManager.openLocalVideo(
                                        context = this@MainActivity,
                                        video = video,
                                        allLocalVideos = completedVideos
                                    )
                                }
                            )
                        }

                        // 2. Tab YouTube Native (State & Posisi Scroll Persisten di ViewModel)
                        if (currentTab == Screen.YoutubeTv.route) {
                            TvYoutubeScreen(
                                youtubeViewModel = youtubeViewModel,
                                appPreferences = appPreferences,
                                completedVideosByVideoId = completedVideosByVideoId,
                                activeDownloadIds = activeDownloadIds,
                                pendingChannelToOpen = pendingChannelToOpen,
                                onChannelOpened = { pendingChannelToOpen = null },
                                onEnqueueNext = { queuedItem ->
                                    MediaPlaybackManager.enqueueNext(queuedItem)
                                },
                                onPlayYoutubeItem = { item, currentList ->
                                    val localMatch = completedVideosByVideoId[item.videoId]
                                    if (localMatch != null) {
                                        MediaPlaybackManager.openLocalVideo(
                                            context = this@MainActivity,
                                            video = localMatch,
                                            allLocalVideos = completedVideos
                                        )
                                    } else {
                                        MediaPlaybackManager.openYoutubeStream(
                                            context = this@MainActivity,
                                            item = item,
                                            currentList = currentList
                                        )
                                    }
                                }
                            )
                        }

                        // 3. Tab Settings
                        if (currentTab == Screen.Settings.route) {
                            SettingsScreen(
                                appPreferences = appPreferences,
                                onShowGuide = { showOnboardingDialog = true }
                            )
                        }
                    }
                }
            }

            // Panduan Penggunaan Nuna TV (Tampil otomatis 1x saat pertama buka atau dari Settings)
            if (showOnboardingDialog || hasSeenOnboarding == false) {
                OnboardingDialog(
                    isTv = isTvDevice,
                    onFinish = {
                        showOnboardingDialog = false
                        coroutineScope.launch {
                            appPreferences.setHasSeenOnboarding(true)
                        }
                    }
                )
            }

            // Popup Gagal Download (terutama jika penyimpanan tidak cukup)
            downloadErrorDialog?.let { errorEvent ->
                AlertDialog(
                    onDismissRequest = { downloadErrorDialog = null },
                    title = {
                        Text(
                            text = if (errorEvent.isStorageFull) "Penyimpanan Tidak Cukup" else "Download Gagal",
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = errorEvent.message,
                            color = Color.LightGray
                        )
                    },
                    confirmButton = {
                        if (errorEvent.isStorageFull) {
                            Button(
                                onClick = {
                                    downloadErrorDialog = null
                                    MediaPlaybackManager.stopAndClear(this@MainActivity)
                                    currentTab = Screen.Home.route
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                modifier = Modifier.dpadFocusable(
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        downloadErrorDialog = null
                                        MediaPlaybackManager.stopAndClear(this@MainActivity)
                                        currentTab = Screen.Home.route
                                    }
                                )
                            ) {
                                Text("Kelola di Home")
                            }
                        } else {
                            Button(
                                onClick = { downloadErrorDialog = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                modifier = Modifier.dpadFocusable(
                                    style = FocusStyle.CTA,
                                    onClick = { downloadErrorDialog = null }
                                )
                            ) {
                                Text("OK")
                            }
                        }
                    },
                    dismissButton = {
                        if (errorEvent.isStorageFull) {
                            TextButton(
                                onClick = { downloadErrorDialog = null },
                                modifier = Modifier.dpadFocusable(
                                    style = FocusStyle.CTA,
                                    onClick = { downloadErrorDialog = null }
                                )
                            ) {
                                Text("Tutup", color = Color.LightGray)
                            }
                        }
                    },
                    containerColor = Color(0xFF1E1E1E)
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MediaPlaybackManager.setAppInForeground(true)
    }

    override fun onStop() {
        super.onStop()
        MediaPlaybackManager.setAppInForeground(false)
        if (!isInPictureInPictureMode && !isChangingConfigurations) {
            MediaPlaybackManager.onAppStoppedWhileNotInPip(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipControlReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val current = MediaPlaybackManager.state.value
            if (canAutoEnterPip(current)) {
                val params = buildPipParams(current) ?: return
                try {
                    enterPictureInPictureMode(params)
                } catch (_: Throwable) {
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
        // Jika jendela PiP ditutup (tombol X): hentikan total bila background audio nonaktif,
        // atau biarkan audio tetap berjalan di layanan latar belakang bila background audio aktif
        if (!isInPictureInPictureMode &&
            !isChangingConfigurations &&
            lifecycle.currentState == Lifecycle.State.CREATED
        ) {
            MediaPlaybackManager.onPipClosed(this)
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(MediaPlaybackService.EXTRA_OPEN_PLAYER, false) == true) {
            if (MediaPlaybackManager.state.value.currentVideo != null) {
                MediaPlaybackManager.setMinimized(false)
            }
        }
    }

    private fun supportsPip(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun canAutoEnterPip(playbackState: MediaPlaybackState): Boolean {
        return !isTvDevice &&
            pipEnabledSetting &&
            supportsPip() &&
            playbackState.currentVideo != null &&
            !playbackState.isMinimizedInApp &&
            !playbackState.isLoadingStream &&
            playbackState.streamLoadError == null &&
            playbackState.isPlaying
    }

    private fun enterPipManually(playbackState: MediaPlaybackState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !supportsPip()) return
        if (playbackState.currentVideo == null || playbackState.isLoadingStream || playbackState.streamLoadError != null) return
        val params = buildPipParams(playbackState) ?: return
        try {
            enterPictureInPictureMode(params)
        } catch (_: Throwable) {
        }
    }

    private fun updatePipParamsIfSupported(playbackState: MediaPlaybackState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !supportsPip()) return
        val params = buildPipParams(playbackState) ?: return
        try {
            setPictureInPictureParams(params)
        } catch (_: Throwable) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(playbackState: MediaPlaybackState): PictureInPictureParams? {
        val ratio = if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
            (playbackState.videoWidth.toFloat() / playbackState.videoHeight.toFloat()).coerceIn(0.5f, 2.3f)
        } else {
            16f / 9f
        }
        val aspectRatio = Rational((ratio * 1000).toInt(), 1000)

        val actions = mutableListOf<RemoteAction>()
        if (playbackState.playlist.size > 1) {
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                    "Sebelumnya",
                    "Video Sebelumnya",
                    pipCommandPendingIntent(PIP_CMD_PREV, 201)
                )
            )
        }
        actions.add(
            RemoteAction(
                Icon.createWithResource(
                    this,
                    if (playbackState.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                ),
                if (playbackState.isPlaying) "Jeda" else "Putar",
                if (playbackState.isPlaying) "Jeda Video" else "Putar Video",
                pipCommandPendingIntent(PIP_CMD_TOGGLE, 202)
            )
        )
        if (playbackState.playlist.size > 1) {
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_next),
                    "Berikutnya",
                    "Video Berikutnya",
                    pipCommandPendingIntent(PIP_CMD_NEXT, 203)
                )
            )
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(canAutoEnterPip(playbackState))
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun pipCommandPendingIntent(command: String, requestCode: Int): PendingIntent {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            setPackage(packageName)
            putExtra(EXTRA_PIP_COMMAND, command)
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
