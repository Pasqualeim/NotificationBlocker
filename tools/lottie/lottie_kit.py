"""Tiny helpers to write Lottie JSON by hand (shared by the generators in this folder).

Rules learned the hard way with lottie-android:
- In every object that has a "ty" key, "ty" must be the FIRST key: the parser skips
  whatever it reads before the type (group transforms were silently dropped).
- Dynamic colors: fills answer to LottieProperty.COLOR, strokes to LottieProperty.STROKE_COLOR.
  Give fills/strokes a role name (nm) and target it with KeyPath("**", "<role>").
- The last keyframe of a property is dropped (its value only closes the previous one), so a
  HOLD right before it would never jump: anim() appends a copy to keep it.
"""

# Easing (x1, y1, x2, y2)
EASE_IN_OUT = (0.42, 0, 0.58, 1)
EASE_IN = (0.55, 0, 0.9, 0.6)
EASE_OUT = (0.1, 0.4, 0.45, 1)
GENTLE_OUT = (0.22, 1, 0.36, 1)
OVERSHOOT_SOFT = (0.34, 1.56, 0.64, 1)
HOLD = "hold"


def rgba(hex_color):
    h = hex_color.lstrip("#")
    return [round(int(h[i:i + 2], 16) / 255, 4) for i in (0, 2, 4)] + [1]


def static(value):
    return {"a": 0, "k": value}


def anim(keys, dims=1):
    """keys: list of (frame, value, easing-to-next or None). HOLD jumps to the next value."""
    out = []
    for i, (t, v, ease) in enumerate(keys):
        s = v if isinstance(v, list) else [v]
        kf = {"t": t, "s": s}
        if i < len(keys) - 1:
            if ease == HOLD:
                kf["h"] = 1
            else:
                e = ease or EASE_IN_OUT
                kf["o"] = {"x": [e[0]] * dims, "y": [e[1]] * dims}
                kf["i"] = {"x": [e[2]] * dims, "y": [e[3]] * dims}
        out.append(kf)
    if len(keys) >= 2 and keys[-2][2] == HOLD:
        # lottie-android drops the last keyframe and reads its value as the previous keyframe's end,
        # but a hold keyframe ends on its own value: without this copy the jump never happens
        out.append({"t": out[-1]["t"] + 1, "s": out[-1]["s"]})
    return {"a": 1, "k": out}


def anim2(keys):
    return anim(keys, dims=2)


def transform(p=(0, 0), a=(0, 0), s=None, r=None, o=None):
    return {
        "p": p if isinstance(p, dict) else static(list(p)),
        "a": static(list(a)),
        "s": s or static([100, 100]),
        "r": r or static(0),
        "o": o or static(100),
    }


def group_tr(p=(0, 0), a=None, s=None, r=None, o=None):
    """Group transform. With `a` (pivot) and a static `p`, pass the same point to both."""
    return {"ty": "tr", **transform(p=p, a=a if a is not None else (0, 0), s=s, r=r, o=o),
            "sk": static(0), "sa": static(0)}


def fill(color, opacity=100, name="Fill"):
    return {"ty": "fl", "nm": name, "c": static(rgba(color)), "o": static(opacity), "r": 1}


def stroke(color, width, opacity=100, name="Stroke"):
    return {"ty": "st", "nm": name, "c": static(rgba(color)), "o": static(opacity),
            "w": static(width), "lc": 2, "lj": 2}


def rect(w, h, r, p=(0, 0)):
    return {"ty": "rc", "nm": "Rect", "p": static(list(p)), "s": static([w, h]), "r": static(r)}


def ellipse(d, p=(0, 0), h=None):
    return {"ty": "el", "nm": "Ellipse", "p": static(list(p)), "s": static([d, h if h is not None else d])}


def sparkle(outer, inner):
    return {"ty": "sr", "nm": "Sparkle", "sy": 1, "pt": static(4), "p": static([0, 0]),
            "r": static(0), "or": static(outer), "os": static(0), "ir": static(inner), "is": static(0)}


def path(vertices, in_t, out_t, closed=True):
    return {"ty": "sh", "nm": "Path",
            "ks": static({"i": in_t, "o": out_t, "v": vertices, "c": closed})}


def smooth_path(points, closed=True):
    """points: list of (vertex, in_tangent, out_tangent), tangents relative to the vertex."""
    return path([list(v) for v, _, _ in points], [list(i) for _, i, _ in points],
                [list(o) for _, _, o in points], closed)


def polygon(vertices, closed=True):
    z = [[0, 0]] * len(vertices)
    return path([list(v) for v in vertices], z, z, closed)


def group(name, items, tr=None):
    return {"ty": "gr", "nm": name, "it": items + [tr or group_tr()]}


def layer(name, ind, shapes, op, ks=None, masks=None):
    lyr = {"ddd": 0, "ind": ind, "ty": 4, "nm": name, "sr": 1, "ks": ks or transform(),
           "ao": 0, "shapes": shapes, "ip": 0, "op": op, "st": 0, "bm": 0}
    if masks:
        lyr["hasMask"] = True
        lyr["masksProperties"] = masks
    return lyr


def composition(name, w, h, fps, op, layers_bottom_to_top):
    return {"v": "5.7.0", "fr": fps, "ip": 0, "op": op, "w": w, "h": h, "nm": name, "ddd": 0,
            "assets": [], "layers": list(reversed(layers_bottom_to_top))}


def assert_type_first(node):
    """Guards the lottie-android "ty first" rule on the whole tree."""
    if isinstance(node, dict):
        if isinstance(node.get("ty"), str):
            assert next(iter(node)) == "ty", f"'ty' is not the first key in {list(node)[:4]}"
        for v in node.values():
            assert_type_first(v)
    elif isinstance(node, list):
        for v in node:
            assert_type_first(v)
