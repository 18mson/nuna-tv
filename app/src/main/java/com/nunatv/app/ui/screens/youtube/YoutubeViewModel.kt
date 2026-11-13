package com.nunatv.app.ui.screens.youtube

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.repository.YoutubeContentFilter
import com.nunatv.app.data.repository.YoutubeSearchItem
import com.nunatv.app.data.repository.YoutubeSearchRepository
import com.nunatv.app.data.repository.YoutubeSortOption
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class YoutubeTabUiState(
    val searchQuery: String = "",
    val activeCategory: String = "FOR_YOU",
    val contentFilter: YoutubeContentFilter = YoutubeContentFilter.ALL,
    val sortOption: YoutubeSortOption = YoutubeSortOption.DEFAULT,
    val activeChannel: ChannelDestination? = null,
    val results: List<YoutubeSearchItem> = emptyList(),
    val shortsList: List<YoutubeSearchItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false
)

class YoutubeViewModel(
    private val appContext: Context,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(YoutubeTabUiState())
    val uiState: StateFlow<YoutubeTabUiState> = _uiState.asStateFlow()

    // Pertahankan posisi scroll di mobile maupun TV saat berpindah tab atau kembali dari player
    val gridState: LazyGridState = LazyGridState()
    val mobileListState: LazyListState = LazyListState()

    fun ensureInitialFeedLoaded() {
        val current = _uiState.value
        if (!current.hasLoadedOnce && current.activeCategory == "FOR_YOU" && !current.isLoading) {
            loadRecommendedFeed(force = false)
        }
    }

    fun loadRecommendedFeed(force: Boolean = true, asRefresh: Boolean = false) {
        val current = _uiState.value
        if (!force && current.hasLoadedOnce && current.results.isNotEmpty() && current.activeCategory == "FOR_YOU") {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeCategory = "FOR_YOU",
                    searchQuery = "",
                    activeChannel = null,
                    contentFilter = YoutubeContentFilter.ALL,
                    isLoading = !asRefresh && it.results.isEmpty(),
                    isRefreshing = asRefresh,
                    errorMessage = null
                )
            }
            val lastId = appPreferences.lastWatchedVideoId.first()
            val lastTitle = appPreferences.lastWatchedVideoTitle.first()
            val searches = appPreferences.recentSearchQueries.first()

            val mainDeferred = async {
                YoutubeSearchRepository.getRecommendedFeed(
                    context = appContext,
                    lastVideoId = lastId,
                    lastVideoTitle = lastTitle,
                    recentSearches = searches
                )
            }
            val shortsDeferred = async {
                YoutubeSearchRepository.getShortsFeed(
                    context = appContext,
                    topic = searches.firstOrNull().orEmpty()
                )
            }

            val res = mainDeferred.await()
            val fetchedShorts = shortsDeferred.await().getOrDefault(emptyList())

            res.fold(
                onSuccess = { list ->
                    val inlineShorts = list.filter { it.isShort }
                    val mergedShorts = (inlineShorts + fetchedShorts).distinctBy { it.videoId }
                    val regularVideos = list.filterNot { it.isShort }.ifEmpty { list }
                    _uiState.update {
                        it.copy(
                            results = regularVideos,
                            shortsList = mergedShorts.ifEmpty { it.shortsList },
                            isLoading = false,
                            isRefreshing = false,
                            hasLoadedOnce = true,
                            errorMessage = if (regularVideos.isEmpty()) "Belum ada video tersedia." else null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            shortsList = fetchedShorts.ifEmpty { it.shortsList },
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = "Gagal memuat video: ${err.message ?: "Periksa koneksi internet"}"
                        )
                    }
                }
            )
        }
    }

    fun loadShortsFeed(topic: String = "", asRefresh: Boolean = false) {
        viewModelScope.launch {
            val existingShorts = _uiState.value.shortsList
            _uiState.update {
                it.copy(
                    activeCategory = "SHORTS",
                    activeChannel = null,
                    contentFilter = YoutubeContentFilter.ALL,
                    isLoading = !asRefresh && existingShorts.isEmpty(),
                    isRefreshing = asRefresh,
                    errorMessage = null
                )
            }
            val res = YoutubeSearchRepository.getShortsFeed(appContext, topic)
            res.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            results = list,
                            shortsList = list.ifEmpty { it.shortsList },
                            isLoading = false,
                            isRefreshing = false,
                            hasLoadedOnce = true,
                            errorMessage = if (list.isEmpty()) "Belum ada video Shorts ditemukan." else null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            results = existingShorts,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = if (existingShorts.isEmpty()) {
                                "Gagal memuat Shorts: ${err.message ?: "Periksa koneksi internet"}"
                            } else null
                        )
                    }
                }
            )
        }
    }

    fun refreshActiveTab() {
        val current = _uiState.value
        when {
            current.activeCategory == "FOR_YOU" -> loadRecommendedFeed(force = true, asRefresh = true)
            current.activeCategory == "SHORTS" -> loadShortsFeed(topic = current.searchQuery, asRefresh = true)
            current.activeCategory == "CHANNEL" && current.activeChannel != null -> openChannelPage(current.activeChannel)
            current.activeCategory == "WATCH_HISTORY" -> _uiState.update { it.copy(isRefreshing = false) }
            else -> performSearch(
                query = current.searchQuery.ifBlank { current.activeCategory.removePrefix("HIST:").ifBlank { "Trending Indonesia" } },
                categoryKey = current.activeCategory,
                asRefresh = true
            )
        }
    }

    fun performSearch(
        query: String,
        categoryKey: String = "",
        saveToHistory: Boolean = false,
        asRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            if (saveToHistory && query.isNotBlank()) {
                appPreferences.addSearchHistory(query)
            }
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    activeCategory = categoryKey,
                    activeChannel = null,
                    isLoading = !asRefresh,
                    isRefreshing = asRefresh,
                    errorMessage = null
                )
            }
            val res = YoutubeSearchRepository.search(appContext, query)
            res.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            results = list,
                            isLoading = false,
                            isRefreshing = false,
                            hasLoadedOnce = true,
                            errorMessage = if (list.isEmpty()) "Video tidak ditemukan untuk \"$query\"." else null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = "Gagal memuat hasil: ${err.message ?: "Periksa koneksi internet"}"
                        )
                    }
                }
            )
        }
    }

    fun selectWatchHistoryMode() {
        _uiState.update {
            it.copy(
                activeCategory = "WATCH_HISTORY",
                searchQuery = "",
                activeChannel = null,
                errorMessage = null
            )
        }
    }

    fun openChannelPage(dest: ChannelDestination) {
        _uiState.update {
            it.copy(
                activeCategory = "CHANNEL",
                searchQuery = "",
                contentFilter = YoutubeContentFilter.ALL,
                activeChannel = dest,
                isLoading = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            var targetId = dest.channelId
            var targetName = dest.channelName
            if ((targetId.isBlank() || !targetId.startsWith("UC")) && dest.sourceVideoId.isNotBlank()) {
                val owner = YoutubeSearchRepository.fetchVideoChannelInfo(dest.sourceVideoId)
                if (owner != null) {
                    if (owner.first.isNotBlank()) targetId = owner.first
                    if (owner.second.isNotBlank()) targetName = owner.second
                }
            }
            val resolvedDest = dest.copy(
                channelId = targetId,
                channelName = targetName.ifBlank { "Channel YouTube" }
            )
            _uiState.update { it.copy(activeChannel = resolvedDest) }

            val res = YoutubeSearchRepository.getChannelData(appContext, targetId, targetName, dest.sourceVideoId)
            res.fold(
                onSuccess = { channelData ->
                    _uiState.update {
                        it.copy(
                            activeChannel = channelData.channel,
                            results = channelData.videos,
                            isLoading = false,
                            hasLoadedOnce = true,
                            errorMessage = if (channelData.videos.isEmpty()) "Belum ada video di channel ini." else null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal memuat channel: ${err.message ?: "Periksa koneksi internet"}"
                        )
                    }
                }
            )
        }
    }

    fun updateSearchInput(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setContentFilter(filter: YoutubeContentFilter) {
        _uiState.update { it.copy(contentFilter = filter) }
    }

    fun setSortOption(sort: YoutubeSortOption) {
        _uiState.update { it.copy(sortOption = sort) }
    }
}
