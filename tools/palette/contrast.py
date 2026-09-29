#!/usr/bin/env python3
"""WCAG 2.1 contrast report for ui/theme/Color.kt, as the Markdown tables in docs/DESIGN_SYSTEM.md.

Run after changing a color and paste the output into the "Contrasto" section. Exit code 1 if a
required pair (text roles, primary as text up to surfaceContainerHigh) drops below 4.5:1.
"""
import colorsys
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "app/src/main/java/com/pasquale/notificationblocker/ui/theme/Color.kt"
COLORS = {k: v[2:] for k, v in re.findall(r"val (\w+) = Color\(0x([0-9A-F]{8})\)", SRC.read_text())}


def rgb(h):
    return [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]


def lum(c):
    c = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def ratio(a, b):
    la, lb = sorted([lum(a), lum(b)], reverse=True)
    return (la + 0.05) / (lb + 0.05)


def mix(top, bottom, alpha):
    return [t * alpha + b * (1 - alpha) for t, b in zip(top, bottom)]


def c(theme, role):
    return rgb(COLORS[theme + role])


def cell(values, threshold=4.5):
    return " / ".join(f"**{v:.2f}**" if v < threshold else f"{v:.2f}" for v in values)


failures = []
out = ["Coppie `onX` / `X`:", "", "| Coppia | Chai | Lo-fi night |", "|---|---|---|"]
for bg, fg in [("Primary", "OnPrimary"), ("PrimaryContainer", "OnPrimaryContainer"), ("Secondary", "OnSecondary"),
               ("SecondaryContainer", "OnSecondaryContainer"), ("Tertiary", "OnTertiary"),
               ("TertiaryContainer", "OnTertiaryContainer"), ("Background", "OnBackground"), ("Surface", "OnSurface"),
               ("SurfaceVariant", "OnSurfaceVariant"), ("InverseSurface", "InverseOnSurface")]:
    vals = [ratio(c(t, fg), c(t, bg)) for t in ("Light", "Dark")]
    failures += [f"{fg}/{bg}" for v in vals if v < 4.5]
    name = lambda r: r[0].lower() + r[1:]
    out.append(f"| `{name(fg)}` / `{name(bg)}` | {vals[0]:.2f} | {vals[1]:.2f} |")

out += ["", "Testo sulle superfici (Chai / Lo-fi night; in grassetto i valori sotto 4.5):", "",
        "| Superficie | `onSurface` | `onSurfaceVariant` | `primary` come testo |", "|---|---|---|---|"]
required = {"Background", "Surface", "SurfaceContainerLowest", "SurfaceContainerLow", "SurfaceContainer", "SurfaceContainerHigh"}
for s in ["Background", "Surface", "SurfaceContainerLowest", "SurfaceContainerLow", "SurfaceContainer",
          "SurfaceContainerHigh", "SurfaceContainerHighest", "SurfaceVariant", "PrimaryContainer"]:
    row = []
    for fg in ("OnSurface", "OnSurfaceVariant", "Primary"):
        vals = [ratio(c(t, fg), c(t, s)) for t in ("Light", "Dark")]
        if fg == "Primary" and s in required:
            failures += [f"primary text on {s}" for v in vals if v < 4.5]
        row.append(cell(vals))
    out.append(f"| `{s[0].lower() + s[1:]}` | " + " | ".join(row) + " |")

# Status card: container with a 6% gradient toward the accent, subtitle at alpha 0.85
out += ["", "Card di stato, caso peggiore (gradiente al 6% verso l'accento, sottotitolo con alpha 0.85):", ""]
for label, cont, accent, text in [("ACTIVE_INSIDE", "PrimaryContainer", "Primary", "OnPrimaryContainer"),
                                  ("ACTIVE_OUTSIDE", "SecondaryContainer", "Secondary", "OnSecondaryContainer")]:
    vals = []
    for t in ("Light", "Dark"):
        bg = mix(c(t, accent), c(t, cont), 0.06)
        vals.append(ratio(mix(c(t, text), bg, 0.85), bg))
    failures += [label for v in vals if v < 4.5]
    out.append(f"- `{label}`: {vals[0]:.2f} in Chai, {vals[1]:.2f} in Lo-fi night")

sat = lambda t, r: colorsys.rgb_to_hls(*c(t, r))[2]
out += ["", "Saturazione HSL degli accenti (Chai / Lo-fi night): " + ", ".join(
    f"`{r.lower()}` {sat('Light', r):.2f} / {sat('Dark', r):.2f}" for r in ("Primary", "Secondary", "Tertiary"))]

print("\n".join(out))
if failures:
    print("\nFAIL: " + ", ".join(failures), file=sys.stderr)
    sys.exit(1)
