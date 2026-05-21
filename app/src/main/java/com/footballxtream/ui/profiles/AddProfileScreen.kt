package com.footballxtream.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.ui.components.TvTextField

@Composable
fun AddProfileScreen(
    onSaved: () -> Unit,
    viewModel: AddProfileViewModel = viewModel(factory = AddProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { serverFocus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Conecta tu servidor Xtream",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        val fieldModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()

        TvTextField(
            value = state.server,
            onValueChange = viewModel::onServerChange,
            label = "URL del servidor (http://host:puerto)",
            modifier = fieldModifier,
            keyboardType = KeyboardType.Uri,
            focusRequester = serverFocus,
        )
        TvTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = "Usuario",
            modifier = fieldModifier,
        )
        TvTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Contraseña",
            modifier = fieldModifier,
            isPassword = true,
            keyboardType = KeyboardType.Password,
        )
        TvTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = "Nombre del perfil (opcional)",
            modifier = fieldModifier,
        )

        if (state.error != null) {
            Text(text = state.error.orEmpty(), color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = { viewModel.save(onSaved) },
            enabled = state.canSubmit,
        ) {
            Text(text = if (state.isConnecting) "Conectando…" else "Guardar y entrar")
        }
    }
}
