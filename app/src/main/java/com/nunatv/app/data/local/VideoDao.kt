package com.nunatv.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM downloaded_videos ORDER BY downloadTimestamp DESC")
    fun getAllVideos(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE status = 'COMPLETED' ORDER BY downloadTimestamp DESC")
    fun getCompletedVideos(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): DownloadedVideoEntity?

    @Query("SELECT * FROM downloaded_videos WHERE videoId = :videoId LIMIT 1")
    suspend fun getVideoByVideoId(videoId: String): DownloadedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: DownloadedVideoEntity): Long

    @Update
    suspend fun updateVideo(video: DownloadedVideoEntity)

    @Query("UPDATE downloaded_videos SET progress = :progress, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, status: DownloadStatus)

    @Query("UPDATE downloaded_videos SET localFilePath = :filePath, fileSize = :fileSize, status = 'COMPLETED', progress = 100 WHERE id = :id")
    suspend fun markCompleted(id: Long, filePath: String, fileSize: Long)

    @Query("UPDATE downloaded_videos SET status = 'FAILED' WHERE id = :id")
    suspend fun markFailed(id: Long)

    @Query("UPDATE downloaded_videos SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE downloaded_videos SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("SELECT DISTINCT category FROM downloaded_videos WHERE category != ''")
    fun getAllCategories(): Flow<List<String>>

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("DELETE FROM downloaded_videos WHERE status = 'FAILED'")
    suspend fun deleteAllFailed()

    @Delete
    suspend fun deleteVideo(video: DownloadedVideoEntity)
}
