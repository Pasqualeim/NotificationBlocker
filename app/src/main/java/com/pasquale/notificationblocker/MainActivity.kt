package com.pasquale.notificationblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
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
import com.pasquale.notificationblocker.ui.theme.Motion
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
        transitionSpec = { push() },
        popTransitionSpec = { pop() },
        predictivePopTransitionSpec = { pop() },
    ) { key ->
        when (key) {
            Route.Home -> NavEntry(key) {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToAppSelection = { backStack.add(Route.AppSelection) },
                    // Frozen while the list slides over it: the slide is smoother without the scene
                    sceneActive = backStack.lastOrNull() == Route.Home,
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

// Screens slide, opaque: the default 700 ms cross-fade drew both screens see-through, each in its own
// offscreen buffer, about 25 fps on a Galaxy A32. The new screen covers the old one, which moves a
// quarter of its width the same way; going back, the leaving screen stays on top
private const val COVERED_SHIFT = 4

private fun AnimatedContentTransitionScope<*>.push(): ContentTransform =
    slideIntoContainer(SlideDirection.Start, Motion.standard(Motion.MEDIUM)) togetherWith
        slideOutOfContainer(SlideDirection.Start, Motion.standard(Motion.MEDIUM)) { it / COVERED_SHIFT }

private fun AnimatedContentTransitionScope<*>.pop(): ContentTransform =
    (
        slideIntoContainer(SlideDirection.End, Motion.standard(Motion.MEDIUM)) { it / COVERED_SHIFT } togetherWith
            slideOutOfContainer(SlideDirection.End, Motion.standard(Motion.MEDIUM))
        ).apply { targetContentZIndex = -1f }
