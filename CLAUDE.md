# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Android app (Kotlin + Jetpack Compose) that silently dismisses notifications from user-selected "work" apps during a daily off-hours window. Single module `:app`, package `com.pasquale.notificationblocker`.

- minSdk 28, targetSdk 36, compileSdk 37
- AGP 9.4 with built-in Kotlin 2.2 (no `kotlin-android` plugin), Gradle 9.6, version catalog in `gradle/libs.versions.toml`
- UI: Compose Material 3 + Navigation 3 (`androidx.navigation3`), font Manrope via downloadable Google Fonts
- Persistence: `SharedPreferences` only (no Room, no DataStore, no network)

Related docs (read before UI work):

- `docs/DESIGN_SYSTEM.md`: colors, type, shapes, components, copy tone
- `docs/MOTION.md`: animation tokens and rules, Lottie workflow
- `docs/ROADMAP.md`: prioritized improvements; update it when you ship or drop an item

## Build & test

No system JDK on this machine: use the one bundled with Android Studio.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug          # build APK
./gradlew testDebugUnitTest      # JVM unit tests (app/src/test)
./gradlew lintDebug              # must report 0 errors
./gradlew connectedDebugAndroidTest   # needs emulator/device
```

Install and run on emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

### Testing the blocking end-to-end without UI

```bash
P=com.pasquale.notificationblocker
adb shell cmd notification allow_listener $P/.service.NotificationBlockerService
# write shared_prefs/notification_blocker_prefs.xml via `run-as $P` with
# blocking_enabled=true, start_time=end_time (whole day), blocked_apps={com.android.shell}
adb shell cmd notification post -t Test tag "hello"   # posted as com.android.shell
adb logcat -s NotificationBlocker                      # expect "Canceling notification from ..."
```

## Architecture

```
app/src/main/java/com/pasquale/notificationblocker/
├── MainActivity.kt                  # Activity, Route (NavKey) and NavDisplay graph
├── data/
│   ├── OffHours.kt                  # pure time-window logic (unit tested)
│   └── PreferencesManager.kt        # SharedPreferences singleton, shouldBlock()
├── notification/
│   ├── ZenNotificationState.kt      # pure: what the zen notification shows now (unit tested)
│   └── ZenNotificationManager.kt    # posts/updates/removes the silent ongoing zen notification
├── service/
│   └── NotificationBlockerService.kt  # NotificationListenerService, cancels notifications, drives zen refresh
├── tile/
│   ├── ZenTileState.kt              # pure: tile icon/label/subtitle (unit tested)
│   └── ZenTileService.kt            # Quick Settings tile "Unplug & Sun": toggles blocking
└── ui/
    ├── MainViewModel.kt             # AndroidViewModel shared by both screens (StateFlows)
    ├── screens/
    │   ├── MainScreen.kt            # staggered entrance, hero, schedule, time pickers, bottom CTA
    │   └── AppSelectionScreen.kt    # MediumTopAppBar, pill search, All/Selected filter, sections
    ├── components/
    │   ├── HeroHeader.kt            # status card (3 HeroStatus states) + master Switch
    │   ├── MutedBellIcon.kt         # bell that shakes, then gets slashed when blocking turns on
    │   ├── ScheduleCard.kt          # start/end TimeCards + quiet duration
    │   ├── Timeline24h.kt           # 24h bar showing the off-hours window
    │   ├── PermissionCard.kt        # listener-permission prompt (animated visibility)
    │   ├── EndOfShiftCard.kt        # Lottie "end of shift" celebration, once per off-hours window
    │   ├── SceneCard.kt             # 2:1 card framing the scene on Home (fixed height inside the scrolling column)
    │   ├── ZenScene.kt              # animated scene: desk (work) / nature (off work)
    │   ├── ZenNotificationCard.kt   # one-time POST_NOTIFICATIONS prompt, shown after the listener permission
    │   ├── CurrentMinutes.kt        # rememberCurrentMinutes(): minute tick for clocks, timeline, greeting
    │   ├── AppItemRow.kt, AppIconImage.kt
    │   └── ShimmerSkeleton.kt, EmptyState.kt   # loading / empty states
    ├── zen/                         # vector scene, pure Kotlin: ZenEnvironment (season/time of day/light/rain),
    │                                # ScenePainting → DeskPainting, NaturePainting; ZenPainter (Compose + AWT in tests);
    │                                # LifeCopy (daylight / free-time messages on Home, unit tested)
    └── theme/                       # Color ("Chai" light, "Lo-fi night" dark), Motion (tokens), Type (Manrope),
                                     # Shape, Theme, ZenPalette (scene colors)
```

The app has two themes: `QuietHoursTheme` (used by `MainActivity`) and `NotificationBlockerTheme` (a thin alias used by previews). `dynamicColor` defaults to `false` on purpose, to keep the brand palette.

### Blocking rule (single source of truth)

`PreferencesManager.shouldBlock(pkg) = isBlockingEnabled && isInOffHoursNow() && pkg in blockedApps`

- The off-hours window is evaluated **at notification post time** from the current clock. There are deliberately **no alarms** (`AlarmManager`), boot receivers or cached "currently off-hours" flags: they drifted after reboot/Doze. Do not reintroduce them unless a feature needs to act at the window boundary (e.g. clearing already-visible notifications).
- `OffHours.isWithin(current, start, end)` uses a half-open window `[start, end)`, wraps past midnight when `start > end`, and treats `start == end` as the whole day.
- Times are stored as minutes from midnight (defaults 22:00 → 07:00).

### Preferences keys (`notification_blocker_prefs`)

`blocking_enabled` (Boolean), `start_time` / `end_time` (Int minutes), `blocked_apps` (StringSet), `last_celebrated_window` (String, ISO date of the window start that last played the "end of shift" animation; see `OffHours.windowStartDate`), `filtered_window` / `filtered_count` / `filtered_keys` (work notifications held in that window, counted once per notification key; group summaries and non-clearable ones are not counted), `zen_dismissed_window` (window in which the user swiped the zen notification away), `zen_prompt_dismissed` (Boolean, notification-permission prompt answered "Not now"). Always write a new set for `blocked_apps` (never mutate the one returned by `getStringSet`).

### App list

Apps come from `queryIntentActivities(ACTION_MAIN + CATEGORY_LAUNCHER)`, matched by the `<queries>` block in the manifest. Do **not** add `QUERY_ALL_PACKAGES` (Play policy) and do not filter by `FLAG_SYSTEM` (hides preinstalled apps like Gmail/Teams).

### Permissions

Only notification listener access is required, granted by the user in system settings (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`). `MainScreen` re-checks it on `ON_RESUME` and shows `PermissionCard` when missing. `POST_NOTIFICATIONS` is optional: it only enables the zen status notification. On Android 13+ `ZenNotificationCard` asks for it once, after the listener permission; denying it changes nothing else. The app uses no alarms, so `SCHEDULE_EXACT_ALARM` is intentionally absent.

### Zen status notification

Silent ongoing notification (channel `zen_status`, `IMPORTANCE_LOW`, `DecoratedCustomViewStyle`) shown while blocking is active inside the window: animated illustration for the phase of the day (`avd_zen_day/sunset/night`, hosted by an indeterminate `ProgressBar` because RemoteViews do not start animated vectors in an `ImageView`), a calm message, window end and held-notification count. Still no alarms: the system removes it at the window end via `setTimeoutAfter`; `ZenNotificationManager.refresh()` runs from the app (toggle, times, resume), from the listener service on each posted notification, and from a receiver the service registers at runtime for `TIME_TICK` (screen on only), `SCREEN_ON`, `USER_PRESENT` and time/zone changes. It re-posts only when the visible content changes (`contentKey`). Swiped away → hidden until the next window (or until blocking is turned on again).

The Quick Settings tile (`ZenTileService`, active tile: no polling) toggles `blocking_enabled` directly; `MainViewModel` follows preference changes with an `OnSharedPreferenceChangeListener`, and calls `ZenTileService.requestUpdate()` after changes made in the app. Details and diagrams: `docs/MOTION.md` ("Live Notification: architettura").

## Conventions

- All user-facing text goes in `res/values/strings.xml` **and** `res/values-it/strings.xml` (English default, Italian translation). Use `plurals` for counts; Italian needs `one`, `many`, `other`.
- Collect flows in Compose with `collectAsStateWithLifecycle()`; lifecycle hooks with `LifecycleEventEffect`.
- Format times with `OffHours.format()` (locale-independent `HH:mm`).
- Colors: always `MaterialTheme.colorScheme.*`, never `Color(0x…)` or `Color.White`/`Color.Black` outside `ui/theme/Color.kt` (zen scene colors live in `ui/theme/ZenPalette.kt`). `primary` as text only on `background`/`surface`/`surfaceContainerLow..High`, never on `surfaceContainerHighest` or `primaryContainer` (contrast table in `docs/DESIGN_SYSTEM.md`). Shapes from `MaterialTheme.shapes` (or `CircleShape`), no `RoundedCornerShape(N.dp)` in components.
- Components: stateless composables in `ui/components`, taking state + lambdas, `modifier: Modifier = Modifier` as the first optional parameter. Every component gets `@Preview`s for its main states.
- Animations: follow `docs/MOTION.md` (`label =` on every animation, no motion on first composition unless it is an entrance). Durations, easing and press springs come from `ui/theme/Motion.kt` (`Motion.standard(Motion.SHORT)`, `Motion.press()`, `Motion.Stagger`…), never inline numbers; choreography owned by one component is a named `private const` listed in the `MOTION.md` catalog. Animated state that must survive a status change lives **outside** `AnimatedContent`.
- Icons: only `material-icons-core` is on the classpath. Adding `material-icons-extended` roughly doubles the debug APK; prefer copying the single needed icon as an `ImageVector` or vector drawable (see `res/drawable/ic_moon*.xml`).
- Keep dependencies minimal: the original scaffold pulled in Room, Retrofit, CameraX, Coil, Play Services etc. without using them; they were removed on purpose. Any new dependency needs a reason in `docs/ROADMAP.md` (`lottie-compose` is in, for `EndOfShiftCard`). Lottie JSON generated by `tools/lottie/*.py` (shared helpers in `lottie_kit.py`): edit the script, not the JSON.
- Log tag for the service: `NotificationBlocker`.

## Known gaps / ideas

See `docs/ROADMAP.md` for the full list. Most relevant:

- Notifications already visible when the window starts are not cleared.
- No weekday selection (the window applies every day).
- `.agent/plan.md` is the original scaffolding brief: historical, outdated (mentions AlarmManager and components that no longer exist).
- Release builds run R8; extra keep rules go in `app/proguard-rules.pro`.
