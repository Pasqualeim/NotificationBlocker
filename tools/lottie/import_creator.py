"""Import a Lottie JSON exported from Lottie Creator into app/src/main/res/raw/.

Usage:
    python3 tools/lottie/import_creator.py ~/Downloads/scene_work.json --as scene_work
    python3 tools/lottie/import_creator.py ~/Downloads/scene_rest.json --as scene_rest
    python3 tools/lottie/import_creator.py FILE --check     # validate only, write nothing

What it does:
- validates what lottie-android needs: no raster images, no expressions, no text layers,
  the expected size / frame rate / loop length and the file-size budget;
- checks that the layers the app drives at runtime exist with the exact names below
  (the app changes their opacity or color from the real time of day and season);
- fixes the pitfalls listed in lottie_kit.py: "ty" first in every object, and a copy of the
  last keyframe after a HOLD keyframe (lottie-android drops the last one);
- writes the result minified to app/src/main/res/raw/<name>.json.

The exported file is the source of truth for these scenes (they are drawn in Creator, not by
a script): re-export from Creator and re-run this tool, never hand-edit the JSON in res/raw.
"""

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "app/src/main/res/raw"

WIDTH, HEIGHT = 1200, 600
FPS = 30
LOOP_SECONDS = (8, 12)
BUDGET_KB = 150

# Layer names the app drives at runtime (KeyPath(name, "**")), shared by both scenes so the
# same code handles work and rest. Opacity layers are faded in/out by the time of day.
SHARED_LAYERS = {
    "SkyDay": "window sky, daylight (opacity driven by the app)",
    "SkySunset": "window sky, golden hour / dusk (opacity driven by the app)",
    "SkyNight": "window sky, night (opacity driven by the app)",
    "Sun": "sun disc in the window (opacity driven by the app)",
    "Moon": "moon in the window (opacity driven by the app)",
    "Stars": "stars in the window (opacity driven by the app)",
    "Clouds": "clouds drifting in the window (opacity driven by the app: faint at night)",
    "CityLights": "lit windows of the city skyline (opacity driven by the app: dusk and night)",
    "TreeOutside": "tree seen through the window; its fills named 'Foliage' are recolored by season",
    "RoomShade": "plum overlay on the room, window cut out (opacity driven by the app: evening and night)",
    "LampGlow": "warm halo of the desk lamp, above RoomShade (opacity driven by the app: evening and night)",
}
# Both scenes show the same room and the same view, so they share every driven layer.
SCENE_LAYERS = {"scene_work": {}, "scene_rest": {}}


def reorder_type_first(node):
    """lottie-android ignores keys read before "ty": move it to the front, recursively."""
    if isinstance(node, dict):
        items = {k: reorder_type_first(v) for k, v in node.items()}
        if "ty" in items:
            return {"ty": items.pop("ty"), **items}
        return items
    if isinstance(node, list):
        return [reorder_type_first(v) for v in node]
    return node


def fix_trailing_hold(node, fixes):
    """After a HOLD keyframe the last keyframe is dropped by lottie-android: append a copy."""
    if isinstance(node, dict):
        k = node.get("k")
        if node.get("a") == 1 and isinstance(k, list) and len(k) >= 2 and isinstance(k[-2], dict):
            if k[-2].get("h") == 1 and "t" in k[-1] and "s" in k[-1]:
                k.append({"t": k[-1]["t"] + 1, "s": k[-1]["s"]})
                fixes.append("hold keyframe before the last one: appended a copy")
        for v in node.values():
            fix_trailing_hold(v, fixes)
    elif isinstance(node, list):
        for v in node:
            fix_trailing_hold(v, fixes)


def walk(node, visit, path="root"):
    if isinstance(node, dict):
        visit(node, path)
        for key, value in node.items():
            walk(value, visit, f"{path}.{key}")
    elif isinstance(node, list):
        for i, value in enumerate(node):
            walk(value, visit, f"{path}[{i}]")


def all_layer_names(doc):
    names = [layer.get("nm", "") for layer in doc.get("layers", [])]
    for asset in doc.get("assets", []):
        names += [layer.get("nm", "") for layer in asset.get("layers", [])]
    return names


def all_shape_names(doc):
    names = []
    walk(doc, lambda node, _: names.append(node["nm"]) if node.get("ty") in ("fl", "gf", "st", "gs") and "nm" in node else None)
    return names


def validate(doc, scene):
    errors, warnings = [], []

    w, h, fr = doc.get("w"), doc.get("h"), doc.get("fr")
    frames = doc.get("op", 0) - doc.get("ip", 0)
    if (w, h) != (WIDTH, HEIGHT):
        errors.append(f"size is {w}x{h}, expected {WIDTH}x{HEIGHT} (2:1, the SceneCard ratio)")
    if fr != FPS:
        errors.append(f"frame rate is {fr}, expected {FPS}")
    seconds = frames / fr if fr else 0
    if not LOOP_SECONDS[0] <= seconds <= LOOP_SECONDS[1]:
        warnings.append(f"loop is {seconds:.1f} s, expected {LOOP_SECONDS[0]}-{LOOP_SECONDS[1]} s")

    for asset in doc.get("assets", []):
        if "p" in asset or asset.get("e") == 1:
            errors.append(f"raster image asset '{asset.get('id')}': vectors only (no embedded PNG/JPG)")

    def visit(node, path):
        if node.get("ty") == 5:
            errors.append(f"text layer '{node.get('nm')}': not allowed (fonts are not bundled); convert to shapes")
        if "x" in node and isinstance(node["x"], str):
            errors.append(f"expression at {path}: lottie-android does not run expressions")
        if node.get("ef"):
            warnings.append(f"effects on '{node.get('nm')}': lottie-android supports few effects, check on device")
        if node.get("ty") == 2:
            errors.append(f"image layer '{node.get('nm')}': vectors only")
    walk(doc, visit)

    names = all_layer_names(doc)
    expected = {**SHARED_LAYERS, **SCENE_LAYERS.get(scene, {})}
    for name, role in expected.items():
        count = names.count(name)
        if count == 0:
            errors.append(f"missing layer '{name}' ({role})")
        elif count > 1:
            errors.append(f"layer name '{name}' used {count} times: must be unique")
    if "Foliage" not in all_shape_names(doc):
        warnings.append("no fill named 'Foliage' inside TreeOutside: the season color will not change")

    return errors, warnings


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("file", type=Path)
    parser.add_argument("--as", dest="name", choices=sorted(SCENE_LAYERS), help="raw resource name")
    parser.add_argument("--check", action="store_true", help="validate only")
    args = parser.parse_args()

    doc = json.loads(args.file.read_text(encoding="utf-8"))
    scene = args.name or args.file.stem
    errors, warnings = validate(doc, scene)

    doc = reorder_type_first(doc)
    fixes = []
    fix_trailing_hold(doc, fixes)
    out = json.dumps(doc, separators=(",", ":"), ensure_ascii=False)
    size_kb = len(out.encode("utf-8")) / 1024
    if size_kb > BUDGET_KB:
        errors.append(f"minified size {size_kb:.0f} KB > {BUDGET_KB} KB budget: simplify paths or remove layers")

    print(f"{args.file.name}: {doc.get('w')}x{doc.get('h')} @ {doc.get('fr')} fps, "
          f"{(doc.get('op', 0) - doc.get('ip', 0)) / (doc.get('fr') or 1):.1f} s, {size_kb:.0f} KB minified")
    for fix in sorted(set(fixes)):
        print(f"  fixed: {fix} (x{fixes.count(fix)})")
    for warning in warnings:
        print(f"  warning: {warning}")
    for error in errors:
        print(f"  ERROR: {error}")

    if errors:
        sys.exit(1)
    if args.check:
        print("  OK (check only, nothing written)")
        return
    if not args.name:
        sys.exit("  pass --as scene_work or --as scene_rest to write the resource")
    target = RAW / f"{args.name}.json"
    target.write_text(out, encoding="utf-8")
    print(f"  written {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
