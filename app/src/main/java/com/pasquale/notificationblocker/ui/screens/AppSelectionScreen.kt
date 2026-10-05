package com.pasquale.notificationblocker.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.AppInfo
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.AppItemRow
import com.pasquale.notificationblocker.ui.components.EmptyState
import com.pasquale.notificationblocker.ui.components.SearchField
import com.pasquale.notificationblocker.ui.components.ShimmerSkeleton
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import kotlinx.coroutines.delay

// The slide into the screen lasts Motion.MEDIUM; a moment more so the rows do not land on its last frames
private const val ROWS_DELAY_MILLIS = Motion.MEDIUM + 50L

enum class AppFilter {
    ALL,
    SELECTED
}

@Composable
fun AppSelectionScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingApps.collectAsStateWithLifecycle()

    // On every resume: apps installed or removed while away show up without restarting the app
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadInstalledApps()
    }

    AppSelectionScreenContent(
        apps = apps,
        isLoading = isLoading,
        onToggleBlocked = viewModel::toggleAppBlocked,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreenContent(
    apps: List<AppInfo>,
    isLoading: Boolean,
    onToggleBlocked: (String, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    rowsReadyAtStart: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterState by rememberSaveable { mutableStateOf(AppFilter.ALL) }

    // The rows cost about 4 ms each to compose on a Galaxy A32: composed with the screen they froze the
    // first frame of the slide for ~130 ms. They come once the slide is over, with the screen already there
    var rowsReady by rememberSaveable { mutableStateOf(rowsReadyAtStart) }
    LaunchedEffect(Unit) {
        if (!rowsReady) {
            delay(ROWS_DELAY_MILLIS)
            rowsReady = true
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // The apps picked when the list opened go first; after that rows never move while you pick: a switch
    // turned on or off leaves its app where it is, so a slip is undone on the spot
    val pickedAtStart = rememberSaveable(isLoading) {
        if (isLoading) emptySet() else apps.filter { it.isBlocked }.mapTo(HashSet()) { it.packageName }
    }
    val orderedApps = remember(apps, pickedAtStart) {
        apps.sortedBy { it.packageName !in pickedAtStart }
    }
    val matchingApps = remember(orderedApps, query) {
        if (query.isBlank()) {
            orderedApps
        } else {
            orderedApps.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    // Apps turned off while looking at "Selected": still listed, switch off, until the filter changes
    var keptInSelected by rememberSaveable { mutableStateOf(listOf<String>()) }

    val toggle: (String, Boolean) -> Unit = { packageName, isChecked ->
        if (!isChecked && filterState == AppFilter.SELECTED) keptInSelected = keptInSelected + packageName
        onToggleBlocked(packageName, isChecked)
    }
    val showFilter: (AppFilter) -> Unit = { filter ->
        filterState = filter
        keptInSelected = emptyList()
    }

    val selectedCount = remember(apps) { apps.count { it.isBlocked } }

    // A new search or filter starts at the top; the list would otherwise stay on the row it was showing.
    // Done once the new rows are composed, and not again after a rotation (the scroll position is restored)
    val listState = rememberLazyListState()
    var scrolledFor by rememberSaveable { mutableStateOf("$query|$filterState") }
    LaunchedEffect(query, filterState) {
        val shown = "$query|$filterState"
        if (shown != scrolledFor) {
            scrolledFor = shown
            listState.scrollToItem(0)
        }
    }

    val selectedRows = remember(matchingApps, keptInSelected) {
        matchingApps.filter { it.isBlocked || it.packageName in keptInSelected }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_selection_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Filter chips row
            // Warm selected state (the M3 default is secondaryContainer, emerald in this palette)
            val filterChipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filterState == AppFilter.ALL,
                    onClick = { showFilter(AppFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    shape = CircleShape,
                    colors = filterChipColors,
                )
                FilterChip(
                    selected = filterState == AppFilter.SELECTED,
                    onClick = { showFilter(AppFilter.SELECTED) },
                    label = {
                        Text(
                            if (selectedCount > 0) {
                                stringResource(R.string.filter_selected_count, selectedCount)
                            } else {
                                stringResource(R.string.filter_selected)
                            },
                        )
                    },
                    shape = CircleShape,
                    colors = filterChipColors,
                )
            }

            when {
                !rowsReady -> Unit
                isLoading -> ShimmerSkeleton()
                matchingApps.isEmpty() -> EmptyState()
                // Nothing picked yet (not a search miss): point to the "All" filter
                (filterState == AppFilter.SELECTED) && selectedRows.isEmpty() && query.isBlank() -> EmptyState(
                    title = stringResource(R.string.selected_empty_title),
                    subtitle = stringResource(R.string.selected_empty_subtitle),
                )

                (filterState == AppFilter.SELECTED) && selectedRows.isEmpty() -> EmptyState()

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = if (filterState == AppFilter.ALL) matchingApps else selectedRows,
                        key = { it.packageName },
                    ) { app ->
                        AppItemRow(
                            app = app,
                            onToggleBlocked = { isChecked -> toggle(app.packageName, isChecked) },
                            modifier = Modifier.rowMotion(this).padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Rows that come, go or move do it smoothly (Motion tokens). */
private fun Modifier.rowMotion(item: LazyItemScope): Modifier = with(item) {
    this@rowMotion.animateItem(
        fadeInSpec = Motion.standard(Motion.MEDIUM),
        placementSpec = Motion.standard(Motion.MEDIUM),
        fadeOutSpec = Motion.standard(Motion.SHORT),
    )
}

@Preview(showBackground = true, name = "App Selection - Light Theme")
@Composable
fun AppSelectionScreenPreviewLight() {
    NotificationBlockerTheme(darkTheme = false) {
        AppSelectionScreenContent(
            apps = listOf(
                AppInfo("com.slack", "Slack", isBlocked = true),
                AppInfo("com.microsoft.teams", "Microsoft Teams", isBlocked = true),
                AppInfo("com.google.android.gm", "Gmail", isBlocked = false),
                AppInfo("com.whatsapp", "WhatsApp", isBlocked = false),
            ),
            isLoading = false,
            onToggleBlocked = { _, _ -> },
            onNavigateBack = {},
            rowsReadyAtStart = true,
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "App Selection - Dark Theme")
@Composable
fun AppSelectionScreenPreviewDark() {
    NotificationBlockerTheme(darkTheme = true) {
        AppSelectionScreenContent(
            apps = listOf(
                AppInfo("com.slack", "Slack", isBlocked = true),
                AppInfo("com.microsoft.teams", "Microsoft Teams", isBlocked = true),
                AppInfo("com.google.android.gm", "Gmail", isBlocked = false),
                AppInfo("com.whatsapp", "WhatsApp", isBlocked = false),
            ),
            isLoading = false,
            onToggleBlocked = { _, _ -> },
            onNavigateBack = {},
            rowsReadyAtStart = true,
        )
    }
}
