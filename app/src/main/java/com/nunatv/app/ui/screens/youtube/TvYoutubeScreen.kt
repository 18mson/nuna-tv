package com.nunatv.app.ui.screens.youtube

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.preferences.VideoResolution
import com.nunatv.app.data.repository.YoutubeContentFilter
import com.nunatv.app.data.repository.YoutubeSearchItem
import com.nunatv.app.data.repository.YoutubeSearchRepository
import com.nunatv.app.data.repository.YoutubeSortOption
import com.nunatv.app.downloader.DownloadService
import com.nunatv.app.downloader.ResolvedYoutubeVideo
import com.nunatv.app.downloader.YoutubeDlManager
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.TvChip
import com.nunatv.app.ui.components.TvSearchDialog
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.util.DeviceUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChannelDestination(
    val channelId: String = "",
    val channelName: String = "",
    val sourceVideoId: String = ""
)

@Composable
fun TvYoutubeScreen(
    appPreferences: AppPreferences,
    completedVideosByVideoId: Map<String, DownloadedVideoEntity> = emptyMap(),
    activeDownloadIds: Set<String> = emptySet(),
    pendingChannelToOpen: ChannelDestination? = null,
    onChannelOpened: () -> Unit = {},
    onPlayLocalVideo: (DownloadedVideoEntity) -> Unit = {},
    onEnqueueNext: (YoutubeSearchItem) -> Unit = {},
    onPlayStream: (item: YoutubeSearchItem, resolved: ResolvedYoutubeVideo, currentList: List<YoutubeSearchItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isTv = remember { DeviceUtils.isTvDevice(context) }

    val recentSearches by appPreferences.recentSearchQueries.collectAsState(initial = emptyList())
    val lastWatchedId by appPreferences.lastWatchedVideoId.collectAsState(initial = null)
    val watchHistory by appPreferences.watchHistory.collectAsState(initial = emptyList())
    val selectedResolution by appPreferences.selectedResolution.collectAsState(initial = VideoResolution.RES_1080P)

    val quickCategories = remember {
        listOf(
            "Trending Indonesia" to "Trending",
            "Musik Indonesia Terbaru" to "Musik",
            "Kartun Anak Indonesia" to "Anak",
            "Film Indonesia Full Movie" to "Film",
            "Berita Terkini" to "Berita",
            "Podcast Indonesia" to "Podcast"
        )
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showTvSearchDialog by remember { mutableStateOf(false) }
    var showPasteLinkDialog by remember { mutableStateOf(false) }
    var pastedUrl by remember { mutableStateOf("") }
    var linkErrorMessage by remember { mutableStateOf<String?>(null) }
    var isResolvingPastedLink by remember { mutableStateOf(false) }
    var activeCategory by rememberSaveable { mutableStateOf("FOR_YOU") }
    var contentFilter by rememberSaveable { mutableStateOf(YoutubeContentFilter.ALL) }
    var sortOption by rememberSaveable { mutableStateOf(YoutubeSortOption.DEFAULT) }
    var activeChannel by remember { mutableStateOf<ChannelDestination?>(null) }
    var menuItemForCard by remember { mutableStateOf<YoutubeSearchItem?>(null) }
    var results by remember { mutableStateOf<List<YoutubeSearchItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var resolvingVideoId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val displayedResults = remember(results, watchHistory, activeCategory, contentFilter, sortOption) {
        val baseList = if (activeCategory == "WATCH_HISTORY") watchHistory else results
        YoutubeSearchRepository.filterAndSortItems(baseList, contentFilter, sortOption)
    }

    fun readClipboardText(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            return clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
        }
        return ""
    }

    fun startDownloadItem(videoUrl: String, videoId: String) {
        if (videoId in completedVideosByVideoId) {
            Toast.makeText(context, "Video ini sudah tersimpan di Home.", Toast.LENGTH_SHORT).show()
            return
        }
        if (videoId in activeDownloadIds) {
            Toast.makeText(context, "Video ini sedang dalam proses download.", Toast.LENGTH_SHORT).show()
            return
        }
        DownloadService.start(
            context = context,
            videoUrl = YoutubeDlManager.cleanUrl(videoUrl),
            resolutionName = selectedResolution.name
        )
        Toast.makeText(
            context,
            "Memulai download (${selectedResolution.label})...",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun openChannelPage(dest: ChannelDestination) {
        activeCategory = "CHANNEL"
        searchQuery = ""
        contentFilter = YoutubeContentFilter.ALL
        activeChannel = dest
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            var targetId = dest.channelId
            var targetName = dest.channelName
            if ((targetId.isBlank() || !targetId.startsWith("UC")) && dest.sourceVideoId.isNotBlank()) {
                val owner = YoutubeSearchRepository.fetchVideoChannelInfo(dest.sourceVideoId)
                if (owner != null) {
                    if (owner.first.isNotBlank()) targetId = owner.first
                    if (owner.second.isNotBlank()) targetName = owner.second
                }
            }
            activeChannel = ChannelDestination(
                channelId = targetId,
                channelName = targetName.ifBlank { "Channel YouTube" },
                sourceVideoId = dest.sourceVideoId
            )
            val res = YoutubeSearchRepository.getChannelVideos(context, targetId, targetName)
            res.fold(
                onSuccess = { list ->
                    results = list
                    if (list.isEmpty()) {
                        errorMessage = "Belum ada video di channel ini."
                    }
                },
                onFailure = { err ->
                    errorMessage = "Gagal memuat channel: ${err.message ?: "Periksa koneksi internet"}"
                }
            )
            isLoading = false
        }
    }

    fun playYoutubeItem(item: YoutubeSearchItem) {
        if (resolvingVideoId != null) return
        val savedEntity = completedVideosByVideoId[item.videoId]
        if (savedEntity != null) {
            coroutineScope.launch {
                appPreferences.recordWatchedItem(item)
            }
            onPlayLocalVideo(savedEntity)
            return
        }
        resolvingVideoId = item.videoId
        coroutineScope.launch {
            val resolved = YoutubeDlManager.resolvePlayableVideo(
                context = context,
                videoUrl = item.watchUrl,
                preferredHeight = selectedResolution.preferredHeight
            )
            resolvingVideoId = null
            if (resolved != null) {
                val enriched = item.copy(
                    title = item.title.ifBlank { resolved.title },
                    channel = item.channel.takeIf { it.isNotBlank() && it != "YouTube" } ?: resolved.channel,
                    channelId = item.channelId.ifBlank { resolved.channelId },
                    durationText = item.durationText.ifBlank { resolved.durationText },
                    thumbnailUrl = item.thumbnailUrl.ifBlank { resolved.thumbnailUrl }
                )
                appPreferences.recordWatchedItem(enriched)
                onPlayStream(enriched, resolved, displayedResults.ifEmpty { results })
            } else {
                Toast.makeText(
                    context,
                    "Gagal memuat stream video. Silakan coba video lain.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun loadRecommendedFeed() {
        activeChannel = null
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val lastId = appPreferences.lastWatchedVideoId.first()
            val lastTitle = appPreferences.lastWatchedVideoTitle.first()
            val searches = appPreferences.recentSearchQueries.first()
            val res = YoutubeSearchRepository.getRecommendedFeed(
                context = context,
                lastVideoId = lastId,
                lastVideoTitle = lastTitle,
                recentSearches = searches
            )
            res.fold(
                onSuccess = { list ->
                    results = list
                    if (list.isEmpty()) {
                        errorMessage = "Belum ada rekomendasi tersedia."
                    }
                },
                onFailure = { err ->
                    errorMessage = "Gagal memuat rekomendasi: ${err.message ?: "Periksa koneksi internet"}"
                }
            )
            isLoading = false
        }
    }

    fun performSearch(query: String, saveToHistory: Boolean = false) {
        activeChannel = null
        coroutineScope.launch {
            if (saveToHistory && query.isNotBlank()) {
                appPreferences.addSearchHistory(query)
            }
            isLoading = true
            errorMessage = null
            val res = YoutubeSearchRepository.search(context, query)
            res.fold(
                onSuccess = { list ->
                    results = list
                    if (list.isEmpty()) {
                        errorMessage = "Video tidak ditemukan untuk \"$query\"."
                    }
                },
                onFailure = { err ->
                    errorMessage = "Gagal memuat hasil: ${err.message ?: "Periksa koneksi internet"}"
                }
            )
            isLoading = false
        }
    }

    LaunchedEffect(pendingChannelToOpen) {
        if (pendingChannelToOpen != null) {
            openChannelPage(pendingChannelToOpen)
            onChannelOpened()
        }
    }

    // Muat & perbarui feed "Untuk Anda" otomatis setiap kali ada video baru yang selesai ditonton
    LaunchedEffect(lastWatchedId) {
        if (activeCategory == "FOR_YOU" && pendingChannelToOpen == null) {
            loadRecommendedFeed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        // Header Search Bar + Tombol Cari (Pada TV membuka Keyboard QWERTY bawaan agar tidak hilang saat diklik)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isTv) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E1E1E),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .dpadFocusable(
                            shape = RoundedCornerShape(10.dp),
                            style = FocusStyle.PILL,
                            focusedScale = 1.01f,
                            onClick = { showTvSearchDialog = true }
                        )
                        .clickable { showTvSearchDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = searchQuery.ifBlank { "Cari video YouTube... (Klik / OK untuk ketik)" },
                            color = if (searchQuery.isBlank()) Color.Gray else Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari video YouTube...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            if (searchQuery.isNotBlank()) {
                                activeCategory = ""
                                performSearch(searchQuery, saveToHistory = true)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .dpadFocusable(shape = RoundedCornerShape(10.dp), focusedScale = 1.01f, onClick = {})
                )
            }

            if (isTv) {
                Button(
                    onClick = { showTvSearchDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .dpadFocusable(
                            shape = RoundedCornerShape(10.dp),
                            style = FocusStyle.CTA,
                            onClick = { showTvSearchDialog = true }
                        )
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Cari")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cari", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    onClick = {
                        keyboardController?.hide()
                        val q = searchQuery.ifBlank { activeCategory.ifBlank { "Trending Indonesia" } }
                        performSearch(q)
                    },
                    color = Color(0xFFE50914),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = Color.White
                        )
                    }
                }

                // Tombol Paste Link YouTube khusus versi HP di sebelah ikon Search
                Surface(
                    onClick = {
                        keyboardController?.hide()
                        val clip = readClipboardText()
                        pastedUrl = if (YoutubeDlManager.extractVideoId(clip) != null) clip else ""
                        linkErrorMessage = null
                        showPasteLinkDialog = true
                    },
                    color = Color(0xFF242424),
                    border = BorderStroke(1.dp, Color(0xFF3A3A3A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Tempel Link YouTube",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (showPasteLinkDialog) {
            val previewVideoId = remember(pastedUrl) { YoutubeDlManager.extractVideoId(pastedUrl) }
            val triggerPlayPasted: () -> Unit = {
                val videoId = YoutubeDlManager.extractVideoId(pastedUrl)
                if (videoId == null) {
                    linkErrorMessage = "Link YouTube tidak valid."
                } else {
                    val cleanUrl = YoutubeDlManager.cleanUrl(pastedUrl)
                    val savedEntity = completedVideosByVideoId[videoId]
                    if (savedEntity != null) {
                        val historyEntry = YoutubeSearchItem(
                            videoId = savedEntity.videoId,
                            title = savedEntity.title,
                            channel = savedEntity.author.ifBlank { "YouTube" },
                            durationText = "",
                            viewCountText = "",
                            thumbnailUrl = savedEntity.thumbnailUrl
                        )
                        showPasteLinkDialog = false
                        coroutineScope.launch {
                            appPreferences.recordWatchedItem(historyEntry)
                        }
                        onPlayLocalVideo(savedEntity)
                    } else {
                        isResolvingPastedLink = true
                        linkErrorMessage = null
                        coroutineScope.launch {
                            val resolved = YoutubeDlManager.resolvePlayableVideo(
                                context = context,
                                videoUrl = cleanUrl,
                                preferredHeight = selectedResolution.preferredHeight
                            )
                            isResolvingPastedLink = false
                            if (resolved != null) {
                                val item = YoutubeSearchItem(
                                    videoId = resolved.videoId.ifBlank { videoId },
                                    title = resolved.title,
                                    channel = resolved.channel,
                                    durationText = resolved.durationText,
                                    viewCountText = "",
                                    thumbnailUrl = resolved.thumbnailUrl,
                                    channelId = resolved.channelId
                                )
                                appPreferences.recordWatchedItem(item)
                                showPasteLinkDialog = false
                                onPlayStream(item, resolved, displayedResults.ifEmpty { results })
                            } else {
                                linkErrorMessage = "Gagal memuat video dari link ini. Periksa link atau koneksi internet."
                            }
                        }
                    }
                }
            }
            val triggerDownloadPasted: () -> Unit = {
                val videoId = YoutubeDlManager.extractVideoId(pastedUrl)
                if (videoId == null) {
                    linkErrorMessage = "Link YouTube tidak valid."
                } else {
                    startDownloadItem(pastedUrl, videoId)
                    showPasteLinkDialog = false
                }
            }

            Dialog(
                onDismissRequest = {
                    if (!isResolvingPastedLink) {
                        showPasteLinkDialog = false
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E1E1E),
                    border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 440.dp)
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Buka Link YouTube",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Tempel tautan video YouTube (watch, youtu.be, atau Shorts) untuk langsung diputar atau diunduh.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )

                        if (previewVideoId != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF121212))
                            ) {
                                AsyncImage(
                                    model = "https://img.youtube.com/vi/$previewVideoId/mqdefault.jpg",
                                    contentDescription = "Preview Video YouTube",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        OutlinedTextField(
                            value = pastedUrl,
                            onValueChange = {
                                pastedUrl = it
                                linkErrorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = "https://youtu.be/... atau youtube.com/watch?v=...",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            enabled = !isResolvingPastedLink,
                            trailingIcon = {
                                if (pastedUrl.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            pastedUrl = ""
                                            linkErrorMessage = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Hapus",
                                            tint = Color.Gray
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            val clip = readClipboardText()
                                            if (clip.isNotBlank()) {
                                                pastedUrl = clip
                                                linkErrorMessage = null
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Clipboard kosong",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Tempel dari Clipboard",
                                            tint = Color.LightGray
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE50914),
                                unfocusedBorderColor = Color(0xFF444444),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF141414),
                                unfocusedContainerColor = Color(0xFF141414)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (linkErrorMessage != null) {
                            Text(
                                text = linkErrorMessage ?: "",
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp
                            )
                        }

                        if (isResolvingPastedLink) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFE50914),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Mengambil stream video...",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val stackVertically = maxWidth < 280.dp
                            if (stackVertically) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = triggerPlayPasted,
                                        enabled = !isResolvingPastedLink,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Putar",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Button(
                                        onClick = triggerDownloadPasted,
                                        enabled = !isResolvingPastedLink,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Download",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = triggerPlayPasted,
                                        enabled = !isResolvingPastedLink,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Putar",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Button(
                                        onClick = triggerDownloadPasted,
                                        enabled = !isResolvingPastedLink,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Download",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showPasteLinkDialog = false },
                            enabled = !isResolvingPastedLink,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF444444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = "Batal",
                                color = Color.LightGray,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        if (showTvSearchDialog) {
            TvSearchDialog(
                initialQuery = searchQuery,
                onSearch = { newQuery ->
                    searchQuery = newQuery
                    activeCategory = ""
                    showTvSearchDialog = false
                    performSearch(newQuery, saveToHistory = true)
                },
                onDismiss = {
                    showTvSearchDialog = false
                }
            )
        }

        // Header Channel Utama jika sedang membuka halaman channel pemilik video
        if (activeCategory == "CHANNEL" && activeChannel != null) {
            val currentCh = activeChannel!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = {
                        activeCategory = "FOR_YOU"
                        loadRecommendedFeed()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF282828),
                    modifier = Modifier.dpadFocusable(
                        shape = RoundedCornerShape(8.dp),
                        style = FocusStyle.PILL,
                        onClick = {
                            activeCategory = "FOR_YOU"
                            loadRecommendedFeed()
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kembali", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFFE50914),
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentCh.channelName.ifBlank { "Channel YouTube" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Halaman Utama Channel (${displayedResults.size} video)",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Baris Kategori Cepat, Riwayat Tontonan (20 Video), & Rekomendasi Personal
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
        ) {
            // 1. Chip "Untuk Anda"
            item {
                val isForYou = activeCategory == "FOR_YOU"
                TvChip(
                    label = "Untuk Anda",
                    selected = isForYou,
                    leadingIcon = Icons.Default.AutoAwesome,
                    leadingIconTint = Color(0xFFFFD54F),
                    onClick = {
                        activeCategory = "FOR_YOU"
                        searchQuery = ""
                        loadRecommendedFeed()
                    }
                )
            }

            // 2. Chip "Riwayat" (Menampilkan hingga 20 video terakhir ditonton)
            item {
                val isHistory = activeCategory == "WATCH_HISTORY"
                TvChip(
                    label = if (watchHistory.isNotEmpty()) "Riwayat (${watchHistory.size})" else "Riwayat",
                    selected = isHistory,
                    leadingIcon = Icons.Default.History,
                    leadingIconTint = Color(0xFF90CAF9),
                    onClick = {
                        activeChannel = null
                        activeCategory = "WATCH_HISTORY"
                        searchQuery = ""
                        errorMessage = null
                    }
                )
            }

            // 3. Chip Riwayat Pencarian Terakhir
            items(recentSearches.take(3)) { historyQuery ->
                val isSelected = activeCategory == "HIST:$historyQuery"
                TvChip(
                    label = historyQuery,
                    selected = isSelected,
                    leadingIcon = Icons.Default.History,
                    leadingIconTint = Color.Gray,
                    onClick = {
                        activeCategory = "HIST:$historyQuery"
                        searchQuery = historyQuery
                        performSearch(historyQuery)
                    }
                )
            }

            // 4. Kategori Bawaan
            items(quickCategories) { (query, label) ->
                val isSelected = activeCategory == query
                TvChip(
                    label = label,
                    selected = isSelected,
                    onClick = {
                        activeCategory = query
                        searchQuery = ""
                        performSearch(query)
                    }
                )
            }
        }

        // Baris Filter Tipe Konten, Durasi, & Urutkan
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
        ) {
            items(YoutubeContentFilter.entries) { filter ->
                TvChip(
                    label = filter.label,
                    selected = contentFilter == filter,
                    onClick = { contentFilter = filter }
                )
            }

            item {
                val nextSort = when (sortOption) {
                    YoutubeSortOption.DEFAULT -> YoutubeSortOption.DURATION_DESC
                    YoutubeSortOption.DURATION_DESC -> YoutubeSortOption.DURATION_ASC
                    YoutubeSortOption.DURATION_ASC -> YoutubeSortOption.DEFAULT
                }
                TvChip(
                    label = sortOption.label,
                    selected = sortOption != YoutubeSortOption.DEFAULT,
                    leadingIcon = Icons.Default.Sort,
                    leadingIconTint = Color.LightGray,
                    isOutlined = sortOption == YoutubeSortOption.DEFAULT,
                    onClick = { sortOption = nextSort }
                )
            }

            if (activeCategory == "WATCH_HISTORY" && watchHistory.isNotEmpty()) {
                item {
                    TvChip(
                        label = "Hapus Riwayat",
                        selected = false,
                        leadingIcon = Icons.Default.DeleteSweep,
                        leadingIconTint = Color(0xFFFF8A80),
                        isOutlined = true,
                        onClick = {
                            coroutineScope.launch {
                                appPreferences.clearWatchHistory()
                                Toast.makeText(context, "Riwayat tontonan dibersihkan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // Area Konten Hasil
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE50914))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat video YouTube...", color = Color.LightGray)
                    }
                }
                activeCategory == "WATCH_HISTORY" && watchHistory.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum ada riwayat video yang ditonton.",
                            color = Color.LightGray
                        )
                    }
                }
                errorMessage != null && results.isEmpty() && activeCategory != "WATCH_HISTORY" -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage ?: "", color = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                when {
                                    activeCategory == "FOR_YOU" -> loadRecommendedFeed()
                                    activeCategory == "CHANNEL" && activeChannel != null -> openChannelPage(activeChannel!!)
                                    else -> performSearch(
                                        searchQuery.ifBlank { activeCategory.removePrefix("HIST:").ifBlank { "Trending Indonesia" } }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            modifier = Modifier.dpadFocusable(onClick = {
                                when {
                                    activeCategory == "FOR_YOU" -> loadRecommendedFeed()
                                    activeCategory == "CHANNEL" && activeChannel != null -> openChannelPage(activeChannel!!)
                                    else -> performSearch(
                                        searchQuery.ifBlank { activeCategory.removePrefix("HIST:").ifBlank { "Trending Indonesia" } }
                                    )
                                }
                            })
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                displayedResults.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Tidak ada video yang cocok dengan filter \"${contentFilter.label}\".",
                            color = Color.LightGray
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { contentFilter = YoutubeContentFilter.ALL },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                modifier = Modifier.dpadFocusable(
                                    style = FocusStyle.CTA,
                                    onClick = { contentFilter = YoutubeContentFilter.ALL }
                                )
                            ) {
                                Text("Reset Filter")
                            }
                            if (contentFilter == YoutubeContentFilter.SHORTS || contentFilter == YoutubeContentFilter.LIVE) {
                                val modifierKeyword = if (contentFilter == YoutubeContentFilter.SHORTS) "shorts" else "live"
                                val baseQ = searchQuery.ifBlank { activeCategory.removePrefix("HIST:").takeIf { it != "FOR_YOU" && it != "WATCH_HISTORY" && it != "CHANNEL" }.orEmpty() }
                                if (baseQ.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { performSearch("$baseQ $modifierKeyword") },
                                        modifier = Modifier.dpadFocusable(
                                            style = FocusStyle.PILL,
                                            onClick = { performSearch("$baseQ $modifierKeyword") }
                                        )
                                    ) {
                                        Text("Cari $modifierKeyword", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 250.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedResults, key = { it.videoId }) { item ->
                            val isResolving = resolvingVideoId == item.videoId
                            val savedEntity = completedVideosByVideoId[item.videoId]
                            val isDownloading = item.videoId in activeDownloadIds
                            TvYoutubeVideoCard(
                                item = item,
                                isTv = isTv,
                                isLoading = isResolving,
                                statusLabel = when {
                                    savedEntity != null -> "Tersimpan"
                                    isDownloading -> "Mengunduh..."
                                    else -> null
                                },
                                onClick = { playYoutubeItem(item) },
                                onOpenMenu = { menuItemForCard = item }
                            )
                        }
                    }
                }
            }
        }

        // Menu Opsi Kartu Video YouTube (Dari Tombol Titik Tiga HP atau Tahan OK / Menu di TV)
        menuItemForCard?.let { target ->
            val firstMenuFocus = remember { FocusRequester() }
            var allowConfirmKey by remember(target.videoId) { mutableStateOf(false) }
            val guardConfirmKey: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val action = keyEvent.nativeKeyEvent.action
                if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                    code == KeyEvent.KEYCODE_SPACE
                ) {
                    if (!allowConfirmKey) {
                        if (action == KeyEvent.ACTION_DOWN && keyEvent.nativeKeyEvent.repeatCount == 0) {
                            allowConfirmKey = true
                            false
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            LaunchedEffect(target.videoId) {
                delay(50)
                try {
                    firstMenuFocus.requestFocus()
                } catch (_: Exception) {
                }
            }

            Dialog(
                onDismissRequest = { menuItemForCard = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E1E1E),
                    border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                            .onPreviewKeyEvent(guardConfirmKey),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = target.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = target.channel,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Button(
                            onClick = {
                                menuItemForCard = null
                                playYoutubeItem(target)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(firstMenuFocus)
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        menuItemForCard = null
                                        playYoutubeItem(target)
                                    }
                                )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Putar Sekarang", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                menuItemForCard = null
                                onEnqueueNext(target)
                                Toast.makeText(
                                    context,
                                    "Ditambahkan ke antrean Putar Berikutnya",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        menuItemForCard = null
                                        onEnqueueNext(target)
                                        Toast.makeText(
                                            context,
                                            "Ditambahkan ke antrean Putar Berikutnya",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                        ) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Putar Berikutnya (Antrean)")
                        }

                        Button(
                            onClick = {
                                menuItemForCard = null
                                startDownloadItem(target.watchUrl, target.videoId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        menuItemForCard = null
                                        startDownloadItem(target.watchUrl, target.videoId)
                                    }
                                )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download (${selectedResolution.label})")
                        }

                        Button(
                            onClick = {
                                menuItemForCard = null
                                openChannelPage(
                                    ChannelDestination(
                                        channelId = target.channelId,
                                        channelName = target.channel,
                                        sourceVideoId = target.videoId
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        menuItemForCard = null
                                        openChannelPage(
                                            ChannelDestination(
                                                channelId = target.channelId,
                                                channelName = target.channel,
                                                sourceVideoId = target.videoId
                                            )
                                        )
                                    }
                                )
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buka Channel")
                        }

                        if (!isTv) {
                            Button(
                                onClick = {
                                    menuItemForCard = null
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, target.title)
                                        putExtra(Intent.EXTRA_TEXT, target.watchUrl)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Link Video"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bagikan Link")
                            }
                        } else {
                            Button(
                                onClick = {
                                    menuItemForCard = null
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("YouTube URL", target.watchUrl))
                                    Toast.makeText(context, "Link video disalin", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dpadFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        style = FocusStyle.CTA,
                                        onClick = {
                                            menuItemForCard = null
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("YouTube URL", target.watchUrl))
                                            Toast.makeText(context, "Link video disalin", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Salin Link")
                            }
                        }

                        OutlinedButton(
                            onClick = { menuItemForCard = null },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF444444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.PILL,
                                    onClick = { menuItemForCard = null }
                                )
                        ) {
                            Text("Batal", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvYoutubeVideoCard(
    item: YoutubeSearchItem,
    isTv: Boolean,
    isLoading: Boolean,
    statusLabel: String? = null,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            longPressJob?.cancel()
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(
                shape = RoundedCornerShape(12.dp),
                onClick = onClick
            )
            .onPreviewKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val action = keyEvent.nativeKeyEvent.action

                if (code == KeyEvent.KEYCODE_MENU ||
                    code == KeyEvent.KEYCODE_INFO ||
                    code == KeyEvent.KEYCODE_PROG_RED
                ) {
                    if (action == KeyEvent.ACTION_UP) {
                        onOpenMenu()
                    }
                    return@onPreviewKeyEvent true
                }

                if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER
                ) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                            longPressJob?.cancel()
                            longPressTriggered = false
                            longPressJob = coroutineScope.launch {
                                delay(650)
                                longPressTriggered = true
                                onOpenMenu()
                            }
                        }
                        return@onPreviewKeyEvent true
                    } else if (action == KeyEvent.ACTION_UP) {
                        longPressJob?.cancel()
                        longPressJob = null
                        if (!longPressTriggered && !keyEvent.nativeKeyEvent.isCanceled) {
                            onClick()
                        }
                        longPressTriggered = false
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) {
                    longPressJob?.cancel()
                    longPressJob = null
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOpenMenu
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (statusLabel != null) {
                    Text(
                        text = statusLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xCC2E7D32), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.durationText.isNotBlank()) {
                    Text(
                        text = item.durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x88000000), RoundedCornerShape(24.dp))
                        .padding(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFE50914),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar",
                            tint = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = if (isTv) 12.dp else 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val subText = buildString {
                        append(item.channel)
                        if (item.viewCountText.isNotBlank()) {
                            append(" • ")
                            append(item.viewCountText)
                        }
                    }
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isTv) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu Video",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
