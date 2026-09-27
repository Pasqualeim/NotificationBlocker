#!/usr/bin/env python3
"""Generates app/src/main/res/raw/end_of_shift.json ("Fine turno" Lottie animation).

Storyboard (60 fps, 240x240, 300 frames = 5 s):
  0-36    three notification cards wobble (the work noise)
  36-72   cards swipe away to the right, staggered 5 frames (~80 ms)
  56-126  a sun appears, then sets behind the horizon; a lavender night sky fades in
  128-176 a crescent moon rises from behind the horizon with a soft overshoot
  160-190 three sparkles pop in, staggered
  196-232 the moon "breathes" once (scale 100 -> 106 -> 100)
  236-300 sparkles twinkle once, ending fully visible (last frame = resting state)

Group names (card_bg, card_dot, card_line, sky_night, sun, horizon, moon, star)
are the keypaths EndOfShiftCard.kt uses to recolor the animation with MaterialTheme colors.
Default colors are the dark "Quiet Hours" palette from ui/theme/Color.kt.

Run: python3 tools/lottie/end_of_shift.py
"""

import json
import math
from pathlib import Path

import lottie_kit as kit
from lottie_kit import (EASE_IN, EASE_IN_OUT, GENTLE_OUT, OVERSHOOT_SOFT, anim, ellipse, fill, group,
                        group_tr, path, rect, sparkle, static, stroke, transform)

FPS = 60
W = H = 240
OP = 300
CX = 120
HORIZON_Y = 150

SECONDARY = "#FFC56B"
SECONDARY_CONTAINER = "#5E4000"
ON_SECONDARY_CONTAINER = "#FFE0B2"
PRIMARY = "#B8A6FF"
ON_SURFACE_VARIANT = "#C8CCDF"



def layer(name, ind, shapes, ks=None, masks=None):
    return kit.layer(name, ind, shapes, OP, ks=ks, masks=masks)


def above_horizon_mask():
    # Layer-local coords equal comp coords (identity layer transform)
    v = [[0, 0], [W, 0], [W, HORIZON_Y], [0, HORIZON_Y]]
    z = [[0, 0]] * 4
    return [{"inv": False, "mode": "a", "nm": "Horizon clip",
             "pt": static({"i": z, "o": z, "v": v, "c": True}),
             "o": static(100), "x": static(0)}]


def arc_beziers(cx, cy, r, a0, a1):
    """Cubic bezier segments (<= 90 deg) along a circle from angle a0 to a1 (radians)."""
    n = max(1, math.ceil(abs(a1 - a0) / (math.pi / 2)))
    step = (a1 - a0) / n
    k = 4 / 3 * math.tan(step / 4)
    segs = []
    for i in range(n):
        t0, t1 = a0 + i * step, a0 + (i + 1) * step
        p0 = (cx + r * math.cos(t0), cy + r * math.sin(t0))
        p1 = (cx + r * math.cos(t1), cy + r * math.sin(t1))
        c0 = (p0[0] - k * r * math.sin(t0), p0[1] + k * r * math.cos(t0))
        c1 = (p1[0] + k * r * math.sin(t1), p1[1] - k * r * math.cos(t1))
        segs.append((p0, c0, c1, p1))
    return segs


def pick_arc(cx, cy, r, p_start, p_end, keep):
    """Arc on circle (cx, cy, r) from p_start to p_end, in the direction whose midpoint satisfies keep()."""
    t0 = math.atan2(p_start[1] - cy, p_start[0] - cx)
    t1 = math.atan2(p_end[1] - cy, p_end[0] - cx)
    ccw = (t1 - t0) % (2 * math.pi)
    for sweep in (ccw, ccw - 2 * math.pi):
        tm = t0 + sweep / 2
        if keep((cx + r * math.cos(tm), cy + r * math.sin(tm))):
            return arc_beziers(cx, cy, r, t0, t0 + sweep)
    raise ValueError("no arc satisfies the predicate")


def crescent(r1=28.0, r2=25.0, dx=12.0, dy=-10.0):
    """Outer circle (0,0,r1) minus inner circle (dx,dy,r2) as a single closed bezier path."""
    d = math.hypot(dx, dy)
    a = (r1 * r1 - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(r1 * r1 - a * a)
    ux, uy = dx / d, dy / d
    pa = (a * ux - h * uy, a * uy + h * ux)
    pb = (a * ux + h * uy, a * uy - h * ux)
    outer = pick_arc(0, 0, r1, pa, pb, lambda m: math.hypot(m[0] - dx, m[1] - dy) > r2)
    inner = pick_arc(dx, dy, r2, pb, pa, lambda m: math.hypot(*m) < r1)

    segs = outer + inner
    v, i_t, o_t = [], [], []
    for idx, (p0, c0, _, _) in enumerate(segs):
        prev_c1 = segs[idx - 1][2]
        v.append([round(p0[0], 3), round(p0[1], 3)])
        o_t.append([round(c0[0] - p0[0], 3), round(c0[1] - p0[1], 3)])
        i_t.append([round(prev_c1[0] - p0[0], 3), round(prev_c1[1] - p0[1], 3)])
    return path(v, i_t, o_t)


def build():
    layers = []  # bottom -> top, reversed at the end
    ind = iter(range(1, 100))

    # Night sky tint fades in as the sun sets (an amber day tint read as muddy grey on the card)
    layers.append(layer("sky_night", next(ind), [
        group("sky_night", [ellipse(196), fill(PRIMARY, 16)], group_tr(p=(CX, 120))),
    ], ks=transform(o=anim([(100, 0, None), (136, 100, None)]))))

    # Sun: sets behind the horizon (group moves, layer mask stays put)
    layers.append(layer("sun", next(ind), [
        group("sun", [ellipse(56), fill(SECONDARY)], group_tr(
            p=anim([(80, [CX, 108], EASE_IN), (126, [CX, 190], None)], dims=2),
            o=anim([(56, 0, None), (80, 100, None)]),
        )),
    ], masks=above_horizon_mask()))

    # Moon: rises from behind the horizon with a soft overshoot, then breathes once
    layers.append(layer("moon", next(ind), [
        group("moon", [crescent(), fill(PRIMARY)], group_tr(
            p=anim([(128, [CX, 186], OVERSHOOT_SOFT), (176, [CX, 90], None)], dims=2),
            s=anim([(196, [100, 100], EASE_IN_OUT), (214, [106, 106], EASE_IN_OUT),
                    (232, [100, 100], None)], dims=2),
            r=anim([(128, -20, GENTLE_OUT), (184, 0, None)]),
        )),
    ], masks=above_horizon_mask()))

    # Horizon line
    layers.append(layer("horizon", next(ind), [
        group("horizon", [
            path([[48, HORIZON_Y], [192, HORIZON_Y]], [[0, 0], [0, 0]], [[0, 0], [0, 0]], closed=False),
            stroke(ON_SURFACE_VARIANT, 4, 60),
        ]),
    ], ks=transform(o=anim([(56, 0, None), (84, 100, None)]))))

    # Sparkles: pop in staggered, twinkle once at the end
    stars = [((64, 70), 10, 3.2, 0), ((178, 56), 8, 2.6, 6), ((172, 116), 6, 2, 12)]
    for n, ((x, y), outer, inner, delay) in enumerate(stars, start=1):
        t = 160 + delay
        tw = 236 + delay * 2
        layers.append(layer(f"star_{n}", next(ind), [
            group("star", [sparkle(outer, inner), fill(PRIMARY)]),
        ], ks=transform(
            p=(x, y),
            s=anim([(t, [0, 0], GENTLE_OUT), (t + 10, [125, 125], EASE_IN_OUT), (t + 18, [100, 100], None),
                    (tw, [100, 100], EASE_IN_OUT), (tw + 20, [80, 80], EASE_IN_OUT), (tw + 40, [100, 100], None)],
                   dims=2),
            r=anim([(t, -45, GENTLE_OUT), (t + 18, 0, None)]),
            o=anim([(t, 0, None), (t + 8, 100, None),
                    (tw, 100, EASE_IN_OUT), (tw + 20, 45, EASE_IN_OUT), (tw + 40, 100, None)]),
        )))

    # Notification cards: wobble, then swipe away one after the other (top card first)
    cards = [(72, 0, 2.5), (120, 5, -2), (168, 10, 1.5)]
    for n, (y, delay, amp) in enumerate(cards, start=1):
        s0 = 36 + delay
        layers.append(layer(f"card_{n}", next(ind), [
            group("card_line", [rect(70, 6, 3, (-6, -6)), fill(ON_SECONDARY_CONTAINER, 70)]),
            group("card_line", [rect(44, 6, 3, (-19, 6)), fill(ON_SECONDARY_CONTAINER, 45)]),
            group("card_dot", [ellipse(18, (-60, 0)), fill(SECONDARY)]),
            group("card_bg", [rect(168, 40, 12), fill(SECONDARY_CONTAINER)]),
        ], ks=transform(
            p=anim([(s0, [CX, y], EASE_IN), (s0 + 26, [CX + 190, y], None)], dims=2),
            r=anim([(0, 0, None), (6, -amp, None), (12, amp, None), (18, -amp * 0.6, None),
                    (24, amp * 0.6, None), (30, 0, None), (s0, 0, EASE_IN), (s0 + 26, 8, None)]),
            o=anim([(s0 + 8, 100, None), (s0 + 26, 0, None)]),
        )))

    return kit.composition("end_of_shift", W, H, FPS, OP, layers)


if __name__ == "__main__":
    out = Path(__file__).resolve().parents[2] / "app/src/main/res/raw/end_of_shift.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    data = build()
    kit.assert_type_first(data)
    out.write_text(json.dumps(data, separators=(",", ":")))
    print(f"{out} ({out.stat().st_size / 1024:.1f} KB)")
