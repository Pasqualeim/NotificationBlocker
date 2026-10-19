# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Unclock** (formerly Nook, formerly NotificationBlocker; renamed 2026-10-19, `docs/DESIGN_SYSTEM.md` "Nome"; the code keeps `Nook*` identifiers such as `NookBadge`, `NookWidgetProvider`, `nook_widget_info.xml`, `ic_notification_nook`, and `applicationId` `com.pasquale.nook` stays because it is fixed forever once published): Android app (Kotlin + Jetpack Compose) that silently dismisses notifications from user-selected "work" apps when the user is not working: the user sets WHEN THEY WORK (days and hours, default Mon-Fri 09:00-18:00) and everything outside it is a pause; extra pauses (lunch...) have name, days and optionally their own apps.

The question behind the app (`docs/IDEA.md`): why do apps reward you and show statistics only for the time you focus and work, while nothing exists for free time and personal health? Unclock counts the time you get back.

Purpose: we have a life beyond work. "Off-hours" means any time you are not working (an afternoon, an evening, a whole day), not "night and sleep". Copy, scenes and icon celebrate the free time you get back, never rest-as-a-reward or the work you are missing. Single module `:app`. `applicationId` is `com.pasquale.nook` (fixed forever once published); the Kotlin namespace stays `com.pasquale.notificationblocker`, so class names in `adb` commands need the full path. Naming and color rationale: `docs/DESIGN_SYSTEM.md`.

- minSdk 28, targetSdk 37, compileSdk 37
- AGP 9.4 with built-in Kotlin, pinned to 2.4 through the `kotlin` version in the catalog (no `kotlin-android` plugin), Gradle 9.8, version catalog in `gradle/libs.versions.toml`
- UI: Compose Material 3 Expressive (`androidx.compose.material3` 1.5.0-beta01, pinned in the catalog because the BOM 2026.09 ships 1.4 without the Expressive APIs; go back to the BOM once 1.5 stable is in it) + Navigation 3 (`androidx.navigation3`). Native Pixel look: `MaterialExpressiveTheme` + `MotionScheme.expressive()`, the brand Chai / Lo-fi night palette (no wallpaper colors), the phone's system font (no downloaded font), default `Shapes()`
- Persistence: `SharedPreferences` only (no Room, no DataStore, no network)

Related docs (read before UI work):

- `docs/DESIGN_SYSTEM.md`: colors, type, shapes, components, copy tone
- `docs/MOTION.md`: animation tokens and rules
- `docs/ROADMAP.md`: project status (publishing, open items, done); update it when you ship or drop an item
- `docs/IDEA.md`: the question behind Unclock, the four rules and the anti-stress checklist every new idea (statistics, rewards, copy) must pass. Read before designing anything that counts or rewards free time
- `docs/REDESIGN.md`: the Nook 2 plan (done: pauses, history, three tabs)
- `docs/PUBLISHING.md`: the owner's step-by-step Google Play checklist (manual steps: accounts, testing, forms)
- `docs/store/LISTING.md`: store texts IT/EN, screenshots in `docs/store/screenshots/`, Play Console form answers. Keep it in sync with the app (a new permission, data flow or feature changes the Data safety answers)

Website: a separate repo at `~/Documents/Developer/nook-site` (static site for Cloudflare Pages: home, tester sign-up into D1, privacy policy, IT/EN; its own `CLAUDE.md`). It reuses the app's palette, scene renders and screenshots, so a change to `Color.kt`, the scenes or Home may need new site assets. The app's `privacy_policy_url` still points to the GitHub Pages copy (`docs/index.html`) until the site has its domain; then update the string, `docs/store/LISTING.md` and turn `docs/index.html` into a redirect.

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

Install and run on emulator or phone: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The debug build is **"Unclock dev"**, package `com.pasquale.nook.dev` (`applicationIdSuffix`, name from `app/src/debug/res`): it installs next to the Unclock from the Play Store with its own data, so the owner's phone keeps the real app while updates are tried. The copy from Play is signed by Google (Play App Signing), not with the upload key, so a locally built release can never update it; a new version reaches it only through the Play Console.

### Testing the blocking end-to-end without UI

```bash
P=com.pasquale.nook.dev   # the debug build
adb shell cmd notification allow_listener $P/com.pasquale.notificationblocker.service.NotificationBlockerService
# write shared_prefs/notification_blocker_prefs.xml via `run-as $P` with
# blocking_enabled=true, start_time=end_time (whole day), blocked_apps={com.android.shell}
adb shell cmd notification post -t Test tag "hello"   # posted as com.android.shell
adb logcat -s NotificationBlocker                      # expect "Canceling notification from ..."
```

## Architecture

```
app/src/main/java/com/pasquale/notificationblocker/
├── MainActivity.kt                  # Activity, Route (Onboarding, Tabs, DaysOff, AppSelection(pauseId)), NavDisplay graph, TabsScreen: bottom ShortNavigationBar with Tab enum (Today / Breaks / Stats), one LargeFlexibleTopAppBar per tab with its own saveable TopAppBarState (Today: greeting title + date subtitle + NookBadge, medium expanded height, pinned scroll behavior; Breaks and Stats: large title, exitUntilCollapsed), Material Home icon for Today, one SaveableStateHolder per tab
├── data/
│   ├── Pause.kt                     # Pause (name, start, end, days, enabled, apps, date: set for one-off pauses, `startsOn`), Occurrence (one time it happens, key "date#id"), PauseCodec (prefs text, 8th field = date)
│   ├── QuickPause.kt                # pure: "Pause now", a one-off pause from this minute for the chosen time (ids "quick<HHmm>", one at a time; unit tested)
│   ├── DaysOff.kt                   # pure: ranges of days off (whole-day dated pauses, id "off<stamp>", max 62 days) and Italian public holidays incl. Easter (id "holiday"); unit tested
│   ├── PauseSchedule.kt             # pure: which pauses are on, the blocking rule, blockedUntil, nextStart (unit tested)
│   ├── WorkSchedule.kt              # pure: when the user works (days, day shift start<end; default Mon-Fri 09:00-18:00), `offWorkPauses()` generates the pauses outside work, one per stretch from the end of a work day to the start of the next (`work_off` Mon-Thu, `work_off3` Friday→Monday: `Pause.nights` = 3, so ending it early, its held count and its morning report cover the whole weekend), `colorSlots`, `nightsOf` (unit tested)
│   ├── SleepWindow.kt               # pure: when the user sleeps (default 23:00-07:00, start == end = none); asleep minutes are held as usual but not counted as free time (unit tested)
│   ├── WeekCoverage.kt              # pure: the typical week for the Stats tab ("Planned week") and onboarding (stretches per weekday, days grouped when their free times match, overlaps merged and counted once; sleep hours kept apart and excluded from the weekly total) (unit tested)
│   ├── Stats.kt                     # pure: StatsRange, PeriodStats, DaySpans (sleep cut out; uses the recorded stretches, an estimate from the pause times for days recorded before) for Stats (unit tested)
│   ├── FreeTimeLedger.kt            # pure: per-day history (lived, daylight, held, minutes per pause and the stretches `LivedSpan`; `Config.sleep` leaves asleep minutes out), HistoryCodec (unit tested)
│   ├── SunTimes.kt                  # pure: sunrise/sunset for a fixed latitude in the phone's zone (scenes and daylight in the history)
│   ├── OffHours.kt                  # minutes helpers (format, currentMinutes), `date()` (lowercases Italian day and month names: Samsung capitalizes them) and the single-window test that `LifeCopy` still uses for the daylight messages
│   ├── MorningReport.kt             # pure: held notifications per app, when to show the report (unit tested)
│   └── PreferencesManager.kt        # SharedPreferences singleton, shouldBlock(), allPauses(), needsOnboarding
├── notification/
│   ├── ZenNotificationState.kt      # pure: what the zen notification shows now (unit tested)
│   └── ZenNotificationManager.kt    # posts/updates/removes the silent ongoing zen notification
├── service/
│   └── NotificationBlockerService.kt  # NotificationListenerService, cancels notifications (also sweeps the active ones on connect), drives zen refresh
├── widget/
│   ├── NookWidgetState.kt           # pure: what the home screen widget shows (status, break in progress or next, "Pause 1 h"; unit tested)
│   └── NookWidgetProvider.kt        # RemoteViews widget (no Glance), redrawn from ZenNotificationManager.refresh only when its text changes; private QuickPauseReceiver (not exported) starts the 1 h pause
├── tile/
│   ├── ZenTileState.kt              # pure: tile icon/label/subtitle (unit tested)
│   └── ZenTileService.kt            # Quick Settings tile "Unclock": toggles blocking
└── ui/
    ├── MainViewModel.kt             # AndroidViewModel shared by all screens (StateFlows)
    ├── Permissions.kt               # listener access / post-notifications checks, openNotificationAccess (Unclock's own switch)
    ├── screens/
    │   ├── MainScreen.kt            # Today tab: own header (no TopAppBar), staggered entrance of top card, scene, TodayMetrics, TodayCard; one top card at a time (TopCard); also holds TimePickerDialog
    │   ├── PausesScreen.kt          # Breaks tab: "When you work" (WorkScheduleCard), "What gets paused" (one Work apps row, opens the app list), then the "Other breaks" segmented group: weekly PauseRows, "Days off" row; the "Add a break" FAB bottom right opens PauseEditorSheet (the owner wants it there). `LinkRow` and `SectionHeader` are internal, shared with Stats
    │   ├── DaysOffScreen.kt         # Route.DaysOff: add/remove ranges of days off, public holidays switch (`holidays_enabled`)
    │   ├── OnboardingScreen.kt      # Route.Onboarding, first launch: work apps, "When do you work?" (WorkScheduleCard + optional lunch 13-14 on work days + week chart), notification access (the four-point disclosure as icon rows, re-checked on resume with a confirmation, one filled button at a time), then the `POST_NOTIFICATIONS` prompt
    │   ├── StatsScreen.kt           # Stats tab: week / month / day ranges, charts of StatsCharts (each figure once: count-up total, chart, list only for what the number does not say), fed by data/Stats.kt; at the bottom "Planned week" (WeekCoverageCard from allPauses + sleep) and the "Sleep" row (TimeRangeSheet: sleep only changes what counts as free time, so it lives with the numbers)
    │   └── AppSelectionScreen.kt    # MediumTopAppBar, pill search, All/Selected filter; one plain list: the apps picked when it opens come first, rows never move while you pick; rows are composed once the slide is over
    ├── components/
    │   ├── HeroHeader.kt            # status card (4 HeroStatus states: off, break at hh:mm, break in progress, pause ended) + master Switch
    │   ├── MutedBellIcon.kt         # bell that shakes, then gets slashed when blocking turns on
    │   ├── TodayHeader.kt               # todayGreeting, todayDate, NookBadge (the padlock, `ic_logo`), compactDuration and TodayMetrics (free today, daylight, paused); no header composable, the top app bar of Today shows them
    │   ├── Visuals.kt                   # PauseAvatar (time-of-day icon), expressiveShape(index) (MaterialShapes cookie/clover/burst), segmentColors() (segments on surfaceContainer), Modifier.entrance, CountUpText, MetricTile; PauseAvatar and MetricTile use the expressive shapes
    │   ├── WeekCoverageCard.kt      # "Planned week" at the bottom of Stats and onboarding step 2 (redrawn as templates are ticked; onboarding uses the default sleep window): hours a week without work notifications, sleep excluded (sleep hours drawn faded on the bars, the legend names the sleep window; `compact` (Stats): no inner "Every week" label and one legend dot "Sleep, not counted" instead of the sentence, onboarding keeps the full card), one 24 h bar per group of alike days ("Lun–ven", "Sab–dom": never "Weekend", which is a preset pause name) with the free times written under it ("fino alle 08:30 · 13:00–14:00 · dalle 18:30"), a one-line legend (data/WeekCoverage.kt groups/merge)
    │   ├── TodayCard.kt                 # "Today" on Home as a segmented list (segment shapes from `ListItemDefaults.segmentedShapes`; compact custom rows, not `SegmentedListItem`, whose minimum height ate the scene's room): first segment = now/next line with a LinearWavyProgressIndicator (waveSpeed = 0.dp) and the day bar, then one segment per pause with state chips (TodayPauses, unit tested), hours lived
    │   ├── PauseRow.kt              # PauseRow (SegmentedListItem), DayPills (editor), daysSummary, pauseColor/pauseName/pauseTimes: one pause in the Breaks list, with its PauseAvatar
    │   ├── PauseEditorSheet.kt      # ModalBottomSheet: name, start/end (TimePickerDialog), days, apps row (opens AppSelection for that pause), "Use all work apps"
    │   ├── StatsCharts.kt           # WeekChart, MonthCalendar, DayChart (horizontal 24 h bar like Today and Breaks: stretches in pause colors, thin daylight line above, dot at now, times written under it), LegendItem, MonthLegend (Less ○●●● More free time, in the card under MonthCalendar), TotalsRow (two lines at most: "Notifications paused" and, for week/month, "Average a day"; free time and daylight are the big figure)
    │   ├── PermissionCard.kt        # listener-permission prompt (in the top card's place until access is granted)
    │   ├── MorningReportCard.kt     # held notifications after a pause (top apps with icon, count and Open), in the top card's place until dismissed
    │   ├── TimeRangeSheet.kt        # shared bottom sheet for a time range + Save: sleep (bedtime / wake-up, "Sleep" row on Stats) and work hours (WorkHoursSheet)
    │   ├── WorkScheduleCard.kt      # work days as pills toggled in place, hours row (opens WorkHoursSheet), one line on free days
    │   ├── QuickPauseSheet.kt       # bottom sheet of "Pause now" (button on the status card): choose how long
    │   ├── SceneCard.kt             # card framing the scene on Home; fills the size it is given (MainScreen gives it full width at 2:1, `SCENE_ASPECT`)
    │   ├── ZenScene.kt              # animated scene: office desk (work) / lake pier (off work)
    │   ├── AmbientClock.kt          # the one 30 fps clock of Home's slow motion (scene + glow behind the bell)
    │   ├── SearchField.kt           # pill search field: bare BasicTextField (the Material TextField froze the list screen ~60 ms when it opened)
    │   ├── ZenNotificationCard.kt   # one-time POST_NOTIFICATIONS prompt, in the top card's place right after the listener permission
    │   ├── CurrentMinutes.kt        # rememberCurrentMinutes(): minute tick for clocks, timeline, greeting
    │   ├── LifeMessageText.kt       # localized text of a `LifeMessage` (stringResource), `durationText`, `isSunny()`
    │   ├── AppItemRow.kt, AppIconImage.kt
    │   └── ShimmerSkeleton.kt, EmptyState.kt   # loading / empty states
    ├── zen/                         # vector scene, pure Kotlin: ZenEnvironment (season/time of day/light/rain/snow),
    │                                # ScenePainting → DeskPainting (office), NaturePainting (lake pier); ZenPainter (Compose + AWT in tests);
    │                                # LifeCopy (daylight / free-time messages on Home, unit tested)
    └── theme/                       # Color ("Chai" light, "Lo-fi night" dark: fallback below Android 12 and base for previews/contrast tooling; `ColorScheme.sunlight` = daylight bars of the charts, never text), Motion (tokens), Type (M3 default Typography in the system font, headlines/titleLarge Medium),
                                     # Shape (default Shapes()), Theme (MaterialExpressiveTheme), ZenPalette (scene colors)
```

The app has two themes: `QuietHoursTheme` (used by `MainActivity`, a `MaterialExpressiveTheme` with `MotionScheme.expressive()`) and `NotificationBlockerTheme` (a thin alias used by previews). `dynamicColor` defaults to `false` on purpose: the native look comes from the Expressive components, type and shapes, while the colors stay Unclock's own Chai / Lo-fi night palette (the owner tried the wallpaper colors, Material You, and chose the brand palette).

### Blocking rule (single source of truth)

`PauseSchedule.shouldBlock(pkg) = isBlockingEnabled && some pause p (from `PreferencesManager.allPauses()`) is in progress now && p was not ended early && pkg in (p.apps ?: blockedApps)`

- Nook 2 (plan: `docs/REDESIGN.md`) has a list of pauses, each with name, start, end, the days it **starts** on, an on/off switch and optional own apps (null = all the work apps). Pauses may overlap: a notification is held if any pause in progress holds its app.
- Evaluated **at notification post time** from the current clock. There are deliberately **no alarms** (`AlarmManager`), boot receivers or cached "currently off-hours" flags: they drifted after reboot/Doze. Do not reintroduce them unless a feature needs to act at the window boundary (e.g. clearing already-visible notifications).
- `allPauses()` = the pauses generated from the work schedule (`WorkSchedule.offWorkPauses()`: one pause per stretch off work, end→start of the next work day, `work_off` 1 night, `work_offN` N nights (Friday→Monday is `work_off3`): `Pause.nights` lets a pause cross several midnights, never saved by `PauseCodec`, `Pause.MAX_NIGHTS` = 7 bounds every lookback (occurrences in progress, Today, history, day chart, morning report, ended keys); all share color slot 0, see `WorkSchedule.colorSlots`) + the weekly `pauses` + `one_off_pauses` + the public holidays (when `holidays_enabled`; generated for yesterday..a week ahead, never stored). Blocking, history, break notification, tile and widget all read it. A pause with `date` set is a dated one-off: it starts only on that day (`Pause.startsOn`), ignoring `days`. Three kinds: "Pause now" (`QuickPause`, id `quick<HHmm>`; starting one turns blocking on and replaces the previous one), days off (`DaysOff`: whole-day pauses with `start == end`, one per date sharing the id `off<stamp>`, max 62 days, a range stays whole until its last day is past), Italian public holidays (id `holiday`, Easter and Easter Monday included, off by default). Other expired one-offs are dropped after yesterday.
- Each pause runs half-open `[start, end)`, wraps past midnight when `start > end` (Friday's 18:30-08:30 belongs to Friday) and `start == end` covers its whole calendar day. Times are minutes from midnight.
- Each time a pause happens is an `Occurrence` with key `"2026-10-15#lunch"`: ended early, notification swiped away and held notifications use it. "End the pause" ends every pause in progress.
- The history (`FreeTimeLedger`, plan phase 2): a minute counts when blocking is on and a pause in progress, not ended early, has apps to silence, and it is not inside the sleep window (`sleep_start`/`sleep_end`: still held, but not free time; history recorded before this was added still includes sleep); it belongs to its calendar day, counts once for the first-started pause when pauses overlap, and its daylight part uses `SunTimes`. Still no alarms: `PreferencesManager.settleHistory()` credits the minutes since the last look (`history_seen_at`), on every `ZenNotificationManager.refresh()` (each minute with the screen on) and Home refresh, and **before** every write of a setting that decides whether the pause is on (`settleThen`: master switch, pauses, work apps, end/resume). Any new such setting must go through `settleThen`. Gaps longer than 3 days count only their last 3 (a weekend with the phone asleep; longer is more likely the phone off or a restored backup). Computed on intervals, real time across daylight saving.
- Home edits no times: pauses are created and edited in the Breaks tab (`PauseEditorSheet`); Home shows only today's pauses (`TodayCard`). The work apps are chosen per pause (`Route.AppSelection(pauseId)`; null = the shared `blocked_apps`).

### Preferences keys (`notification_blocker_prefs`)

`blocking_enabled` (Boolean), `work_schedule` (String "1,2,3,4,5|540|1080": ISO weekdays|start|end in minutes; day shift only, start < end; missing = never set: old installs keep their own `pauses`, the Breaks card shows the default as "not in use yet" and the first change saves it; written through `settleThen`), `one_off_pauses` (String, same format as `pauses` with the date as 8th tab-separated field: quick pause and days off), `holidays_enabled` (Boolean, default false), `onboarding_done` (Boolean; the first-launch setup runs only if it is false and none of `pauses`, `start_time`, `blocking_enabled` exists, so updates from older versions skip it; finishing saves the work schedule and the optional lunch pause and turns blocking on), `pauses` (String, one pause per line, see `PauseCodec`; when missing, one pause every day built from the legacy `start_time` / `end_time`, defaults 22:00 → 07:00), `blocked_apps` (StringSet), `last_celebrated_window` (String, legacy: written by the removed "end of shift" card, no longer read), `filtered_window` / `filtered_count` / `filtered_keys` (work notifications held in that pause occurrence, counted once per notification key; group summaries and non-clearable ones are not counted; `filtered_keys` entries are `package\nfingerprint`, the first 8 bytes of the SHA-256 of the notification key in hex: the key is never stored, its tag can be a chat id with a phone number and the file goes into the Android backup; older versions wrote `package\nkey` or the bare key), `report_seen_window` (occurrence whose morning report was dismissed), `zen_dismissed_window` (occurrence in which the user swiped the zen notification away), `pause_ended_keys` (StringSet of occurrences ended early with "End the pause": nothing is held until the next pause; keys older than `Pause.MAX_NIGHTS + 1` (8) days are dropped; cleared when blocking is turned on again; the old `pause_ended_window` is no longer read). Window keys written before Nook 2 are bare ISO dates (`Occurrence.dateOf` reads both), `zen_prompt_dismissed` (Boolean, notification-permission prompt answered "Not now"), `history` (String, one line per day, see `HistoryCodec`: lived, daylight, held, minutes per pause, 6th field the lived stretches "pauseId@start-end;..." in minutes of that day, wall clock; five-field lines from before still decode, without times; kept forever, about 40 KB a year, goes into the Android backup), `history_seen_at` (Long epoch millis of the last settle), `sleep_start` / `sleep_end` (Int minutes from midnight, default 23:00 → 07:00, equal = no sleep window; written through `settleThen`; blocking ignores them). Always write a new set for `blocked_apps` (never mutate the one returned by `getStringSet`).

### App list

Apps come from `queryIntentActivities(ACTION_MAIN + CATEGORY_LAUNCHER)`, matched by the `<queries>` block in the manifest. Do **not** add `QUERY_ALL_PACKAGES` (Play policy) and do not filter by `FLAG_SYSTEM` (hides preinstalled apps like Gmail/Teams).

### Permissions

Only notification listener access is required, granted by the user in system settings: `ui/Permissions.kt` (`openNotificationAccess`) opens Unclock's own switch (`ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS`, Android 11+; the full list `ACTION_NOTIFICATION_LISTENER_SETTINGS` is the fallback), and holds the two permission checks. The Today tab (`MainScreen`) re-checks it on `ON_RESUME` and shows `PermissionCard` in place of the status card while it is missing. `POST_NOTIFICATIONS` is optional: it only enables the zen status notification. On Android 13+ the first-launch setup asks for it when it ends (only if the listener access was given; step 3 says so first, or the system prompt reads as a contradiction); `ZenNotificationCard` on Today asks once for whoever skipped it or updated from an older version, in place of the status card right after the listener permission; denying it changes nothing else. The app uses no alarms, so `SCHEDULE_EXACT_ALARM` is intentionally absent.

### Sprout MVP (parked, not in the code)

A pixel-art sprout that grew with the break time (`SproutCard`, `SproutArt`, an older `FreeTimeLedger`, prefs `growth_*`, `settleGrowth`) was never committed. It is parked in `git stash` (`stash@{0}`). The committed app has no sprout, no character, no growth logic and no `growth_*` keys: Home shows the scene (`SceneCard`/`ZenScene`). There is no character planned for now; any reward for free time is to be designed from scratch against `docs/IDEA.md`. Do not restore the stash as is.

### Zen status notification

User-facing name: "notifica della pausa" / "break notification" ("zen" survives only in code and resource names).

Silent ongoing notification (channel `zen_status`, `IMPORTANCE_LOW`, `DecoratedCustomViewStyle`, small icon `ic_notification_nook`, the Unclock padlock) shown **only while Unclock really holds work notifications**: blocking on, a pause in progress, that occurrence not ended early, at least one app to silence and the notification access granted (`ZenNotificationState.resolve`); never between pauses. Title "Work paused until 07:00" (or "all day"); text how many were paused and from which apps (names left out of the public version on a locked screen; copy says "paused", never "waiting": held notifications do not come back). Expanded: the lake pier in miniature for the phase of the day (`avd_zen_day/sunset/night`, generated by `tools/notification/zen_art.py`, hosted by an indeterminate `ProgressBar` because RemoteViews do not start animated vectors in an `ImageView`) and a calm line. Action "End the pause" (`EndPauseReceiver`) writes the occurrences in progress to `pause_ended_keys`: `shouldBlock` lets work through until the next pause, Home shows "Pause ended" with "Pause again", the tile says when the next one starts. No confirmation notification: the break notification just goes away (a second one with "Undo" confused users). Still no alarms: the system removes it at the pause end via `setTimeoutAfter`; `ZenNotificationManager.refresh()` runs from the app (toggle, times, resume), from the listener service on each posted notification, and from a receiver the service registers at runtime for `TIME_TICK` (screen on only), `SCREEN_ON`, `USER_PRESENT` and time/zone changes. It re-posts only when the visible content changes (`contentKey`). Swiped away → hidden until the next pause (or until blocking is turned on again).

The Quick Settings tile (`ZenTileService`, active tile: no polling) toggles `blocking_enabled` directly; `MainViewModel` follows preference changes with an `OnSharedPreferenceChangeListener`, and calls `ZenTileService.requestUpdate()` after changes made in the app. Details and diagrams: `docs/MOTION.md` ("Live Notification: architettura").

## Conventions

- All user-facing text goes in `res/values/strings.xml` **and** `res/values-it/strings.xml` (English default, Italian translation). Use `plurals` for counts; Italian needs `one`, `many`, `other`.
- Collect flows in Compose with `collectAsStateWithLifecycle()`; lifecycle hooks with `LifecycleEventEffect`.
- Format times with `OffHours.format()` (locale-independent `HH:mm`).
- Colors: always `MaterialTheme.colorScheme.*`, never `Color(0x…)` or `Color.White`/`Color.Black` outside `ui/theme/Color.kt` (zen scene colors live in `ui/theme/ZenPalette.kt`). The only XML colors are `window_background` (`res/values[-night]/colors.xml`, the launch and splash color): keep it equal to `LightBackground` / `DarkBackground`; and the `widget_*` set of the home screen widget (RemoteViews cannot read the Compose theme): keep each equal to its `Color.kt` token (surface container, onSurface, onSurfaceVariant, primary/onPrimary for the badge, secondaryContainer/onSecondaryContainer for the button). `primary` as text only on `background`/`surface`/`surfaceContainerLow..High`, never on `surfaceContainerHighest` or `primaryContainer` (contrast table in `docs/DESIGN_SYSTEM.md`). Shapes from `MaterialTheme.shapes` (or `CircleShape`), no `RoundedCornerShape(N.dp)` in components.
- Components: stateless composables in `ui/components`, taking state + lambdas, `modifier: Modifier = Modifier` as the first optional parameter. Every component gets `@Preview`s for its main states.
- Animations: follow `docs/MOTION.md` (`label =` on every animation, no motion on first composition unless it is an entrance). Durations, easing and press springs come from `ui/theme/Motion.kt` (`Motion.standard(Motion.SHORT)`, `Motion.press()`, `Motion.Stagger`…), never inline numbers; choreography owned by one component is a named `private const` listed in the `MOTION.md` catalog. Animated state that must survive a status change lives **outside** `AnimatedContent`.
- Icons: only `material-icons-core` is on the classpath. Adding `material-icons-extended` roughly doubles the debug APK; prefer copying the single needed icon as an `ImageVector` or vector drawable (see `res/drawable/ic_moon*.xml`).
- Keep dependencies minimal (`ui-text-google-fonts` was removed with Manrope; `material3` is pinned to 1.5.0-beta01 for the Expressive APIs, see `docs/ROADMAP.md`): the original scaffold pulled in Room, Retrofit, CameraX, Coil, Play Services etc. without using them; they were removed on purpose. Any new dependency needs a reason in `docs/ROADMAP.md` (`lottie-compose` was removed with the "end of shift" card; `tools/lottie/lottie_kit.py` stays for a future Lottie asset). Generated assets: edit the script, not the output. The launcher icon: `tools/icon/launcher_icon.py` writes the adaptive-icon drawables and `docs/store/icon-512.png`; `tools/character/sketch.py` writes the historical A/B/C character drafts (`docs/character/*.png`, `docs/mockups/character_home.svg`); it reads `SproutArt.kt`, which now exists only in the stash, so it does not run on the committed tree; `tools/palette/contrast.py` checks `Color.kt` and prints the contrast tables of `DESIGN_SYSTEM.md` (run it after any color change).
- Log tag for the service: `NotificationBlocker`.
- User-facing copy: plain, human, concrete (see "Testi e tono" in `docs/DESIGN_SYSTEM.md`). Review new strings with the `unslop` skill rules; the permission disclosure must keep its four points (what the app sees, what it doesn't do, nothing leaves the phone, revocable).

## Known gaps / ideas

See `docs/ROADMAP.md` for the full list. Most relevant:

- Notifications already visible when a pause starts are not cleared. (When the listener service connects, e.g. after boot or an update, `sweepActiveNotifications()` does remove the ones that `shouldBlock`, so nothing posted while it was unbound leaks.)
- Date exceptions exist (days off ranges, Italian public holidays, "Pause now"); no holidays for other countries yet.
- `.agent/plan.md` is the original scaffolding brief: historical, outdated (mentions AlarmManager and components that no longer exist).
- Release builds run R8; extra keep rules go in `app/proguard-rules.pro`.
- **Home is a plain scrolling column; nothing is resized to fit** (rule: `docs/DESIGN_SYSTEM.md`, "Home: la scena non cambia misura"). `MainScreen` is a `Column` (`spacedBy(12.dp)`) in `verticalScroll` (`overscrollEffect = null`) under the pinned medium top bar. The scene is always full width at 2:1 (`SCENE_ASPECT`), cards and text keep their size; a taller page (permission card, notification prompt, largest system font) just scrolls. The old `HomeColumn`/`HomeFit` scaling was removed: the scene grew and shrank as the top card changed. The top of Home still holds **one card at a time** (`TopCard`: permission → notification prompt → morning report → status card with the switch). The metric tiles under the scene have equal heights (`Row` with `IntrinsicSize.Min`) and a label may take two lines. Home blocks are composed from the first frame and enter with `homeEntrance` (alpha + offset), never `AnimatedVisibility`. Text sizes stay compact (16dp card padding, `titleLarge` status, `bodyMedium` texts) so common phones show most of Home without scrolling; check new blocks on the phone (`wm size`/`wm density`/`font_scale`, always reset after).
- Performance: judge smoothness on a **release** build (debug Compose is ~2x slower and starts in 1.8 s instead of 0.4 s on a Galaxy A32). Measure with `adb shell dumpsys gfxinfo <package> framestats` (only the last 120 frames are kept, so reset right before the gesture; `Total frames rendered` over a few idle seconds gives the real frame rate). The test phone (Galaxy A32) is **90 Hz** (11 ms per frame) with a Mali-G52 whose clock sits at its minimum (299 of 950 MHz) even at 100% load, and it redraws the whole window on every frame (no partial updates): ~7 ms of GPU for an empty window, ~16 ms for Home, ~20 ms with the scene. So: **never start an animation that runs on its own clock** (`rememberInfiniteTransition`, `animate*AsState` loops) on a screen that stays open: it redraws the window on every vsync (90 per second) and the GPU never catches up. The breathing glow behind the bell did exactly that whenever the pause was on (the main state): 458 frames in 5 s, every one late, scroll and taps stuttering. Slow ambient motion reads `AmbientClock` (30 fps, shared by the scene and the glow, frozen while the page scrolls or another screen slides over Home); idle Home now renders 150 frames in 5 s. The Home scroll has no edge stretch (`overscrollEffect = null`): the page is barely taller than the screen and the stretch cost 18 ms of GPU per frame. Also: opaque slide transitions instead of cross-fades (two see-through screens cost ~27 ms of GPU per frame), app icons rasterized once at display size (`AppIconImage`), no Material `TextField` (60 ms to compose), and the app list's rows composed after the slide (`rowsReady`: ~4 ms per row, they froze the first frame of the slide). `app/src/main/baseline-prof.txt` compiles the app's own code ahead of time (the libraries bring their own profiles); a release build has the Play package `com.pasquale.nook`: on a phone with the Play copy it cannot be installed over it (different signature), so judge release smoothness on the build that testers get from the Play Console, or on a phone without the Play copy. The wavy progress bar of Today (`LinearWavyProgressIndicator`) must keep `waveSpeed = 0.dp`: a moving wave redraws every frame. Always count the idle frames of Home in the state users spend most time in (pause on, inside the window), not only in the others.

## Priorities and focus

The owner wants to be told when a request is not the most useful thing to do now. Before starting anything bigger than a quick answer, compare it with what is still open:

1. `docs/ROADMAP.md`, section "0. Fase di test (prima del Play Store)", in the owner's order: (1) fix the app (`docs/IDEA.md` is the yardstick); (2) the site advertises the final product; (3) site email sign-up (D1 `signups`, `community` telegram|email): a new sign-up gets an email right away with the links to the Google Play closed test (an open Google Group plus the opt-in page; the closed test is only the delivery tool, not publishing) and to a one-way Telegram channel (updates, tips), to validate the product before publishing. The site is not deployed yet.
2. Uncommitted or half-finished work in the tree (`git status`): finish, commit or drop it before opening a new front.
3. Then the rest of the roadmap, in order: foundations, look & feel, features, quality.
4. Last: Google Play publishing (`docs/ROADMAP.md`, "Pubblicazione": trademark check, Play Console, closed test with 12 testers for 14 days, store listing). It comes after the independent testing phase. Until then, do not prepare Play Store material (store screenshots, feature graphic, listing texts, Play Console forms) unless the owner asks: the project is in the testing phase.

If the request sits clearly below an open item above (e.g. new scenes, nice-to-have features while the testing phase is still open), say so **before** doing it, in two or three lines: what it is, why it can wait, what should come first. Then do what the owner decides; it is a warning, never a refusal. Do not warn on quick questions, bug fixes, things that unblock the testing phase, or when the owner already said they want to do it anyway in this conversation. Warn once per topic, not at every turn.

### Antigravity for side work

To save Claude's tokens, suggest doing **side tasks that do not touch the code** in Antigravity: image generation (Gemini), moodboards, store graphic drafts, web research. When suggesting it, give a ready-to-paste prompt for Antigravity with the constraints from the docs (palette, `docs/IDEA.md` rules, sizes). Claude keeps everything that ends up in the repo as code or project docs: Kotlin, scripts in `tools/`, importing and converting assets, tests, docs, commits.

## Agents

Three subagents in `.claude/agents/`. Use them without being asked:

- `android-reviewer`: after every change to app code, before committing. Read-only.
- `docs-keeper`: when a feature, permission or data flow is added, changed or removed, and after a release. Keeps `docs/ROADMAP.md`, `docs/store/LISTING.md`, both CLAUDE.md files and the facts on the site (`~/Documents/Developer/nook-site`) in line with the code.
- `copywriter`: for new or changed user-facing text (app, site, store), IT and EN.

Everything else the main session does itself.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
