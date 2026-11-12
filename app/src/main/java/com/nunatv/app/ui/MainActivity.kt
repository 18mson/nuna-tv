package com.nunatv.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nunatv.app.NunaTvApplication
import com.nunatv.app.data.local.DownloadStatus
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.preferences.AudioBoostLevel
import com.nunatv.app.data.preferences.VideoResolution
import com.nunatv.app.data.repository.VideoRepository
import com.nunatv.app.data.repository.YoutubeSearchItem
import com.nunatv.app.data.repository.YoutubeSearchRepository
import com.nunatv.app.downloader.DownloadErrorEvent
import com.nunatv.app.downloader.DownloadEventBus
import com.nunatv.app.downloader.DownloadService
import com.nunatv.app.downloader.StreamQualityOption
import com.nunatv.app.downloader.YoutubeDlManager
import com.nunatv.app.ui.components.AdaptiveScaffold
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.ui.navigation.Screen
import com.nunatv.app.ui.screens.home.HomeScreen
import com.nunatv.app.ui.screens.home.HomeViewModel
import com.nunatv.app.ui.screens.player.PlayerScreen
import com.nunatv.app.ui.screens.settings.SettingsScreen
import com.nunatv.app.ui.screens.youtube.ChannelDestination
import com.nunatv.app.ui.screens.youtube.TvYoutubeScreen
import com.nunatv.app.util.DeviceUtils
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Minta izin notifikasi untuk Android 13+ agar download progress tampil
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Mencegah layar TV/Proyektor mati atau blank saat aplikasi Nuna TV terbuka
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = application as NunaTvApplication
        val repository = VideoRepository(app.database.videoDao())
        val appPreferences = AppPreferences(this)
        val homeViewModel = HomeViewModel(repository, appPreferences)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val allVideos by homeViewModel.videos.collectAsState()
            val completedVideos = remember(allVideos) {
                allVideos.filter { it.status == DownloadStatus.COMPLETED && it.localFilePath.isNotEmpty() && File(it.localFilePath).exists() }
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

            var currentTab by rememberSaveable { mutableStateOf(Screen.Home.route) }
            var activePlayingVideo by remember { mutableStateOf<DownloadedVideoEntity?>(null) }
            var isPlayingRemoteStream by remember { mutableStateOf(false) }
            var remoteQueue by remember { mutableStateOf<List<DownloadedVideoEntity>>(emptyList()) }
            var upNextQueue by remember { mutableStateOf<List<DownloadedVideoEntity>>(emptyList()) }
            var activeStreamAudioUrl by remember { mutableStateOf<String?>(null) }
            var streamQualityOptions by remember { mutableStateOf<List<StreamQualityOption>>(emptyList()) }
            var activeStreamQuality by remember { mutableStateOf<StreamQualityOption?>(null) }
            var activeChannelId by remember { mutableStateOf("") }
            var channelIdByVideoId by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var pendingChannelToOpen by remember { mutableStateOf<ChannelDestination?>(null) }
            var isResolvingNextStream by remember { mutableStateOf(false) }
            var downloadErrorDialog by remember { mutableStateOf<DownloadErrorEvent?>(null) }
            val isTv = remember { DeviceUtils.isTvDevice(this) }

            LaunchedEffect(Unit) {
                DownloadEventBus.errors.collect { event ->
                    downloadErrorDialog = event
                }
            }

            // Ambil daftar video terkait YouTube di background untuk fitur Autoplay pada tab YouTube Native
            LaunchedEffect(activePlayingVideo?.videoId, isPlayingRemoteStream) {
                val playing = activePlayingVideo
                if (isPlayingRemoteStream && playing != null) {
                    val related = YoutubeSearchRepository.fetchRelatedVideos(playing.videoId)
                    if (related.isNotEmpty()) {
                        val updatedMap = channelIdByVideoId.toMutableMap()
                        related.forEach { item ->
                            if (item.channelId.isNotBlank()) {
                                updatedMap[item.videoId] = item.channelId
                            }
                        }
                        channelIdByVideoId = updatedMap
                        remoteQueue = related.map { item ->
                            item.toRemoteEntity(resolutionLabel = defaultResolution.label)
                        }
                    }
                }
            }

            // Tangani tombol back untuk kembali ke tab Home jika di tab lain
            BackHandler(enabled = activePlayingVideo == null && currentTab != Screen.Home.route) {
                currentTab = Screen.Home.route
            }

            AdaptiveScaffold(
                isTv = isTv,
                currentRoute = if (activePlayingVideo != null) "player" else currentTab,
                onNavigate = { targetScreen ->
                    currentTab = targetScreen.route
                }
            ) {
                if (activePlayingVideo != null) {
                    val playing = activePlayingVideo!!
                    val shouldOfferDownload = isPlayingRemoteStream && (playing.videoId !in savedOrActiveVideoIds)
                    val activePlaylist = remember(playing, isPlayingRemoteStream, upNextQueue, remoteQueue, completedVideos) {
                        if (isPlayingRemoteStream) {
                            val combinedTail = (upNextQueue + remoteQueue)
                                .distinctBy { it.videoId }
                                .filter { it.videoId != playing.videoId }
                            listOf(playing) + combinedTail
                        } else {
                            completedVideos
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        PlayerScreen(
                            currentVideo = playing,
                            allVideos = activePlaylist,
                            isRemoteStream = isPlayingRemoteStream,
                            downloadResolutionLabel = defaultResolution.label,
                            audioBoostLevel = audioBoostLevel,
                            onAudioBoostChange = { nextLevel ->
                                coroutineScope.launch {
                                    appPreferences.setAudioBoostLevel(nextLevel)
                                }
                            },
                            activeAudioStreamUrl = if (isPlayingRemoteStream) activeStreamAudioUrl else null,
                            streamQualityOptions = if (isPlayingRemoteStream) streamQualityOptions else emptyList(),
                            activeStreamQuality = if (isPlayingRemoteStream) activeStreamQuality else null,
                            activeChannelId = activeChannelId.ifBlank { channelIdByVideoId[playing.videoId].orEmpty() },
                            onSelectStreamQuality = { option ->
                                activeStreamAudioUrl = option.audioUrl
                                activeStreamQuality = option
                                activePlayingVideo = playing.copy(
                                    localFilePath = option.videoUrl,
                                    resolution = option.label
                                )
                            },
                            onOpenChannel = { chId, chName, vidId ->
                                activePlayingVideo = null
                                isPlayingRemoteStream = false
                                activeStreamAudioUrl = null
                                streamQualityOptions = emptyList()
                                activeStreamQuality = null
                                activeChannelId = ""
                                isResolvingNextStream = false
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
                            } else null,
                            onVideoChange = { nextVideo ->
                                upNextQueue = upNextQueue.filterNot { it.videoId == nextVideo.videoId }
                                if (!isPlayingRemoteStream) {
                                    activeStreamAudioUrl = null
                                    streamQualityOptions = emptyList()
                                    activeStreamQuality = null
                                    activeChannelId = ""
                                    homeViewModel.recordPlay(nextVideo)
                                    activePlayingVideo = nextVideo
                                } else {
                                    val localMatch = completedVideosByVideoId[nextVideo.videoId]
                                    if (localMatch != null) {
                                        isPlayingRemoteStream = false
                                        activeStreamAudioUrl = null
                                        streamQualityOptions = emptyList()
                                        activeStreamQuality = null
                                        activeChannelId = channelIdByVideoId[localMatch.videoId].orEmpty()
                                        coroutineScope.launch {
                                            appPreferences.recordWatchedVideo(localMatch.videoId, localMatch.title)
                                        }
                                        homeViewModel.recordPlay(localMatch)
                                        activePlayingVideo = localMatch
                                    } else if (!isResolvingNextStream) {
                                        isResolvingNextStream = true
                                        coroutineScope.launch {
                                            val resolved = YoutubeDlManager.resolvePlayableVideo(
                                                context = this@MainActivity,
                                                videoUrl = nextVideo.originalUrl,
                                                preferredHeight = defaultResolution.preferredHeight
                                            )
                                            isResolvingNextStream = false
                                            if (resolved != null) {
                                                val resolvedChannelId = channelIdByVideoId[nextVideo.videoId]
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?: resolved.channelId
                                                if (resolvedChannelId.isNotBlank()) {
                                                    channelIdByVideoId = channelIdByVideoId + (nextVideo.videoId to resolvedChannelId)
                                                }
                                                appPreferences.recordWatchedItem(
                                                    YoutubeSearchItem(
                                                        videoId = nextVideo.videoId,
                                                        title = nextVideo.title,
                                                        channel = nextVideo.author.ifBlank { resolved.channel },
                                                        durationText = resolved.durationText,
                                                        viewCountText = "",
                                                        thumbnailUrl = nextVideo.thumbnailUrl,
                                                        channelId = resolvedChannelId
                                                    )
                                                )
                                                activeStreamAudioUrl = resolved.audioStreamUrl
                                                activeStreamQuality = resolved.activeQuality
                                                streamQualityOptions = resolved.qualityOptions
                                                activeChannelId = resolvedChannelId
                                                activePlayingVideo = nextVideo.copy(
                                                    localFilePath = resolved.streamUrl,
                                                    resolution = resolved.activeQuality.label,
                                                    author = nextVideo.author.ifBlank { resolved.channel }
                                                )
                                            } else {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Gagal memuat video berikutnya.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            onBack = {
                                activePlayingVideo = null
                                isPlayingRemoteStream = false
                                remoteQueue = emptyList()
                                activeStreamAudioUrl = null
                                streamQualityOptions = emptyList()
                                activeStreamQuality = null
                                activeChannelId = ""
                                isResolvingNextStream = false
                            }
                        )

                        if (isResolvingNextStream) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFFE50914))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Memuat video berikutnya...", color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Tab Home (Video Tersimpan)
                        if (currentTab == Screen.Home.route) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onPlayVideo = { video ->
                                    isPlayingRemoteStream = false
                                    remoteQueue = emptyList()
                                    activeStreamAudioUrl = null
                                    streamQualityOptions = emptyList()
                                    activeStreamQuality = null
                                    activeChannelId = ""
                                    homeViewModel.recordPlay(video)
                                    activePlayingVideo = video
                                }
                            )
                        }

                        // 2. Tab YouTube Native (Autoplay, Pencarian QWERTY, Riwayat 20 Video, Channel, Filter)
                        if (currentTab == Screen.YoutubeTv.route) {
                            TvYoutubeScreen(
                                appPreferences = appPreferences,
                                completedVideosByVideoId = completedVideosByVideoId,
                                activeDownloadIds = activeDownloadIds,
                                pendingChannelToOpen = pendingChannelToOpen,
                                onChannelOpened = { pendingChannelToOpen = null },
                                onPlayLocalVideo = { localVideo ->
                                    isPlayingRemoteStream = false
                                    remoteQueue = emptyList()
                                    activeStreamAudioUrl = null
                                    streamQualityOptions = emptyList()
                                    activeStreamQuality = null
                                    activeChannelId = ""
                                    homeViewModel.recordPlay(localVideo)
                                    activePlayingVideo = localVideo
                                },
                                onEnqueueNext = { queuedItem ->
                                    if (queuedItem.channelId.isNotBlank()) {
                                        channelIdByVideoId = channelIdByVideoId + (queuedItem.videoId to queuedItem.channelId)
                                    }
                                    val entity = queuedItem.toRemoteEntity(resolutionLabel = defaultResolution.label)
                                    upNextQueue = (upNextQueue.filterNot { it.videoId == entity.videoId } + entity)
                                },
                                onPlayStream = { item, resolved, currentList ->
                                    isPlayingRemoteStream = true
                                    val updatedMap = channelIdByVideoId.toMutableMap()
                                    if (item.channelId.isNotBlank()) {
                                        updatedMap[item.videoId] = item.channelId
                                    } else if (resolved.channelId.isNotBlank()) {
                                        updatedMap[item.videoId] = resolved.channelId
                                    }
                                    currentList.forEach { listItem ->
                                        if (listItem.channelId.isNotBlank()) {
                                            updatedMap[listItem.videoId] = listItem.channelId
                                        }
                                    }
                                    channelIdByVideoId = updatedMap

                                    activeStreamAudioUrl = resolved.audioStreamUrl
                                    activeStreamQuality = resolved.activeQuality
                                    streamQualityOptions = resolved.qualityOptions
                                    activeChannelId = item.channelId.ifBlank { resolved.channelId }
                                    remoteQueue = currentList
                                        .filter { it.videoId != item.videoId }
                                        .map { it.toRemoteEntity(resolutionLabel = defaultResolution.label) }
                                    activePlayingVideo = item.toRemoteEntity(
                                        streamUrl = resolved.streamUrl,
                                        resolutionLabel = resolved.activeQuality.label
                                    )
                                }
                            )
                        }

                        // 3. Tab Settings
                        if (currentTab == Screen.Settings.route) {
                            SettingsScreen(appPreferences = appPreferences)
                        }
                    }
                }
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
                                    activePlayingVideo = null
                                    isPlayingRemoteStream = false
                                    currentTab = Screen.Home.route
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                modifier = Modifier.dpadFocusable(
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        downloadErrorDialog = null
                                        activePlayingVideo = null
                                        isPlayingRemoteStream = false
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
}

private fun YoutubeSearchItem.toRemoteEntity(
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
