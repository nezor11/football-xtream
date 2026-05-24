package com.footballxtream.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.data.local.ProfileEntity
import com.footballxtream.ui.components.BrandHeader

@Composable
fun ProfilesScreen(
    onProfileSelected: () -> Unit,
    onAddProfile: () -> Unit,
    viewModel: ProfilesViewModel = viewModel(factory = ProfilesViewModel.Factory),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BrandHeader(compact = true, tagline = null)

            Text(
                text = "¿Quién está viendo?",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        onClick = { viewModel.select(profile, onProfileSelected) },
                        onLongClick = { viewModel.delete(profile) },
                    )
                }
                item {
                    AddCard(onClick = onAddProfile)
                }
            }
            if (profiles.isNotEmpty()) {
                Text(
                    text = "Mantén pulsado un perfil para eliminarlo",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = onClick, onLongClick = onLongClick, modifier = Modifier.width(190.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Letter avatar in a tinted circle.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.name.take(1).uppercase().ifBlank { "?" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.primary,
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = profile.name.ifBlank { profile.username },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TypeBadge(isM3u = profile.isM3u)
        }
    }
}

/** Small pill that tells Xtream profiles apart from M3U lists at a glance. */
@Composable
private fun TypeBadge(isM3u: Boolean) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = if (isM3u) "Lista M3U" else "Xtream",
        style = MaterialTheme.typography.labelSmall,
        color = colors.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun AddCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = onClick, modifier = Modifier.width(190.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", style = MaterialTheme.typography.displayMedium, color = colors.primary)
        }
        Text(
            text = "Añadir",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}
