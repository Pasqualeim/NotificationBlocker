#!/usr/bin/env python3
"""Generates the three illustrations of the break notification: the lake pier of the Home scene in
miniature, by day, at sunset and at night.

Writes:
  app/src/main/res/drawable/avd_zen_day.xml     (also used in the morning)
  app/src/main/res/drawable/avd_zen_sunset.xml
  app/src/main/res/drawable/avd_zen_night.xml

Edit this script, not the generated files. Colors come from ui/theme/ZenPalette.kt (Daylight,
GoldenHour, Night lights; pier, boat, reeds and firs multiplied by the ambient light, as the
scene does). Each file is an animated vector in a 48 x 48 rounded square: the notification hosts
it in an indeterminate ProgressBar, which starts and loops it (an ImageView in RemoteViews would not).
Motion is slow and small: water shimmer, a bobbing boat, reeds in the breeze, the sun's glow or the
lamp and the stars at night. Every loop is "reverse", so there is no jump at the seam.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res/drawable"

FRAME = "M10,0 H38 A10,10 0 0 1 48,10 V38 A10,10 0 0 1 38,48 H10 A10,10 0 0 1 0,38 V10 A10,10 0 0 1 10,0 Z"
HORIZON = 27

# Lights (ZenPalette) and the scene objects already multiplied by each light's ambient color
VARIANTS = {
    "day": dict(
        comment="by day (also in the morning): high sun with a soft glow, a drifting cloud",
        sky=("#3F9BEA", "#74BFF7", "#BFE6FF"), water=("#4FA3D1", "#3683B5"), shine="#D9F4FF",
        hill="#86AFC6", fir="#2E5444", pier="#B58358", pierShade="#7E5537", pierGap="#6A4630",
        hull="#EDE6DA", stripe="#D97757", reed="#7FA06A", reedDark="#5E7F4E",
        lamp="#3E414C", lampGlass="#FFF1D6",
    ),
    "sunset": dict(
        comment="at sunset: a large sun touching the hills, a golden path on the water",
        sky=("#5B4C8E", "#E2786A", "#FFC77A"), water=("#C87A62", "#8C5266"), shine="#FFE3A3",
        hill="#9C6475", fir="#2E462C", pier="#B56E39", pierShade="#7E4724", pierGap="#6A3B1F",
        hull="#EDC191", stripe="#D96A3A", reed="#7F8645", reedDark="#5E6A34",
        lamp="#3E3A3C", lampGlass="#FFE7C2",
    ),
    "night": dict(
        comment="at night: crescent moon, twinkling stars, the lamp lit at the end of the pier",
        sky=("#0B1238", "#1A2C78", "#2E4AA0"), water=("#1E3272", "#142352"), shine="#B8C8F0",
        hill="#24356E", fir="#102431", pier="#40393F", pierShade="#2C2528", pierGap="#251A23",
        hull="#54639D", stripe="#4D333F", reed="#2D454C", reedDark="#22363D",
        lamp="#1C1F2A", lampGlass="#FFF1D6",
    ),
}


def argb(color):
    return "#FF" + color[1:]


def path(d, fill=None, stroke=None, width=None, alpha=None, name=None):
    attrs = [f'android:name="{name}"'] if name else []
    attrs.append(f'android:pathData="{d}"')
    if fill:
        attrs.append(f'android:fillColor="{argb(fill)}"')
        if alpha is not None:
            attrs.append(f'android:fillAlpha="{alpha}"')
    if stroke:
        attrs.append(f'android:strokeColor="{argb(stroke)}"')
        attrs.append(f'android:strokeWidth="{width}"')
        attrs.append('android:strokeLineCap="round"')
        if alpha is not None:
            attrs.append(f'android:strokeAlpha="{alpha}"')
    return "<path " + " ".join(attrs) + " />"


def gradient_path(d, stops, y0, y1):
    items = "".join(
        f'<item android:color="{argb(c)}" android:offset="{i / (len(stops) - 1):.2f}" />' for i, c in enumerate(stops)
    )
    return (
        f'<path android:pathData="{d}"><aapt:attr name="android:fillColor">'
        f'<gradient android:type="linear" android:startX="0" android:startY="{y0}" android:endX="0" android:endY="{y1}">'
        f"{items}</gradient></aapt:attr></path>"
    )


def circle(cx, cy, r):
    return f"M{cx - r:g},{cy:g} a{r:g},{r:g} 0 1,0 {2 * r:g},0 a{r:g},{r:g} 0 1,0 {-2 * r:g},0"


def group(name, body, pivot=None):
    pv = f' android:pivotX="{pivot[0]}" android:pivotY="{pivot[1]}"' if pivot else ""
    return f'<group android:name="{name}"{pv}>{body}</group>'


def animator(prop, start, end, millis):
    return (
        f'<objectAnimator android:propertyName="{prop}" android:valueFrom="{start}" android:valueTo="{end}" '
        f'android:valueType="floatType" android:duration="{millis}" android:repeatCount="infinite" '
        'android:repeatMode="reverse" android:interpolator="@android:interpolator/accelerate_decelerate" />'
    )


def target(name, *animators):
    body = animators[0] if len(animators) == 1 else "<set>" + "".join(animators) + "</set>"
    return f'<target android:name="{name}"><aapt:attr name="android:animation">{body}</aapt:attr></target>'


def pier(v):
    """Wooden pier from the bottom left toward the middle of the lake, planks across, a lamp at its end."""
    near_l, near_r, far_l, far_r, far_y = 1.0, 17.5, 24.6, 27.4, 32.6
    parts = [path(f"M{near_l},48 L{near_r},48 L{far_r},{far_y} L{far_l},{far_y} Z", fill=v["pier"])]
    # Right edge in shade, then the posts under it
    parts.append(path(f"M{near_r},48 L{near_r + 1.6},48 L{far_r + 0.4},{far_y + 0.6} L{far_r},{far_y} Z", fill=v["pierShade"]))
    for t in (0.18, 0.45, 0.72):
        x = near_r + 0.8 + (far_r - near_r) * t
        y = 48 + (far_y - 48) * t
        parts.append(path(f"M{x:.2f},{y:.2f} V{y + 1.6 - t:.2f}", stroke=v["pierShade"], width=0.7))
    planks = []
    for k in range(1, 9):
        t = (k / 9) ** 0.8
        y = 48 + (far_y - 48) * t
        xl = near_l + (far_l - near_l) * t
        xr = near_r + (far_r - near_r) * t
        planks.append(f"M{xl:.2f},{y:.2f} L{xr:.2f},{y:.2f}")
    parts.append(path(" ".join(planks), stroke=v["pierGap"], width=0.35))
    # Lamp post at the far end
    parts.append(path(f"M{far_r - 0.6},{far_y} V24.6", stroke=v["lamp"], width=0.7))
    parts.append(path("M25.9,22.9 H27.7 V24.8 H25.9 Z", fill=v["lampGlass"]))
    parts.append(path("M25.6,22.9 H28 L26.8,21.9 Z", fill=v["lamp"]))
    return "".join(parts)


def boat(v):
    hull = path("M33.2,36.4 H42.4 L40.9,38.7 H34.6 Z", fill=v["hull"])
    stripe = path("M33.6,37 H42", stroke=v["stripe"], width=0.6)
    shadow = path("M34.4,39.2 H41", stroke=v["water"][1], width=0.6, alpha=0.7)
    return group("boat", shadow + hull + stripe, pivot=(37.8, 38))


def reeds(v):
    stalks = "M44.2,48 Q44.6,42 43.6,37.5 M45.8,48 Q46.4,43 46.6,38.8 M47.2,48 Q47.2,44 48.4,40.6"
    tips = path(circle(43.6, 38.6, 0.55) + " " + circle(46.6, 39.9, 0.55), fill=v["reedDark"])
    return group("reeds", path(stalks, stroke=v["reed"], width=0.7) + tips, pivot=(45.8, 48))


def water(v, shine_x):
    base = gradient_path(f"M0,{HORIZON} H48 V48 H0 Z", v["water"], HORIZON, 48)
    ripples = path(
        "M3,30.5 H9 M14,29.2 H19 M36,30.2 H44 M6,34 H11 M38,41.5 H45 M22,44.5 H28",
        stroke=v["shine"], width=0.5, alpha=0.55,
    )
    # Light path of the sun or moon: short strokes under it, shorter toward the viewer
    trail = " ".join(
        f"M{shine_x - w:.1f},{y:.1f} H{shine_x + w:.1f}"
        for y, w in ((28.2, 2.6), (29.8, 2.2), (31.4, 1.8), (33.2, 1.4), (35.2, 1.0))
    )
    return (
        base
        + group("ripples", ripples, pivot=(24, 36))
        + path(trail, stroke=v["shine"], width=0.6, alpha=0.8, name="trail")
    )


def horizon(v):
    hill = path(f"M0,24.6 Q10,21.6 21,23.6 Q33,25.6 48,22.6 V{HORIZON + 0.5} H0 Z", fill=v["hill"])
    firs = " ".join(
        f"M{x - w},{HORIZON} L{x},{HORIZON - h} L{x + w},{HORIZON} Z"
        for x, h, w in ((3, 4.2, 1.3), (6.2, 3.0, 1.0), (13.5, 3.6, 1.2), (40, 4.4, 1.4), (43.4, 3.2, 1.0))
    )
    shore = path(f"M0,{HORIZON - 0.8} H48 V{HORIZON} H0 Z", fill=v["fir"])
    return hill + path(firs, fill=v["fir"]) + shore


def art_day(v):
    sky = gradient_path(f"M0,0 H48 V{HORIZON} H0 Z", v["sky"], 0, HORIZON)
    glow = group("sunGlow", path(circle(33, 10, 8), fill="#FFE08A", alpha=0.4), pivot=(33, 10))
    sun = path(circle(33, 10, 4.2), fill="#FFF6D5")
    cloud = group(
        "cloud",
        path(circle(10, 12, 2.6) + " " + circle(13.4, 10.8, 3.2) + " " + circle(16.8, 12.2, 2.4), fill="#FFFFFF", alpha=0.92)
        + path("M8,13.4 H19 V14.6 H8 Z", fill="#E3EAF2"),
    )
    body = sky + glow + sun + cloud + horizon(v) + water(v, 33) + pier(v) + boat(v) + reeds(v)
    anims = [
        target("sunGlow", animator("scaleX", 0.88, 1.14, 3200), animator("scaleY", 0.88, 1.14, 3200)),
        target("cloud", animator("translateX", -1.5, 2.5, 9000)),
    ]
    return body, anims


def art_sunset(v):
    sky = gradient_path(f"M0,0 H48 V{HORIZON} H0 Z", v["sky"], 0, HORIZON)
    glow = group("sunGlow", path(circle(31, 22, 10), fill="#FFC77A", alpha=0.45), pivot=(31, 22))
    sun = path(circle(31, 22.5, 5.2), fill="#FFE3A3")
    body = sky + glow + sun + horizon(v) + water(v, 31) + pier(v) + boat(v) + reeds(v)
    anims = [target("sunGlow", animator("scaleX", 0.9, 1.12, 3600), animator("scaleY", 0.9, 1.12, 3600))]
    return body, anims


def art_night(v):
    sky = gradient_path(f"M0,0 H48 V{HORIZON} H0 Z", v["sky"], 0, HORIZON)
    stars_a = path(" ".join(circle(x, y, 0.5) for x, y in ((6, 5), (18, 3.5), (25, 9), (44, 15))), fill="#FFF1C1", name="starsA")
    stars_b = path(" ".join(circle(x, y, 0.45) for x, y in ((11, 9.5), (21, 15), (40, 4), (14, 18))), fill="#FFF1C1", name="starsB")
    # Crescent: the full moon covered by a disc of the sky color at that height (gradient at y = 9)
    moon = path(circle(34, 10, 4.2), fill="#F4EEDC") + path(circle(35.9, 8.7, 3.7), fill="#152363")
    lamp_glow = group("lampGlow", path(circle(26.8, 23.8, 4.2), fill="#FFD27F", alpha=0.35), pivot=(26.8, 23.8))
    body = sky + stars_a + stars_b + moon + horizon(v) + water(v, 34) + pier(v) + lamp_glow + boat(v) + reeds(v)
    anims = [
        target("starsA", animator("fillAlpha", 0.35, 1, 2200)),
        target("starsB", animator("fillAlpha", 1, 0.3, 3100)),
        target("lampGlow", animator("scaleX", 0.85, 1.15, 2600), animator("scaleY", 0.85, 1.15, 2600)),
    ]
    return body, anims


def common_anims():
    return [
        target("ripples", animator("translateX", -1.2, 1.2, 4200)),
        target("trail", animator("strokeAlpha", 0.45, 0.95, 1800)),
        target("boat", animator("translateY", -0.25, 0.35, 2400), animator("rotation", -2, 2, 3000)),
        target("reeds", animator("rotation", -3, 2, 3800)),
    ]


def write(name, variant, art):
    v = VARIANTS[variant]
    body, anims = art(v)
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/notification/zen_art.py: edit the script, not this file.\n"
        f"     Break notification, {v['comment']}. -->\n"
        '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt">\n'
        '    <aapt:attr name="android:drawable">\n'
        '        <vector android:width="48dp" android:height="48dp" android:viewportWidth="48" android:viewportHeight="48">\n'
        f'            <group android:name="frame"><clip-path android:pathData="{FRAME}" />\n'
        f"            {body}\n"
        "            </group>\n"
        "        </vector>\n"
        "    </aapt:attr>\n"
        + "".join(f"    {t}\n" for t in anims + common_anims())
        + "</animated-vector>\n"
    )
    (RES / name).write_text(xml)


if __name__ == "__main__":
    write("avd_zen_day.xml", "day", art_day)
    write("avd_zen_sunset.xml", "sunset", art_sunset)
    write("avd_zen_night.xml", "night", art_night)
    print("ok")
