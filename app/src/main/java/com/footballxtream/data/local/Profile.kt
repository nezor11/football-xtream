package com.footballxtream.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = ProfileType.XTREAM,
    // Xtream fields (blank for M3U profiles):
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    // M3U field (blank for Xtream profiles):
    val m3uUrl: String = "",
) {
    val isM3u: Boolean get() = type == ProfileType.M3U

    fun toXtreamProfile() = XtreamProfile(
        name = name,
        serverUrl = serverUrl,
        username = username,
        password = password,
    )
}

object ProfileType {
    const val XTREAM = "XTREAM"
    const val M3U = "M3U"
}

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity): Long

    @Delete
    suspend fun delete(profile: ProfileEntity)
}
