package com.nunatv.app.ui.screens.home

import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nunatv.app.data.local.DownloadStatus
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.ui.components.CompactStorageBar
import com.nunatv.app.ui.components.FocusStyle
import com.nunatv.app.ui.components.TvChip
import com.nunatv.app.ui.components.dpadFocusable
import com.nunatv.app.util.DeviceUtils
import com.nunatv.app.util.StorageUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayVideo: (video: DownloadedVideoEntity) -> Unit,
    onPlayPlaylist: ((playlist: List<DownloadedVideoEntity>, startIndex: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTv = remember { DeviceUtils.isTvDevice(context) }
    val videos by viewModel.videos.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val randomSeed by viewModel.randomSeed.collectAsState()

    val storageInfo = remember(videos) {
        StorageUtils.getStorageInfo(StorageUtils.getDefaultDownloadDir(context))
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var videoForOptions by remember { mutableStateOf<DownloadedVideoEntity?>(null) }
    var videoToEditCategory by remember { mutableStateOf<DownloadedVideoEntity?>(null) }
    var videoToDelete by remember { mutableStateOf<DownloadedVideoEntity?>(null) }

    val completedVideos = remember(videos) {
        videos.filter { it.status == DownloadStatus.COMPLETED && it.localFilePath.isNotEmpty() }
    }

    val mostPlayed = remember(completedVideos) {
        completedVideos.sortedByDescending { it.playCount }
    }

    val randomList = remember(completedVideos, randomSeed) {
        val rand = Random(randomSeed)
        completedVideos.shuffled(rand)
    }

    val playPlaylistAction: (List<DownloadedVideoEntity>, Boolean) -> Unit = { targetVideos, isRandom ->
        val completedOnly = targetVideos.filter { it.status == DownloadStatus.COMPLETED && it.localFilePath.isNotEmpty() }
        if (completedOnly.isNotEmpty()) {
            val finalQueue = if (isRandom) {
                completedOnly.shuffled()
            } else {
                completedOnly.sortedBy { it.title.lowercase() }
            }
            val firstVideo = finalQueue.first()
            viewModel.recordPlay(firstVideo)
            if (onPlayPlaylist != null) {
                onPlayPlaylist(finalQueue, 0)
            } else {
                onPlayVideo(firstVideo)
            }
        }
    }

    // Banner video acak dinamis yang berganti berdasarkan randomSeed
    val heroVideo = remember(completedVideos, randomSeed) {
        if (completedVideos.isNotEmpty()) {
            val rand = Random(randomSeed)
            completedVideos[rand.nextInt(completedVideos.size)]
        } else null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isCompactMobile = !isTv && maxWidth < 600.dp
        val isMediumWidth = !isTv && maxWidth >= 600.dp && maxWidth < 900.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Pustaka Offline (Ramping di mobile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(
                        start = if (isCompactMobile) 12.dp else 16.dp,
                        end = if (isCompactMobile) 8.dp else 16.dp,
                        top = if (isCompactMobile) 8.dp else 12.dp,
                        bottom = 2.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Pustaka Offline",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                CompactStorageBar(
                    storageInfo = storageInfo,
                    compactHeader = true
                )
                if (isCompactMobile) {
                    IconButton(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Kelola Kategori",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Baris Kategori Filter Chips
            val visibleCustomCategories = remember(customCategories, videos, isTv) {
                if (isTv) {
                    customCategories.filter { cat ->
                        videos.any { it.category.equals(cat, ignoreCase = true) }
                    }
                } else {
                    customCategories.toList()
                }
            }
            LazyRow(
                contentPadding = PaddingValues(
                    horizontal = if (isCompactMobile) 12.dp else 16.dp,
                    vertical = if (isCompactMobile) 6.dp else 10.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                val defaultFilters = listOf(
                    "Semua" to "Semua",
                    "Terakhir Ditambahkan" to if (isCompactMobile || isTv) "Terbaru" else "Terakhir Ditambahkan",
                    "Paling Sering Diputar" to if (isCompactMobile || isTv) "Sering Diputar" else "Paling Sering Diputar",
                    "Acak" to "Acak"
                )
                items(items = defaultFilters) { (filterKey, displayLabel) ->
                    TvChip(
                        label = displayLabel,
                        selected = selectedCategory == filterKey,
                        onClick = { viewModel.selectCategory(filterKey) }
                    )
                }

                // Kategori Kustom User (di TV hanya tampilkan kategori yang ada videonya agar ringkas)
                items(items = visibleCustomCategories) { categoryName ->
                    TvChip(
                        label = categoryName,
                        selected = selectedCategory == categoryName,
                        onClick = { viewModel.selectCategory(categoryName) }
                    )
                }

                // Tombol Tambah Kategori Baru (di HP ada ikon + di header)
                if (!isCompactMobile) {
                    item {
                        TvChip(
                            label = "Kategori",
                            selected = false,
                            leadingIcon = Icons.Default.Add,
                            isOutlined = true,
                            onClick = { showAddCategoryDialog = true }
                        )
                    }
                }
            }

            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum ada video offline tersimpan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Video yang Anda unduh akan muncul di sini dan bisa diputar tanpa internet. Buka tab YouTube untuk menonton atau mengunduh video.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                when (selectedCategory) {
                    "Semua" -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(if (isCompactMobile) 14.dp else 20.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. Hero Spotlight Banner Video
                            if (heroVideo != null) {
                                item {
                                    HeroBanner(
                                        video = heroVideo,
                                        compact = isCompactMobile,
                                        onPlay = {
                                            viewModel.recordPlay(heroVideo)
                                            onPlayVideo(heroVideo)
                                        },
                                        onShuffle = { viewModel.refreshRandom() }
                                    )
                                }
                            }

                            // 2. Section: Terakhir Ditambahkan
                            item {
                                val latestVideos = videos.sortedByDescending { it.downloadTimestamp }
                                HomeSectionRow(
                                    title = "Terakhir Ditambahkan",
                                    videos = latestVideos,
                                    isTv = isTv,
                                    compact = isCompactMobile,
                                    onPlayVideo = { video ->
                                        viewModel.recordPlay(video)
                                        onPlayVideo(video)
                                    },
                                    onPlayAll = { playPlaylistAction(latestVideos, false) },
                                    onPlayRandom = { playPlaylistAction(latestVideos, true) },
                                    onOpenOptions = { videoForOptions = it },
                                    onEditCategory = { videoToEditCategory = it },
                                    onDelete = { videoToDelete = it }
                                )
                            }

                            // 3. Section: Paling Sering Diputar
                            if (mostPlayed.isNotEmpty()) {
                                item {
                                    HomeSectionRow(
                                        title = "Paling Sering Diputar",
                                        videos = mostPlayed,
                                        isTv = isTv,
                                        compact = isCompactMobile,
                                        onPlayVideo = { video ->
                                            viewModel.recordPlay(video)
                                            onPlayVideo(video)
                                        },
                                        onPlayAll = { playPlaylistAction(mostPlayed, false) },
                                        onPlayRandom = { playPlaylistAction(mostPlayed, true) },
                                        onOpenOptions = { videoForOptions = it },
                                        onEditCategory = { videoToEditCategory = it },
                                        onDelete = { videoToDelete = it }
                                    )
                                }
                            }

                            // 4. Section: Acak Untukmu
                            if (randomList.isNotEmpty()) {
                                item {
                                    HomeSectionRow(
                                        title = "Acak Untukmu",
                                        videos = randomList,
                                        isTv = isTv,
                                        compact = isCompactMobile,
                                        onPlayVideo = { video ->
                                            viewModel.recordPlay(video)
                                            onPlayVideo(video)
                                        },
                                        onPlayAll = { playPlaylistAction(randomList, false) },
                                        onPlayRandom = { playPlaylistAction(randomList, true) },
                                        onOpenOptions = { videoForOptions = it },
                                        onEditCategory = { videoToEditCategory = it },
                                        onDelete = { videoToDelete = it }
                                    )
                                }
                            }

                            // 5. Sections per Kategori Kustom
                            customCategories.forEach { cat ->
                                val categoryVideos = completedVideos.filter { it.isIn(cat) }
                                if (categoryVideos.isNotEmpty()) {
                                    item {
                                        HomeSectionRow(
                                            title = cat,
                                            videos = categoryVideos,
                                            isTv = isTv,
                                            compact = isCompactMobile,
                                            onPlayVideo = { video ->
                                                viewModel.recordPlay(video)
                                                onPlayVideo(video)
                                            },
                                            onPlayAll = { playPlaylistAction(categoryVideos, false) },
                                            onPlayRandom = { playPlaylistAction(categoryVideos, true) },
                                            onOpenOptions = { videoForOptions = it },
                                            onEditCategory = { videoToEditCategory = it },
                                            onDelete = { videoToDelete = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val filteredList = remember(videos, selectedCategory, mostPlayed, randomList) {
                            when (selectedCategory) {
                                "Terakhir Ditambahkan" -> videos.sortedByDescending { it.downloadTimestamp }
                                "Paling Sering Diputar" -> mostPlayed
                                "Acak" -> randomList
                                else -> videos.filter { it.isIn(selectedCategory) }
                            }
                        }

                        if (filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada video pada kategori '$selectedCategory'",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            val gridColumns = when {
                                isCompactMobile -> GridCells.Fixed(1)
                                isMediumWidth -> GridCells.Fixed(2)
                                else -> GridCells.Adaptive(minSize = 260.dp)
                            }
                            val pad = if (isCompactMobile) 12.dp else if (isMediumWidth) 14.dp else 16.dp
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = pad, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = selectedCategory,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = if (isCompactMobile) 17.sp else 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Play berurutan abjad
                                    Surface(
                                        onClick = { playPlaylistAction(filteredList, false) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .dpadFocusable(
                                                shape = CircleShape,
                                                style = FocusStyle.CTA,
                                                onClick = { playPlaylistAction(filteredList, false) }
                                            )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Putar Berurutan Sesuai Abjad",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Play acak
                                    Surface(
                                        onClick = { playPlaylistAction(filteredList, true) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .dpadFocusable(
                                                shape = CircleShape,
                                                style = FocusStyle.CTA,
                                                onClick = { playPlaylistAction(filteredList, true) }
                                            )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Shuffle,
                                                contentDescription = "Putar Acak (Random)",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        text = "${filteredList.size} video",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }

                                LazyVerticalGrid(
                                    columns = gridColumns,
                                    contentPadding = PaddingValues(pad),
                                    horizontalArrangement = Arrangement.spacedBy(pad),
                                    verticalArrangement = Arrangement.spacedBy(pad),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(items = filteredList, key = { it.id }) { video: DownloadedVideoEntity ->
                                        HomeVideoCard(
                                            video = video,
                                            isTv = isTv,
                                            onClick = {
                                                if (video.status == DownloadStatus.COMPLETED && video.localFilePath.isNotEmpty()) {
                                                    viewModel.recordPlay(video)
                                                    onPlayVideo(video)
                                                }
                                            },
                                            onOpenOptions = { videoForOptions = video },
                                            onEditCategory = { videoToEditCategory = video },
                                            onDelete = { videoToDelete = video }
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

    // Dialog Tambah Kategori Baru & Deteksi Otomatis
    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Kelola Kategori", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nama Kategori Baru (misal: Anime, Musik, Vlog)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            showAddCategoryDialog = false
                            coroutineScope.launch {
                                val updated = viewModel.autoCategorizeUmumVideos()
                                val msg = if (updated > 0) {
                                    "$updated video dikategorikan otomatis"
                                } else {
                                    "Semua video sudah memiliki kategori yang sesuai"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deteksi Otomatis Video 'Umum'", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim())
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // Dialog Opsi Video (Muncul saat tahan OK / tekan Menu pada kartu video di Remote TV)
    videoForOptions?.let { targetVideo ->
        val firstOptionFocus = remember { FocusRequester() }
        // Cegah sisa repeat ACTION_DOWN / ACTION_UP dari tahan tombol OK langsung memicu tombol Putar di dialog
        var allowConfirmKey by remember(targetVideo.id) { mutableStateOf(false) }
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

        LaunchedEffect(targetVideo.id) {
            delay(50)
            try {
                firstOptionFocus.requestFocus()
            } catch (_: Exception) {
            }
        }

        AlertDialog(
            onDismissRequest = { videoForOptions = null },
            title = { Text("Opsi Video", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.onPreviewKeyEvent(guardConfirmKey)
                ) {
                    Text(
                        text = targetVideo.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (targetVideo.status == DownloadStatus.COMPLETED && targetVideo.localFilePath.isNotEmpty()) {
                        Button(
                            onClick = {
                                videoForOptions = null
                                viewModel.recordPlay(targetVideo)
                                onPlayVideo(targetVideo)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(firstOptionFocus)
                                .dpadFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    style = FocusStyle.CTA,
                                    onClick = {
                                        videoForOptions = null
                                        viewModel.recordPlay(targetVideo)
                                        onPlayVideo(targetVideo)
                                    }
                                )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Putar Video", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            videoForOptions = null
                            videoToEditCategory = targetVideo
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (targetVideo.status != DownloadStatus.COMPLETED) Modifier.focusRequester(firstOptionFocus)
                                else Modifier
                            )
                            .dpadFocusable(
                                shape = RoundedCornerShape(8.dp),
                                style = FocusStyle.CTA,
                                onClick = {
                                    videoForOptions = null
                                    videoToEditCategory = targetVideo
                                }
                            )
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Kategori")
                    }

                    Button(
                        onClick = {
                            videoForOptions = null
                            videoToDelete = targetVideo
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                shape = RoundedCornerShape(8.dp),
                                style = FocusStyle.CTA,
                                onClick = {
                                    videoForOptions = null
                                    videoToDelete = targetVideo
                                }
                            )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus Video", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { videoForOptions = null },
                    modifier = Modifier
                        .onPreviewKeyEvent(guardConfirmKey)
                        .dpadFocusable(
                            shape = RoundedCornerShape(8.dp),
                            style = FocusStyle.CTA,
                            onClick = { videoForOptions = null }
                        )
                ) {
                    Text("Batal", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Dialog Ganti Kategori Video (Mendukung multi-kategori)
    videoToEditCategory?.let { targetVideo ->
        var selectedCats by remember(targetVideo.id) {
            mutableStateOf(targetVideo.categoriesList().toSet())
        }

        AlertDialog(
            onDismissRequest = { videoToEditCategory = null },
            title = { Text("Atur Kategori Video", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = targetVideo.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Pilih satu atau beberapa kategori untuk video ini:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    customCategories.forEach { categoryName ->
                        val isChecked = selectedCats.contains(categoryName)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadFocusable(
                                    shape = RoundedCornerShape(14.dp),
                                    style = FocusStyle.PILL,
                                    onClick = {
                                        selectedCats = if (isChecked) {
                                            selectedCats - categoryName
                                        } else {
                                            selectedCats + categoryName
                                        }
                                    }
                                )
                                .clickable {
                                    selectedCats = if (isChecked) {
                                        selectedCats - categoryName
                                    } else {
                                        selectedCats + categoryName
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = categoryName,
                                    color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalCats = if (selectedCats.isEmpty()) "Umum" else selectedCats.joinToString(", ")
                        viewModel.updateVideoCategory(targetVideo, finalCats)
                        videoToEditCategory = null
                    },
                    modifier = Modifier.dpadFocusable(
                        shape = RoundedCornerShape(8.dp),
                        style = FocusStyle.CTA,
                        onClick = {
                            val finalCats = if (selectedCats.isEmpty()) "Umum" else selectedCats.joinToString(", ")
                            viewModel.updateVideoCategory(targetVideo, finalCats)
                            videoToEditCategory = null
                        }
                    )
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { videoToEditCategory = null },
                    modifier = Modifier.dpadFocusable(
                        shape = RoundedCornerShape(8.dp),
                        style = FocusStyle.CTA,
                        onClick = { videoToEditCategory = null }
                    )
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // Dialog Konfirmasi Hapus Video
    videoToDelete?.let { video ->
        val deleteBtnFocus = remember { FocusRequester() }
        LaunchedEffect(video.id) {
            delay(50)
            try {
                deleteBtnFocus.requestFocus()
            } catch (_: Exception) {
            }
        }

        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Hapus Video?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = "Video \"${video.title}\" beserta file fisiknya akan dihapus.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video)
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .focusRequester(deleteBtnFocus)
                        .dpadFocusable(
                            shape = RoundedCornerShape(50),
                            style = FocusStyle.CTA,
                            onClick = {
                                viewModel.deleteVideo(video)
                                videoToDelete = null
                            }
                        )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { videoToDelete = null },
                    modifier = Modifier.dpadFocusable(
                        shape = RoundedCornerShape(8.dp),
                        style = FocusStyle.CTA,
                        onClick = { videoToDelete = null }
                    )
                ) {
                    Text("Batal", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

@Composable
fun HeroBanner(
    video: DownloadedVideoEntity,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 4.dp else 8.dp
            )
            .dpadFocusable(
                shape = RoundedCornerShape(14.dp),
                onClick = onPlay
            )
            .clickable(onClick = onPlay)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 164.dp else 200.dp)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x99000000),
                                Color(0xEE111111)
                            )
                        )
                    )
            )

            // Tombol Acak Banner di Sudut Kanan Atas
            IconButton(
                onClick = onShuffle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .dpadFocusable(
                        shape = CircleShape,
                        style = FocusStyle.ICON,
                        onClick = onShuffle
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Ganti Banner",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Info Video di Bagian Bawah Banner
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SOROTAN UTAMA",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${video.resolution}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    if (video.playCount > 0) {
                        Text(
                            text = "• Diputar ${video.playCount}x",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.focusProperties { canFocus = false }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Putar Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HomeSectionRow(
    title: String,
    videos: List<DownloadedVideoEntity>,
    isTv: Boolean,
    onPlayVideo: (DownloadedVideoEntity) -> Unit,
    onOpenOptions: (DownloadedVideoEntity) -> Unit,
    onEditCategory: (DownloadedVideoEntity) -> Unit,
    onDelete: (DownloadedVideoEntity) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onPlayAll: (() -> Unit)? = null,
    onPlayRandom: (() -> Unit)? = null
) {
    val horizontalPad = if (compact) 12.dp else 16.dp
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = if (compact) 16.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (onPlayAll != null) {
                Surface(
                    onClick = onPlayAll,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(if (compact) 28.dp else 32.dp)
                        .dpadFocusable(
                            shape = CircleShape,
                            style = FocusStyle.CTA,
                            onClick = onPlayAll
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar Berurutan Sesuai Abjad",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                        )
                    }
                }
            }

            if (onPlayRandom != null) {
                Surface(
                    onClick = onPlayRandom,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(if (compact) 28.dp else 32.dp)
                        .dpadFocusable(
                            shape = CircleShape,
                            style = FocusStyle.CTA,
                            onClick = onPlayRandom
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Putar Acak (Random)",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${videos.size} video",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPad),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = videos, key = { it.id }) { video ->
                Box(modifier = Modifier.width(if (compact) 192.dp else 240.dp)) {
                    HomeVideoCard(
                        video = video,
                        isTv = isTv,
                        onClick = {
                            if (video.status == DownloadStatus.COMPLETED && video.localFilePath.isNotEmpty()) {
                                onPlayVideo(video)
                            }
                        },
                        onOpenOptions = { onOpenOptions(video) },
                        onEditCategory = { onEditCategory(video) },
                        onDelete = { onDelete(video) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeVideoCard(
    video: DownloadedVideoEntity,
    isTv: Boolean = false,
    onClick: () -> Unit,
    onOpenOptions: () -> Unit = {},
    onEditCategory: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .dpadFocusable(
                shape = RoundedCornerShape(16.dp),
                onClick = onClick
            )
            .onPreviewKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val action = keyEvent.nativeKeyEvent.action

                // Tombol Menu / Info / Del / Merah di remote langsung membuka Opsi Video (Hapus / Kategori)
                if (code == KeyEvent.KEYCODE_MENU ||
                    code == KeyEvent.KEYCODE_INFO ||
                    code == KeyEvent.KEYCODE_DEL ||
                    code == KeyEvent.KEYCODE_FORWARD_DEL ||
                    code == KeyEvent.KEYCODE_PROG_RED
                ) {
                    if (action == KeyEvent.ACTION_UP) {
                        onOpenOptions()
                    }
                    return@onPreviewKeyEvent true
                }

                // Tahan OK / Enter (~650ms) untuk membuka Opsi Video; tekan singkat untuk Putar Video
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
                                onOpenOptions()
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
                onLongClick = onOpenOptions
            )
    ) {
        Column {
            // Thumbnail dengan Badge Status & Durasi
            val isPreparingMetadata = video.status != DownloadStatus.COMPLETED && video.title.isBlank()
            val skeletonAlpha = if (isPreparingMetadata || video.thumbnailUrl.isBlank()) {
                val transition = rememberInfiniteTransition(label = "card_skeleton")
                transition.animateFloat(
                    initialValue = 0.28f,
                    targetValue = 0.62f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 750),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "card_skeleton_alpha"
                ).value
            } else {
                0.4f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF232323))
            ) {
                if (video.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF333333).copy(alpha = skeletonAlpha))
                    )
                }

                // Overlay progres unduhan: sebelah kiri sesuai persentase terang, sisanya gelap
                if (video.status == DownloadStatus.DOWNLOADING || video.status == DownloadStatus.QUEUED) {
                    val progressFraction = if (video.status == DownloadStatus.QUEUED) {
                        0f
                    } else {
                        (video.progress.coerceIn(0, 100) / 100f)
                    }
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (progressFraction > 0f) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(progressFraction)
                            )
                        }
                        if (progressFraction in 0.01f..0.99f) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        if (1f - progressFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f - progressFraction)
                                    .background(Color.Black.copy(alpha = 0.72f))
                            )
                        }
                    }
                }

                // Badge Durasi / Status
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (video.status) {
                            DownloadStatus.COMPLETED -> StorageUtils.formatDuration(video.durationSeconds)
                            DownloadStatus.DOWNLOADING -> "${video.progress}%"
                            DownloadStatus.QUEUED -> "0%"
                            DownloadStatus.FAILED -> "Gagal"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Badge Kategori (Touch/Mouse; di TV diakses lewat Tahan OK / Menu)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .focusProperties { canFocus = !isTv }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onEditCategory)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        val cats = video.categoriesList()
                        val catLabel = when {
                            cats.size > 1 -> "${cats.first()} (+${cats.size - 1})"
                            cats.isNotEmpty() -> cats.first()
                            else -> "Umum"
                        }
                        Text(
                            text = catLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play Icon Overlay hanya di TV saat Completed
                if (isTv && video.status == DownloadStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(4.dp)
                    )
                }
            }

            // Progress bar saat sedang mengunduh / menyiapkan (0%)
            if (video.status == DownloadStatus.DOWNLOADING || video.status == DownloadStatus.QUEUED) {
                LinearProgressIndicator(
                    progress = { (video.progress.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            // Info Detail Video (atau Skeleton saat metadata sedang diambil)
            Column(modifier = Modifier.padding(12.dp)) {
                if (isPreparingMetadata) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(14.dp)
                            .background(
                                Color(0xFF383838).copy(alpha = skeletonAlpha),
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.54f)
                            .height(12.dp)
                            .background(
                                Color(0xFF303030).copy(alpha = skeletonAlpha),
                                RoundedCornerShape(4.dp)
                            )
                    )
                } else {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${video.resolution} • ${StorageUtils.formatFileSize(video.fileSize)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        )
                        if (video.playCount > 0) {
                            Text(
                                text = "Diputar ${video.playCount}x",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF90CAF9),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (!isTv) {
                        IconButton(
                            onClick = onOpenOptions,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opsi Video",
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
