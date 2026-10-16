package com.pasquale.notificationblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.screens.AppSelectionScreen
import com.pasquale.notificationblocker.ui.screens.DaysOffScreen
import com.pasquale.notificationblocker.ui.screens.MainScreen
import com.pasquale.notificationblocker.ui.screens.OnboardingScreen
import com.pasquale.notificationblocker.ui.screens.PausesScreen
import com.pasquale.notificationblocker.ui.screens.StatsScreen
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.QuietHoursTheme
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    /** The three tabs (Today, Breaks, Your time) under the bottom bar. */
    @Serializable
    data object Tabs : Route

    /** First launch: work apps, first breaks, notification access. */
    @Serializable
    data object Onboarding : Route

    /** Days off: ranges of dates and the public holidays. */
    @Serializable
    data object DaysOff : Route

    /** The work apps, or with [pauseId] the apps of one pause. */
    @Serializable
    data class AppSelection(val pauseId: String? = null) : Route
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

/** The tabs of the bottom bar, in order, with the title of their top bar. [icon] null: Material "Home". */
enum class Tab(@StringRes val label: Int, @StringRes val title: Int, @DrawableRes val icon: Int?) {
    TODAY(R.string.nav_today, R.string.main_title, null),
    PAUSES(R.string.nav_pauses, R.string.pauses_title, R.drawable.ic_nav_pauses),
    TIME(R.string.nav_time, R.string.stats_title, R.drawable.ic_nav_time),
}

@Composable
private fun Tab.painter(): Painter = icon?.let { painterResource(it) } ?: rememberVectorPainter(Icons.Filled.Home)

@Composable
fun NotificationBlockerApp(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
    val backStack = rememberNavBackStack(if (viewModel.needsOnboarding) Route.Onboarding else Route.Tabs)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        transitionSpec = { push() },
        popTransitionSpec = { pop() },
        predictivePopTransitionSpec = { pop() },
    ) { key ->
        when (key) {
            Route.Tabs -> NavEntry(key) {
                TabsScreen(
                    viewModel = viewModel,
                    onEditApps = { pauseId -> backStack.add(Route.AppSelection(pauseId)) },
                    onDaysOff = { backStack.add(Route.DaysOff) },
                    // Frozen while the list slides over it: the slide is smoother without the scene
                    sceneActive = backStack.lastOrNull() == Route.Tabs,
                )
            }
            is Route.AppSelection -> NavEntry(key) {
                AppSelectionScreen(
                    viewModel = viewModel,
                    pauseId = key.pauseId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
            Route.Onboarding -> NavEntry(key) {
                OnboardingScreen(
                    viewModel = viewModel,
                    onEditAllApps = { backStack.add(Route.AppSelection(null)) },
                    onDone = {
                        backStack.add(Route.Tabs)
                        backStack.removeAll { it == Route.Onboarding }
                    },
                )
            }
            Route.DaysOff -> NavEntry(key) {
                DaysOffScreen(viewModel = viewModel, onNavigateBack = { backStack.removeLastOrNull() })
            }
            else -> error("Unknown route: $key")
        }
    }
}

/**
 * The bottom bar and the tab it shows. Switching tabs swaps the content at once, opaque: a cross-fade
 * would draw two screens see-through (see the note on [push]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsScreen(
    viewModel: MainViewModel,
    onEditApps: (pauseId: String?) -> Unit,
    onDaysOff: () -> Unit,
    sceneActive: Boolean,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    // Each tab keeps its saved state (chart range, scroll) while another one is shown
    val tabStates = rememberSaveableStateHolder()
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        // Today draws its own header (date and greeting); the other tabs get a plain top bar
        topBar = {
            if (tab != Tab.TODAY) TopAppBar(
                title = { Text(stringResource(tab.title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                for (t in Tab.entries) {
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.painter(), contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        tabStates.SaveableStateProvider(tab.name) {
        when (tab) {
            Tab.TODAY -> MainScreen(viewModel = viewModel, modifier = content, sceneActive = sceneActive)
            Tab.PAUSES -> PausesScreen(
                viewModel = viewModel,
                onEditWorkApps = { onEditApps(null) },
                onEditPauseApps = { onEditApps(it) },
                onDaysOff = onDaysOff,
                modifier = content,
            )
            Tab.TIME -> StatsScreen(viewModel = viewModel, modifier = content)
        }
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
