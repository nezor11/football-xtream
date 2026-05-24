package com.footballxtream.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory),
) {
    if (!viewModel.canPlay) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    // Back closes the quality menu first; otherwise it leaves the player.
    BackHandler(enabled = ui.qualityMenuOpen) { viewModel.closeQualityMenu() }
    BackHandler(enabled = !ui.qualityMenuOpen, onBack = onBack)

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (ui.qualityMenuOpen) {
                    when (event.key) {
                        Key.DirectionUp, Key.DirectionLeft -> {
                            viewModel.moveQualitySelection(-1); true
                        }
                        Key.DirectionDown, Key.DirectionRight -> {
                            viewModel.moveQualitySelection(1); true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            viewModel.confirmQualitySelection(); true
                        }
                        else -> false
                    }
                } else {
                    when (event.key) {
                        Key.DirectionUp, Key.DirectionLeft -> {
                            viewModel.previousChannel(); true
                        }
                        Key.DirectionDown, Key.DirectionRight -> {
                            viewModel.nextChannel(); true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            viewModel.openQualityMenu(); true
                        }
                        else -> false
                    }
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatsOverlay(
                channelName = ui.channelName,
                emissionLabel = ui.emissionLabel,
                throughputMbps = ui.throughputMbps,
                resolution = ui.resolution,
                isBuffering = ui.isBuffering,
            )
            ui.nowProgram?.let { now ->
                EpgOverlay(now = now, next = ui.nextProgram)
            }
            if (ui.qualityMenuOpen) {
                QualityMenu(
                    options = ui.qualityOptions,
                    selectedIndex = ui.qualitySelectedIndex,
                )
            }
        }

        ui.errorMessage?.let { msg ->
            Text(
                text = "$msg  ·  ▲▼◀▶ para cambiar de canal",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE6EAEE),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Text(
            text = if (ui.qualityMenuOpen) "▲▼ elegir · OK confirmar" else "OK: calidad  ·  ▲▼◀▶: canal",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0x99FFFFFF),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
}

@Composable
private fun StatsOverlay(
    channelName: String,
    emissionLabel: String,
    throughputMbps: Double,
    resolution: String?,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
) {
    val rate = "%.1f".format(throughputMbps)
    val parts = buildList {
        if (channelName.isNotBlank()) add(channelName)
        add("‹ $emissionLabel ›")
        add("$rate Mbps")
        resolution?.let { add(it) }
        if (isBuffering) add("⟳")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x990A0E12))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = parts.joinToString("  •  "),
            style = MaterialTheme.typography.labelMedium,
            color = if (isBuffering) MaterialTheme.colorScheme.primary else Color(0xCCE6EAEE),
        )
    }
}

@Composable
private fun EpgOverlay(now: String, next: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x990A0E12))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = buildString {
                append("Ahora: ").append(now)
                if (!next.isNullOrBlank()) append("   ·   Luego: ").append(next)
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xCCE6EAEE),
        )
    }
}

@Composable
private fun QualityMenu(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60A0E12))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Tipo de emisión",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = (if (selected) "● " else "○ ") + label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) colors.primary else Color(0xFFE6EAEE),
            )
        }
    }
}
