#!/usr/bin/env bash
# Captures the raw store and site screenshots on an emulator, in Italian and English.
# Usage: tools/store/capture_screenshots.sh RAW_DIR   (then tools/store/compose_screenshots.py RAW_DIR font.ttf)
# Needs: the debug APK installed (run-as writes the preferences) on an emulator (the clock is moved with
# `cmd alarm set-time`, automatic time is switched off). The data is the demo of tools/store/demo_prefs.py:
# work Mon-Fri 09:00-18:00, lunch 13-14, five weeks of history.
# Writes 1_break (Today), 2_shade, 3_apps, 4_report, 5_dark, 6_breaks, 7_stats.
set -e
RAW=${1:?raw dir}
D=$(cd "$(dirname "$0")" && pwd)
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
E=${ANDROID_SERIAL:-emulator-5554}
P=${P:-com.pasquale.freetime.dev}   # the debug build ("Unclock dev")
S=$P/com.pasquale.notificationblocker.service.NotificationBlockerService
A=$P/com.pasquale.notificationblocker.MainActivity
GM=com.google.android.gm; CAL=com.google.android.calendar; DOCS=com.google.android.apps.docs
# A weekday at sunset (golden scene, the evening break just started), and the Monday after a weekend (report)
EVENING="2026-10-14 18:15"; EVENING_KEY="2026-10-14#work_off"
MONDAY="2026-10-12 09:40"; WEEKEND_KEY="2026-10-09#work_off3"
adb -s $E shell pm grant $P android.permission.POST_NOTIFICATIONS
adb -s $E shell settings put system time_12_24 24
adb -s $E shell settings put global auto_time 0
adb -s $E shell cmd alarm set-timezone Europe/Rome
demo(){ adb -s $E shell settings put global sysui_demo_allowed 1
  for args in "enter" "clock -e hhmm $(adb -s $E shell date +%H%M | tr -d '\r')" "battery -e level 100 -e plugged false" \
      "network -e wifi show -e level 4 -e fully true" "network -e mobile show -e level 4 -e datatype none" \
      "notifications -e visible $1"; do
    adb -s $E shell am broadcast -a com.android.systemui.demo -e command $args >/dev/null
  done; }
# A live Nook process (the listener service) keeps its old prefs in memory and writes them back over ours:
# write only with the listener off and the process dead. open_app turns the listener on again.
# $1 lang, $2 "date time" (the emulator clock is set to it), $3 held occurrence key, $4 pkg:count,...
# The clock goes first: TIME_SET wakes Nook up, and a process started then would write its old prefs back.
prefs(){ adb -s $E shell cmd alarm set-time $(( $(date -j -f "%Y-%m-%d %H:%M" "$2" +%s) * 1000 )); sleep 2
  adb -s $E shell cmd notification disallow_listener $S >/dev/null; adb -s $E shell am force-stop $P; sleep 1
  python3 "$D/demo_prefs.py" "$@" | adb -s $E shell "run-as $P sh -c 'mkdir -p shared_prefs && cat > shared_prefs/notification_blocker_prefs.xml'"; }
open_app(){ adb -s $E shell am force-stop $P; adb -s $E shell cmd notification allow_listener $S
  adb -s $E shell am start -W -f 0x10008000 -n $A >/dev/null; sleep 9; }
tab(){ adb -s $E shell input tap $1 2232; sleep 4; }   # bottom bar: Today 180, Breaks 540, Stats 900
snap(){ # $2: "exit" = real status bar (demo mode fakes the date shown in the shade), else demo mode with notification icons $2 (default false)
  if [ "$2" = exit ]; then adb -s $E shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null; else demo ${2:-false}; fi
  sleep 1; adb -s $E exec-out screencap -p > "$1"; }   # demo mode re-applied each time: it drops the mobile icon now and then
for L in ${LANGS:-it-IT en-US}; do
  mkdir -p "$RAW/$L"; adb -s $E shell am force-stop $P; adb -s $E shell cmd locale set-app-locales $P --locales $L >/dev/null
  adb -s $E shell cmd locale set-app-locales com.android.systemui --locales $L >/dev/null   # shade, tiles and date follow the language too
  # the first launch after a locale change is slow and blank (and later ones can keep the old language): warm up once
  adb -s $E shell am start -n $A >/dev/null; sleep 10; adb -s $E shell am force-stop $P; sleep 2
  demo false; adb -s $E shell cmd uimode night no
  prefs $L "$EVENING" "$EVENING_KEY" $GM:3,$CAL:2,$DOCS:1; open_app; snap "$RAW/$L/1_break.png"
  tab 540; snap "$RAW/$L/6_breaks.png"
  adb -s $E shell input tap 540 1306; sleep 4; snap "$RAW/$L/3_apps.png"   # the "Work apps" row
  adb -s $E shell input keyevent BACK; sleep 2
  tab 900; adb -s $E shell input tap 106 611; sleep 3; snap "$RAW/$L/7_stats.png"   # the week before: a full one
  tab 180; adb -s $E shell input keyevent HOME; sleep 1
  # on a headless emulator the shade sometimes comes out black: open it again until it shows
  for i in 1 2 3 4 5; do
    adb -s $E shell cmd statusbar expand-notifications; sleep 4; snap "$RAW/$L/2_shade.png" exit
    python3 -c "
from PIL import Image
r,g,b=Image.open('$RAW/$L/2_shade.png').convert('RGB').getpixel((540,600))
raise SystemExit(0 if r+g+b>60 else 1)" && break
    adb -s $E shell cmd statusbar collapse; sleep 2
  done
  adb -s $E shell cmd statusbar collapse
  adb -s $E shell cmd uimode night yes; sleep 2; open_app; snap "$RAW/$L/5_dark.png"; adb -s $E shell cmd uimode night no; sleep 2
  prefs $L "$MONDAY" "$WEEKEND_KEY" $GM:7,$CAL:4,$DOCS:3; open_app; snap "$RAW/$L/4_report.png"
done
adb -s $E shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
adb -s $E shell settings put global auto_time 1
adb -s $E shell cmd locale set-app-locales $P --locales "" >/dev/null 2>&1 || true
adb -s $E shell cmd locale set-app-locales com.android.systemui --locales "" >/dev/null 2>&1 || true
echo "Raw captures in $RAW"
