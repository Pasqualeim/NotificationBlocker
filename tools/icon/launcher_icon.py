#!/usr/bin/env python3
"""Generates the Unclock icon (an open padlock whose body is a clock) from one set of shapes.

Writes:
  app/src/main/res/drawable/ic_launcher_foreground.xml  (cream padlock, transparent; also the monochrome layer)
  app/src/main/res/drawable/ic_launcher_background.xml  ("sunset" gradient, peach to terracotta)
  app/src/main/res/drawable/ic_logo.xml                  (the same padlock as a tintable 32dp in-app icon)
  app/src/main/res/drawable/ic_notification_nook.xml     (the same padlock, white, for the status bar)
  docs/store/icon-512.png                                (Play Store icon, full bleed square)

Edit the shapes here, not the generated files. Shapes are drawn on a 256 grid and scaled into the
108 x 108 adaptive-icon viewport; the glyph stays inside the central 66dp safe zone. The clock hands
are holes in the body (evenOdd), so the mark stays one color. Needs Pillow (and numpy) for the PNG.
"""
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res/drawable"

CREAM = "#FFF6EC"
# Darker at both ends than a plain peach-terracotta blend: the cream glyph keeps >= 3:1 against
# the background everywhere it sits (3.4 at the shackle top, 4.9 at the body bottom).
BG_TOP, BG_BOTTOM = "#D9845C", "#9C4329"

K = 0.5523  # cubic handle length for a quarter circle


def arc(cx, cy, r, a0, a1):
    """Cubic segments along a circle from angle a0 to a1 (degrees, y down, 0 = east), <= 90 deg each."""
    out, n = [], max(1, math.ceil(abs(a1 - a0) / 90))
    step = (a1 - a0) / n
    for i in range(n):
        s, e = math.radians(a0 + i * step), math.radians(a0 + (i + 1) * step)
        h = 4 / 3 * math.tan((e - s) / 4) * r
        p0 = (cx + r * math.cos(s), cy + r * math.sin(s))
        p3 = (cx + r * math.cos(e), cy + r * math.sin(e))
        p1 = (p0[0] - h * math.sin(s), p0[1] + h * math.cos(s))
        p2 = (p3[0] + h * math.sin(e), p3[1] - h * math.cos(e))
        out.append("C{:.2f},{:.2f} {:.2f},{:.2f} {:.2f},{:.2f}".format(*p1, *p2, *p3))
    return " ".join(out)


# On the 256 grid. Body: a clock face, r 62. Shackle: a 20-wide band (centerline r 36), open on the right.
BODY = "M66,166 " + arc(128, 166, 62, 180, 540) + " Z"
SHACKLE = ("M82,130 V84 " + arc(128, 84, 46, 180, 360) + " V94 " + arc(164, 94, 10, 0, 180)
           + " V84 " + arc(128, 84, 26, 360, 180) + " V130 Z")
# Hands at ten past twelve-ish (hour up, minute at three), 14 wide with round ends: an L-shaped hole.
HANDS = ("M121,130 " + arc(128, 130, 7, 180, 360) + " V159 H156 " + arc(156, 166, 7, 270, 450)
         + " H128 " + arc(128, 166, 7, 90, 180) + " Z")

S = 58 / 190  # glyph height 190 on the grid -> 58 viewport units (inside the 66dp safe zone)


def scaled(d):
    return re.sub(r"-?\d*\.?\d+", lambda m: f"{float(m.group()) * S:.2f}", d)


# name -> (path, evenOdd). The hands are a hole in the body, so they share its path.
SHAPES = {
    "Body (clock face) with the hands cut out": (scaled(BODY + " " + HANDS), True),
    "Shackle, open": (scaled(SHACKLE), False),
}


def subpaths(d, scale, steps=24):
    """Flattens M/L/H/V/C/Z path data into one polygon per subpath."""
    tokens = re.findall(r"[MLHVCZ]|-?\d*\.?\d+", d)
    polys, pts, cur, cmd = [], [], (0.0, 0.0), None
    num = lambda: float(tokens.pop(0))
    while tokens:
        if re.fullmatch(r"[MLHVCZ]", tokens[0]):
            cmd = tokens.pop(0)
        if cmd == "M":
            if pts:
                polys.append(pts)
            cur = (num(), num()); pts = [cur]
        elif cmd == "L":
            cur = (num(), num()); pts.append(cur)
        elif cmd == "H":
            cur = (num(), cur[1]); pts.append(cur)
        elif cmd == "V":
            cur = (cur[0], num()); pts.append(cur)
        elif cmd == "C":
            p1, p2, p3 = (num(), num()), (num(), num()), (num(), num())
            for k in range(1, steps + 1):
                t = k / steps; u = 1 - t
                pts.append((u**3 * cur[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
                            u**3 * cur[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1]))
            cur = p3
    if pts:
        polys.append(pts)
    return [[(x * scale, y * scale) for x, y in p] for p in polys]


def mask(size, dx=0.0, dy=0.0):
    """The glyph as an L mask on a size x size canvas for the 108 viewport (hands cut out)."""
    m = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(m)
    s = size / 108
    for d, even_odd in SHAPES.values():
        for i, poly in enumerate(subpaths(d, s)):
            draw.polygon([(x + dx * s, y + dy * s) for x, y in poly], fill=0 if even_odd and i else 255)
    return m


def glyph_center(res=20):
    """(mass_x, mass_y, box_x, box_y) of the drawn glyph, in viewport units.

    Horizontally the center of mass wins (the open shackle makes the box lopsided); vertically we
    split the difference between mass (pulled down by the solid body) and box."""
    import numpy as np
    ys, xs = np.nonzero(np.asarray(mask(108 * res)))
    return xs.mean() / res, ys.mean() / res, (xs.min() + xs.max()) / 2 / res, (ys.min() + ys.max()) / 2 / res


def argb(hex_color):
    return "#FF" + hex_color[1:]


def paths_xml(color, indent):
    return "\n".join(
        f'{indent}<!-- {name} -->\n{indent}<path\n{indent}    android:fillColor="{color}"\n'
        + (f'{indent}    android:fillType="evenOdd"\n' if even_odd else "")
        + f'{indent}    android:pathData="{d}" />'
        for name, (d, even_odd) in SHAPES.items()
    )


def vector(comment, size_dp, viewport, group, body):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!-- Generated by tools/icon/launcher_icon.py: edit the script, not this file.\n     {comment} -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{size_dp}dp"\n    android:height="{size_dp}dp"\n'
        f'    android:viewportWidth="{viewport}"\n    android:viewportHeight="{viewport}">\n'
        f"    <group {group}>\n{body}\n    </group>\n</vector>\n"
    )


def write_xml():
    (RES / "ic_launcher_foreground.xml").write_text(vector(
        "Launcher icon: an open padlock whose body is a clock, your time unlocked. Single color, so it\n"
        "     also works as the themed (monochrome) icon. Inside the 66dp safe zone.",
        108, 108, f'android:translateX="{DX:.1f}" android:translateY="{DY:.1f}"', paths_xml(argb(CREAM), "        ")))
    (RES / "ic_launcher_background.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/icon/launcher_icon.py: edit the script, not this file.\n"
        "     Launcher icon background: sunset gradient, peach at the top to terracotta at the bottom. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        '    <path android:pathData="M0,0h108v108h-108z">\n'
        '        <aapt:attr name="android:fillColor">\n'
        "            <gradient\n"
        '                android:type="linear"\n'
        '                android:startX="54"\n                android:startY="0"\n'
        '                android:endX="54"\n                android:endY="108"\n'
        f'                android:startColor="{argb(BG_TOP)}"\n'
        f'                android:endColor="{argb(BG_BOTTOM)}" />\n'
        "        </aapt:attr>\n    </path>\n</vector>\n"
    )


def write_logo_icon():
    """In-app icon (Today badge, metric tile, widget): the glyph in a 64 x 64 viewport (32dp). It fills
    almost the whole height, so vertically it is centered on its bounding box, not optically."""
    (RES / "ic_logo.xml").write_text(vector(
        "The Unclock padlock, tinted by the caller (Icon / LocalContentColor / android:tint).",
        32, 64, f'android:translateX="{32 - CX:.1f}" android:translateY="{32 - BY:.1f}"', paths_xml("#FF000000", "        ")))


def write_notification_icon():
    """Status bar icon of the break notification: the padlock in white (the system only reads the alpha)
    in a 48 x 48 viewport (24dp), scaled to 40 units tall and centered on its bounding box."""
    k = 40 / 58
    group = (f'android:pivotX="{BX:.1f}" android:pivotY="{BY:.1f}" android:scaleX="{k:.3f}" android:scaleY="{k:.3f}" '
             f'android:translateX="{24 - BX:.1f}" android:translateY="{24 - BY:.1f}"')
    (RES / "ic_notification_nook.xml").write_text(vector(
        "Status bar icon of the break notification: the Unclock padlock, white on transparent.",
        24, 48, group, paths_xml("#FFFFFFFF", "        ")))


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def write_png(size=512, ss=4):
    big = size * ss
    img = Image.new("RGB", (big, big))
    top, bottom = rgb(BG_TOP), rgb(BG_BOTTOM)
    draw = ImageDraw.Draw(img)
    for y in range(big):
        t = y / (big - 1)
        draw.line([(0, y), (big, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    img.paste(Image.new("RGB", (big, big), rgb(CREAM)), mask=mask(big, DX, DY))
    # Play wants a 32-bit PNG: RGBA, fully opaque
    img.resize((size, size), Image.LANCZOS).convert("RGBA").save(ROOT / "docs/store/icon-512.png")


if __name__ == "__main__":
    MX, MY, BX, BY = glyph_center()
    CX, CY = MX, (MY + BY) / 2
    DX, DY = 54 - CX, 54 - CY
    write_xml()
    write_logo_icon()
    write_notification_icon()
    write_png()
    print("ok")
