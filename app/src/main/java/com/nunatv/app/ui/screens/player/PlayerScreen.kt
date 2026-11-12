package com.nunatv.app.ui.screens.player

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.preferences.AudioBoostLevel
import com.nunatv.app.downloader.StreamQualityOption
import com.nunatv.app.player.MediaPlaybackManager
import com.nunatv.app.player.MediaPlaybackState
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.util.DeviceUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    playbackState: MediaPlaybackState,
    onVideoChange: (DownloadedVideoEntity) -> Unit,
    onBack: () -> Unit,
    onMinimizeToMiniPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    downloadResolutionLabel: String = "1080p",
    isInPictureInPictureMode: Boolean = false,
    pipEnabled: Boolean = false,
    onEnterPip: (() -> Unit)? = null,
    onRetryStream: () -> Unit = {},
    onAudioBoostChange: ((AudioBoostLevel) -> Unit)? = null,
    onSelectStreamQuality: ((StreamQualityOption) -> Unit)? = null,
    onOpenChannel: ((channelId: String, channelName: String, videoId: String) -> Unit)? = null,
    onTriggerDownload: (() -> Unit)? = null
) {
    val currentVideo = playbackState.currentVideo ?: return
    val allVideos = playbackState.playlist
    val isRemoteStream = playbackState.isRemoteStream
    val isLiveStream = playbackState.isLiveStream
    val isShortStream = playbackState.isShortStream
    val isLoadingStream = playbackState.isLoadingStream
    val streamLoadError = playbackState.streamLoadError
    val audioBoostLevel = playbackState.audioBoostLevel
    val streamQualityOptions = playbackState.streamQualityOptions
    val activeStreamQuality = playbackState.activeStreamQuality
    val activeChannelId = playbackState.activeChannelId
    val isAutoplayEnabled = playbackState.isAutoplayEnabled

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTv = remember { DeviceUtils.isTvDevice(context) }

    // Playlist rekomendasi video selain video yang sedang diputar
    val recommendations = remember(currentVideo, allVideos) {
        allVideos.filter { it.id != currentVideo.id }
    }
    val currentIndex = remember(currentVideo.id, allVideos) {
        allVideos.indexOfFirst { it.id == currentVideo.id }
    }
    val nextVideo = remember(currentIndex, allVideos, recommendations) {
        if (currentIndex >= 0 && allVideos.size > 1) {
            allVideos[(currentIndex + 1) % allVideos.size]
        } else {
            recommendations.firstOrNull()
        }
    }
    val prevVideo = remember(currentIndex, allVideos, recommendations) {
        if (currentIndex >= 0 && allVideos.size > 1) {
            allVideos[(currentIndex - 1 + allVideos.size) % allVideos.size]
        } else {
            recommendations.lastOrNull()
        }
    }

    // State Video Player berasal dari MediaPlaybackManager
    val isPlaying = playbackState.isPlaying
    val currentPositionMs = playbackState.currentPositionMs
    val durationMs = playbackState.durationMs
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf(0L) }

    // State Kontrol & Autoplay
    var showControls by remember { mutableStateOf(!isRemoteStream) }
    var showRecommendations by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showAutoplayCountdown by remember { mutableStateOf(false) }
    var autoplayDismissedForCurrentVideo by remember(currentVideo.id, currentVideo.localFilePath) {
        mutableStateOf(false)
    }
    var countdownSeconds by remember { mutableIntStateOf(5) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var longPressHint by remember { mutableStateOf<String?>(null) }

    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }

    val playerFocusRequester = remember { FocusRequester() }
    val autoplayFocusRequester = remember { FocusRequester() }
    val firstRecFocusRequester = remember { FocusRequester() }
    val topMenuChannelFocusRequester = remember { FocusRequester() }
    val topMenuQualityFocusRequester = remember { FocusRequester() }

    val isOverlayActive = !isInPictureInPictureMode &&
        (showAutoplayCountdown || showRecommendations || showTopMenu)

    // Kembalikan fokus ke Video setiap kali popup/menu selesai atau ditutup
    LaunchedEffect(isOverlayActive, currentVideo.localFilePath) {
        if (!isOverlayActive) {
            delay(60)
            try {
                playerFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    // Tutup overlay sementara saat masuk mode PiP agar jendela PiP bersih
    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            showControls = false
            showTopMenu = false
            showRecommendations = false
            showAutoplayCountdown = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            longPressJob?.cancel()
        }
    }

    val exoPlayer = remember { MediaPlaybackManager.ensurePlayer(context) }

    // Munculkan Popup Autoplay 5 Detik Sebelum Video Selesai (saat Player terbuka dan bukan Live/PiP)
    LaunchedEffect(
        currentPositionMs,
        durationMs,
        isPlaying,
        isSeeking,
        isLiveStream,
        isAutoplayEnabled,
        isLoadingStream,
        streamLoadError,
        isInPictureInPictureMode,
        recommendations,
        autoplayDismissedForCurrentVideo
    ) {
        if (isPlaying &&
            !isSeeking &&
            !isLiveStream &&
            !isLoadingStream &&
            streamLoadError == null &&
            !isInPictureInPictureMode &&
            isAutoplayEnabled &&
            recommendations.isNotEmpty() &&
            durationMs > 6000L
        ) {
            val remainingMs = durationMs - currentPositionMs
            if (remainingMs in 1L..5500L && !showAutoplayCountdown && !autoplayDismissedForCurrentVideo) {
                showControls = false
                showAutoplayCountdown = true
            }
        }
    }

    // Auto-hide Controls setelah 4 detik tidak ada interaksi
    LaunchedEffect(showControls, isPlaying, showRecommendations, isLoadingStream, interactionTick) {
        if (showControls && isPlaying && !showRecommendations && !isLoadingStream) {
            delay(4000)
            showControls = false
        }
    }

    // Hitung Mundur Autoplay (5 Detik)
    LaunchedEffect(showAutoplayCountdown, nextVideo, isInPictureInPictureMode) {
        if (showAutoplayCountdown && nextVideo != null && !isInPictureInPictureMode) {
            countdownSeconds = 5
            while (countdownSeconds > 0 && showAutoplayCountdown && !isInPictureInPictureMode) {
                delay(1000)
                countdownSeconds--
            }
            if (showAutoplayCountdown && !isInPictureInPictureMode) {
                showAutoplayCountdown = false
                onVideoChange(nextVideo)
            }
        }
    }

    fun handleBackNavigation() {
        when {
            showTopMenu -> showTopMenu = false
            showRecommendations -> showRecommendations = false
            showAutoplayCountdown -> {
                autoplayDismissedForCurrentVideo = true
                showAutoplayCountdown = false
            }
            else -> onBack()
        }
    }

    // Tangani Tombol Back fisik / remote
    BackHandler {
        handleBackNavigation()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isShortStream, nextVideo?.id, prevVideo?.id, isInPictureInPictureMode) {
                if (isInPictureInPictureMode) return@pointerInput
                var totalDragY = 0f
                var handledDrag = false
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                        handledDrag = false
                    },
                    onVerticalDrag = { _, dragAmount ->
                        totalDragY += dragAmount
                        if (!handledDrag && isShortStream) {
                            if (totalDragY < -90f && nextVideo != null) {
                                handledDrag = true
                                onVideoChange(nextVideo)
                            } else if (totalDragY > 90f && prevVideo != null) {
                                handledDrag = true
                                onVideoChange(prevVideo)
                            }
                        } else if (!handledDrag && !isShortStream) {
                            if (totalDragY > 60f || totalDragY < -60f) {
                                handledDrag = true
                                showRecommendations = true
                                showControls = false
                            }
                        }
                    }
                )
            }
            .onPreviewKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val action = keyEvent.nativeKeyEvent.action

                // Tangani Escape (di Emulator) agar langsung berfungsi sebagai tombol Back
                if (code == KeyEvent.KEYCODE_ESCAPE) {
                    if (action == KeyEvent.ACTION_UP) {
                        handleBackNavigation()
                    }
                    return@onPreviewKeyEvent true
                }

                // Biarkan sistem menangani Back & Volume
                if (code == KeyEvent.KEYCODE_BACK ||
                    code == KeyEvent.KEYCODE_VOLUME_UP ||
                    code == KeyEvent.KEYCODE_VOLUME_DOWN ||
                    code == KeyEvent.KEYCODE_VOLUME_MUTE
                ) {
                    return@onPreviewKeyEvent false
                }

                // Jika popup (Download / Autoplay / Rekomendasi) sedang tampil,
                // biarkan fokus D-Pad sepenuhnya berada di dalam popup tersebut!
                if (isOverlayActive) {
                    return@onPreviewKeyEvent false
                }

                // --- MODE PEMUTARAN VIDEO ---
                // 1. OK / Enter / Media Play-Pause / Spasi -> Pause atau Mulai Video
                if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                    code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    code == KeyEvent.KEYCODE_MEDIA_PLAY ||
                    code == KeyEvent.KEYCODE_MEDIA_PAUSE ||
                    code == KeyEvent.KEYCODE_HEADSETHOOK ||
                    code == KeyEvent.KEYCODE_SPACE
                ) {
                    if (action == KeyEvent.ACTION_DOWN && keyEvent.nativeKeyEvent.repeatCount == 0) {
                        MediaPlaybackManager.togglePlayPause()
                        showControls = true
                        interactionTick++
                    }
                    return@onPreviewKeyEvent true
                }

                // 2. Panah Bawah -> Munculkan menu Rekomendasi (ringkas di bawah) & pindahkan fokus ke menu
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (action == KeyEvent.ACTION_DOWN && keyEvent.nativeKeyEvent.repeatCount == 0) {
                        if (recommendations.isNotEmpty()) {
                            showControls = false
                            showRecommendations = true
                        } else {
                            showControls = true
                            interactionTick++
                        }
                    }
                    return@onPreviewKeyEvent true
                }

                // 3. Panah Atas -> Munculkan Menu Atas (Channel Utama & Ganti Resolusi untuk stream online)
                if (code == KeyEvent.KEYCODE_DPAD_UP) {
                    if (action == KeyEvent.ACTION_DOWN && keyEvent.nativeKeyEvent.repeatCount == 0) {
                        showControls = false
                        showTopMenu = true
                    }
                    return@onPreviewKeyEvent true
                }

                // 4. Panah Kiri / Kanan:
                //    - Tekan singkat (< 1.5 detik) -> Skip -10s / +10s
                //    - Tahan lama (>= 1.5 detik) -> Pindah ke Video Sebelumnya / Berikutnya
                if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    val isRight = code == KeyEvent.KEYCODE_DPAD_RIGHT
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                            longPressJob?.cancel()
                            longPressTriggered = false
                            longPressJob = coroutineScope.launch {
                                delay(350)
                                longPressHint = if (isRight) {
                                    if (nextVideo != null) "Tahan untuk Video Berikutnya >>" else null
                                } else {
                                    if (prevVideo != null) "<< Tahan untuk Video Sebelumnya" else null
                                }
                                delay(1150) // Total 1.5 detik ditahan
                                longPressHint = null
                                val targetVideo = if (isRight) nextVideo else prevVideo
                                if (targetVideo != null) {
                                    longPressTriggered = true
                                    onVideoChange(targetVideo)
                                }
                            }
                        }
                        return@onPreviewKeyEvent true
                    } else if (action == KeyEvent.ACTION_UP) {
                        longPressJob?.cancel()
                        longPressJob = null
                        longPressHint = null
                        if (!longPressTriggered) {
                            val offset = if (isRight) 10000L else -10000L
                            MediaPlaybackManager.seekBy(offset)
                            showControls = true
                            interactionTick++
                        }
                        longPressTriggered = false
                        return@onPreviewKeyEvent true
                    }
                }

                false
            }
    ) {
        // 1. AndroidView Media3 PlayerView
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = !isOverlayActive && !isLoadingStream && streamLoadError == null }
                .focusRequester(playerFocusRequester)
                .focusable()
                .pointerInput(isInPictureInPictureMode, isLoadingStream, streamLoadError) {
                    detectTapGestures {
                        if (!isInPictureInPictureMode && !isLoadingStream && streamLoadError == null) {
                            showControls = !showControls
                            interactionTick++
                        }
                    }
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                    useController = false // Gunakan UI Overlay Compose Kustom yang responsif
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
                val isPortrait = context.resources.configuration.orientation ==
                    android.content.res.Configuration.ORIENTATION_PORTRAIT
                view.resizeMode = if (isShortStream && isPortrait) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        // 1b. Loading Stream atau Error Overlay langsung di dalam Player (Instant Open UX)
        if ((isLoadingStream || streamLoadError != null) && !isInPictureInPictureMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (currentVideo.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentVideo.thumbnailUrl,
                        contentDescription = currentVideo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f))
                )

                // Bar Atas agar pengguna tetap bisa kembali atau perkecil selama loading
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .dpadFocusable(onClick = onBack)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    if (!isTv) {
                        IconButton(
                            onClick = onMinimizeToMiniPlayer,
                            modifier = Modifier
                                .size(44.dp)
                                .dpadFocusable(onClick = onMinimizeToMiniPlayer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Perkecil ke Mini Player",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentVideo.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status Tengah: Spinner Loading atau Tombol Coba Lagi
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLoadingStream) {
                        CircularProgressIndicator(
                            color = Color(0xFFE50914),
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = "Menyiapkan video...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentVideo.title,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    } else if (streamLoadError != null) {
                        Text(
                            text = "Gagal memutar video",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = streamLoadError,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onRetryStream,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.dpadFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    style = FocusStyle.CTA,
                                    onClick = onRetryStream
                                )
                            ) {
                                Text(
                                    text = "Coba Lagi",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.dpadFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = onBack
                                )
                            ) {
                                Text(
                                    text = "Kembali",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Custom Player Controls Overlay (Responsif Portrait Mobile & Landscape/TV)
        AnimatedVisibility(
            visible = !isInPictureInPictureMode &&
                !isLoadingStream &&
                streamLoadError == null &&
                (showControls || !isPlaying) &&
                !showRecommendations,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = !isTv }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.68f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            ) {
                val isCompact = maxWidth < 600.dp
                val hasTrackSwitch = allVideos.size > 1 && recommendations.isNotEmpty()
                val sideBtnSize = if (isCompact) 44.dp else 52.dp
                val sideIconSize = if (isCompact) 26.dp else 34.dp
                val playBtnSize = if (isCompact) 58.dp else 70.dp
                val centerGap = if (isCompact) 14.dp else 24.dp

                // Top Bar: Baris 1 selalu Back + Judul + Perkecil/Popup (Mobile); Pills reflow ke Baris 2 di layar sempit
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(
                            horizontal = if (isCompact) 12.dp else 16.dp,
                            vertical = if (isCompact) 8.dp else 14.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .dpadFocusable(onClick = onBack)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentVideo.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isCompact) 15.sp else 17.sp,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitleText = buildString {
                                if (isLiveStream) {
                                    append("LIVE • ")
                                }
                                append(currentVideo.resolution)
                                append(" • ")
                                append(currentVideo.category)
                                if (currentVideo.playCount > 0) {
                                    append(" • Diputar ${currentVideo.playCount}x")
                                }
                            }
                            Text(
                                text = subtitleText,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isTv) {
                            if (pipEnabled && onEnterPip != null) {
                                IconButton(
                                    onClick = onEnterPip,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPictureAlt,
                                        contentDescription = "Mode Popup",
                                        tint = Color.White
                                    )
                                }
                            }
                            IconButton(
                                onClick = onMinimizeToMiniPlayer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Perkecil ke Mini Player",
                                    tint = Color.White
                                )
                            }
                        }

                        if (!isCompact) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerTopActionPill(
                                    icon = Icons.Default.Tune,
                                    label = if (isRemoteStream) "Channel / Resolusi" else "Channel",
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    onClick = {
                                        showControls = false
                                        showTopMenu = true
                                    }
                                )
                                if (onAudioBoostChange != null) {
                                    val boostActive = audioBoostLevel != AudioBoostLevel.OFF
                                    PlayerTopActionPill(
                                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                                        label = if (!boostActive) "Boost: OFF" else "Boost: ${audioBoostLevel.shortLabel}",
                                        containerColor = if (boostActive) Color(0xCC2A1416) else Color.White.copy(alpha = 0.12f),
                                        isActive = boostActive,
                                        onClick = {
                                            onAudioBoostChange(audioBoostLevel.next())
                                            interactionTick++
                                        }
                                    )
                                }
                                PlayerTopActionPill(
                                    icon = Icons.Default.Autorenew,
                                    label = if (isAutoplayEnabled) "Autoplay: ON" else "Autoplay: OFF",
                                    containerColor = if (isAutoplayEnabled) Color(0xCC182B1B) else Color.White.copy(alpha = 0.12f),
                                    isActive = isAutoplayEnabled,
                                    activeColor = Color(0xFF66BB6A),
                                    onClick = {
                                        MediaPlaybackManager.setAutoplayEnabled(!isAutoplayEnabled)
                                        interactionTick++
                                    }
                                )
                                if (onTriggerDownload != null) {
                                    PlayerTopActionPill(
                                        icon = Icons.Default.Download,
                                        label = "Download",
                                        containerColor = Color.White.copy(alpha = 0.14f),
                                        onClick = {
                                            onTriggerDownload()
                                            interactionTick++
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isCompact) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlayerTopActionPill(
                                icon = Icons.Default.Tune,
                                label = if (isRemoteStream) "Resolusi" else "Channel",
                                containerColor = Color.White.copy(alpha = 0.12f),
                                onClick = {
                                    showControls = false
                                    showTopMenu = true
                                }
                            )
                            if (onAudioBoostChange != null) {
                                val boostActive = audioBoostLevel != AudioBoostLevel.OFF
                                PlayerTopActionPill(
                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                    label = if (!boostActive) "Boost" else "Boost ${audioBoostLevel.shortLabel}",
                                    containerColor = if (boostActive) Color(0xCC2A1416) else Color.White.copy(alpha = 0.12f),
                                    isActive = boostActive,
                                    onClick = {
                                        onAudioBoostChange(audioBoostLevel.next())
                                        interactionTick++
                                    }
                                )
                            }
                            PlayerTopActionPill(
                                icon = Icons.Default.Autorenew,
                                label = if (isAutoplayEnabled) "Autoplay" else "Autoplay OFF",
                                containerColor = if (isAutoplayEnabled) Color(0xCC182B1B) else Color.White.copy(alpha = 0.12f),
                                isActive = isAutoplayEnabled,
                                activeColor = Color(0xFF66BB6A),
                                onClick = {
                                    MediaPlaybackManager.setAutoplayEnabled(!isAutoplayEnabled)
                                    interactionTick++
                                }
                            )
                            if (onTriggerDownload != null) {
                                PlayerTopActionPill(
                                    icon = Icons.Default.Download,
                                    label = "Download",
                                    containerColor = Color.White.copy(alpha = 0.14f),
                                    onClick = {
                                        onTriggerDownload()
                                        interactionTick++
                                    }
                                )
                            }
                        }
                    }
                }

                // Center Controls Simetris: [Prev] [Rewind 10s] [Play/Pause tepat di tengah] [Forward 10s] [Next]
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(centerGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasTrackSwitch && prevVideo != null) {
                        IconButton(
                            onClick = { onVideoChange(prevVideo) },
                            modifier = Modifier
                                .size(sideBtnSize)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .dpadFocusable(
                                    shape = CircleShape,
                                    onClick = { onVideoChange(prevVideo) }
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Video Sebelumnya",
                                tint = Color.White,
                                modifier = Modifier.size(sideIconSize)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(sideBtnSize))
                    }

                    IconButton(
                        onClick = {
                            MediaPlaybackManager.seekBy(-10000L)
                            interactionTick++
                        },
                        modifier = Modifier
                            .size(sideBtnSize)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .dpadFocusable(
                                shape = CircleShape,
                                onClick = {
                                    MediaPlaybackManager.seekBy(-10000L)
                                    interactionTick++
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Mundur 10 Detik",
                            tint = Color.White,
                            modifier = Modifier.size(sideIconSize)
                        )
                    }

                    // Tombol Utama Play / Pause selalu tepat di tengah
                    Surface(
                        onClick = {
                            MediaPlaybackManager.togglePlayPause()
                            interactionTick++
                        },
                        shape = CircleShape,
                        color = if (isTv) Color(0xFFE50914) else Color.Black.copy(alpha = 0.58f),
                        border = if (isTv) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .size(playBtnSize)
                            .dpadFocusable(
                                shape = CircleShape,
                                onClick = {
                                    MediaPlaybackManager.togglePlayPause()
                                    interactionTick++
                                }
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(if (isCompact) 32.dp else 44.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            MediaPlaybackManager.seekBy(10000L)
                            interactionTick++
                        },
                        modifier = Modifier
                            .size(sideBtnSize)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .dpadFocusable(
                                shape = CircleShape,
                                onClick = {
                                    MediaPlaybackManager.seekBy(10000L)
                                    interactionTick++
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Maju 10 Detik",
                            tint = Color.White,
                            modifier = Modifier.size(sideIconSize)
                        )
                    }

                    if (hasTrackSwitch && nextVideo != null) {
                        IconButton(
                            onClick = { onVideoChange(nextVideo) },
                            modifier = Modifier
                                .size(sideBtnSize)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .dpadFocusable(
                                    shape = CircleShape,
                                    onClick = { onVideoChange(nextVideo) }
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Video Berikutnya",
                                tint = Color.White,
                                modifier = Modifier.size(sideIconSize)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(sideBtnSize))
                    }
                }

                // Bottom Controls: Slider Posisi, Durasi, dan Tombol Buka Rekomendasi
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = if (isCompact) 14.dp else 20.dp,
                            vertical = if (isCompact) 10.dp else 16.dp
                        )
                ) {
                    if (isLiveStream && durationMs <= 0L) {
                        LinearProgressIndicator(
                            progress = { 1f },
                            color = Color(0xFFE50914),
                            trackColor = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    } else {
                        val maxRange = durationMs.toFloat().coerceAtLeast(1f)
                        val sliderPos = (if (isSeeking) seekPositionMs else currentPositionMs)
                            .toFloat()
                            .coerceIn(0f, maxRange)
                        // Slider Seekbar Waktu
                        Slider(
                            value = sliderPos,
                            onValueChange = {
                                isSeeking = true
                                seekPositionMs = it.toLong()
                            },
                            onValueChangeFinished = {
                                MediaPlaybackManager.seekTo(seekPositionMs)
                                isSeeking = false
                            },
                            valueRange = 0f..maxRange,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE50914),
                                activeTrackColor = Color(0xFFE50914),
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLiveStream) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE50914)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "${formatTimeMs(if (isSeeking) seekPositionMs else currentPositionMs)} / ${formatTimeMs(durationMs)}",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Tombol Buka Rekomendasi Video (D-Pad & Touch)
                        if (recommendations.isNotEmpty()) {
                            Surface(
                                onClick = { showRecommendations = !showRecommendations },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF212121),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .dpadFocusable(
                                        shape = RoundedCornerShape(16.dp),
                                        onClick = { showRecommendations = !showRecommendations }
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = "Rekomendasi",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rekomendasi (${recommendations.size})",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (showRecommendations) Icons.Default.KeyboardArrowDown else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Indikator Hold Kiri/Kanan (Video Sebelumnya / Berikutnya)
        AnimatedVisibility(
            visible = longPressHint != null && !isInPictureInPictureMode && !isLoadingStream,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC181818),
                border = BorderStroke(1.dp, Color(0xFFE50914))
            ) {
                Text(
                    text = longPressHint ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 3. Popup Kecil Autoplay Video Berikutnya di Sisi Bawah (Muncul 5 detik sebelum selesai)
        AnimatedVisibility(
            visible = showAutoplayCountdown && nextVideo != null && !isInPictureInPictureMode && !isLoadingStream,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            nextVideo?.let { targetNext ->
                LaunchedEffect(targetNext.id) {
                    delay(60)
                    try {
                        autoplayFocusRequester.requestFocus()
                    } catch (_: Exception) {
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xEE181818),
                    border = BorderStroke(1.dp, Color(0xFF3A3A3A)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .widthIn(max = 620.dp)
                        .fillMaxWidth()
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stackAutoplayButtons = maxWidth < 460.dp
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (stackAutoplayButtons) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = targetNext.thumbnailUrl,
                                            contentDescription = targetNext.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Berikutnya dalam ${countdownSeconds}s",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = targetNext.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                                        .focusGroup()
                                ) {
                                    Surface(
                                        onClick = {
                                            showAutoplayCountdown = false
                                            onVideoChange(targetNext)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE50914),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .focusRequester(autoplayFocusRequester)
                                            .dpadFocusable(
                                                shape = RoundedCornerShape(8.dp),
                                                style = FocusStyle.CTA,
                                                onClick = {
                                                    showAutoplayCountdown = false
                                                    onVideoChange(targetNext)
                                                }
                                            )
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                            Text(
                                                text = "Putar",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            autoplayDismissedForCurrentVideo = true
                                            showAutoplayCountdown = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF333333),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .dpadFocusable(
                                                shape = RoundedCornerShape(8.dp),
                                                style = FocusStyle.CTA,
                                                onClick = {
                                                    autoplayDismissedForCurrentVideo = true
                                                    showAutoplayCountdown = false
                                                }
                                            )
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                            Text(
                                                text = "Batal",
                                                color = Color.LightGray,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(88.dp)
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = targetNext.thumbnailUrl,
                                            contentDescription = targetNext.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Berikutnya dalam ${countdownSeconds}s",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = targetNext.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.focusGroup()
                                    ) {
                                        Surface(
                                            onClick = {
                                                showAutoplayCountdown = false
                                                onVideoChange(targetNext)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE50914),
                                            modifier = Modifier
                                                .focusRequester(autoplayFocusRequester)
                                                .dpadFocusable(
                                                    shape = RoundedCornerShape(8.dp),
                                                    style = FocusStyle.CTA,
                                                    onClick = {
                                                        showAutoplayCountdown = false
                                                        onVideoChange(targetNext)
                                                    }
                                                )
                                        ) {
                                            Text(
                                                text = "Putar",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }

                                        Surface(
                                            onClick = {
                                                autoplayDismissedForCurrentVideo = true
                                                showAutoplayCountdown = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF333333),
                                            modifier = Modifier.dpadFocusable(
                                                shape = RoundedCornerShape(8.dp),
                                                style = FocusStyle.CTA,
                                                onClick = {
                                                    autoplayDismissedForCurrentVideo = true
                                                    showAutoplayCountdown = false
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = "Batal",
                                                color = Color.LightGray,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Indikator hitung mundur 5 detik
                            LinearProgressIndicator(
                                progress = { (countdownSeconds / 5f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)),
                                color = Color(0xFFE50914),
                                trackColor = Color(0xFF2A2A2A)
                            )
                        }
                    }
                }
            }
        }

        // 4. Panel Rekomendasi Video Ringkas di Sisi Bawah (D-Pad Down / Swipe)
        AnimatedVisibility(
            visible = showRecommendations && recommendations.isNotEmpty() && !isInPictureInPictureMode && !isLoadingStream,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LaunchedEffect(Unit) {
                delay(60)
                try {
                    firstRecFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }

            Surface(
                color = Color(0xE6141414),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 10.dp)
                ) {
                    // Header Rekomendasi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rekomendasi Video Lainnya (Tekan Atas untuk tutup)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        IconButton(
                            onClick = { showRecommendations = false },
                            modifier = Modifier
                                .size(32.dp)
                                .focusProperties { canFocus = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Rekomendasi",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Carousel Rekomendasi Video
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusGroup()
                            .onPreviewKeyEvent { keyEvent ->
                                // Tekan Panah Atas mentok -> tutup menu rekomendasi & kembalikan fokus ke Video
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                                ) {
                                    showRecommendations = false
                                    true
                                } else {
                                    false
                                }
                            }
                    ) {
                        itemsIndexed(recommendations, key = { _, item -> item.id }) { index, video ->
                            Card(
                                onClick = {
                                    showRecommendations = false
                                    onVideoChange(video)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                                modifier = Modifier
                                    .width(176.dp)
                                    .then(
                                        if (index == 0) Modifier.focusRequester(firstRecFocusRequester)
                                        else Modifier
                                    )
                                    .dpadFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        onClick = {
                                            showRecommendations = false
                                            onVideoChange(video)
                                        }
                                    )
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                    ) {
                                        AsyncImage(
                                            model = video.thumbnailUrl,
                                            contentDescription = video.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Badge Kategori & Resolusi
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.75f),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = video.resolution,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = video.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = video.category,
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                            if (video.playCount > 0) {
                                                Text(
                                                    text = "Putar ${video.playCount}x",
                                                    color = Color(0xFFFF9800),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Menu Atas Pemutar Video (D-Pad UP): Ke Channel Utama & Ganti Resolusi (khusus stream online)
        AnimatedVisibility(
            visible = showTopMenu && !isInPictureInPictureMode && !isLoadingStream,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            var isChannelFocused by remember { mutableStateOf(false) }
            var isQualityFocused by remember { mutableStateOf(false) }
            val hasQualityOptions = isRemoteStream && streamQualityOptions.isNotEmpty()
            val activeResLabel = activeStreamQuality?.label ?: currentVideo.resolution

            LaunchedEffect(Unit) {
                delay(60)
                try {
                    topMenuChannelFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }

            Surface(
                color = Color(0xEE141414),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                        ) {
                            if (!hasQualityOptions || isQualityFocused) {
                                showTopMenu = false
                                true
                            } else if (isChannelFocused) {
                                try {
                                    topMenuQualityFocusRequester.requestFocus()
                                } catch (_: Exception) {
                                    showTopMenu = false
                                }
                                true
                            } else {
                                showTopMenu = false
                                true
                            }
                        } else if (hasQualityOptions &&
                            isQualityFocused &&
                            keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                        ) {
                            try {
                                topMenuChannelFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            true
                        } else {
                            false
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                onClick = {
                                    showTopMenu = false
                                    MediaPlaybackManager.stopAndClear(context)
                                    onOpenChannel?.invoke(activeChannelId, currentVideo.author, currentVideo.videoId)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF262626),
                                border = BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                modifier = Modifier
                                    .focusRequester(topMenuChannelFocusRequester)
                                    .onFocusChanged { isChannelFocused = it.isFocused }
                                    .dpadFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        style = FocusStyle.PILL,
                                        onClick = {
                                            showTopMenu = false
                                            MediaPlaybackManager.stopAndClear(context)
                                            onOpenChannel?.invoke(activeChannelId, currentVideo.author, currentVideo.videoId)
                                        }
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFE50914),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Ke Channel Utama",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = currentVideo.author.ifBlank { "Pemilik Video YouTube" },
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (!isTv) {
                                IconButton(
                                    onClick = { showTopMenu = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Menu",
                                        tint = Color.LightGray
                                    )
                                }
                            }
                        }

                        if (isTv) {
                            Text(
                                text = "Tekan Bawah untuk kembali ke video",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Opsi Ganti Resolusi Video hanya untuk stream YouTube online (video offline sesuai file unduhan)
                    if (hasQualityOptions) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Resolusi Video (Sedang diputar: $activeResLabel)",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusGroup()
                            ) {
                                itemsIndexed(streamQualityOptions) { index, option ->
                                    val isCurrentRes = activeStreamQuality?.height == option.height ||
                                        (activeStreamQuality == null && activeResLabel.startsWith(option.label))
                                    val isFirstOrCurrent = (activeStreamQuality?.height == option.height) ||
                                        (activeStreamQuality == null && index == 0)

                                    Surface(
                                        onClick = {
                                            if (!isCurrentRes) {
                                                onSelectStreamQuality?.invoke(option)
                                            }
                                            showTopMenu = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrentRes) Color(0xFFE50914) else Color(0xFF262626),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCurrentRes) Color(0xFFE50914) else Color(0xFF3E3E3E)
                                        ),
                                        modifier = Modifier
                                            .then(
                                                if (isFirstOrCurrent) Modifier.focusRequester(topMenuQualityFocusRequester)
                                                else Modifier
                                            )
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    isQualityFocused = true
                                                } else if (!focusState.hasFocus) {
                                                    isQualityFocused = false
                                                }
                                            }
                                            .dpadFocusable(
                                                shape = RoundedCornerShape(10.dp),
                                                style = FocusStyle.PILL,
                                                onClick = {
                                                    if (!isCurrentRes) {
                                                        onSelectStreamQuality?.invoke(option)
                                                    }
                                                    showTopMenu = false
                                                }
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            if (isCurrentRes) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                            }
                                            Text(
                                                text = if (isCurrentRes) "${option.label} • Aktif" else option.label,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (isCurrentRes) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTopActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
    isActive: Boolean = false,
    activeColor: Color = Color(0xFFE50914)
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .heightIn(min = 36.dp)
            .dpadFocusable(
                shape = RoundedCornerShape(10.dp),
                style = FocusStyle.PILL,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Format milidetik ke representasi string MM:SS atau HH:MM:SS
 */
private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
