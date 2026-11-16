package com.nunatv.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscribed_channels")
data class SubscribedChannelEntity(
    @PrimaryKey
    val channelId: String,
    val channelName: String,
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val subscriberCount: String = "",
    val subscribedTimestamp: Long = System.currentTimeMillis()
)
