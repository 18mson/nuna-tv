package com.nunatv.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM subscribed_channels ORDER BY subscribedTimestamp DESC")
    fun getAllSubscriptions(): Flow<List<SubscribedChannelEntity>>

    @Query("SELECT * FROM subscribed_channels WHERE channelId = :channelId LIMIT 1")
    suspend fun getSubscription(channelId: String): SubscribedChannelEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM subscribed_channels WHERE channelId = :channelId)")
    fun isSubscribed(channelId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun subscribe(channel: SubscribedChannelEntity)

    @Query("DELETE FROM subscribed_channels WHERE channelId = :channelId")
    suspend fun unsubscribe(channelId: String)
}
