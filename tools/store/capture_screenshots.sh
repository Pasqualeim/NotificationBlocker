#!/usr/bin/env bash
# Captures the raw store screenshots on an emulator, in Italian and English.
# Usage: tools/store/capture_screenshots.sh RAW_DIR   (then tools/store/compose_screenshots.py RAW_DIR font.ttf)
# Needs: the debug APK installed (run-as writes the preferences), notification access granted.
# Uses demo mode for a clean status bar, and a pause window that contains the current time.
set -e
RAW=${1:?raw dir}
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
E=${ANDROID_SERIAL:-emulator-5554}
P=com.pasquale.nook
S=$P/com.pasquale.notificationblocker.service.NotificationBlockerService
A=$P/com.pasquale.notificationblocker.MainActivity
adb -s $E shell pm grant $P android.permission.POST_NOTIFICATIONS
adb -s $E shell cmd notification allow_listener $S
adb -s $E shell settings put system time_12_24 24
demo(){ adb -s $E shell settings put global sysui_demo_allowed 1
  for args in "enter" "clock -e hhmm $(date +%H%M)" "battery -e level 100 -e plugged false" \
      "network -e wifi show -e level 4 -e fully true" "network -e mobile show -e level 4 -e datatype none" \
      "notifications -e visible $1"; do
    adb -s $E shell am broadcast -a com.android.systemui.demo -e command $args >/dev/null
  done; }
# A live Nook process (the listener service) keeps its old prefs in memory and writes them back over ours:
# write only with the listener off and the process dead. open_app turns the listener on again.
prefs(){ adb -s $E shell cmd notification disallow_listener $S >/dev/null; adb -s $E shell am force-stop $P; sleep 1
  adb -s $E shell "run-as $P sh -c 'mkdir -p shared_prefs && cat > shared_prefs/notification_blocker_prefs.xml'" <<X
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="blocking_enabled" value="true" />
    <int name="start_time" value="$1" />
    <int name="end_time" value="510" />
    <boolean name="zen_prompt_dismissed" value="true" />
    <set name="blocked_apps"><string>com.google.android.gm</string><string>com.google.android.calendar</string><string>com.google.android.apps.docs</string></set>
    $2
</map>
X
}
# Home has the round app icon at the top left (brown in light, peach in dark); the app list and the launcher do not
is_home(){ adb -s $E shell dumpsys window | grep mCurrentFocus | grep -q "$P" || return 1
  adb -s $E exec-out screencap -p | python3 -c "
import sys,io
from PIL import Image
r,g,b=Image.open(io.BytesIO(sys.stdin.buffer.read())).convert('RGB').getpixel((60,170))
sys.exit(0 if r-b>50 else 1)"; }
# fresh task, then wait for the entrance animation; if the system still restored the app list, go back to Home
open_app(){ local i; for i in 1 2 3; do
    adb -s $E shell am force-stop $P; adb -s $E shell cmd notification allow_listener $S
    adb -s $E shell am start -W -f 0x10008000 -n $A >/dev/null; sleep 9
    is_home && return
    adb -s $E shell input keyevent BACK; sleep 3; is_home && return
  done; echo "Nook Home never showed up" >&2; exit 1; }
snap(){ # $2: "exit" = real status bar (demo mode fakes the date shown in the shade), else demo mode with notification icons $2 (default false)
  if [ "$2" = exit ]; then adb -s $E shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null; else demo ${2:-false}; fi
  sleep 1; adb -s $E exec-out screencap -p > "$1"; }   # demo mode re-applied each time: it drops the mobile icon now and then
held(){ # $1 window date, $2..: package:count
  local keys="" total=0 i
  for pc in "${@:2}"; do for i in $(seq 1 ${pc#*:}); do keys="$keys<string>${pc%:*}&#10;k$total</string>"; total=$((total+1)); done; done
  echo "<string name=\"filtered_window\">$1</string><int name=\"filtered_count\" value=\"$total\" /><set name=\"filtered_keys\">$keys</set>"; }
NOW=$(( 10#$(date +%H) * 60 + 10#$(date +%M) )); START=$(( (NOW / 60 - 1) * 60 ))   # a pause that started this hour
TODAY=$(date +%Y-%m-%d); YESTERDAY=$(date -v-1d +%Y-%m-%d 2>/dev/null || date -d yesterday +%Y-%m-%d)
for L in it-IT en-US; do
  mkdir -p "$RAW/$L"; adb -s $E shell am force-stop $P; adb -s $E shell cmd locale set-app-locales $P --locales $L >/dev/null
  adb -s $E shell cmd locale set-app-locales com.android.systemui --locales $L >/dev/null   # shade, tiles and date follow the language too
  # the first launch after a locale change is slow and blank (and later ones can keep the old language): warm up once
  adb -s $E shell am start -n $A >/dev/null; sleep 10; adb -s $E shell am force-stop $P; sleep 2
  demo false; adb -s $E shell cmd uimode night no
  prefs $START "$(held $TODAY com.google.android.gm:3 com.google.android.calendar:2 com.google.android.apps.docs:1)"; open_app; snap "$RAW/$L/1_break.png"
  adb -s $E shell input keyevent HOME; sleep 1
  adb -s $E shell cmd statusbar expand-notifications; sleep 3; snap "$RAW/$L/2_shade.png" exit; adb -s $E shell cmd statusbar collapse
  adb -s $E shell cmd uimode night yes; sleep 2; open_app; snap "$RAW/$L/5_dark.png"; adb -s $E shell cmd uimode night no; sleep 2
  prefs 1080 "$(held $YESTERDAY com.google.android.gm:7 com.google.android.calendar:4 com.google.android.apps.docs:3)"; open_app; snap "$RAW/$L/4_report.png"
  for i in 1 2 3; do adb -s $E shell input tap 540 2280; sleep 4; is_home || break; done   # the "Work apps" button
  snap "$RAW/$L/3_apps.png"
  adb -s $E shell input keyevent BACK; sleep 1   # leave Home as the last screen: the next launch must not restore the app list
done
adb -s $E shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
adb -s $E shell cmd locale set-app-locales $P --locales "" >/dev/null 2>&1 || true
adb -s $E shell cmd locale set-app-locales com.android.systemui --locales "" >/dev/null 2>&1 || true
echo "Raw captures in $RAW"
