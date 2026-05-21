package com.footballxtream.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.XtreamRepository
import com.footballxtream.data.local.FavoriteChannelDao
import com.footballxtream.data.local.FavoriteChannelEntity
import com.footballxtream.model.ChannelCategory
import com.footballxtream.model.LiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChannelRow(
    val title: String,
    val channels: List<LiveChannel>,
    val isFavorites: Boolean = false,
)

sealed interface LiveUiState {
    data object Loading : LiveUiState
    data class Error(val message: String) : LiveUiState
    data class Content(val rows: List<ChannelRow>) : LiveUiState
}

class LiveViewModel(
    private val repository: XtreamRepository,
    private val favoriteDao: FavoriteChannelDao,
) : ViewModel() {

    private sealed interface Load {
        data object Loading : Load
        data class Error(val message: String) : Load
        data class Data(
            val categories: List<ChannelCategory>,
            val streams: List<LiveChannel>,
        ) : Load
    }

    private val load = MutableStateFlow<Load>(Load.Loading)

    val favoriteIds: StateFlow<Set<Int>> = favoriteDao.observeAll()
        .map { entities -> entities.map { it.streamId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val uiState: StateFlow<LiveUiState> =
        combine(load, favoriteDao.observeAll()) { loadState, favorites ->
            when (loadState) {
                Load.Loading -> LiveUiState.Loading
                is Load.Error -> LiveUiState.Error(loadState.message)
                is Load.Data -> LiveUiState.Content(
                    buildRows(loadState, favorites.map { it.toModel() }),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        load.value = Load.Loading
        viewModelScope.launch {
            val categories = repository.liveCategories()
            val streams = repository.liveStreams()
            load.value = if (categories.isSuccess && streams.isSuccess) {
                Load.Data(categories.getOrThrow(), streams.getOrThrow())
            } else {
                Load.Error("No se pudieron cargar los canales.")
            }
        }
    }

    fun toggleFavorite(channel: LiveChannel) {
        viewModelScope.launch {
            if (favoriteIds.value.contains(channel.streamId)) {
                favoriteDao.remove(channel.streamId)
            } else {
                favoriteDao.add(FavoriteChannelEntity.from(channel))
            }
        }
    }

    private fun buildRows(data: Load.Data, favorites: List<LiveChannel>): List<ChannelRow> {
        val byCategory = data.streams.groupBy { it.categoryId }
        val orderedCategories = data.categories.sortedWith(
            compareByDescending<ChannelCategory> { it.name.isSportsRelated() }
                .thenBy { it.name.lowercase() },
        )

        val rows = mutableListOf<ChannelRow>()
        if (favorites.isNotEmpty()) {
            rows += ChannelRow(title = "Favoritos", channels = favorites, isFavorites = true)
        }
        orderedCategories.forEach { category ->
            val channels = byCategory[category.id].orEmpty()
            if (channels.isNotEmpty()) {
                rows += ChannelRow(title = category.name, channels = channels)
            }
        }
        return rows
    }

    companion object {
        private val SPORTS_KEYWORDS = listOf(
            "sport", "deporte", "football", "futbol", "fútbol", "soccer",
            "espn", "dazn", "bein", "laliga", "premier", "champions",
            "nba", "nfl", "ufc", "f1", "motogp", "tennis", "tenis",
        )

        private fun String.isSportsRelated(): Boolean {
            val lower = lowercase()
            return SPORTS_KEYWORDS.any { lower.contains(it) }
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                LiveViewModel(container.repository, container.favoriteChannelDao)
            }
        }
    }
}
