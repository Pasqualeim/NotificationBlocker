package com.pasquale.notificationblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.screens.AppSelectionScreen
import com.pasquale.notificationblocker.ui.screens.MainScreen
import com.pasquale.notificationblocker.ui.theme.QuietHoursTheme
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data object AppSelection : Route
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuietHoursTheme {
                NotificationBlockerApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun NotificationBlockerApp(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
    val backStack = rememberNavBackStack(Route.Home)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
    ) { key ->
        when (key) {
            Route.Home -> NavEntry(key) {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToAppSelection = { backStack.add(Route.AppSelection) },
                )
            }
            Route.AppSelection -> NavEntry(key) {
                AppSelectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
            else -> error("Unknown route: $key")
        }
    }
}
