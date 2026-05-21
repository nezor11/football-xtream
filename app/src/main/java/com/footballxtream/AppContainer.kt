package com.footballxtream

import android.content.Context
import androidx.room.Room
import com.footballxtream.data.XtreamRepository
import com.footballxtream.data.local.AppDatabase
import com.footballxtream.data.local.FavoriteChannelDao
import com.footballxtream.data.local.ProfileStore

/** Manual dependency container. One instance lives in [FootballXtreamApp]. */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "football-xtream.db",
    ).build()

    val profileStore: ProfileStore = ProfileStore(context.applicationContext)
    val favoriteChannelDao: FavoriteChannelDao = database.favoriteChannelDao()
    val repository: XtreamRepository = XtreamRepository()
}
