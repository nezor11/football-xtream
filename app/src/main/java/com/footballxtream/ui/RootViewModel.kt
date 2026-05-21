package com.footballxtream.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.XtreamRepository
import com.footballxtream.data.local.ProfileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class StartState { LOADING, AUTHENTICATED, NEEDS_LOGIN }

/** Decides the start destination: restores a saved profile or sends the user to login. */
class RootViewModel(
    private val profileStore: ProfileStore,
    private val repository: XtreamRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StartState.LOADING)
    val state: StateFlow<StartState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileStore.profile.first()
            _state.value = if (profile != null && profile.isComplete) {
                repository.bind(profile)
                StartState.AUTHENTICATED
            } else {
                StartState.NEEDS_LOGIN
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                RootViewModel(container.profileStore, container.repository)
            }
        }
    }
}
