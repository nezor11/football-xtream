package com.footballxtream.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.Quality
import com.footballxtream.model.QualityMode

@Composable
fun ChannelsScreen(
    onPlay: () -> Unit,
    viewModel: ChannelsViewModel = viewModel(factory = ChannelsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            ChannelsUiState.Loading ->
                Text("Cargando…", color = MaterialTheme.colorScheme.onBackground)

            is ChannelsUiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(current.message, color = MaterialTheme.colorScheme.onBackground)
                Button(onClick = viewModel::refresh) { Text("Reintentar") }
            }

            is ChannelsUiState.Content -> ChannelsContent(
                content = current,
                favoriteIds = favoriteIds,
                onQualitySelected = viewModel::selectQuality,
                onPlay = { group -> viewModel.play(group, onPlay) },
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }
    }
}

@Composable
private fun ChannelsContent(
    content: ChannelsUiState.Content,
    favoriteIds: Set<Int>,
    onQualitySelected: (QualityMode) -> Unit,
    onPlay: (ChannelGroup) -> Unit,
    onToggleFavorite: (ChannelGroup) -> Unit,
) {
    val firstChipFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstChipFocus.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp)) {
        Text(
            text = "Deporte en directo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 48.dp, bottom = 14.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QualityMode.entries.forEachIndexed { index, mode ->
                QualityChip(
                    label = mode.label,
                    selected = mode == content.qualityMode,
                    onClick = { onQualitySelected(mode) },
                    modifier = if (index == 0) Modifier.focusRequester(firstChipFocus) else Modifier,
                )
            }
        }

        if (content.rows.isEmpty()) {
            Text(
                text = "No se encontraron canales de deporte en este perfil.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, top = 24.dp),
            )
            return@Column
        }

        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            items(content.rows) { row ->
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
                        items(row.groups) { group ->
                            ChannelCard(
                                group = group,
                                isFavorite = group.variants.any {
                                    favoriteIds.contains(it.channel.streamId)
                                },
                                onClick = { onPlay(group) },
                                onLongClick = { onToggleFavorite(group) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val background = if (selected) colors.primary else colors.surfaceVariant
    val textColor = if (selected) colors.onPrimary else colors.onSurface

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(2.dp, if (focused) colors.onBackground else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}

@Composable
private fun ChannelCard(
    group: ChannelGroup,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.width(210.dp),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, colors.primary),
                shape = RoundedCornerShape(12.dp),
            ),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(112.dp).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (group.iconUrl != null) {
                AsyncImage(
                    model = group.iconUrl,
                    contentDescription = group.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                )
            } else {
                Text(
                    text = group.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (isFavorite) {
                Text(
                    text = "★",
                    color = colors.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = Quality.tiers
                    .filter { group.availableQualities.contains(it) }
                    .joinToString(" · ") { it.label },
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                maxLines = 1,
            )
        }
    }
}
