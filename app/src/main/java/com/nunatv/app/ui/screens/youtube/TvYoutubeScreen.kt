package com.nunatv.app.ui.screens.youtube

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
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
import com.nunatv.app.downloader.YoutubeDlManager
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.TvChip
import com.nunatv.app.ui.components.TvSearchDialog
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.util.DeviceUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

data class ChannelDestination(
    val channelId: String = "",
    val channelName: String = "",
    val sourceVideoId: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val handle: String = "",
    val subscriberCount: String = "",
    val videoCount: String = "",
    val description: String = ""
)

private val AVATAR_COLORS = listOf(
    Color(0xFFD32F2F),
    Color(0xFF1976D2),
    Color(0xFF388E3C),
    Color(0xFF7B1FA2),
    Color(0xFFF57C00),
    Color(0xFF00796B)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TvYoutubeScreen(
    youtubeViewModel: YoutubeViewModel,
    appPreferences: AppPreferences,
    completedVideosByVideoId: Map<String, DownloadedVideoEntity> = emptyMap(),
    activeDownloadIds: Set<String> = emptySet(),
    pendingChannelToOpen: ChannelDestination? = null,
    onChannelOpened: () -> Unit = {},
    onEnqueueNext: (YoutubeSearchItem) -> Unit = {},
    onPlayYoutubeItem: (item: YoutubeSearchItem, currentList: List<YoutubeSearchItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isTv = remember { DeviceUtils.isTvDevice(context) }

    val uiState by youtubeViewModel.uiState.collectAsState()
    val recentSearches by appPreferences.recentSearchQueries.collectAsState(initial = emptyList())
    val watchHistory by appPreferences.watchHistory.collectAsState(initial = emptyList())
    val selectedResolution by appPreferences.selectedResolution.collectAsState(initial = VideoResolution.HIGH)
    val hideShortsEnabled by appPreferences.hideShortsEnabled.collectAsState(initial = false)

    LaunchedEffect(hideShortsEnabled, uiState.activeCategory, uiState.contentFilter) {
        if (hideShortsEnabled) {
            if (uiState.activeCategory == "SHORTS") {
                youtubeViewModel.loadRecommendedFeed(force = false)
            }
            if (uiState.contentFilter == YoutubeContentFilter.SHORTS) {
                youtubeViewModel.setContentFilter(YoutubeContentFilter.ALL)
            }
        }
    }

    val quickCategories = remember {
        listOf(
            "Musik Indonesia Terbaru" to "Musik",
            "Gaming Indonesia" to "Gaming",
            "Tutorial Teknologi Edukasi" to "Edukasi",
            "Kartun Anak Indonesia" to "Anak",
            "Berita Terkini" to "Berita",
            "Podcast Indonesia" to "Podcast",
            "Film Indonesia Full Movie" to "Film"
        )
    }

    var showTvSearchDialog by remember { mutableStateOf(false) }
    var showTvChannelSearchDialog by remember { mutableStateOf(false) }
    var showPasteLinkDialog by remember { mutableStateOf(false) }
    var showMobileFilterSheet by remember { mutableStateOf(false) }
    var pastedUrl by remember { mutableStateOf("") }
    var linkErrorMessage by remember { mutableStateOf<String?>(null) }
    var menuItemForCard by remember { mutableStateOf<YoutubeSearchItem?>(null) }
    var channelSearchQuery by remember { mutableStateOf("") }
    var channelSortTab by remember { mutableStateOf("ALL") }

    LaunchedEffect(uiState.activeCategory, uiState.activeChannel?.channelId) {
        if (uiState.activeCategory != "CHANNEL") {
            channelSearchQuery = ""
            channelSortTab = "ALL"
        }
    }

    val isShortsView = uiState.activeCategory == "SHORTS" || uiState.contentFilter == YoutubeContentFilter.SHORTS

    val displayedResults = remember(
        uiState.results,
        uiState.shortsList,
        watchHistory,
        uiState.activeCategory,
        uiState.contentFilter,
        uiState.sortOption,
        hideShortsEnabled,
        channelSearchQuery,
        channelSortTab
    ) {
        val baseList = when {
            uiState.activeCategory == "WATCH_HISTORY" -> watchHistory
            uiState.contentFilter == YoutubeContentFilter.SHORTS && uiState.activeCategory != "SHORTS" -> {
                (uiState.results.filter { it.isShort } + uiState.shortsList).distinctBy { it.videoId }
            }
            else -> uiState.results
        }
        var filtered = YoutubeSearchRepository.filterAndSortItems(baseList, uiState.contentFilter, uiState.sortOption)
        if (hideShortsEnabled) filtered = filtered.filterNot { it.isShort }

        if (uiState.activeCategory == "CHANNEL") {
            if (channelSearchQuery.isNotBlank()) {
                val q = channelSearchQuery.trim().lowercase()
                filtered = filtered.filter { it.title.lowercase().contains(q) || it.channel.lowercase().contains(q) }
            }
            if (channelSortTab == "POPULAR") {
                filtered = filtered.sortedByDescending { item ->
                    val digits = item.viewCountText.filter { it.isDigit() }
                    digits.toLongOrNull() ?: 0L
                }
            }
        }
        filtered
    }

    val hasActiveFilterOrHistory = uiState.contentFilter != YoutubeContentFilter.ALL ||
        uiState.sortOption != YoutubeSortOption.DEFAULT ||
        uiState.activeCategory == "WATCH_HISTORY"

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

    fun playYoutubeItem(item: YoutubeSearchItem, sourceList: List<YoutubeSearchItem> = displayedResults) {
        onPlayYoutubeItem(item, sourceList.ifEmpty { uiState.results })
    }

    LaunchedEffect(pendingChannelToOpen) {
        if (pendingChannelToOpen != null) {
            youtubeViewModel.openChannelPage(pendingChannelToOpen)
            onChannelOpened()
        } else {
            youtubeViewModel.ensureInitialFeedLoaded()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isCompactMobile = !isTv && maxWidth < 600.dp
        val isMediumWidth = !isTv && maxWidth >= 600.dp && maxWidth < 900.dp
        val isWideOrTv = isTv || maxWidth >= 900.dp

        val isMobileScrolledDown by remember(isShortsView) {
            derivedStateOf {
                if (isShortsView) {
                    youtubeViewModel.gridState.firstVisibleItemIndex > 0 ||
                        youtubeViewModel.gridState.firstVisibleItemScrollOffset > 60
                } else {
                    youtubeViewModel.mobileListState.firstVisibleItemIndex > 0 ||
                        youtubeViewModel.mobileListState.firstVisibleItemScrollOffset > 80
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.activeCategory != "CHANNEL") {
                // 1. Header Pencarian (Ramping untuk Mobile, D-Pad besar untuk TV)
                if (isTv) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .dpadFocusable(
                                shape = RoundedCornerShape(24.dp),
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
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.searchQuery.ifBlank { "Cari video YouTube... (Klik / OK untuk ketik)" },
                                color = if (uiState.searchQuery.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = { showTvSearchDialog = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .dpadFocusable(
                                shape = CircleShape,
                                style = FocusStyle.CTA,
                                onClick = { showTvSearchDialog = true }
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    val hasActiveTvFilter = uiState.contentFilter != YoutubeContentFilter.ALL ||
                        uiState.sortOption != YoutubeSortOption.DEFAULT ||
                        uiState.activeCategory == "WATCH_HISTORY" ||
                        uiState.activeCategory.startsWith("HIST:")
                    Surface(
                        onClick = { showMobileFilterSheet = true },
                        shape = RoundedCornerShape(24.dp),
                        color = if (hasActiveTvFilter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (hasActiveTvFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .dpadFocusable(
                                shape = RoundedCornerShape(24.dp),
                                style = FocusStyle.PILL,
                                onClick = { showMobileFilterSheet = true }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = if (hasActiveTvFilter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "Filter",
                                color = if (hasActiveTvFilter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // Header Mobile & Tablet: 1 baris ramping dengan tinggi seragam 46dp (>= 44dp tap target)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(
                            horizontal = if (isCompactMobile) 12.dp else 16.dp,
                            vertical = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(23.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = if (hideShortsEnabled) "Cari video YouTube..." else "Cari video atau Shorts...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { youtubeViewModel.updateSearchInput(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            keyboardController?.hide()
                                            if (uiState.searchQuery.isNotBlank()) {
                                                youtubeViewModel.performSearch(
                                                    uiState.searchQuery,
                                                    categoryKey = "",
                                                    saveToHistory = true
                                                )
                                            }
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { youtubeViewModel.updateSearchInput("") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus teks",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tombol Filter & Riwayat
                    Surface(
                        onClick = { showMobileFilterSheet = true },
                        shape = CircleShape,
                        color = if (hasActiveFilterOrHistory) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (hasActiveFilterOrHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter dan Riwayat",
                                tint = if (hasActiveFilterOrHistory) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            if (hasActiveFilterOrHistory) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(7.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }

                    // Tombol Paste Link YouTube
                    Surface(
                        onClick = {
                            keyboardController?.hide()
                            val clip = readClipboardText()
                            pastedUrl = if (YoutubeDlManager.extractVideoId(clip) != null) clip else ""
                            linkErrorMessage = null
                            showPasteLinkDialog = true
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Tempel Link YouTube",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // 2. Baris Kategori Tunggal yang Ramping (Otomatis terlipat saat scroll di Mobile)
            AnimatedVisibility(
                visible = !isCompactMobile || !isMobileScrolledDown,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = if (isCompactMobile) 12.dp else 16.dp,
                            vertical = if (isCompactMobile) 4.dp else 6.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        // Beranda / Untuk Anda
                        item {
                            TvChip(
                                label = if (isTv) "Untuk Anda" else "Beranda",
                                selected = uiState.activeCategory == "FOR_YOU",
                                leadingIcon = if (isTv) Icons.Default.AutoAwesome else null,
                                leadingIconTint = Color(0xFFFFD54F),
                                onClick = { youtubeViewModel.loadRecommendedFeed(force = true) }
                            )
                        }

                        // Shorts (Video Vertikal) - disembunyikan jika user aktifkan Hide Shorts
                        if (!hideShortsEnabled) {
                            item {
                                TvChip(
                                    label = "Shorts",
                                    selected = uiState.activeCategory == "SHORTS",
                                    leadingIcon = Icons.Default.Bolt,
                                    leadingIconTint = Color(0xFFFF5252),
                                    onClick = { youtubeViewModel.loadShortsFeed() }
                                )
                            }
                        }

                        // Live Cepat pada Mobile (untuk TV juga berguna)
                        item {
                            TvChip(
                                label = "Live",
                                selected = uiState.activeCategory == "LIVE_FEED",
                                onClick = { youtubeViewModel.performSearch("live streaming indonesia", categoryKey = "LIVE_FEED") }
                            )
                        }

                        items(quickCategories) { (query, label) ->
                            TvChip(
                                label = label,
                                selected = uiState.activeCategory == query,
                                onClick = { youtubeViewModel.performSearch(query, categoryKey = query) }
                            )
                        }
                    }

                    // Baris Filter & Sort khusus tablet non-TV (di HP & TV pindah ke panel Filter agar ringkas)
                    if (!isTv && isWideOrTv) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF101010))
                        ) {
                            items(YoutubeContentFilter.entries) { filter ->
                                TvChip(
                                    label = filter.label,
                                    selected = uiState.contentFilter == filter,
                                    onClick = { youtubeViewModel.setContentFilter(filter) }
                                )
                            }

                            item {
                                val nextSort = when (uiState.sortOption) {
                                    YoutubeSortOption.DEFAULT -> YoutubeSortOption.DURATION_DESC
                                    YoutubeSortOption.DURATION_DESC -> YoutubeSortOption.DURATION_ASC
                                    YoutubeSortOption.DURATION_ASC -> YoutubeSortOption.DEFAULT
                                }
                                TvChip(
                                    label = uiState.sortOption.label,
                                    selected = uiState.sortOption != YoutubeSortOption.DEFAULT,
                                    leadingIcon = Icons.AutoMirrored.Filled.Sort,
                                    leadingIconTint = Color.LightGray,
                                    isOutlined = uiState.sortOption == YoutubeSortOption.DEFAULT,
                                    onClick = { youtubeViewModel.setSortOption(nextSort) }
                                )
                            }

                            if (uiState.activeCategory == "FOR_YOU") {
                                item {
                                    TvChip(
                                        label = "Segarkan",
                                        selected = false,
                                        leadingIcon = Icons.Default.Refresh,
                                        leadingIconTint = Color.LightGray,
                                        isOutlined = true,
                                        onClick = { youtubeViewModel.loadRecommendedFeed(force = true) }
                                    )
                                }
                            }

                            if (uiState.activeCategory == "WATCH_HISTORY" && watchHistory.isNotEmpty()) {
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
                    }
                }
            }

            // Banner filter aktif ringan saat mobile sedang menyaring konten
            if (!isWideOrTv && hasActiveFilterOrHistory) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181818))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeLabel = buildString {
                        if (uiState.activeCategory == "WATCH_HISTORY") append("Riwayat Tontonan")
                        if (uiState.contentFilter != YoutubeContentFilter.ALL) {
                            if (isNotEmpty()) append(" • ")
                            append(uiState.contentFilter.label)
                        }
                        if (uiState.sortOption != YoutubeSortOption.DEFAULT) {
                            if (isNotEmpty()) append(" • ")
                            append(uiState.sortOption.label)
                        }
                    }
                    Text(
                        text = activeLabel,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            youtubeViewModel.setContentFilter(YoutubeContentFilter.ALL)
                            youtubeViewModel.setSortOption(YoutubeSortOption.DEFAULT)
                            if (uiState.activeCategory == "WATCH_HISTORY") {
                                youtubeViewModel.loadRecommendedFeed(force = false)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.heightIn(min = 32.dp)
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            } else if (uiState.activeChannel != null) {
                val currentCh = uiState.activeChannel!!
                val avatarColor = remember(currentCh.channelName) {
                    AVATAR_COLORS[abs(currentCh.channelName.hashCode()) % AVATAR_COLORS.size]
                }
                val avatarLetter = remember(currentCh.channelName) {
                    currentCh.channelName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // 1. BANNER CHANNEL (Tinggi & responsif dengan gradient fallback & scrim)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactMobile) 112.dp else 148.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                    ) {
                        if (currentCh.bannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentCh.bannerUrl,
                                contentDescription = "Banner ${currentCh.channelName}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                    modifier = Modifier.size(92.dp)
                                )
                            }
                        }

                        // Gradient Scrim agar tombol kembali & konten kontras
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )

                        // Tombol Kembali melayang di kiri atas banner
                        Surface(
                            onClick = { youtubeViewModel.loadRecommendedFeed(force = true) },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(start = 12.dp, top = 12.dp)
                                .height(36.dp)
                                .dpadFocusable(
                                    shape = RoundedCornerShape(20.dp),
                                    style = FocusStyle.PILL,
                                    onClick = { youtubeViewModel.loadRecommendedFeed(force = true) }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Beranda",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kembali",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 2. PROFIL & METADATA CHANNEL (Avatar bulat, Nama, Handle, Subscribers)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isCompactMobile) 16.dp else 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = avatarColor,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                            modifier = Modifier.size(if (isCompactMobile) 64.dp else 74.dp)
                        ) {
                            if (currentCh.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentCh.avatarUrl,
                                    contentDescription = currentCh.channelName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = avatarLetter,
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentCh.channelName.ifBlank { "Channel YouTube" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = if (isCompactMobile) 17.sp else 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val statsList = buildList {
                                if (currentCh.handle.isNotBlank()) add(currentCh.handle)
                                if (currentCh.subscriberCount.isNotBlank()) add(currentCh.subscriberCount)
                                add(currentCh.videoCount.ifBlank { "${displayedResults.size} video" })
                            }
                            Text(
                                text = statsList.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            if (currentCh.description.isNotBlank()) {
                                Text(
                                    text = currentCh.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // 3. BARIS PENCARIAN DI DALAM CHANNEL & TAB FILTER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isCompactMobile) 16.dp else 24.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .then(
                                    if (isTv) {
                                        Modifier
                                            .dpadFocusable(
                                                shape = RoundedCornerShape(24.dp),
                                                style = FocusStyle.PILL,
                                                onClick = { showTvChannelSearchDialog = true }
                                            )
                                            .clickable { showTvChannelSearchDialog = true }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cari di channel",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (isTv) {
                                        Text(
                                            text = channelSearchQuery.ifBlank { "Cari di ${currentCh.channelName.ifBlank { "channel ini" }}..." },
                                            color = if (channelSearchQuery.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        if (channelSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Cari di ${currentCh.channelName.ifBlank { "channel ini" }}...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = channelSearchQuery,
                                            onValueChange = { channelSearchQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(
                                                onSearch = { keyboardController?.hide() }
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                if (channelSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { channelSearchQuery = "" },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Hapus pencarian",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        TvChip(
                            label = "Semua",
                            selected = channelSortTab == "ALL",
                            onClick = { channelSortTab = "ALL" }
                        )
                        TvChip(
                            label = "Populer",
                            selected = channelSortTab == "POPULAR",
                            leadingIcon = Icons.Default.Bolt,
                            onClick = { channelSortTab = "POPULAR" }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // 3. Area Konten Hasil (Dengan PullToRefresh pada Non-TV)
            val contentArea: @Composable () -> Unit = {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Memuat video YouTube...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        uiState.activeCategory == "WATCH_HISTORY" && watchHistory.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Belum ada riwayat video yang ditonton.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        uiState.errorMessage != null && displayedResults.isEmpty() && uiState.activeCategory != "WATCH_HISTORY" -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { youtubeViewModel.refreshActiveTab() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.dpadFocusable(onClick = { youtubeViewModel.refreshActiveTab() })
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
                                    text = "Tidak ada video yang cocok dengan filter \"${uiState.contentFilter.label}\".",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { youtubeViewModel.setContentFilter(YoutubeContentFilter.ALL) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.dpadFocusable(
                                            style = FocusStyle.CTA,
                                            onClick = { youtubeViewModel.setContentFilter(YoutubeContentFilter.ALL) }
                                        )
                                    ) {
                                        Text("Reset Filter")
                                    }
                                    if (uiState.contentFilter == YoutubeContentFilter.SHORTS || uiState.contentFilter == YoutubeContentFilter.LIVE) {
                                        val modKw = if (uiState.contentFilter == YoutubeContentFilter.SHORTS) "shorts" else "live"
                                        OutlinedButton(
                                            onClick = {
                                                if (uiState.contentFilter == YoutubeContentFilter.SHORTS) {
                                                    youtubeViewModel.loadShortsFeed(uiState.searchQuery)
                                                } else {
                                                    youtubeViewModel.performSearch("${uiState.searchQuery.ifBlank { "indonesia" }} $modKw")
                                                }
                                            },
                                            modifier = Modifier.dpadFocusable(
                                                style = FocusStyle.PILL,
                                                onClick = {
                                                    if (uiState.contentFilter == YoutubeContentFilter.SHORTS) {
                                                        youtubeViewModel.loadShortsFeed(uiState.searchQuery)
                                                    } else {
                                                        youtubeViewModel.performSearch("${uiState.searchQuery.ifBlank { "indonesia" }} $modKw")
                                                    }
                                                }
                                            )
                                        ) {
                                            Text("Cari $modKw", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        isShortsView -> {
                            val shortsCols = when {
                                isCompactMobile -> GridCells.Fixed(2)
                                isMediumWidth -> GridCells.Fixed(3)
                                else -> GridCells.Adaptive(minSize = 175.dp)
                            }
                            LazyVerticalGrid(
                                state = youtubeViewModel.gridState,
                                columns = shortsCols,
                                contentPadding = PaddingValues(
                                    horizontal = if (isCompactMobile) 12.dp else 16.dp,
                                    vertical = 12.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactMobile) 10.dp else 14.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isCompactMobile) 12.dp else 16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(displayedResults, key = { "short_${it.videoId}" }) { shortItem ->
                                    val savedEntity = completedVideosByVideoId[shortItem.videoId]
                                    val isDownloading = shortItem.videoId in activeDownloadIds
                                    YoutubeShortCard(
                                        item = shortItem,
                                        isTv = isTv,
                                        statusLabel = when {
                                            savedEntity != null -> "Tersimpan"
                                            isDownloading -> "Mengunduh..."
                                            else -> null
                                        },
                                        onClick = { playYoutubeItem(shortItem, displayedResults) },
                                        onOpenMenu = { menuItemForCard = shortItem }
                                    )
                                }
                            }
                        }
                        isCompactMobile -> {
                            val showInlineShorts = uiState.activeCategory == "FOR_YOU" &&
                                uiState.contentFilter == YoutubeContentFilter.ALL &&
                                uiState.sortOption == YoutubeSortOption.DEFAULT &&
                                uiState.shortsList.isNotEmpty()
                            val firstBatch = if (showInlineShorts) displayedResults.take(2) else displayedResults
                            val remainingBatch = if (showInlineShorts) displayedResults.drop(2) else emptyList()

                            LazyColumn(
                                state = youtubeViewModel.mobileListState,
                                contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(firstBatch, key = { "mob_${it.videoId}" }) { item ->
                                    val savedEntity = completedVideosByVideoId[item.videoId]
                                    val isDownloading = item.videoId in activeDownloadIds
                                    MobileYoutubeFeedItem(
                                        item = item,
                                        statusLabel = when {
                                            savedEntity != null -> "Tersimpan"
                                            isDownloading -> "Mengunduh..."
                                            else -> null
                                        },
                                        onClick = { playYoutubeItem(item, displayedResults) },
                                        onOpenMenu = { menuItemForCard = item }
                                    )
                                }

                                if (showInlineShorts) {
                                    item(key = "mob_shorts_shelf") {
                                        InlineShortsShelf(
                                            shorts = uiState.shortsList,
                                            isTv = false,
                                            completedVideosByVideoId = completedVideosByVideoId,
                                            activeDownloadIds = activeDownloadIds,
                                            onOpenAllShorts = { youtubeViewModel.loadShortsFeed() },
                                            onPlayShort = { shortItem -> playYoutubeItem(shortItem, uiState.shortsList) },
                                            onOpenMenu = { shortItem -> menuItemForCard = shortItem }
                                        )
                                    }
                                }

                                items(remainingBatch, key = { "mob_rest_${it.videoId}" }) { item ->
                                    val savedEntity = completedVideosByVideoId[item.videoId]
                                    val isDownloading = item.videoId in activeDownloadIds
                                    MobileYoutubeFeedItem(
                                        item = item,
                                        statusLabel = when {
                                            savedEntity != null -> "Tersimpan"
                                            isDownloading -> "Mengunduh..."
                                            else -> null
                                        },
                                        onClick = { playYoutubeItem(item, displayedResults) },
                                        onOpenMenu = { menuItemForCard = item }
                                    )
                                }
                            }
                        }
                        else -> {
                            val showInlineShorts = uiState.activeCategory == "FOR_YOU" &&
                                uiState.contentFilter == YoutubeContentFilter.ALL &&
                                uiState.sortOption == YoutubeSortOption.DEFAULT &&
                                uiState.shortsList.isNotEmpty()
                            val gridCols = if (isMediumWidth) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 260.dp)
                            val topLeadCount = if (isMediumWidth) 2 else 3
                            val firstBatch = if (showInlineShorts) displayedResults.take(topLeadCount) else displayedResults
                            val remainingBatch = if (showInlineShorts) displayedResults.drop(topLeadCount) else emptyList()

                            LazyVerticalGrid(
                                state = youtubeViewModel.gridState,
                                columns = gridCols,
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(firstBatch, key = { "grid_${it.videoId}" }) { item ->
                                    val savedEntity = completedVideosByVideoId[item.videoId]
                                    val isDownloading = item.videoId in activeDownloadIds
                                    TvYoutubeVideoCard(
                                        item = item,
                                        isTv = isTv,
                                        statusLabel = when {
                                            savedEntity != null -> "Tersimpan"
                                            isDownloading -> "Mengunduh..."
                                            else -> null
                                        },
                                        onClick = { playYoutubeItem(item, displayedResults) },
                                        onOpenMenu = { menuItemForCard = item }
                                    )
                                }

                                if (showInlineShorts) {
                                    item(
                                        key = "grid_shorts_shelf",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        InlineShortsShelf(
                                            shorts = uiState.shortsList,
                                            isTv = isTv,
                                            completedVideosByVideoId = completedVideosByVideoId,
                                            activeDownloadIds = activeDownloadIds,
                                            onOpenAllShorts = { youtubeViewModel.loadShortsFeed() },
                                            onPlayShort = { shortItem -> playYoutubeItem(shortItem, uiState.shortsList) },
                                            onOpenMenu = { shortItem -> menuItemForCard = shortItem }
                                        )
                                    }
                                }

                                items(remainingBatch, key = { "grid_rest_${it.videoId}" }) { item ->
                                    val savedEntity = completedVideosByVideoId[item.videoId]
                                    val isDownloading = item.videoId in activeDownloadIds
                                    TvYoutubeVideoCard(
                                        item = item,
                                        isTv = isTv,
                                        statusLabel = when {
                                            savedEntity != null -> "Tersimpan"
                                            isDownloading -> "Mengunduh..."
                                            else -> null
                                        },
                                        onClick = { playYoutubeItem(item, displayedResults) },
                                        onOpenMenu = { menuItemForCard = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isTv) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    contentArea()
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { youtubeViewModel.refreshActiveTab() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    contentArea()
                }
            }
        }

        // Bottom Sheet Filter & Riwayat Khusus Mobile/Tablet
        if (showMobileFilterSheet) {
            val firstFilterFocus = remember { FocusRequester() }
            val filterSheetBody: @Composable (Modifier) -> Unit = { bodyModifier ->
                Column(
                    modifier = bodyModifier,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter, Urutan & Riwayat",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvChip(
                                label = "Segarkan",
                                selected = false,
                                leadingIcon = Icons.Default.Refresh,
                                leadingIconTint = MaterialTheme.colorScheme.primary,
                                isOutlined = true,
                                modifier = if (isTv) Modifier.focusRequester(firstFilterFocus) else Modifier,
                                onClick = {
                                    showMobileFilterSheet = false
                                    youtubeViewModel.refreshActiveTab()
                                }
                            )
                            if (isTv) {
                                TvChip(
                                    label = "Tutup",
                                    selected = false,
                                    isOutlined = true,
                                    onClick = { showMobileFilterSheet = false }
                                )
                            }
                        }
                    }

                    Text("Tipe Konten", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        YoutubeContentFilter.entries
                            .filterNot { hideShortsEnabled && it == YoutubeContentFilter.SHORTS }
                            .forEach { filter ->
                                TvChip(
                                    label = filter.label,
                                    selected = uiState.contentFilter == filter,
                                    onClick = {
                                        youtubeViewModel.setContentFilter(filter)
                                        showMobileFilterSheet = false
                                    }
                                )
                            }
                    }

                    Text("Urutkan", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        YoutubeSortOption.entries.forEach { option ->
                            TvChip(
                                label = option.label,
                                selected = uiState.sortOption == option,
                                onClick = {
                                    youtubeViewModel.setSortOption(option)
                                    showMobileFilterSheet = false
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2B2B2B))

                    Text("Riwayat Tontonan & Pencarian", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TvChip(
                            label = if (watchHistory.isNotEmpty()) "Riwayat Tonton (${watchHistory.size})" else "Riwayat Tonton",
                            selected = uiState.activeCategory == "WATCH_HISTORY",
                            leadingIcon = Icons.Default.History,
                            leadingIconTint = Color(0xFF90CAF9),
                            onClick = {
                                youtubeViewModel.selectWatchHistoryMode()
                                showMobileFilterSheet = false
                            }
                        )
                        if (watchHistory.isNotEmpty()) {
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
                                    showMobileFilterSheet = false
                                }
                            )
                        }
                        recentSearches.forEach { historyQuery ->
                            val catKey = "HIST:$historyQuery"
                            TvChip(
                                label = historyQuery,
                                selected = uiState.activeCategory == catKey,
                                leadingIcon = Icons.Default.History,
                                leadingIconTint = Color.Gray,
                                onClick = {
                                    youtubeViewModel.performSearch(historyQuery, categoryKey = catKey)
                                    showMobileFilterSheet = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (isTv) {
                LaunchedEffect(Unit) {
                    delay(80)
                    runCatching { firstFilterFocus.requestFocus() }
                }
                Dialog(
                    onDismissRequest = { showMobileFilterSheet = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .widthIn(min = 420.dp, max = 660.dp)
                            .heightIn(max = 520.dp)
                            .padding(20.dp)
                    ) {
                        filterSheetBody(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
                        )
                    }
                }
            } else {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showMobileFilterSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    filterSheetBody(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Dialog Tempel Link YouTube
        if (showPasteLinkDialog) {
            val previewVideoId = remember(pastedUrl) { YoutubeDlManager.extractVideoId(pastedUrl) }
            val triggerPlayPasted: () -> Unit = {
                val videoId = YoutubeDlManager.extractVideoId(pastedUrl)
                if (videoId == null) {
                    linkErrorMessage = "Link YouTube tidak valid."
                } else {
                    val isPastedShort = pastedUrl.contains("/shorts/", ignoreCase = true)
                    val item = YoutubeSearchItem(
                        videoId = videoId,
                        title = if (isPastedShort) "YouTube Shorts ($videoId)" else "Video YouTube ($videoId)",
                        channel = "YouTube",
                        durationText = if (isPastedShort) "SHORTS" else "",
                        viewCountText = "",
                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    )
                    showPasteLinkDialog = false
                    playYoutubeItem(item, listOf(item))
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
                onDismissRequest = { showPasteLinkDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            text = "Tempel tautan video YouTube (watch, youtu.be, live, atau Shorts) untuk langsung diputar atau diunduh.",
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
                                    text = "https://youtu.be/... atau youtube.com/shorts/...",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
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
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
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

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val stackVertically = maxWidth < 280.dp
                            if (stackVertically) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = triggerPlayPasted,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Putar", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }

                                    Button(
                                        onClick = triggerDownloadPasted,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = triggerPlayPasted,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Putar", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }

                                    Button(
                                        onClick = triggerDownloadPasted,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showPasteLinkDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF444444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text("Batal", color = Color.LightGray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        if (showTvSearchDialog) {
            TvSearchDialog(
                initialQuery = uiState.searchQuery,
                onSearch = { newQuery ->
                    showTvSearchDialog = false
                    youtubeViewModel.performSearch(newQuery, categoryKey = "", saveToHistory = true)
                },
                onDismiss = { showTvSearchDialog = false }
            )
        }

        if (showTvChannelSearchDialog) {
            TvSearchDialog(
                initialQuery = channelSearchQuery,
                onSearch = { newQuery ->
                    showTvChannelSearchDialog = false
                    channelSearchQuery = newQuery
                },
                onDismiss = { showTvChannelSearchDialog = false }
            )
        }

        // Menu Opsi Kartu Video YouTube
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
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                playYoutubeItem(
                                    target,
                                    if (target.isShort) uiState.shortsList.ifEmpty { displayedResults } else displayedResults
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .focusRequester(firstMenuFocus)
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        menuItemForCard = null
                                        playYoutubeItem(
                                            target,
                                            if (target.isShort) uiState.shortsList.ifEmpty { displayedResults } else displayedResults
                                        )
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
                                Toast.makeText(context, "Ditambahkan ke antrean Putar Berikutnya", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        menuItemForCard = null
                                        onEnqueueNext(target)
                                        Toast.makeText(context, "Ditambahkan ke antrean Putar Berikutnya", Toast.LENGTH_SHORT).show()
                                    }
                                )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Putar Berikutnya (Antrean)")
                        }

                        if (!target.durationText.equals("LIVE", ignoreCase = true)) {
                            Button(
                                onClick = {
                                    menuItemForCard = null
                                    startDownloadItem(target.watchUrl, target.videoId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
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
                        }

                        Button(
                            onClick = {
                                menuItemForCard = null
                                youtubeViewModel.openChannelPage(
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
                                .heightIn(min = 46.dp)
                                .dpadFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        menuItemForCard = null
                                        youtubeViewModel.openChannelPage(
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
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
                                    .heightIn(min = 46.dp)
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
                                .heightIn(min = 44.dp)
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

@Composable
private fun InlineShortsShelf(
    shorts: List<YoutubeSearchItem>,
    isTv: Boolean,
    completedVideosByVideoId: Map<String, DownloadedVideoEntity>,
    activeDownloadIds: Set<String>,
    onOpenAllShorts: () -> Unit,
    onPlayShort: (YoutubeSearchItem) -> Unit,
    onOpenMenu: (YoutubeSearchItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 4.dp else 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Shorts",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                onClick = onOpenAllShorts,
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF222222),
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .dpadFocusable(
                        shape = RoundedCornerShape(18.dp),
                        style = FocusStyle.PILL,
                        onClick = onOpenAllShorts
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Lihat semua",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = if (isTv) 4.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
        ) {
            items(shorts.take(12), key = { "shelf_${it.videoId}" }) { shortItem ->
                val savedEntity = completedVideosByVideoId[shortItem.videoId]
                val isDownloading = shortItem.videoId in activeDownloadIds
                YoutubeShortCard(
                    item = shortItem,
                    isTv = isTv,
                    statusLabel = when {
                        savedEntity != null -> "Tersimpan"
                        isDownloading -> "Mengunduh..."
                        else -> null
                    },
                    onClick = { onPlayShort(shortItem) },
                    onOpenMenu = { onOpenMenu(shortItem) },
                    modifier = Modifier.width(if (isTv) 168.dp else 144.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MobileYoutubeFeedItem(
    item: YoutubeSearchItem,
    statusLabel: String?,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val isLive = item.durationText.equals("LIVE", ignoreCase = true)
    val avatarColor = remember(item.channel) {
        AVATAR_COLORS[abs(item.channel.hashCode()) % AVATAR_COLORS.size]
    }
    val avatarLetter = remember(item.channel) {
        item.channel.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOpenMenu
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
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
                        .background(Color(0xCC2E7D32), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            if (item.durationText.isNotBlank()) {
                Text(
                    text = item.durationText,
                    color = if (isLive) MaterialTheme.colorScheme.onPrimary else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            if (isLive) MaterialTheme.colorScheme.primary else Color(0xCC000000),
                            RoundedCornerShape(5.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarLetter,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val meta = buildString {
                    append(item.channel)
                    if (item.viewCountText.isNotBlank()) {
                        append(" • ")
                        append(item.viewCountText)
                    }
                }
                Text(
                    text = meta,
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu Video",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YoutubeShortCard(
    item: YoutubeSearchItem,
    isTv: Boolean,
    statusLabel: String?,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .dpadFocusable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .combinedClickable(onClick = onClick, onLongClick = onOpenMenu)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(Color(0xFF121212))
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(start = 8.dp, top = 8.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (statusLabel != null) Color(0xCC2E7D32) else Color(0xCCE50914)
                ) {
                    Text(
                        text = statusLabel ?: "SHORTS",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (!isTv) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu Shorts",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.viewCountText.isNotBlank()) {
                    Text(
                        text = item.viewCountText,
                        color = Color(0xFFD0D0D0),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    statusLabel: String? = null,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val isLive = item.durationText.equals("LIVE", ignoreCase = true)

    DisposableEffect(Unit) {
        onDispose {
            longPressJob?.cancel()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(
                shape = RoundedCornerShape(16.dp),
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
                        color = if (isLive) MaterialTheme.colorScheme.onPrimary else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                if (isLive) MaterialTheme.colorScheme.primary else Color(0xCC000000),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = if (isTv) 12.dp else 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isTv) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu Video",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
