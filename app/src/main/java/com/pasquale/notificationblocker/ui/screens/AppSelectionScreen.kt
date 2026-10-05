package com.pasquale.notificationblocker.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.AppInfo
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.AppIconImage
import com.pasquale.notificationblocker.ui.components.AppItemRow
import com.pasquale.notificationblocker.ui.components.EmptyState
import com.pasquale.notificationblocker.ui.components.ShimmerSkeleton
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppSelectionScreenContent(
    apps: List<AppInfo>,
    isLoading: Boolean,
    onToggleBlocked: (String, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterState by rememberSaveable { mutableStateOf(AppFilter.ALL) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val matchingApps = remember(apps, query) {
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    // Rows never jump while you pick: a switch turned on or off leaves its app where it is, so a slip is
    // undone on the spot. What you picked shows at the top instead, in the tray, newest first.
    // Apps picked in this visit, newest first
    var recentPicks by rememberSaveable { mutableStateOf(listOf<String>()) }
    // Apps turned off while looking at "Selected": still listed, switch off, until the filter changes
    var keptInSelected by rememberSaveable { mutableStateOf(listOf<String>()) }

    val toggle: (String, Boolean) -> Unit = { packageName, isChecked ->
        recentPicks = if (isChecked) listOf(packageName) + (recentPicks - packageName) else recentPicks - packageName
        if (!isChecked && filterState == AppFilter.SELECTED) keptInSelected = keptInSelected + packageName
        onToggleBlocked(packageName, isChecked)
    }
    val showFilter: (AppFilter) -> Unit = { filter ->
        filterState = filter
        keptInSelected = emptyList()
    }

    // All the picked apps (whatever the search), the ones picked in this visit first
    val pickedApps = remember(apps, recentPicks) {
        apps.filter { it.isBlocked }.sortedBy { app ->
            recentPicks.indexOf(app.packageName).let { if (it < 0) Int.MAX_VALUE else it }
        }
    }

    val selectedApps = remember(matchingApps) {
        matchingApps.filter { it.isBlocked }
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
            // Pill-shaped filled search field (NOT OutlinedTextField)
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_apps),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search_icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.cd_clear_search),
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    label = { Text(stringResource(R.string.filter_selected)) },
                    shape = CircleShape,
                    colors = filterChipColors,
                )
            }

            when {
                isLoading -> ShimmerSkeleton()
                matchingApps.isEmpty() -> EmptyState()
                // Nothing picked yet (not a search miss): point to the "All" filter
                (filterState == AppFilter.SELECTED) && selectedRows.isEmpty() && query.isBlank() -> EmptyState(
                    title = stringResource(R.string.selected_empty_title),
                    subtitle = stringResource(R.string.selected_empty_subtitle),
                )

                (filterState == AppFilter.SELECTED) && selectedRows.isEmpty() -> EmptyState()

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (filterState == AppFilter.ALL) {
                        // Pinned while the list scrolls: what you picked is always in sight
                        if (pickedApps.isNotEmpty()) {
                            stickyHeader(key = "tray") {
                                PickedTray(apps = pickedApps, onUnpick = { toggle(it, false) })
                            }
                        }
                        item(key = "header_all") {
                            SectionHeader(title = stringResource(R.string.section_all_apps, matchingApps.size))
                        }
                        items(matchingApps, key = { it.packageName }) { app ->
                            AppItemRow(
                                app = app,
                                onToggleBlocked = { isChecked -> toggle(app.packageName, isChecked) },
                                modifier = Modifier.rowMotion(this).padding(horizontal = 16.dp),
                            )
                        }
                    } else {
                        stickyHeader(key = "header_selected") {
                            SectionHeader(title = stringResource(R.string.section_selected, selectedApps.size))
                        }
                        items(selectedRows, key = { it.packageName }) { app ->
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
}

/** Rows and chips that come, go or move do it smoothly (Motion tokens). */
private fun Modifier.rowMotion(item: LazyItemScope): Modifier = with(item) {
    this@rowMotion.animateItem(
        fadeInSpec = Motion.standard(Motion.MEDIUM),
        placementSpec = Motion.standard(Motion.MEDIUM),
        fadeOutSpec = Motion.standard(Motion.SHORT),
    )
}

/**
 * The apps picked so far, newest first, pinned above the list: a new pick shows up here at once,
 * and each chip turns its app back off.
 */
@Composable
private fun PickedTray(
    apps: List<AppInfo>,
    onUnpick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()
    // A new pick goes first: bring it into view
    val newest = apps.first().packageName
    LaunchedEffect(newest) { rowState.animateScrollToItem(0) }

    val chipColors = InputChipDefaults.inputChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            SectionHeader(title = stringResource(R.string.section_selected, apps.size))
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    InputChip(
                        selected = true,
                        onClick = { onUnpick(app.packageName) },
                        label = { Text(app.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        avatar = {
                            AppIconImage(
                                packageName = app.packageName,
                                contentDescription = null,
                                size = InputChipDefaults.AvatarSize,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_unpick_app, app.name),
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                        shape = CircleShape,
                        colors = chipColors,
                        border = null,
                        modifier = Modifier.rowMotion(this),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
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
        )
    }
}
