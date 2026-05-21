package com.footballxtream.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }

    val ui by viewModel.ui.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
        )

        StatsOverlay(
            channelName = ui.channelName,
            qualityLabel = ui.qualityLabel,
            throughputMbps = ui.throughputMbps,
            resolution = ui.resolution,
            isBuffering = ui.isBuffering,
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
        )
    }
}

@Composable
private fun StatsOverlay(
    channelName: String,
    qualityLabel: String,
    throughputMbps: Double,
    resolution: String?,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
) {
    val rate = "%.1f".format(throughputMbps)
    val parts = buildList {
        if (channelName.isNotBlank()) add(channelName)
        if (qualityLabel.isNotBlank()) add(qualityLabel)
        add("$rate Mbps")
        resolution?.let { add(it) }
        if (isBuffering) add("⟳")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC0A0E12))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = parts.joinToString("  •  "),
            style = MaterialTheme.typography.labelLarge,
            color = if (isBuffering) MaterialTheme.colorScheme.primary else Color(0xFFE6EAEE),
        )
    }
}
