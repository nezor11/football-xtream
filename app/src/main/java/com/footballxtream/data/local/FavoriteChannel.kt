package com.footballxtream.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.footballxtream.model.LiveChannel
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey val streamId: Int,
    val name: String,
    val iconUrl: String?,
) {
    companion object {
        fun from(channel: LiveChannel) = FavoriteChannelEntity(
            streamId = channel.streamId,
            name = channel.name,
            iconUrl = channel.iconUrl,
        )
    }
}

@Dao
interface FavoriteChannelDao {

    @Query("SELECT * FROM favorite_channels ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FavoriteChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(channel: FavoriteChannelEntity)

    @Query("DELETE FROM favorite_channels WHERE streamId = :streamId")
    suspend fun remove(streamId: Int)
}
