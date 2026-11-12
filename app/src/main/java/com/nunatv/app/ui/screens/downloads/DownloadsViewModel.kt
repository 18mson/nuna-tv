package com.nunatv.app.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.repository.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val repository: VideoRepository
) : ViewModel() {

    val videos: StateFlow<List<DownloadedVideoEntity>> = repository.allVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteVideo(video: DownloadedVideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
        }
    }
}
