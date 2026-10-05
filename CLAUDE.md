# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Nook** (formerly NotificationBlocker): Android app (Kotlin + Jetpack Compose) that silently dismisses notifications from user-selected "work" apps during a daily off-hours window.

Purpose: we have a life beyond work. "Off-hours" means any time you are not working (an afternoon, an evening, a whole day), not "night and sleep". Copy, scenes and icon celebrate the free time you get back, never rest-as-a-reward or the work you are missing. Single module `:app`. `applicationId` is `com.pasquale.nook` (fixed forever once published); the Kotlin namespace stays `com.pasquale.notificationblocker`, so class names in `adb` commands need the full path. Naming and color rationale: `docs/DESIGN_SYSTEM.md`.

- minSdk 28, targetSdk 37, compileSdk 37
- AGP 9.4 with built-in Kotlin, pinned to 2.4 through the `kotlin` version in the catalog (no `kotlin-android` plugin), Gradle 9.8, version catalog in `gradle/libs.versions.toml`
- UI: Compose Material 3 + Navigation 3 (`androidx.navigation3`), font Manrope via downloadable Google Fonts
- Persistence: `SharedPreferences` only (no Room, no DataStore, no network)

Related docs (read before UI work):

- `docs/DESIGN_SYSTEM.md`: colors, type, shapes, components, copy tone
- `docs/MOTION.md`: animation tokens and rules
- `docs/ROADMAP.md`: project status (publishing, open items, done); update it when you ship or drop an item
- `docs/PUBLISHING.md`: the owner's step-by-step Google Play checklist (manual steps: accounts, testing, forms)
- `docs/store/LISTING.md`: store texts IT/EN, screenshots in `docs/store/screenshots/`, Play Console form answers. Keep it in sync with the app (a new permission, data flow or feature changes the Data safety answers)

## Build & test

No system JDK on this machine: use the one bundled with Android Studio.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug          # build APK
./gradlew testDebugUnitTest      # JVM unit tests (app/src/test)
./gradlew lintDebug              # must report 0 errors
./gradlew connectedDebugAndroidTest   # needs emulator/device (11 tests: manifest wiring, PreferencesManager); uninstalls the app when done
```

Release: `./gradlew bundleRelease` (AAB for Play) or `assembleRelease`. Signing reads `keystore.properties` in the repo root (git-ignored; keys `storeFile`, `storePassword`, `keyAlias`, `keyPassword`); the upload keystore lives outside the repo in `~/keystores/`. Without that file the release build is unsigned, so CI and fresh clones still build.

Install and run on emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

### Testing the blocking end-to-end without UI

```bash
P=com.pasquale.nook
adb shell cmd notification allow_listener $P/com.pasquale.notificationblocker.service.NotificationBlockerService
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
│   ├── MorningReport.kt             # pure: held notifications per app, when to show the report (unit tested)
│   └── PreferencesManager.kt        # SharedPreferences singleton, shouldBlock()
├── notification/
│   ├── ZenNotificationState.kt      # pure: what the zen notification shows now (unit tested)
│   └── ZenNotificationManager.kt    # posts/updates/removes the silent ongoing zen notification
├── service/
│   └── NotificationBlockerService.kt  # NotificationListenerService, cancels notifications (also sweeps the active ones on connect), drives zen refresh
├── tile/
│   ├── ZenTileState.kt              # pure: tile icon/label/subtitle (unit tested)
│   └── ZenTileService.kt            # Quick Settings tile "Nook": toggles blocking
└── ui/
    ├── MainViewModel.kt             # AndroidViewModel shared by both screens (StateFlows)
    ├── screens/
    │   ├── MainScreen.kt            # staggered entrance: top card, scene, schedule; one top card at a time (TopCard); time pickers, bottom CTA
    │   └── AppSelectionScreen.kt    # MediumTopAppBar, pill search, All/Selected filter; one plain list: the apps picked when it opens come first, rows never move while you pick; rows are composed once the slide is over
    ├── components/
    │   ├── HeroHeader.kt            # status card (3 HeroStatus states) + master Switch
    │   ├── MutedBellIcon.kt         # bell that shakes, then gets slashed when blocking turns on
    │   ├── ScheduleCard.kt          # start/end TimeCards + quiet duration
    │   ├── Timeline24h.kt           # 24h bar showing the off-hours window
    │   ├── PermissionCard.kt        # listener-permission prompt (in the top card's place until access is granted)
    │   ├── MorningReportCard.kt     # "While you were off": held notifications of the last window, in the top card's place until dismissed
    │   ├── HomeColumn.kt            # Home layout (homeScene() marks the scene): fits one screen, cards scale down first, then the scene; never crops the scene; anchored at the top, gaps grow into spare room
    │   ├── HomeFit.kt               # pure: the scale policy of HomeColumn (unit tested)
    │   ├── SceneCard.kt             # card framing the scene on Home; fills the size it is given (HomeColumn gives it 2:1 or smaller, never cropped)
    │   ├── ZenScene.kt              # animated scene: office desk (work) / lake pier (off work)
    │   ├── AmbientClock.kt          # the one 30 fps clock of Home's slow motion (scene + glow behind the bell)
    │   ├── SearchField.kt           # pill search field: bare BasicTextField (the Material TextField froze the list screen ~60 ms when it opened)
    │   ├── ZenNotificationCard.kt   # one-time POST_NOTIFICATIONS prompt, in the top card's place right after the listener permission
    │   ├── CurrentMinutes.kt        # rememberCurrentMinutes(): minute tick for clocks, timeline, greeting
    │   ├── AppItemRow.kt, AppIconImage.kt
    │   └── ShimmerSkeleton.kt, EmptyState.kt   # loading / empty states
    ├── zen/                         # vector scene, pure Kotlin: ZenEnvironment (season/time of day/light/rain/snow),
    │                                # ScenePainting → DeskPainting (office), NaturePainting (lake pier); ZenPainter (Compose + AWT in tests);
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

`blocking_enabled` (Boolean), `start_time` / `end_time` (Int minutes), `blocked_apps` (StringSet), `last_celebrated_window` (String, legacy: written by the removed "end of shift" card, no longer read), `filtered_window` / `filtered_count` / `filtered_keys` (work notifications held in that window, counted once per notification key; group summaries and non-clearable ones are not counted; `filtered_keys` entries are `package\nfingerprint`, the first 8 bytes of the SHA-256 of the notification key in hex: the key is never stored, its tag can be a chat id with a phone number and the file goes into the Android backup; older versions wrote `package\nkey` or the bare key), `report_seen_window` (window whose morning report was dismissed), `zen_dismissed_window` (window in which the user swiped the zen notification away), `pause_ended_window` (window the user ended early with "End the pause": nothing is held until the next window; cleared when blocking is turned on again), `zen_prompt_dismissed` (Boolean, notification-permission prompt answered "Not now"). Always write a new set for `blocked_apps` (never mutate the one returned by `getStringSet`).

### App list

Apps come from `queryIntentActivities(ACTION_MAIN + CATEGORY_LAUNCHER)`, matched by the `<queries>` block in the manifest. Do **not** add `QUERY_ALL_PACKAGES` (Play policy) and do not filter by `FLAG_SYSTEM` (hides preinstalled apps like Gmail/Teams).

### Permissions

Only notification listener access is required, granted by the user in system settings (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`). `MainScreen` re-checks it on `ON_RESUME` and shows `PermissionCard` in place of the status card while it is missing. `POST_NOTIFICATIONS` is optional: it only enables the zen status notification. On Android 13+ `ZenNotificationCard` asks for it once, in place of the status card right after the listener permission; denying it changes nothing else. The app uses no alarms, so `SCHEDULE_EXACT_ALARM` is intentionally absent.

### Zen status notification

User-facing name: "notifica della pausa" / "break notification" ("zen" survives only in code and resource names).

Silent ongoing notification (channel `zen_status`, `IMPORTANCE_LOW`, `DecoratedCustomViewStyle`, small icon `ic_notification_nook`, the mug) shown **only while Nook really holds work notifications**: blocking on, inside the window, not ended early, at least one work app and the notification access granted (`ZenNotificationState.resolve`); never during work hours. Title "Work paused until 07:00"; text how many were paused and from which apps (names left out of the public version on a locked screen; copy says "paused", never "waiting": held notifications do not come back). Expanded: the lake pier in miniature for the phase of the day (`avd_zen_day/sunset/night`, generated by `tools/notification/zen_art.py`, hosted by an indeterminate `ProgressBar` because RemoteViews do not start animated vectors in an `ImageView`) and a calm line. Action "End the pause" (`EndPauseReceiver`) writes `pause_ended_window`: `shouldBlock` lets work through until the next window, Home shows "Pause ended" with "Pause again", the tile says when the next one starts. No confirmation notification: the break notification just goes away (a second one with "Undo" confused users). Still no alarms: the system removes it at the window end via `setTimeoutAfter`; `ZenNotificationManager.refresh()` runs from the app (toggle, times, resume), from the listener service on each posted notification, and from a receiver the service registers at runtime for `TIME_TICK` (screen on only), `SCREEN_ON`, `USER_PRESENT` and time/zone changes. It re-posts only when the visible content changes (`contentKey`). Swiped away → hidden until the next window (or until blocking is turned on again).

The Quick Settings tile (`ZenTileService`, active tile: no polling) toggles `blocking_enabled` directly; `MainViewModel` follows preference changes with an `OnSharedPreferenceChangeListener`, and calls `ZenTileService.requestUpdate()` after changes made in the app. Details and diagrams: `docs/MOTION.md` ("Live Notification: architettura").

## Conventions

- All user-facing text goes in `res/values/strings.xml` **and** `res/values-it/strings.xml` (English default, Italian translation). Use `plurals` for counts; Italian needs `one`, `many`, `other`.
- Collect flows in Compose with `collectAsStateWithLifecycle()`; lifecycle hooks with `LifecycleEventEffect`.
- Format times with `OffHours.format()` (locale-independent `HH:mm`).
- Colors: always `MaterialTheme.colorScheme.*`, never `Color(0x…)` or `Color.White`/`Color.Black` outside `ui/theme/Color.kt` (zen scene colors live in `ui/theme/ZenPalette.kt`). The only XML color is `window_background` (`res/values[-night]/colors.xml`, the launch and splash color): keep it equal to `LightBackground` / `DarkBackground`. `primary` as text only on `background`/`surface`/`surfaceContainerLow..High`, never on `surfaceContainerHighest` or `primaryContainer` (contrast table in `docs/DESIGN_SYSTEM.md`). Shapes from `MaterialTheme.shapes` (or `CircleShape`), no `RoundedCornerShape(N.dp)` in components.
- Components: stateless composables in `ui/components`, taking state + lambdas, `modifier: Modifier = Modifier` as the first optional parameter. Every component gets `@Preview`s for its main states.
- Animations: follow `docs/MOTION.md` (`label =` on every animation, no motion on first composition unless it is an entrance). Durations, easing and press springs come from `ui/theme/Motion.kt` (`Motion.standard(Motion.SHORT)`, `Motion.press()`, `Motion.Stagger`…), never inline numbers; choreography owned by one component is a named `private const` listed in the `MOTION.md` catalog. Animated state that must survive a status change lives **outside** `AnimatedContent`.
- Icons: only `material-icons-core` is on the classpath. Adding `material-icons-extended` roughly doubles the debug APK; prefer copying the single needed icon as an `ImageVector` or vector drawable (see `res/drawable/ic_moon*.xml`).
- Keep dependencies minimal: the original scaffold pulled in Room, Retrofit, CameraX, Coil, Play Services etc. without using them; they were removed on purpose. Any new dependency needs a reason in `docs/ROADMAP.md` (`lottie-compose` was removed with the "end of shift" card; `tools/lottie/lottie_kit.py` stays for a future Lottie asset). Generated assets: edit the script, not the output. The launcher icon: `tools/icon/launcher_icon.py` writes the adaptive-icon drawables and `docs/store/icon-512.png`; `tools/palette/contrast.py` checks `Color.kt` and prints the contrast tables of `DESIGN_SYSTEM.md` (run it after any color change).
- Log tag for the service: `NotificationBlocker`.
- User-facing copy: plain, human, concrete (see "Testi e tono" in `docs/DESIGN_SYSTEM.md`). Review new strings with the `unslop` skill rules; the permission disclosure must keep its four points (what the app sees, what it doesn't do, nothing leaves the phone, revocable).

## Known gaps / ideas

See `docs/ROADMAP.md` for the full list. Most relevant:

- Notifications already visible when the window starts are not cleared. (When the listener service connects, e.g. after boot or an update, `sweepActiveNotifications()` does remove the ones that `shouldBlock`, so nothing posted while it was unbound leaks.)
- No weekday selection (the window applies every day).
- `.agent/plan.md` is the original scaffolding brief: historical, outdated (mentions AlarmManager and components that no longer exist).
- Release builds run R8; extra keep rules go in `app/proguard-rules.pro`.
- **Home fits one screen on every phone and the scene is never cropped** (rule and verification: `docs/DESIGN_SYSTEM.md`, "Home in una schermata"). If space is short the *rest* is resized, never the animation: cards scale down to 0.8 together (`HomeColumn`/`HomeFit`), only then the scene shrinks, whole, to 0.6, and only then the page scrolls. Home blocks are composed from the first frame and enter with `homeEntrance` (alpha + offset), never `AnimatedVisibility`. Text must not change size by itself: Home sizes are compact (16dp card padding, `titleLarge` status, `bodyMedium` texts, 22sp times) so common phones fit at scale 1, and the top of Home holds **one card at a time** (`TopCard`: permission → notification prompt → morning report → status card with the switch); a card stacked on top pushed the page past the screen, every text shrank, then grew back by 25% once it was gone. Every new block on Home eats height: re-check on the phone (`wm size`/`wm density`/`font_scale`, always reset after) before committing. Cost: `HomeColumn` measures the cards once per layout, like a `Column`; the scale never follows an animation frame by frame (that relaid out every card each frame and made the switch stutter), it changes once the heights are still (`SETTLE_MILLIS`) and grows back only for more than 56dp of room.
- Performance: judge smoothness on a **release** build (debug Compose is ~2x slower and starts in 1.8 s instead of 0.4 s on a Galaxy A32). Measure with `adb shell dumpsys gfxinfo com.pasquale.nook framestats` (only the last 120 frames are kept, so reset right before the gesture; `Total frames rendered` over a few idle seconds gives the real frame rate). The test phone (Galaxy A32) is **90 Hz** (11 ms per frame) with a Mali-G52 whose clock sits at its minimum (299 of 950 MHz) even at 100% load, and it redraws the whole window on every frame (no partial updates): ~7 ms of GPU for an empty window, ~16 ms for Home, ~20 ms with the scene. So: **never start an animation that runs on its own clock** (`rememberInfiniteTransition`, `animate*AsState` loops) on a screen that stays open: it redraws the window on every vsync (90 per second) and the GPU never catches up. The breathing glow behind the bell did exactly that whenever the pause was on (the main state): 458 frames in 5 s, every one late, scroll and taps stuttering. Slow ambient motion reads `AmbientClock` (30 fps, shared by the scene and the glow, frozen while the page scrolls or another screen slides over Home); idle Home now renders 150 frames in 5 s. The Home scroll has no edge stretch (`overscrollEffect = null`): the page is barely taller than the screen and the stretch cost 18 ms of GPU per frame. Also: opaque slide transitions instead of cross-fades (two see-through screens cost ~27 ms of GPU per frame), app icons rasterized once at display size (`AppIconImage`), no Material `TextField` (60 ms to compose), and the app list's rows composed after the slide (`rowsReady`: ~4 ms per row, they froze the first frame of the slide). `app/src/main/baseline-prof.txt` compiles the app's own code ahead of time (the libraries bring their own profiles); to try a release build on a phone that has the debug one, re-sign it with the debug key (`apksigner sign --ks ~/.android/debug.keystore`) and `adb install -r`, which keeps the preferences. Always count the idle frames of Home in the state users spend most time in (pause on, inside the window), not only in the others.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
