package com.footballxtream

import android.content.Context
import androidx.room.Room
import com.footballxtream.data.XtreamRepository
import com.footballxtream.data.local.AppDatabase
import com.footballxtream.data.local.FavoriteChannelDao
import com.footballxtream.data.local.ProfileDao
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.player.PlaybackSession
import com.footballxtream.player.PlayerEngine

/** Manual dependency container. One instance lives in [FootballXtreamApp]. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "football-xtream.db",
    ).fallbackToDestructiveMigration().build()

    val profileDao: ProfileDao = database.profileDao()
    val favoriteChannelDao: FavoriteChannelDao = database.favoriteChannelDao()
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val repository: XtreamRepository = XtreamRepository()
    val playbackSession: PlaybackSession = PlaybackSession()
    val playerEngine: PlayerEngine = PlayerEngine(appContext, settingsStore)
}
