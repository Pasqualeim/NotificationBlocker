#!/usr/bin/env python3
"""Demo preferences for the screenshots: Mon-Fri 09:00-18:00, lunch, 5 weeks of history.
Usage: demo_prefs.py LANG "YYYY-MM-DD HH:MM" FILTERED_WINDOW HELD_SPEC
HELD_SPEC: pkg:count,pkg:count"""
import sys, datetime as dt, random
lang, now_s, window, held_spec = sys.argv[1:5]
now = dt.datetime.strptime(now_s, "%Y-%m-%d %H:%M")
random.seed(7)
RISE, SET = 7*60+25, 18*60+25
def daylight(a, b): return max(0, min(b, SET) - max(a, RISE))
lines = []
d = now.date() - dt.timedelta(days=35)
while d <= now.date():
    wd = d.weekday()
    limit = now.hour*60 + now.minute if d == now.date() else 1440
    if wd < 5:
        spans = [("work_off3" if wd == 0 else "work_off", 420, 540), ("lunch", 780, 840),
                 ("work_off3" if wd == 4 else "work_off", 1080, 1380 if random.random() > .15 else 1200)]
        held = random.randint(3, 16)
    else:
        spans = [("work_off3", 420, 1380)]
        held = random.randint(0, 5)
    spans = [(i, a, min(b, limit)) for i, a, b in spans if a < limit]
    if spans:
        lived = sum(b-a for _, a, b in spans)
        light = sum(daylight(a, b) for _, a, b in spans)
        by = {}
        for i, a, b in spans: by[i] = by.get(i, 0) + b - a
        lines.append("\t".join([d.isoformat(), str(lived), str(light), str(held),
            ",".join(f"{k}={v}" for k, v in sorted(by.items())),
            ";".join(f"{i}@{a}-{b}" for i, a, b in spans)]))
    d += dt.timedelta(days=1)
esc = lambda s: s.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "&#10;").replace("\t", "&#9;")
keys, total = [], 0
for pc in held_spec.split(","):
    pkg, n = pc.split(":")
    for _ in range(int(n)):
        keys.append(f"<string>{pkg}&#10;{total:016x}</string>"); total += 1
lunch = "Pranzo" if lang.startswith("it") else "Lunch"
millis = int(now.timestamp() * 1000)
print(f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="blocking_enabled" value="true" />
    <boolean name="onboarding_done" value="true" />
    <boolean name="zen_prompt_dismissed" value="true" />
    <string name="work_schedule">1,2,3,4,5|540|1080</string>
    <string name="pauses">{esc(f"lunch	{lunch}	780	840	31	1	*")}</string>
    <set name="blocked_apps"><string>com.google.android.gm</string><string>com.google.android.calendar</string><string>com.google.android.apps.docs</string></set>
    <string name="history">{esc(chr(10).join(lines))}</string>
    <long name="history_seen_at" value="{millis}" />
    <string name="filtered_window">{window}</string>
    <int name="filtered_count" value="{total}" />
    <set name="filtered_keys">{"".join(keys)}</set>
</map>""")
