package com.footballxtream.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.data.local.ProfileType
import com.footballxtream.ui.components.TvTextField

@Composable
fun AddProfileScreen(
    onSaved: () -> Unit,
    viewModel: AddProfileViewModel = viewModel(factory = AddProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val firstField = remember { FocusRequester() }
    LaunchedEffect(state.mode) { runCatching { firstField.requestFocus() } }

    val fieldModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Añade tu lista",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeChip("Xtream", state.mode == ProfileType.XTREAM) {
                viewModel.onModeChange(ProfileType.XTREAM)
            }
            ModeChip("Lista M3U", state.mode == ProfileType.M3U) {
                viewModel.onModeChange(ProfileType.M3U)
            }
        }

        if (state.isM3u) {
            TvTextField(
                value = state.m3uUrl,
                onValueChange = viewModel::onM3uUrlChange,
                label = "URL de la lista M3U",
                modifier = fieldModifier,
                keyboardType = KeyboardType.Uri,
                focusRequester = firstField,
            )
            TvTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Nombre del perfil (opcional)",
                modifier = fieldModifier,
            )
        } else {
            TvTextField(
                value = state.server,
                onValueChange = viewModel::onServerChange,
                label = "URL del servidor (http://host:puerto)",
                modifier = fieldModifier,
                keyboardType = KeyboardType.Uri,
                focusRequester = firstField,
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
        }

        if (state.error != null) {
            Text(text = state.error.orEmpty(), color = MaterialTheme.colorScheme.primary)
        }

        Button(onClick = { viewModel.save(onSaved) }, enabled = state.canSubmit) {
            Text(text = if (state.isConnecting) "Conectando…" else "Guardar y entrar")
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.primary else colors.surfaceVariant)
            .border(2.dp, if (focused) colors.onBackground else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
    }
}
