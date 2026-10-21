#!/usr/bin/env python3
"""Google Play feature graphic (1024 x 500, 24-bit PNG without alpha), one per language.

Background: a frame of the off-work scene, rendered by the unit tests
(`./gradlew testDebugUnitTest --tests '*ZenSceneTest*'` writes app/build/zen-previews/).
On the left, the brand terracotta fading into the scene, the padlock from the launcher icon, the name and a line.
Type as on the site (Fraunces 600 for the name, Inter for the line), read from the site repo's self-hosted fonts.

Usage: python3 tools/store/feature_graphic.py [path/to/nook-site]   (default: ../nook-site next to this repo)
Writes docs/store/feature-graphic-{it,en}.png.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/icon"))
import launcher_icon as icon  # noqa: E402

W, H = 1024, 500
SCENE = ROOT / "app/build/zen-previews/nature_winter_1700_golden_hour.png"
TERRACOTTA = (164, 90, 61)
CREAM = (248, 239, 227)
TEXT = {"it": "Il tempo che ti torna", "en": "The time you get back"}


def font(path, size, **axes):
    f = ImageFont.truetype(str(path), size)
    if axes:
        names = [a["name"].decode() for a in f.get_variation_axes()]
        f.set_variation_by_axes([axes.get(n, a["default"]) for n, a in zip(names, f.get_variation_axes())])
    return f


def padlock(height):
    """The launcher glyph in cream on transparent, the glyph [height] px tall, centered like the icon."""
    ss = 4
    mx, my, bx, by = icon.glyph_center()
    icon.DX, icon.DY = 54 - mx, 54 - (my + by) / 2
    big = round(height * ss * 108 / 58)  # the glyph spans 58 of the 108 viewport units
    m = icon.mask(big, icon.DX, icon.DY)
    img = Image.new("RGBA", (big, big), CREAM + (0,))
    img.putalpha(m)
    img = img.crop(m.getbbox())
    return img.resize((round(img.width / ss), round(img.height / ss)), Image.LANCZOS)


def main(site):
    fonts = Path(site) / "public/assets/fonts"
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

    m = padlock(140)
    for lang, line in TEXT.items():
        img = base.copy()
        img.alpha_composite(m, (76, 104))
        d = ImageDraw.Draw(img)
        d.text((70, 268), "Unclock", font=font(fonts / "fraunces-normal.woff2", 104, Weight=600, **{"Optical Size": 144}), fill=CREAM)
        d.text((76, 392), line, font=font(fonts / "inter-normal.woff2", 36, Weight=600), fill=CREAM)
        out = ROOT / f"docs/store/feature-graphic-{lang}.png"
        img.convert("RGB").save(out, optimize=True)
        print(out)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ROOT.parent / "nook-site")
