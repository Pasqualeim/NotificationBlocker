#!/usr/bin/env python3
"""Google Play feature graphic (1024 x 500, 24-bit PNG without alpha), one per language.

Background: a frame of the off-work scene, rendered by the unit tests
(`./gradlew testDebugUnitTest --tests '*ZenSceneTest*'` writes app/build/zen-previews/).
On the left, the brand terracotta fading into the scene, the mug from the launcher icon, the name and a line.

Usage: python3 tools/store/feature_graphic.py path/to/Manrope[wght].ttf
(Manrope: https://github.com/google/fonts/tree/main/ofl/manrope). Writes docs/store/feature-graphic-{it,en}.png.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/icon"))
import launcher_icon as icon  # noqa: E402

W, H = 1024, 500
SCENE = ROOT / "app/build/zen-previews/nature_autumn_1700_golden_hour.png"
TERRACOTTA = (164, 90, 61)
CREAM = (248, 239, 227)
TEXT = {"it": "Il lavoro può aspettare", "en": "Work can wait"}


def font(path, size, weight):
    f = ImageFont.truetype(path, size)
    f.set_variation_by_name(weight)
    return f


def mug(size):
    """The launcher glyph in cream on transparent, [size] px, centered like the icon."""
    ss = 4
    big = size * ss
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    mx, my, bx, by = icon.glyph_center()
    cx, cy = mx, (my + by) / 2
    scale = big / 60  # the glyph spans ~42 units: leave a margin
    for shape in icon.SHAPES.values():
        pts = [((x - cx) * scale + big / 2, (y - cy) * scale + big / 2) for x, y in icon.flatten(shape, 1)]
        d.polygon(pts, fill=CREAM + (255,))
    return img.resize((size, size), Image.LANCZOS)


def main(font_path):
    scene = Image.open(SCENE).convert("RGB")
    scene = scene.resize((W, int(scene.height * W / scene.width)), Image.LANCZOS)
    top = (scene.height - H) // 2
    base = scene.crop((0, top, W, top + H))

    # Terracotta panel on the left, fading into the scene
    overlay = Image.new("RGBA", (W, H))
    od = ImageDraw.Draw(overlay)
    solid, fade = 470, 700
    for x in range(W):
        a = 1.0 if x < solid else max(0.0, 1 - (x - solid) / (fade - solid))
        od.line([(x, 0), (x, H)], fill=TERRACOTTA + (int(255 * a ** 1.4),))
    base = Image.alpha_composite(base.convert("RGBA"), overlay)

    m = mug(150)
    for lang, line in TEXT.items():
        img = base.copy()
        img.alpha_composite(m, (64, 100))
        d = ImageDraw.Draw(img)
        d.text((72, 270), "Nook", font=font(font_path, 104, "ExtraBold"), fill=CREAM)
        d.text((76, 392), line, font=font(font_path, 38, "SemiBold"), fill=CREAM)
        out = ROOT / f"docs/store/feature-graphic-{lang}.png"
        img.convert("RGB").save(out, optimize=True)
        print(out)


if __name__ == "__main__":
    main(sys.argv[1])
