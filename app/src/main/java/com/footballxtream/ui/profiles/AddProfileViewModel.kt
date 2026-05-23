package com.footballxtream.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.local.ProfileDao
import com.footballxtream.data.local.ProfileEntity
import com.footballxtream.data.local.ProfileType
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddProfileUiState(
    val mode: String = ProfileType.XTREAM,
    val name: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val isConnecting: Boolean = false,
    val error: String? = null,
) {
    val isM3u: Boolean get() = mode == ProfileType.M3U

    val canSubmit: Boolean
        get() = !isConnecting && if (isM3u) {
            m3uUrl.isNotBlank()
        } else {
            server.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        }
}

class AddProfileViewModel(
    private val profileDao: ProfileDao,
    private val repository: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddProfileUiState())
    val state: StateFlow<AddProfileUiState> = _state.asStateFlow()

    fun onModeChange(mode: String) = _state.update { it.copy(mode = mode, error = null) }
    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onServerChange(value: String) = _state.update { it.copy(server = value, error = null) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onM3uUrlChange(value: String) = _state.update { it.copy(m3uUrl = value, error = null) }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isConnecting = true, error = null) }

        viewModelScope.launch {
            if (current.isM3u) {
                val url = current.m3uUrl.trim()
                repository.validateM3u(url)
                    .onSuccess {
                        profileDao.upsert(
                            ProfileEntity(
                                name = current.name.ifBlank { "Lista M3U" },
                                type = ProfileType.M3U,
                                m3uUrl = url,
                            ),
                        )
                        onSaved()
                    }
                    .onFailure { fail("No se pudo cargar la lista M3U. Revisa la URL.") }
            } else {
                val profile = XtreamProfile(
                    name = current.name.ifBlank { current.username },
                    serverUrl = current.server,
                    username = current.username,
                    password = current.password,
                )
                repository.validateXtream(profile)
                    .onSuccess {
                        profileDao.upsert(
                            ProfileEntity(
                                name = profile.name,
                                type = ProfileType.XTREAM,
                                serverUrl = profile.serverUrl,
                                username = profile.username,
                                password = profile.password,
                            ),
                        )
                        onSaved()
                    }
                    .onFailure { fail("No se pudo conectar. Revisa servidor, usuario y contraseña.") }
            }
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(isConnecting = false, error = message) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                AddProfileViewModel(container.profileDao, container.repository)
            }
        }
    }
}
