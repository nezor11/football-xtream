package com.footballxtream

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.ui.RootViewModel
import com.footballxtream.ui.StartState
import com.footballxtream.ui.live.LiveScreen
import com.footballxtream.ui.login.LoginScreen
import com.footballxtream.ui.player.PlayerScreen
import com.footballxtream.ui.theme.FootballXtreamTheme

object Routes {
    const val LOGIN = "login"
    const val LIVE = "live"
    const val PLAYER = "player/{streamId}?title={title}"

    fun player(streamId: Int, title: String): String =
        "player/$streamId?title=${Uri.encode(title)}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FootballXtreamTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val rootViewModel: RootViewModel = viewModel(factory = RootViewModel.Factory)
    val startState by rootViewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (startState) {
            StartState.LOADING -> Text(
                text = "Cargando…",
                color = MaterialTheme.colorScheme.onBackground,
            )
            StartState.AUTHENTICATED -> AppNavigation(startAtLive = true)
            StartState.NEEDS_LOGIN -> AppNavigation(startAtLive = false)
        }
    }
}

@Composable
private fun AppNavigation(startAtLive: Boolean) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (startAtLive) Routes.LIVE else Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.LIVE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LIVE) {
            LiveScreen(
                onChannelSelected = { channel ->
                    navController.navigate(Routes.player(channel.streamId, channel.name))
                },
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("streamId") { type = NavType.IntType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getInt("streamId") ?: 0
            val title = backStackEntry.arguments?.getString("title").orEmpty()
            PlayerScreen(
                streamId = streamId,
                title = title,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
