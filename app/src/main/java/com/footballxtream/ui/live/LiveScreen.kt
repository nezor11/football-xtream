package com.footballxtream.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.footballxtream.model.LiveChannel

@Composable
fun LiveScreen(
    onChannelSelected: (LiveChannel) -> Unit,
    viewModel: LiveViewModel = viewModel(factory = LiveViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            LiveUiState.Loading -> Text(
                text = "Cargando…",
                color = MaterialTheme.colorScheme.onBackground,
            )

            is LiveUiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(current.message, color = MaterialTheme.colorScheme.onBackground)
                Button(onClick = viewModel::refresh) { Text("Reintentar") }
            }

            is LiveUiState.Content -> TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                item {
                    Text(
                        text = "TV en directo",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
                    )
                }
                items(current.rows) { row ->
                    ChannelRowView(
                        row = row,
                        favoriteIds = favoriteIds,
                        onSelect = onChannelSelected,
                        onToggleFavorite = viewModel::toggleFavorite,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRowView(
    row: ChannelRow,
    favoriteIds: Set<Int>,
    onSelect: (LiveChannel) -> Unit,
    onToggleFavorite: (LiveChannel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp),
        )
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(row.channels) { channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = favoriteIds.contains(channel.streamId),
                    onClick = { onSelect(channel) },
                    onLongClick = { onToggleFavorite(channel) },
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: LiveChannel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.width(200.dp),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, colors.primary),
                shape = RoundedCornerShape(12.dp),
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (channel.iconUrl != null) {
                AsyncImage(
                    model = channel.iconUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                )
            } else {
                Text(
                    text = channel.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (isFavorite) {
                Text(
                    text = "★",
                    color = colors.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}
