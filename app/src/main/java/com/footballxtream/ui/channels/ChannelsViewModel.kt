package com.footballxtream.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.local.FavoriteChannelDao
import com.footballxtream.data.local.FavoriteChannelEntity
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.QualityMode
import com.footballxtream.player.PlaybackSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChannelRow(val title: String, val groups: List<ChannelGroup>)

sealed interface ChannelsUiState {
    data object Loading : ChannelsUiState
    data class Error(val message: String) : ChannelsUiState
    data class Content(
        val rows: List<ChannelRow>,
        val qualityMode: QualityMode,
    ) : ChannelsUiState
}

class ChannelsViewModel(
    private val repository: ContentRepository,
    private val favoriteDao: FavoriteChannelDao,
    private val settingsStore: com.footballxtream.data.local.SettingsStore,
    private val playbackSession: PlaybackSession,
) : ViewModel() {

    private sealed interface Load {
        data object Loading : Load
        data class Error(val message: String) : Load
        data class Data(val groups: List<ChannelGroup>) : Load
    }

    private val load = MutableStateFlow<Load>(Load.Loading)

    val favoriteIds: StateFlow<Set<Int>> = favoriteDao.observeAll()
        .map { entities -> entities.map { it.streamId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val uiState: StateFlow<ChannelsUiState> =
        combine(load, favoriteIds, settingsStore.qualityMode) { loadState, favorites, mode ->
            when (loadState) {
                Load.Loading -> ChannelsUiState.Loading
                is Load.Error -> ChannelsUiState.Error(loadState.message)
                is Load.Data -> ChannelsUiState.Content(
                    rows = buildRows(loadState.groups, favorites, mode),
                    qualityMode = mode,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChannelsUiState.Loading)

    init {
        refresh()
    }

    fun refresh(forceRefresh: Boolean = false) {
        load.value = Load.Loading
        viewModelScope.launch {
            load.value = repository.loadLiveGroups(forceRefresh).fold(
                onSuccess = { Load.Data(it) },
                onFailure = { Load.Error("No se pudieron cargar los canales.") },
            )
        }
    }

    /** Forces a fresh download, bypassing the M3U cache. */
    fun reload() = refresh(forceRefresh = true)

    fun selectQuality(mode: QualityMode) {
        viewModelScope.launch { settingsStore.setQualityMode(mode) }
    }

    fun toggleFavorite(group: ChannelGroup) {
        viewModelScope.launch {
            val favorites = favoriteIds.value
            val owned = group.variants.map { it.channel.streamId }.filter { favorites.contains(it) }
            if (owned.isNotEmpty()) {
                owned.forEach { favoriteDao.remove(it) }
            } else {
                favoriteDao.add(FavoriteChannelEntity.from(group.bestVariant().channel))
            }
        }
    }

    fun play(group: ChannelGroup, onReady: () -> Unit) {
        val content = uiState.value as? ChannelsUiState.Content ?: return
        val playlist = content.rows.flatMap { it.groups }
        playbackSession.start(playlist, playlist.indexOf(group).coerceAtLeast(0))
        onReady()
    }

    private fun buildRows(
        groups: List<ChannelGroup>,
        favorites: Set<Int>,
        mode: QualityMode,
    ): List<ChannelRow> {
        val fixed = mode.fixedQuality
        val visible = if (fixed == null) {
            groups
        } else {
            groups.filter { it.availableQualities.contains(fixed) }
        }

        fun isFavorite(group: ChannelGroup) =
            group.variants.any { favorites.contains(it.channel.streamId) }

        val favoriteGroups = visible.filter(::isFavorite)
        val footballGroups = visible.filter { it.isFootball && !isFavorite(it) }
        val otherGroups = visible.filter { !it.isFootball && !isFavorite(it) }

        return buildList {
            if (favoriteGroups.isNotEmpty()) add(ChannelRow("Favoritos", favoriteGroups))
            if (footballGroups.isNotEmpty()) add(ChannelRow("Fútbol", footballGroups))
            if (otherGroups.isNotEmpty()) add(ChannelRow("Más deporte", otherGroups))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                ChannelsViewModel(
                    container.repository,
                    container.favoriteChannelDao,
                    container.settingsStore,
                    container.playbackSession,
                )
            }
        }
    }
}
