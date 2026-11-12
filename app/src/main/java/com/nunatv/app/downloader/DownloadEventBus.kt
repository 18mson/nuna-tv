package com.nunatv.app.downloader

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class DownloadErrorEvent(
    val videoTitle: String,
    val message: String,
    val isStorageFull: Boolean
)

object DownloadEventBus {
    private val _errors = MutableSharedFlow<DownloadErrorEvent>(extraBufferCapacity = 4)
    val errors: SharedFlow<DownloadErrorEvent> = _errors.asSharedFlow()

    fun emitError(event: DownloadErrorEvent) {
        _errors.tryEmit(event)
    }
}
