package com.footballxtream.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.XtreamRepository
import com.footballxtream.data.local.ProfileStore
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val name: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val isConnecting: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = server.isNotBlank() && username.isNotBlank() &&
            password.isNotBlank() && !isConnecting
}

class LoginViewModel(
    private val profileStore: ProfileStore,
    private val repository: XtreamRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onServerChange(value: String) = _state.update { it.copy(server = value, error = null) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun connect(onSuccess: () -> Unit) {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isConnecting = true, error = null) }

        val profile = XtreamProfile(
            name = current.name.ifBlank { current.server },
            serverUrl = current.server,
            username = current.username,
            password = current.password,
        )

        viewModelScope.launch {
            repository.login(profile)
                .onSuccess {
                    profileStore.save(profile)
                    onSuccess()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            error = "No se pudo conectar. Revisa servidor, usuario y contraseña.",
                        )
                    }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                LoginViewModel(container.profileStore, container.repository)
            }
        }
    }
}
