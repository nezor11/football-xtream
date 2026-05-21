package com.footballxtream.ui.login

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.R
import com.footballxtream.ui.components.TvTextField

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
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
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        val fieldModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()

        TvTextField(
            value = state.server,
            onValueChange = viewModel::onServerChange,
            label = stringResource(R.string.login_server),
            modifier = fieldModifier,
            keyboardType = KeyboardType.Uri,
            focusRequester = serverFocus,
        )
        TvTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = stringResource(R.string.login_username),
            modifier = fieldModifier,
        )
        TvTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.login_password),
            modifier = fieldModifier,
            isPassword = true,
            keyboardType = KeyboardType.Password,
        )
        TvTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.login_profile_name),
            modifier = fieldModifier,
        )

        if (state.error != null) {
            Text(
                text = state.error.orEmpty(),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = { viewModel.connect(onLoggedIn) },
            enabled = state.canSubmit,
        ) {
            Text(
                text = if (state.isConnecting) {
                    stringResource(R.string.login_connecting)
                } else {
                    stringResource(R.string.login_connect)
                },
            )
        }
    }
}
