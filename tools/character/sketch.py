#!/usr/bin/env python3
"""Draft sheets for the character study (docs/CHARACTER.md). Nothing here ships in the app.

Writes, in docs/character/:
  concept_a_angolo.png        the corner alone, furnished by your breaks (start, ~1 month, ~3 months)
  concept_b_chai.png          Chai the mug: break by day, break in the evening, work hours; light and dark
  concept_c_chai_angolo.png   Chai living in the corner: the three moments and the three furnishing levels
and docs/mockups/character_home.svg (Home and the stats screen with concept C, light theme).

Pixel art drawn at 1x on small canvases, then scaled up with nearest neighbour. The sprout grids are
read from SproutArt.kt, so the plant in the corner is the one already in the app (face removed: in
the corner Chai is the only one with a face). Needs Pillow.
"""
import base64
import io
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/character"
SPROUT_KT = ROOT / "app/src/main/java/com/pasquale/notificationblocker/ui/sprout/SproutArt.kt"

# Palettes: theme colors from Color.kt, sprout colors from ZenPalette.kt, a few draft-only tones.
LIGHT = {
    "bg": "#F8F2E9", "ink": "#3A2B22", "ink2": "#65503F",
    "wall": "#F2E8DC", "wall2": "#EEE2D4", "floor": "#E3D2BF", "floor2": "#D6C2AC",
    "sky_day": "#DCE5EF", "sky_eve": "#F0C9A8", "sky_eve2": "#E9A08A", "frame": "#8C7565",
    "wood": "#9A6B4C", "wood2": "#7A5238", "rug": "#EBCDBC", "rug2": "#D9A58C",
    "book1": "#4C6682", "book2": "#4B6A55", "book3": "#C47A58", "glow": "#FBE3C4",
    "lamp": "#F4D9A8", "steam": "#B9A796",
    # Chai
    "o": "#3A2B22", "s": "#F8EFE3", "S": "#EADBC8", "T": "#C47A58", "t": "#9A4F34",
    "b": "#9A6B4C", "e": "#3A2B22", "c": "#E9A08A", "w": "#FFFFFF",
}
DARK = {
    "bg": "#1B1714", "ink": "#F2E7DD", "ink2": "#D6C5B8",
    "wall": "#2A2420", "wall2": "#241E1B", "floor": "#352E29", "floor2": "#403833",
    "sky_day": "#4C6682", "sky_eve": "#8E5A44", "sky_eve2": "#B86A4C", "frame": "#A0907F",
    "wood": "#6E4E38", "wood2": "#4F3727", "rug": "#5A3E33", "rug2": "#74503F",
    "book1": "#6F8BA8", "book2": "#5E8569", "book3": "#B86A4C", "glow": "#4A3A2C",
    "lamp": "#E8C58A", "steam": "#8C7B6D",
    "o": "#120E0B", "s": "#F2E7DD", "S": "#D9C8B4", "T": "#E8B093", "t": "#B86A4C",
    "b": "#8E6446", "e": "#120E0B", "c": "#EBA894", "w": "#FFFFFF",
}
SPROUT_LIGHT = {
    "o": "#3A2B22", "z": "#65503F", "t": "#9A4F34", "T": "#C47A58", "g": "#4B6A55", "G": "#7FA488",
    "h": "#A9C7AE", "k": "#3A5544", "S": "#C9A27A", "s": "#E3C9A4", "b": "#7A5238", "r": "#D9765A",
}
SPROUT_DARK = {
    "o": "#120E0B", "z": "#D6C5B8", "t": "#B86A4C", "T": "#E8B093", "g": "#5E8569", "G": "#8DB298",
    "h": "#BBD6C1", "k": "#8DB298", "S": "#D3AE86", "s": "#EBD3B0", "b": "#8E6446", "r": "#E0866A",
}

# --- Chai, the mug -----------------------------------------------------------------------------
# 24x24. '.' transparent; letters are keys of LIGHT/DARK. Face, steam and liquid are layered on top.
MUG = [
    "........................",
    "........................",
    "........................",
    "........................",
    "........................",
    "........................",
    "........................",
    "........................",
    "........................",
    "....ooooooooooooooo.....",
    "....obbbbbbbbbbbbbo.....",
    "....osssssssssssssoooo..",
    "....osssssssssssssoTTo..",
    "....osssssssssssssoo.o..",
    "....osssssssssssssoo.o..",
    "....osssssssssssssooTo..",
    "....osssssssssssssoooo..",
    "....oSsssssssssssSo.....",
    "....oTTTTTTTTTTTTTo.....",
    "....otTTTTTTTTTTTto.....",
    "....oSsssssssssssSo.....",
    ".....ooooooooooooo......",
    "..oooooooooooooooooooo..",
    "........................",
]
assert all(len(r) == 24 for r in MUG), [len(r) for r in MUG]

# Faces as (x, y, key). Body interior spans x 5..17, y 11..17.
FACE_DAY = [(8, 13, "e"), (8, 14, "e"), (14, 13, "e"), (14, 14, "e"), (9, 13, "w"), (15, 13, "w"),
            (7, 15, "c"), (15, 15, "c"), (10, 16, "e"), (12, 16, "e"), (11, 17, "e")]
# Content, eyes as little arches: happy, not asleep
FACE_EVENING = [(7, 14, "e"), (8, 13, "e"), (9, 14, "e"), (13, 14, "e"), (14, 13, "e"), (15, 14, "e"),
                (7, 15, "c"), (8, 15, "c"), (14, 15, "c"), (15, 15, "c"), (10, 16, "e"), (11, 17, "e"), (12, 16, "e")]
# Work hours: awake and calm, looking out of the window (up right); no cheeks, a small smile
FACE_WORK = [(9, 13, "e"), (15, 13, "e"), (10, 16, "e"), (12, 16, "e"), (11, 17, "e")]

STEAM_DAY = [(9, 3), (8, 4), (8, 5), (9, 6), (9, 7), (8, 8), (14, 2), (13, 3), (13, 4), (14, 5), (14, 6), (13, 7)]
STEAM_EVENING = STEAM_DAY + [(11, 0), (11, 1), (12, 2), (12, 3), (11, 4), (11, 5), (11, 6)]


def chai(moment, pal):
    """Chai as a 24x24 RGBA image. moment: 'day' | 'evening' | 'work'."""
    img = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(MUG):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = hex_rgba(pal[ch])
    if moment == "work":
        # Not drunk yet: the cup is full but cool, no steam
        for x in range(5, 18):
            px[x, 10] = hex_rgba(pal["S"])
    face = {"day": FACE_DAY, "evening": FACE_EVENING, "work": FACE_WORK}[moment]
    for x, y, k in face:
        px[x, y] = hex_rgba(pal[k])
    steam = {"day": STEAM_DAY, "evening": STEAM_EVENING, "work": []}[moment]
    for x, y in steam:
        px[x, y] = hex_rgba(pal["steam"])
    return img


# --- The sprout, from the app ------------------------------------------------------------------
def sprout_grids():
    """The awake grid of each stage from SproutArt.kt, in stage order (seed, sprout, plant, sapling, tree)."""
    text = SPROUT_KT.read_text()
    rows = re.findall(r'"([.\w]{24})"', text)
    grids = [rows[i:i + 24] for i in range(0, len(rows), 24)]
    return grids[0::2]


def sprout(stage, dark):
    """The sprout without its face: in the corner only Chai has one."""
    pal = SPROUT_DARK if dark else SPROUT_LIGHT
    grid = [list(r) for r in sprout_grids()[stage]]
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch in "ewc":
                # Take the colour on the left (or above): the face blends into the leaf or the seed
                left = next((row[i] for i in range(x - 1, -1, -1) if row[i] not in "ewco."), None)
                row[x] = left or "S"
    img = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != "." and ch in pal:
                px[x, y] = hex_rgba(pal[ch])
    return img


# --- The corner ---------------------------------------------------------------------------------
W, H = 96, 56
FLOOR_Y = 44


def room(level, moment, pal, dark, with_chai):
    """The nook: level 0 (start), 1 (~1 month), 2 (~3 months). moment: 'day' | 'evening' | 'work'."""
    img = Image.new("RGBA", (W, H), hex_rgba(pal["wall"]))
    d = ImageDraw.Draw(img)
    # Wall, skirting, floor
    d.rectangle([0, FLOOR_Y - 1, W, FLOOR_Y - 1], fill=pal["wall2"])
    d.rectangle([0, FLOOR_Y, W, H], fill=pal["floor"])
    for x in range(0, W, 12):
        d.line([x, FLOOR_Y, x - 6, H], fill=pal["floor2"])
    # Window: the real light of the moment
    sky = pal["sky_eve"] if moment == "evening" else pal["sky_day"]
    d.rectangle([56, 6, 82, 30], fill=pal["frame"])
    d.rectangle([58, 8, 80, 28], fill=sky)
    if moment == "evening":
        d.rectangle([58, 22, 80, 28], fill=pal["sky_eve2"])
        d.ellipse([64, 20, 72, 28], fill=pal["lamp"])  # setting sun, half hidden
        d.rectangle([58, 25, 80, 28], fill=pal["sky_eve2"])
    else:
        d.ellipse([71, 11, 76, 16], fill="#FFF6E0" if not dark else "#E8D9B0")
    d.line([69, 8, 69, 28], fill=pal["frame"])
    d.line([58, 18, 80, 18], fill=pal["frame"])
    d.rectangle([54, 30, 84, 31], fill=pal["wood"])  # sill

    if level >= 1:
        # Rug, a picture, a pile of books
        d.rectangle([8, 49, 60, 53], fill=pal["rug"])
        d.rectangle([10, 50, 58, 52], outline=pal["rug2"])
        d.rectangle([40, 2, 52, 12], fill=pal["frame"])
        d.rectangle([41, 3, 51, 11], fill=pal["sky_day"])
        d.polygon([(41, 11), (44, 6), (47, 9), (51, 5), (51, 11)], fill=pal["book2"])
        for i, c in enumerate([pal["book1"], pal["book3"], pal["book2"]]):
            d.rectangle([64 + (i % 2), 46 - 3 * i, 74 + (i % 2), 48 - 3 * i], fill=c)
    if level >= 2:
        # A shelf with records, a floor cushion, a lamp
        d.rectangle([4, 12, 34, 13], fill=pal["wood2"])
        for i, c in enumerate([pal["book3"], pal["book1"], pal["ink2"], pal["book2"], pal["book3"]]):
            d.rectangle([6 + 3 * i, 4 + (i % 2), 7 + 3 * i, 11], fill=c)
        d.ellipse([22, 5, 28, 11], fill=pal["ink"])  # a record on display
        d.point((25, 8), fill=pal["T"])
        d.rectangle([29, 8, 33, 11], fill=pal["T"])  # small pot
        d.point([(30, 7), (31, 6), (32, 7)], fill=pal["book2"])
        d.rounded_rectangle([66, 49, 86, 54], radius=2, fill=pal["book2"])
        lamp_on = moment == "evening"
        if lamp_on:
            # Warm light, dithered so it reads as light and not as a shape
            glow = Image.new("L", (W, H), 0)
            ImageDraw.Draw(glow).ellipse([76, 4, 104, 40], fill=255)
            gp, ip = glow.load(), img.load()
            for gy in range(H):
                for gx in range(W):
                    if gp[gx, gy] and (gx + gy) % 2 == 0 and gy < FLOOR_Y - 1:
                        ip[gx, gy] = hex_rgba(pal["glow"])
        d.line([90, 18, 90, FLOOR_Y + 2], fill=pal["ink2"])
        d.rectangle([87, FLOOR_Y + 2, 93, FLOOR_Y + 3], fill=pal["ink2"])
        d.polygon([(85, 18), (95, 18), (93, 12), (87, 12)], fill=pal["lamp"] if lamp_on else pal["frame"])

    # The table with the sprout (and Chai)
    d.rectangle([4, 38, 50, 39], fill=pal["wood"])
    d.rectangle([6, 40, 7, FLOOR_Y + 4], fill=pal["wood2"])
    d.rectangle([47, 40, 48, FLOOR_Y + 4], fill=pal["wood2"])
    stage = [0, 2, 3][level]
    img.alpha_composite(sprout(stage, dark), (26, 14))
    if with_chai:
        img.alpha_composite(chai(moment, pal), (4, 15))
    return img


# --- Sheets -------------------------------------------------------------------------------------
def hex_rgba(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def up(img, k):
    return img.resize((img.width * k, img.height * k), Image.NEAREST)


def sheet(title, cells, cols, cell_w, cell_h, pal):
    """A grid of (label, image) cells on the page background, with a title."""
    font_t = ImageFont.load_default(size=28)
    font = ImageFont.load_default(size=20)
    pad, label_h, title_h = 24, 34, 56
    rows = (len(cells) + cols - 1) // cols
    sw = pad + cols * (cell_w + pad)
    sh = title_h + rows * (cell_h + label_h + pad) + pad
    page = Image.new("RGBA", (sw, sh), hex_rgba(pal["bg"]))
    d = ImageDraw.Draw(page)
    d.text((pad, 16), title, fill=pal["ink"], font=font_t)
    for i, (label, img) in enumerate(cells):
        cx = pad + (i % cols) * (cell_w + pad)
        cy = title_h + (i // cols) * (cell_h + label_h + pad)
        page.alpha_composite(img, (cx + (cell_w - img.width) // 2, cy))
        d.text((cx, cy + cell_h + 6), label, fill=pal["ink2"], font=font)
    return page


def framed(img, pal, k):
    """A sprite on a card, like it would sit on Home."""
    card = Image.new("RGBA", (img.width + 8, img.height + 8), hex_rgba(pal["wall"]))
    card.alpha_composite(img, (4, 4))
    return up(card, k)


# --- Mock-up: Home and stats with concept C -----------------------------------------------------
def png_data(img):
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def mockup():
    """Two light-theme screens side by side: Home with the corner, and the stats it opens."""
    corner = png_data(up(room(1, "evening", LIGHT, False, True), 4))
    chai_small = png_data(up(chai("evening", LIGHT), 4))
    week = [("L", 5.5), ("M", 4), ("M", 0), ("G", 6), ("V", 3.5), ("S", 9), ("D", 10)]
    bars = []
    for i, (day, h) in enumerate(week):
        x = 452 + 44 + i * 46
        if h:
            bh = h * 12
            bars.append(f'<rect x="{x}" y="{620 - bh}" width="22" height="{bh}" rx="6" fill="#4B6A55"/>')
        else:
            # A day with no break: a neutral dot, never red, never an empty outline
            bars.append(f'<circle cx="{x + 11}" cy="614" r="4" fill="#DDCDBD"/>')
        bars.append(f'<text x="{x + 11}" y="644" font-size="13" fill="#65503F" text-anchor="middle">{day}</text>')
    tiles = [("14 h 20 min", "tuoi questa settimana"), ("4", "sere libere su 7"),
             ("6 h 10 min", "di luce vissuta in pausa"), ("38", "notifiche di lavoro fuori")]
    tile_svg = []
    for i, (big, small) in enumerate(tiles):
        x, y = 452 + 16 + (i % 2) * 194, 220 + (i // 2) * 112
        tile_svg.append(
            f'<rect x="{x}" y="{y}" width="182" height="100" rx="20" fill="#F2E8DC"/>'
            f'<text x="{x + 16}" y="{y + 42}" font-size="24" font-weight="700" fill="#3A2B22">{big}</text>'
            f'<text x="{x + 16}" y="{y + 72}" font-size="13" fill="#65503F">{small}</text>')
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="864" height="892" viewBox="0 0 864 892" font-family="Manrope, 'Helvetica Neue', Arial, sans-serif">
  <!-- Bozza generata da tools/character/sketch.py: Home e statistiche col concept C (docs/CHARACTER.md) -->
  <rect width="864" height="892" fill="#E9DBCB"/>
  <g>
    <rect x="0" y="0" width="412" height="892" rx="40" fill="#F8F2E9"/>
    <text x="28" y="34" font-size="15" font-weight="600" fill="#3A2B22">21:10</text>
    <text x="24" y="86" font-size="26" font-weight="700" fill="#3A2B22">Nook</text>
    <rect x="16" y="108" width="380" height="96" rx="24" fill="#EBCDBC"/>
    <text x="36" y="148" font-size="20" font-weight="700" fill="#3B1E11">In pausa fino alle 07:00</text>
    <text x="36" y="176" font-size="14" fill="#3B1E11">Il lavoro può aspettare</text>
    <rect x="318" y="134" width="56" height="32" rx="16" fill="#9A4F34"/>
    <circle cx="358" cy="150" r="12" fill="#FFFFFF"/>
    <rect x="16" y="220" width="380" height="222" rx="28" fill="#F2E8DC"/>
    <image x="16" y="220" width="380" height="222" href="{corner}" preserveAspectRatio="xMidYMid slice" style="image-rendering:pixelated" clip-path="inset(0 round 28px)"/>
    <text x="24" y="474" font-size="14" fill="#3A2B22">Chai ha acceso la lampada. 12 notifiche di lavoro</text>
    <text x="24" y="494" font-size="14" fill="#3A2B22">sono rimaste fuori.</text>
    <text x="24" y="516" font-size="13" fill="#9A4F34" font-weight="600">Il tempo tuo  ›</text>
    <rect x="16" y="540" width="380" height="200" rx="24" fill="#F6EEE4"/>
    <text x="36" y="578" font-size="14" fill="#65503F">Orari della pausa (come oggi)</text>
    <text x="36" y="620" font-size="22" font-weight="700" fill="#3A2B22">22:00  →  07:00</text>
    <rect x="36" y="650" width="340" height="10" rx="5" fill="#E9DBCB"/>
    <rect x="296" y="650" width="80" height="10" rx="5" fill="#9A4F34"/>
    <rect x="16" y="780" width="380" height="56" rx="28" fill="#9A4F34"/>
    <text x="206" y="814" font-size="16" font-weight="600" fill="#FFFFFF" text-anchor="middle">Scegli le app di lavoro</text>
  </g>
  <g>
    <rect x="452" y="0" width="412" height="892" rx="40" fill="#F8F2E9"/>
    <text x="480" y="34" font-size="15" font-weight="600" fill="#3A2B22">21:10</text>
    <text x="476" y="88" font-size="22" fill="#3A2B22">←</text>
    <text x="476" y="150" font-size="28" font-weight="700" fill="#3A2B22">Il tempo tuo</text>
    <text x="476" y="178" font-size="14" fill="#65503F">Questa settimana</text>
    <image x="752" y="96" width="96" height="96" href="{chai_small}" style="image-rendering:pixelated"/>
    {''.join(tile_svg)}
    <rect x="468" y="460" width="380" height="210" rx="24" fill="#F2E8DC"/>
    <text x="488" y="494" font-size="14" fill="#65503F">Ore di pausa, giorno per giorno</text>
    {''.join(bars)}
    <text x="476" y="710" font-size="13" fill="#65503F">Mercoledì nessuna pausa. Il conto riparte alla prossima.</text>
  </g>
</svg>
"""
    (ROOT / "docs/mockups/character_home.svg").write_text(svg)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    moments = [("day", "pausa, di giorno"), ("evening", "pausa, di sera"), ("work", "al lavoro")]

    cells = []
    for pal, theme in [(LIGHT, "chiaro"), (DARK, "scuro")]:
        cells += [(f"{label} ({theme})", framed(chai(m, pal), pal, 10)) for m, label in moments]
    sheet("B. Chai, la tazza", cells, 3, 320, 320, LIGHT).save(OUT / "concept_b_chai.png")

    levels = [(0, "inizio"), (1, "dopo ~1 mese"), (2, "dopo ~3 mesi")]
    cells = [(f"{label}, sera", up(room(lv, "evening", LIGHT, False, False), 4)) for lv, label in levels]
    cells.append(("~3 mesi, sera (scuro)", up(room(2, "evening", DARK, True, False), 4)))
    sheet("A. L'angolo, arredato dalle tue pause", cells, 2, W * 4, H * 4, LIGHT).save(OUT / "concept_a_angolo.png")

    cells = [(f"~1 mese, {label}", up(room(1, m, LIGHT, False, True), 4)) for m, label in moments]
    cells += [(f"{label}, sera", up(room(lv, "evening", LIGHT, False, True), 4)) for lv, label in [levels[0], levels[2]]]
    cells.append(("~3 mesi, sera (scuro)", up(room(2, "evening", DARK, True, True), 4)))
    sheet("C. Chai nell'angolo", cells, 3, W * 4, H * 4, LIGHT).save(OUT / "concept_c_chai_angolo.png")
    mockup()
    print("Wrote", ", ".join(p.name for p in sorted(OUT.glob("*.png"))))


if __name__ == "__main__":
    main()
