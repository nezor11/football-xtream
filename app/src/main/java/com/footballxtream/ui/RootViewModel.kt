package com.footballxtream.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.local.ProfileDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartState { LOADING, HAS_PROFILES, NO_PROFILES }

/** Picks the start destination: the profile picker if any profile exists, otherwise add-profile. */
class RootViewModel(private val profileDao: ProfileDao) : ViewModel() {

    private val _state = MutableStateFlow(StartState.LOADING)
    val state: StateFlow<StartState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = if (profileDao.count() > 0) {
                StartState.HAS_PROFILES
            } else {
                StartState.NO_PROFILES
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                RootViewModel(container.profileDao)
            }
        }
    }
}
