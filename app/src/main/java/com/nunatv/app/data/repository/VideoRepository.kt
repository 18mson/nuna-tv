package com.nunatv.app.data.repository

import com.nunatv.app.data.local.DownloadedVideoEntity
import com.nunatv.app.data.local.VideoDao
import kotlinx.coroutines.flow.Flow
import java.io.File

class VideoRepository(private val videoDao: VideoDao) {

    val allVideos: Flow<List<DownloadedVideoEntity>> = videoDao.getAllVideos()
    val completedVideos: Flow<List<DownloadedVideoEntity>> = videoDao.getCompletedVideos()

    suspend fun getVideoById(id: Long): DownloadedVideoEntity? = videoDao.getVideoById(id)

    suspend fun insertVideo(video: DownloadedVideoEntity): Long = videoDao.insertVideo(video)

    suspend fun recordPlay(id: Long) = videoDao.incrementPlayCount(id)

    suspend fun updateCategory(id: Long, category: String) = videoDao.updateCategory(id, category)

    suspend fun deleteVideo(video: DownloadedVideoEntity) {
        // Hapus file fisik jika ada
        if (video.localFilePath.isNotEmpty()) {
            val file = File(video.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
        videoDao.deleteVideo(video)
    }
}
