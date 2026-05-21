package com.footballxtream.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.ChannelNameParser
import com.footballxtream.data.XtreamRepository
import com.footballxtream.data.local.FavoriteChannelDao
import com.footballxtream.data.local.FavoriteChannelEntity
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.model.ChannelCategory
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.ChannelVariant
import com.footballxtream.model.LiveChannel
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
    private val repository: XtreamRepository,
    private val favoriteDao: FavoriteChannelDao,
    private val settingsStore: SettingsStore,
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

    fun refresh() {
        load.value = Load.Loading
        viewModelScope.launch {
            val categories = repository.liveCategories()
            val streams = repository.liveStreams()
            load.value = if (categories.isSuccess && streams.isSuccess) {
                Load.Data(buildGroups(streams.getOrThrow(), categories.getOrThrow()))
            } else {
                Load.Error("No se pudieron cargar los canales.")
            }
        }
    }

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
        playbackSession.current = group
        onReady()
    }

    private fun buildGroups(
        streams: List<LiveChannel>,
        categories: List<ChannelCategory>,
    ): List<ChannelGroup> {
        val categoryName: (String?) -> String? = { id ->
            categories.firstOrNull { it.id == id }?.name
        }

        val sportsStreams = streams.filter { stream ->
            ChannelNameParser.isSports(stream.name, categoryName(stream.categoryId))
        }

        return sportsStreams
            .groupBy { ChannelNameParser.groupKey(it.name).ifBlank { it.name.lowercase() } }
            .map { (key, channels) ->
                val variants = channels
                    .map { ChannelVariant(it, ChannelNameParser.quality(it.name)) }
                    .sortedByDescending { it.quality.rank }
                val representative = variants.first().channel
                ChannelGroup(
                    key = key,
                    displayName = ChannelNameParser.baseName(representative.name)
                        .ifBlank { representative.name },
                    iconUrl = variants.firstNotNullOfOrNull { it.channel.iconUrl },
                    isFootball = channels.any {
                        ChannelNameParser.isFootball(it.name, categoryName(it.categoryId))
                    },
                    variants = variants,
                )
            }
            .sortedBy { it.displayName.lowercase() }
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
