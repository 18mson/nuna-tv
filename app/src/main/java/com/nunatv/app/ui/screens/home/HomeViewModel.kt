package com.nunatv.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: VideoRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    val videos: StateFlow<List<DownloadedVideoEntity>> = repository.allVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customCategories: StateFlow<Set<String>> = appPreferences.customCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = setOf("Umum", "Musik", "Gaming", "Edukasi")
        )

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _randomSeed = MutableStateFlow(System.currentTimeMillis())
    val randomSeed: StateFlow<Long> = _randomSeed.asStateFlow()

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun refreshRandom() {
        _randomSeed.value = System.currentTimeMillis()
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            appPreferences.addCategory(categoryName)
        }
    }

    fun removeCategory(categoryName: String) {
        viewModelScope.launch {
            appPreferences.removeCategory(categoryName)
            if (_selectedCategory.value == categoryName) {
                _selectedCategory.value = "Semua"
            }
        }
    }

    fun updateVideoCategory(video: DownloadedVideoEntity, newCategory: String) {
        viewModelScope.launch {
            repository.updateCategory(video.id, newCategory)
        }
    }

    fun recordPlay(video: DownloadedVideoEntity) {
        viewModelScope.launch {
            repository.recordPlay(video.id)
        }
    }

    fun deleteVideo(video: DownloadedVideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
        }
    }
}
