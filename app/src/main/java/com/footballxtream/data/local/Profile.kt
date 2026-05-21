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
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    fun toModel() = XtreamProfile(
        name = name,
        serverUrl = serverUrl,
        username = username,
        password = password,
    )
}

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun byId(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity): Long

    @Delete
    suspend fun delete(profile: ProfileEntity)
}
