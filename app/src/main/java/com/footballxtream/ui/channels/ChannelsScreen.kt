package com.footballxtream.ui.channels

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.footballxtream.model.ChannelFolder
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.Quality
import com.footballxtream.model.QualityMode

@Composable
fun ChannelsScreen(
    onPlay: () -> Unit,
    viewModel: ChannelsViewModel = viewModel(factory = ChannelsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteNames by viewModel.favoriteNames.collectAsStateWithLifecycle()
    val openedFolder by viewModel.openedFolder.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        val folder = openedFolder
        when {
            folder != null -> {
                BackHandler(onBack = viewModel::closeFolder)
                FolderDetail(
                    folder = folder,
                    onChannelSelected = { index -> viewModel.play(folder, index, onPlay) },
                )
            }

            state is ChannelsUiState.Loading ->
                Text("Cargando…", color = MaterialTheme.colorScheme.onBackground)

            state is ChannelsUiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    (state as ChannelsUiState.Error).message,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Button(onClick = viewModel::reload) { Text("Reintentar") }
            }

            state is ChannelsUiState.Content -> FolderGrid(
                content = state as ChannelsUiState.Content,
                favoriteNames = favoriteNames,
                onQualitySelected = viewModel::selectQuality,
                onReload = viewModel::reload,
                onFolderClick = { f ->
                    if (f.isSingle) viewModel.play(f, 0, onPlay) else viewModel.openFolder(f)
                },
                onFolderLongClick = viewModel::toggleFavorite,
            )
        }
    }
}

@Composable
private fun FolderGrid(
    content: ChannelsUiState.Content,
    favoriteNames: Set<String>,
    onQualitySelected: (QualityMode) -> Unit,
    onReload: () -> Unit,
    onFolderClick: (ChannelFolder) -> Unit,
    onFolderLongClick: (ChannelFolder) -> Unit,
) {
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
            QualityMode.entries.forEach { mode ->
                Chip(
                    label = mode.label,
                    selected = mode == content.qualityMode,
                    onClick = { onQualitySelected(mode) },
                )
            }
            Chip(label = "↻ Recargar", selected = false, onClick = onReload)
        }

        if (content.rows.isEmpty()) {
            Text(
                text = "No se encontraron canales de deporte en este perfil.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, top = 24.dp),
            )
            return@Column
        }

        LazyColumn(
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
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(row.folders) { folder ->
                            FolderCard(
                                folder = folder,
                                isFavorite = favoriteNames.contains(folder.name),
                                onClick = { onFolderClick(folder) },
                                onLongClick = { onFolderLongClick(folder) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDetail(
    folder: ChannelFolder,
    onChannelSelected: (Int) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(folder.name) {
        runCatching { firstFocus.requestFocus() }
    }
    Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp)) {
        Text(
            text = folder.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
        )
        Text(
            text = "Atrás para volver",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp, bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(folder.channels) { index, channel ->
                ChannelCard(
                    group = channel,
                    onClick = { onChannelSelected(index) },
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
private fun FolderCard(
    folder: ChannelFolder,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val subtitle = if (folder.isSingle) {
        qualityLabels(folder.single)
    } else {
        "${folder.channels.size} canales"
    }
    ImageCard(
        title = folder.name,
        subtitle = subtitle,
        iconUrl = folder.iconUrl,
        showFavorite = isFavorite,
        showFolderHint = !folder.isSingle,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun ChannelCard(
    group: ChannelGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImageCard(
        title = group.displayName,
        subtitle = qualityLabels(group),
        iconUrl = group.iconUrl,
        showFavorite = false,
        showFolderHint = false,
        onClick = onClick,
        onLongClick = null,
        modifier = modifier,
    )
}

@Composable
private fun ImageCard(
    title: String,
    subtitle: String,
    iconUrl: String?,
    showFavorite: Boolean,
    showFolderHint: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(210.dp),
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
            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                )
            } else {
                Text(
                    text = title.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (showFavorite) {
                Text(
                    text = "★",
                    color = colors.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            if (showFolderHint) {
                Text(
                    text = "▸",
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun qualityLabels(group: ChannelGroup): String =
    Quality.tiers.filter { group.availableQualities.contains(it) }.joinToString(" · ") { it.label }
