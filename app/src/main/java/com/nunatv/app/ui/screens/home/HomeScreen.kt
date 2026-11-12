package com.nunatv.app.ui.screens.home

import android.view.KeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    // Banner video acak dinamis yang berganti berdasarkan randomSeed
    val heroVideo = remember(completedVideos, randomSeed) {
        if (completedVideos.isNotEmpty()) {
            val rand = Random(randomSeed)
            completedVideos[rand.nextInt(completedVideos.size)]
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        // Baris Kategori Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
        ) {
            val defaultFilters = listOf("Semua", "Terakhir Ditambahkan", "Paling Sering Diputar", "Acak")
            items(items = defaultFilters) { filterName ->
                TvChip(
                    label = filterName,
                    selected = selectedCategory == filterName,
                    onClick = { viewModel.selectCategory(filterName) }
                )
            }

            // Kategori Kustom User
            items(items = customCategories.toList()) { categoryName ->
                TvChip(
                    label = categoryName,
                    selected = selectedCategory == categoryName,
                    onClick = { viewModel.selectCategory(categoryName) }
                )
            }

            // Tombol Tambah Kategori Baru
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

        // Bar status penyimpanan ringkas (menunjukkan sisa ruang bebas secara langsung)
        CompactStorageBar(
            storageInfo = storageInfo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (isTv && videos.isNotEmpty()) {
            Text(
                text = "Tips Remote: Tahan tombol OK atau tekan Menu pada kartu video untuk Hapus / Ubah Kategori",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
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
                        text = "Belum ada video tersimpan di Home",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pilih video di tab YouTube untuk memulai download.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            when (selectedCategory) {
                "Semua" -> {
                    // Tampilan Beranda Dinamis Multi-Kategori (Netflix / Streaming Style)
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Hero Spotlight Banner Video
                        if (heroVideo != null) {
                            item {
                                HeroBanner(
                                    video = heroVideo,
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
                            HomeSectionRow(
                                title = "Terakhir Ditambahkan",
                                videos = videos.sortedByDescending { it.downloadTimestamp },
                                isTv = isTv,
                                onPlayVideo = { video ->
                                    viewModel.recordPlay(video)
                                    onPlayVideo(video)
                                },
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
                                    onPlayVideo = { video ->
                                        viewModel.recordPlay(video)
                                        onPlayVideo(video)
                                    },
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
                                    onPlayVideo = { video ->
                                        viewModel.recordPlay(video)
                                        onPlayVideo(video)
                                    },
                                    onOpenOptions = { videoForOptions = it },
                                    onEditCategory = { videoToEditCategory = it },
                                    onDelete = { videoToDelete = it }
                                )
                            }
                        }

                        // 5. Sections per Kategori Kustom
                        customCategories.forEach { cat ->
                            val categoryVideos = completedVideos.filter { it.category.equals(cat, ignoreCase = true) }
                            if (categoryVideos.isNotEmpty()) {
                                item {
                                    HomeSectionRow(
                                        title = cat,
                                        videos = categoryVideos,
                                        isTv = isTv,
                                        onPlayVideo = { video ->
                                            viewModel.recordPlay(video)
                                            onPlayVideo(video)
                                        },
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
                    // Tampilan Grid Responsif saat Kategori Tertentu Dipilih
                    val filteredList = remember(videos, selectedCategory, mostPlayed, randomList) {
                        when (selectedCategory) {
                            "Terakhir Ditambahkan" -> videos.sortedByDescending { it.downloadTimestamp }
                            "Paling Sering Diputar" -> mostPlayed
                            "Acak" -> randomList
                            else -> videos.filter { it.category.equals(selectedCategory, ignoreCase = true) }
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
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 260.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
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

    // Dialog Tambah Kategori Baru
    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Tambah Kategori Baru", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nama Kategori (misal: Anime, Musik, Vlog)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim())
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Batal", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(8.dp),
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

    // Dialog Ganti Kategori Video
    videoToEditCategory?.let { targetVideo ->
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
                    Spacer(modifier = Modifier.height(4.dp))
                    customCategories.forEach { categoryName ->
                        val isCurrent = targetVideo.category == categoryName
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFFE50914) else Color(0xFF2A2A2A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    style = FocusStyle.PILL,
                                    onClick = {
                                        viewModel.updateVideoCategory(targetVideo, categoryName)
                                        videoToEditCategory = null
                                    }
                                )
                                .clickable {
                                    viewModel.updateVideoCategory(targetVideo, categoryName)
                                    videoToEditCategory = null
                                }
                        ) {
                            Text(
                                text = categoryName,
                                color = Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { videoToEditCategory = null },
                    modifier = Modifier.dpadFocusable(
                        shape = RoundedCornerShape(8.dp),
                        style = FocusStyle.CTA,
                        onClick = { videoToEditCategory = null }
                    )
                ) {
                    Text("Tutup", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
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
            title = { Text("Hapus Video?", color = Color.White) },
            text = {
                Text(
                    text = "Video \"${video.title}\" beserta file fisiknya akan dihapus.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video)
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    modifier = Modifier
                        .focusRequester(deleteBtnFocus)
                        .dpadFocusable(
                            shape = RoundedCornerShape(8.dp),
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
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .dpadFocusable(
                shape = RoundedCornerShape(16.dp),
                onClick = onPlay
            )
            .clickable(onClick = onPlay)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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
                        color = Color(0xFFE50914),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${videos.size} video",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = videos, key = { it.id }) { video ->
                Box(modifier = Modifier.width(240.dp)) {
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = modifier
            .fillMaxWidth()
            .dpadFocusable(
                shape = RoundedCornerShape(12.dp),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF2A2A2A))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Badge Durasi / Status
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (video.status) {
                            DownloadStatus.COMPLETED -> StorageUtils.formatDuration(video.durationSeconds)
                            DownloadStatus.DOWNLOADING -> "${video.progress}%"
                            DownloadStatus.QUEUED -> "Antre"
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
                        .background(Color(0xFFE50914).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onEditCategory)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = video.category,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play Icon Overlay jika Completed
                if (video.status == DownloadStatus.COMPLETED) {
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

            // Progress bar saat sedang mengunduh
            if (video.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { video.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Color(0xFFE50914),
                    trackColor = Color(0xFF333333)
                )
            }

            // Info Detail Video
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

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
                            onClick = onDelete,
                            modifier = Modifier
                                .size(28.dp)
                                .dpadFocusable(
                                    shape = CircleShape,
                                    style = FocusStyle.ICON,
                                    onClick = onDelete
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
