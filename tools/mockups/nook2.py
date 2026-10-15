#!/usr/bin/env python3
"""Mock-ups of Nook 2 (docs/REDESIGN.md, phase 3): the three tabs, the pause editor, the three charts.

Writes docs/mockups/nook2.svg (six phones on one sheet). Light theme, colors from ui/theme/Color.kt.
The hedgehog is a crop of the concept image (docs/character/riccio_concept_antigravity.webp) until the
owner's pixel art exists. Copy is a draft: the copywriter reviews it in phase 5. Needs Pillow.
"""
import base64
import io
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/mockups/nook2.svg"
CONCEPT = ROOT / "docs/character/riccio_concept_antigravity.webp"

C = {
    "bg": "#F8F2E9", "surface": "#FCF8F2", "low": "#F6EEE4", "cont": "#F2E8DC", "high": "#EEE2D4",
    "highest": "#E9DBCB", "on": "#3A2B22", "onVar": "#65503F", "outlineVar": "#DDCDBD", "outline": "#8C7565",
    "primary": "#9A4F34", "onPrimary": "#FFFFFF", "pc": "#EBCDBC", "onPc": "#3B1E11",
    "secondary": "#4B6A55", "sc": "#D6E5D8", "onSc": "#16301F",
    "tertiary": "#4C6682", "tc": "#DCE5EF", "onTc": "#1A2E43",
    # Draft: the daylight part of the charts. Not in Color.kt yet (needs a contrast check)
    "sun": "#D9A55B",
}
W, H = 360, 780
GAP = 40
FONT = "Manrope, 'Helvetica Neue', Arial, sans-serif"

PAUSE_COLORS = {"Pranzo": C["tertiary"], "Sera": C["secondary"], "Weekend": C["primary"]}


def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


class Svg:
    def __init__(self):
        self.parts = []

    def add(self, s):
        self.parts.append(s)

    def rect(self, x, y, w, h, fill, r=0, stroke=None, sw=1, op=None):
        extra = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        extra += f' opacity="{op}"' if op is not None else ""
        self.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}"{extra}/>')

    def text(self, x, y, s, size=14, fill=None, weight=400, anchor="start", op=None):
        extra = f' opacity="{op}"' if op is not None else ""
        self.add(
            f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{fill or C["on"]}" '
            f'text-anchor="{anchor}"{extra}>{esc(s)}</text>'
        )

    def circle(self, cx, cy, r, fill, stroke=None, sw=1):
        extra = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        self.add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}"{extra}/>')

    def path(self, d, fill="none", stroke=None, sw=2):
        extra = f' stroke="{stroke}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round"' if stroke else ""
        self.add(f'<path d="{d}" fill="{fill}"{extra}/>')


def hedgehog_png():
    """The armchair pose of the concept, as a stand-in for the den."""
    img = Image.open(CONCEPT).convert("RGB")
    crop = img.crop((850, 40, 1300, 450)).resize((450, 410))
    buf = io.BytesIO()
    crop.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


HEDGEHOG = None


# --- Shared pieces -------------------------------------------------------------------------------
def status_bar(s, x, y):
    s.text(x + 24, y + 26, "18:52", 13, weight=600)
    s.rect(x + W - 52, y + 15, 24, 12, "none", 3, stroke=C["on"], sw=1.4)
    s.rect(x + W - 49, y + 18, 16, 6, C["on"], 1)


def nav_bar(s, x, y, active):
    top = y + H - 80
    s.rect(x, top, W, 80, C["cont"])
    items = [("Oggi", icon_mug), ("Pause", icon_clock), ("Il tempo tuo", icon_bars)]
    for i, (label, icon) in enumerate(items):
        cx = x + W / 6 + i * W / 3
        if i == active:
            s.rect(cx - 32, top + 12, 64, 32, C["sc"], 16)
        icon(s, cx, top + 28, C["onSc"] if i == active else C["onVar"])
        s.text(cx, top + 62, label, 12, C["on"] if i == active else C["onVar"], 700 if i == active else 500, "middle")


def icon_mug(s, cx, cy, col):
    s.path(f"M{cx-8} {cy-6} h12 v9 a4 4 0 0 1 -4 4 h-4 a4 4 0 0 1 -4 -4 z", stroke=col, sw=2)
    s.path(f"M{cx+4} {cy-3} h2 a3 3 0 0 1 0 6 h-2", stroke=col, sw=2)


def icon_clock(s, cx, cy, col):
    s.circle(cx, cy, 9, "none", stroke=col, sw=2)
    s.path(f"M{cx} {cy-5} V{cy} l4 3", stroke=col, sw=2)


def icon_bars(s, cx, cy, col):
    for i, h in enumerate([8, 14, 11]):
        s.rect(cx - 9 + i * 7, cy + 8 - h, 4, h, col, 1.5)


def switch(s, x, y, on=True):
    if on:
        s.rect(x, y, 52, 32, C["primary"], 16)
        s.circle(x + 36, y + 16, 12, C["onPrimary"])
    else:
        s.rect(x, y, 52, 32, C["highest"], 16, stroke=C["outline"], sw=2)
        s.circle(x + 16, y + 16, 8, C["outline"])


def day_pills(s, x, y, days):
    for i, d in enumerate("LMMGVSD"):
        on = i in days
        s.circle(x + 12 + i * 28, y + 12, 12, C["sc"] if on else "none", stroke=None if on else C["outlineVar"], sw=1.2)
        s.text(x + 12 + i * 28, y + 16.5, d, 11, C["onSc"] if on else C["onVar"], 700 if on else 500, "middle")


def timeline(s, x, y, w, segments, now=None):
    """24 h bar with one colored segment per pause: (start_h, end_h, color)."""
    s.rect(x, y, w, 10, C["highest"], 5)
    for a, b, col in segments:
        s.rect(x + w * a / 24, y, w * (b - a) / 24, 10, col, 5)
    if now is not None:
        nx = x + w * now / 24
        s.circle(nx, y + 5, 7, C["surface"], stroke=C["on"], sw=2.5)
    for h in (0, 6, 12, 18, 24):
        s.text(x + w * h / 24, y + 28, f"{h:02d}", 10, C["onVar"], 500, "middle")


# --- Screens ---------------------------------------------------------------------------------------
def screen_today(s, x, y):
    s.rect(x, y, W, H, C["bg"], 36)
    status_bar(s, x, y)
    s.text(x + 20, y + 74, "Nook", 26, weight=800)
    s.text(x + 20, y + 96, "Giovedì 15 ottobre", 13, C["onVar"])
    # Status card with the master switch
    s.rect(x + 16, y + 112, W - 32, 92, C["pc"], 24)
    s.text(x + 34, y + 146, "In pausa: Sera", 19, C["onPc"], 800)
    s.text(x + 34, y + 170, "Il lavoro torna alle 08:30", 13, C["onPc"])
    s.text(x + 34, y + 190, "3 notifiche lasciate fuori", 13, C["onPc"], op=0.8)
    switch(s, x + W - 84, y + 140)
    # The den (2:1)
    s.rect(x + 16, y + 218, W - 32, 164, C["cont"], 28)
    s.add(f'<clipPath id="den{x}{y}"><rect x="{x+16}" y="{y+218}" width="{W-32}" height="164" rx="28"/></clipPath>')
    s.add(f'<g clip-path="url(#den{x}{y})"><rect x="{x+16}" y="{y+218}" width="{W-32}" height="164" fill="#F3E6D6"/>'
          f'<image x="{x+90}" y="{y+218}" width="180" height="164" href="{HEDGEHOG}" preserveAspectRatio="xMidYMid meet"/></g>')
    s.rect(x + 28, y + 352, 128, 22, C["surface"], 11, op=0.92)
    s.text(x + 40, y + 367, "Ascolta musica", 11, C["onVar"], 600)
    s.text(x + 20, y + 410, "Il riccio ha messo le cuffie. Fuori è buio", 13, C["on"])
    s.text(x + 20, y + 428, "dalle 18:40.", 13, C["on"])
    # Today
    s.rect(x + 16, y + 448, W - 32, 168, C["low"], 24)
    s.text(x + 34, y + 478, "Oggi", 15, weight=800)
    s.text(x + W - 34, y + 478, "2 h 10 min tue", 13, C["secondary"], 700, "end")
    timeline(s, x + 34, y + 496, W - 68, [(0, 8.5, C["secondary"]), (13, 14, C["tertiary"]), (18.5, 24, C["secondary"])], now=18.9)
    for i, (name, when, col) in enumerate([("Pranzo", "13:00-14:00  ·  vissuta", C["tertiary"]),
                                            ("Sera", "18:30-08:30  ·  in corso", C["secondary"])]):
        yy = y + 552 + i * 28
        s.circle(x + 40, yy - 4, 5, col)
        s.text(x + 54, yy, name, 13, weight=700)
        s.text(x + W - 34, yy, when, 12, C["onVar"], 500, "end")
    nav_bar(s, x, y, 0)


def pause_card(s, x, y, name, times, days, on=True, note=None):
    s.rect(x, y, W - 32, 104, C["low"], 24)
    s.rect(x, y + 24, 6, 56, PAUSE_COLORS[name], 3)
    s.text(x + 20, y + 32, name, 17, weight=800)
    s.text(x + 20, y + 56, times, 22 if len(times) < 14 else 17, weight=700, op=1 if on else 0.5)
    day_pills(s, x + 14, y + 68, days)
    if note:
        s.text(x + W - 52, y + 32, note, 11, C["onVar"], 600, "end")
    switch(s, x + W - 100, y + 36, on)


def screen_pauses(s, x, y):
    s.rect(x, y, W, H, C["bg"], 36)
    status_bar(s, x, y)
    s.text(x + 20, y + 80, "Pause", 26, weight=800)
    s.text(x + 20, y + 102, "Nei momenti segnati, le app di lavoro tacciono.", 13, C["onVar"])
    pause_card(s, x + 16, y + 120, "Pranzo", "13:00 - 14:00", {0, 1, 2, 3, 4})
    pause_card(s, x + 16, y + 236, "Sera", "18:30 - 08:30", set(range(7)), note="in corso")
    pause_card(s, x + 16, y + 352, "Weekend", "Tutto il giorno", {5, 6})
    # Work apps
    s.rect(x + 16, y + 476, W - 32, 64, C["cont"], 20)
    for i in range(4):
        s.circle(x + 44 + i * 18, y + 508, 12, [C["tertiary"], C["primary"], C["secondary"], C["outline"]][i], stroke=C["cont"], sw=2)
    s.text(x + 120, y + 504, "App di lavoro", 14, weight=700)
    s.text(x + 120, y + 522, "6 app, valgono per tutte le pause", 12, C["onVar"])
    s.text(x + W - 34, y + 514, "›", 22, C["onVar"], 500, "end")
    # Extended FAB
    s.rect(x + W - 196, y + H - 160, 176, 56, C["pc"], 18)
    s.text(x + W - 168, y + H - 126, "+", 22, C["onPc"], 600)
    s.text(x + W - 146, y + H - 126, "Aggiungi pausa", 15, C["onPc"], 700)
    nav_bar(s, x, y, 1)


def screen_editor(s, x, y):
    screen_pauses(s, x, y)
    s.rect(x, y, W, H, "#1B1714", 36, op=0.45)
    top = y + 196
    s.rect(x, top, W, H - 196, C["surface"], 28)
    s.rect(x, top + 40, W, H - 236, C["surface"])
    s.rect(x + W / 2 - 16, top + 10, 32, 4, C["outlineVar"], 2)
    s.text(x + 20, top + 50, "Modifica pausa", 20, weight=800)
    s.rect(x + 20, top + 66, W - 40, 52, C["cont"], 14)
    s.text(x + 36, top + 86, "Nome", 11, C["onVar"], 600)
    s.text(x + 36, top + 106, "Pranzo", 16, weight=700)
    for i, (lab, val) in enumerate([("INIZIO", "13:00"), ("FINE", "14:00")]):
        bx = x + 20 + i * ((W - 40) / 2 + 6)
        s.rect(bx, top + 130, (W - 52) / 2, 72, C["cont"], 18)
        s.text(bx + 16, top + 154, lab, 11, C["onVar"], 700)
        s.text(bx + 16, top + 186, val, 26, weight=800)
    s.text(x + 20, top + 230, "Giorni", 13, C["onVar"], 700)
    day_pills(s, x + 20, top + 240, {0, 1, 2, 3, 4})
    s.rect(x + 20, top + 282, W - 40, 56, C["low"], 16)
    s.text(x + 36, top + 305, "App da silenziare", 12, C["onVar"], 600)
    s.text(x + 36, top + 325, "Tutte le app di lavoro", 15, weight=700)
    s.text(x + W - 36, top + 318, "›", 22, C["onVar"], 500, "end")
    s.text(x + 20, top + 372, "Elimina pausa", 14, C["primary"], 700)
    s.rect(x + W - 136, top + 350, 116, 44, C["primary"], 22)
    s.text(x + W - 78, top + 377, "Salva", 15, C["onPrimary"], 700, "middle")


def stats_header(s, x, y, active, period):
    s.rect(x, y, W, H, C["bg"], 36)
    status_bar(s, x, y)
    s.text(x + 20, y + 80, "Il tempo tuo", 26, weight=800)
    # Segmented button
    sx, sw = x + 20, (W - 40) / 3
    s.rect(sx, y + 98, W - 40, 40, "none", 20, stroke=C["outline"], sw=1)
    for i, lab in enumerate(["Giorno", "Settimana", "Mese"]):
        if i == active:
            r = 20 if i in (0, 2) else 0
            s.rect(sx + i * sw, y + 98, sw, 40, C["sc"], r)
            if i == 0:
                s.rect(sx + sw - 20, y + 98, 20, 40, C["sc"])
            if i == 2:
                s.rect(sx + 2 * sw, y + 98, 20, 40, C["sc"])
        s.text(sx + i * sw + sw / 2, y + 123, lab, 13, C["onSc"] if i == active else C["on"], 700 if i == active else 500, "middle")
    s.add(f'<line x1="{sx+sw}" y1="{y+98}" x2="{sx+sw}" y2="{y+138}" stroke="{C["outline"]}"/>')
    s.add(f'<line x1="{sx+2*sw}" y1="{y+98}" x2="{sx+2*sw}" y2="{y+138}" stroke="{C["outline"]}"/>')
    s.text(x + 24, y + 172, "‹", 24, C["onVar"])
    s.text(x + W / 2, y + 170, period, 15, weight=700, anchor="middle")
    s.text(x + W - 24, y + 172, "›", 24, C["outlineVar"], anchor="end")


def totals(s, x, y, rows):
    for i, (big, small, col) in enumerate(rows):
        bx = x + 16 + i * ((W - 32) / 3)
        s.rect(bx + 2, y, (W - 32) / 3 - 4, 84, C["low"], 18)
        s.circle(bx + 18, y + 20, 5, col)
        s.text(bx + 14, y + 50, big, 17, weight=800)
        s.text(bx + 14, y + 70, small, 11, C["onVar"])


def legend(s, x, y):
    s.rect(x, y - 9, 10, 10, C["secondary"], 3)
    s.text(x + 16, y, "Pausa", 11, C["onVar"])
    s.rect(x + 70, y - 9, 10, 10, C["sun"], 3)
    s.text(x + 86, y, "Con la luce del giorno", 11, C["onVar"])


def screen_week(s, x, y):
    stats_header(s, x, y, 1, "12 - 18 ottobre")
    s.rect(x + 16, y + 190, W - 32, 290, C["low"], 24)
    s.text(x + 34, y + 222, "14 h 20 min tue", 22, weight=800)
    s.text(x + 34, y + 242, "di cui 4 h 30 min con la luce del giorno", 12, C["onVar"])
    base, top = y + 430, y + 270
    for h in (0, 4, 8, 12):
        yy = base - (base - top) * h / 12
        s.add(f'<line x1="{x+34}" y1="{yy}" x2="{x+W-50}" y2="{yy}" stroke="{C["outlineVar"]}" stroke-dasharray="3 4"/>')
        s.text(x + W - 34, yy + 4, f"{h} h", 10, C["onVar"], 500, "end")
    data = [(2.5, 0.5), (3, 0.8), (0, 0), (2.2, 0.2), (1.5, 0.4), (9, 3.2), (10.5, 3.5)]
    for i, (lived, sun) in enumerate(data):
        bx = x + 42 + i * 38
        if lived == 0:
            s.circle(bx + 11, base - 4, 4, C["outlineVar"])
        else:
            hh = (base - top) * lived / 12
            sh = (base - top) * sun / 12
            s.rect(bx, base - hh, 22, hh, C["secondary"], 7)
            s.rect(bx, base - sh, 22, sh, C["sun"], 7 if sun > 0.4 else 3)
        s.text(bx + 11, base + 22, "LMMGVSD"[i], 11, C["onVar"], 700 if i == 3 else 500, "middle")
    legend(s, x + 34, y + 470)
    totals(s, x, y + 498, [("14 h 20", "tue", C["secondary"]), ("4 h 30", "di luce", C["sun"]), ("38", "notifiche fuori", C["primary"])])
    s.text(x + 20, y + 610, "Mercoledì nessuna pausa. Il conto riparte alla prossima.", 12, C["onVar"])
    nav_bar(s, x, y, 2)


def screen_day(s, x, y):
    stats_header(s, x, y, 0, "Giovedì 15 ottobre")
    s.rect(x + 16, y + 190, W - 32, 290, C["low"], 24)
    s.text(x + 34, y + 222, "2 h 10 min tue", 22, weight=800)
    s.text(x + 34, y + 242, "finora, di cui 40 min con la luce del giorno", 12, C["onVar"])
    # A vertical 24 h clock-like band: daylight behind, pauses on top
    left, right = x + 54, x + W - 40
    top, bottom = y + 266, y + 452
    def yy(h):
        return top + (bottom - top) * h / 24
    s.rect(left, yy(7.6), right - left, yy(18.7) - yy(7.6), "#F4E2C2", 10)
    s.text(right - 8, yy(7.6) + 14, "alba 07:36", 10, "#8A6A35", 600, "end")
    s.text(right - 8, yy(18.7) - 6, "tramonto 18:42", 10, "#8A6A35", 600, "end")
    for h in (0, 6, 12, 18, 24):
        s.text(x + 34, yy(h) + 4, f"{h:02d}", 10, C["onVar"], 500)
    for a, b, col, lab in [(0, 8.5, C["secondary"], "Sera (da ieri)"), (13, 14, C["tertiary"], "Pranzo"), (18.5, 18.9, C["secondary"], "Sera")]:
        s.rect(left + 12, yy(a), 120, max(6, yy(b) - yy(a)), col, 6)
        s.text(left + 142, yy(a) + max(6, yy(b) - yy(a)) / 2 + 4, lab, 11, C["on"], 600)
    s.add(f'<line x1="{left}" y1="{yy(18.9)}" x2="{right}" y2="{yy(18.9)}" stroke="{C["on"]}" stroke-width="1.5" stroke-dasharray="4 3"/>')
    s.text(right - 8, yy(18.9) + 14, "ora", 10, C["on"], 700, "end")
    totals(s, x, y + 498, [("2 h 10", "tue", C["secondary"]), ("40 min", "di luce", C["sun"]), ("5", "notifiche fuori", C["primary"])])
    nav_bar(s, x, y, 2)


def screen_month(s, x, y):
    stats_header(s, x, y, 2, "Ottobre 2026")
    s.rect(x + 16, y + 190, W - 32, 290, C["low"], 24)
    s.text(x + 34, y + 222, "61 h tue", 22, weight=800)
    s.text(x + 34, y + 242, "di cui 19 h con la luce del giorno", 12, C["onVar"])
    # Calendar with intensity
    for i, d in enumerate("LMMGVSD"):
        s.text(x + 52 + i * 40, y + 272, d, 11, C["onVar"], 600, "middle")
    import random
    rnd = random.Random(4)
    start_col = 3  # 1 October 2026 is a Thursday
    for day in range(1, 32):
        idx = start_col + day - 1
        cx, cy = x + 52 + (idx % 7) * 40, y + 300 + (idx // 7) * 34
        if day > 15:
            s.circle(cx, cy, 14, "none", stroke=C["outlineVar"], sw=1)
            s.text(cx, cy + 4, str(day), 10, C["outlineVar"], 500, "middle")
            continue
        weekend = idx % 7 >= 5
        hours = 0 if day == 14 else (rnd.uniform(7, 11) if weekend else rnd.uniform(1, 4))
        if hours == 0:
            s.circle(cx, cy, 14, "none", stroke=C["outlineVar"], sw=1)
            s.text(cx, cy + 4, str(day), 10, C["onVar"], 500, "middle")
        else:
            op = 0.25 + 0.75 * min(1, hours / 11)
            s.add(f'<circle cx="{cx}" cy="{cy}" r="14" fill="{C["secondary"]}" opacity="{op:.2f}"/>')
            s.text(cx, cy + 4, str(day), 10, "#FFFFFF" if op > 0.55 else C["onSc"], 700, "middle")
        if day == 15:
            s.circle(cx, cy, 17, "none", stroke=C["on"], sw=1.5)
    totals(s, x, y + 498, [("61 h", "tue", C["secondary"]), ("19 h", "di luce", C["sun"]), ("212", "notifiche fuori", C["primary"])])
    s.text(x + 20, y + 610, "Più è pieno il cerchio, più tempo tuo quel giorno.", 12, C["onVar"])
    nav_bar(s, x, y, 2)


def main():
    global HEDGEHOG
    HEDGEHOG = hedgehog_png()
    s = Svg()
    screens = [
        (screen_today, "Oggi"), (screen_pauses, "Pause"), (screen_editor, "Pause: modifica"),
        (screen_day, "Il tempo tuo: giorno"), (screen_week, "Il tempo tuo: settimana"), (screen_month, "Il tempo tuo: mese"),
    ]
    cols = 3
    total_w = GAP + cols * (W + GAP)
    total_h = 2 * (H + GAP + 30) + GAP
    for i, (fn, label) in enumerate(screens):
        x = GAP + (i % cols) * (W + GAP)
        y = GAP + 30 + (i // cols) * (H + GAP + 30)
        s.text(x + 4, y - 12, label, 15, C["onVar"], 700)
        s.add(f'<clipPath id="ph{i}"><rect x="{x}" y="{y}" width="{W}" height="{H}" rx="36"/></clipPath><g clip-path="url(#ph{i})">')
        fn(s, x, y)
        s.add("</g>")
        s.rect(x, y, W, H, "none", 36, stroke=C["outlineVar"], sw=1)
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{total_w}" height="{total_h}" viewBox="0 0 {total_w} {total_h}" font-family="{FONT}">\n'
        f'<!-- Generated by tools/mockups/nook2.py: edit the script, not this file -->\n'
        f'<rect width="{total_w}" height="{total_h}" fill="#E9DBCB"/>\n' + "\n".join(s.parts) + "\n</svg>\n"
    )
    OUT.write_text(svg)
    print("Wrote", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
