package com.pasquale.notificationblocker.ui.zen

/**
 * The few drawing primitives the scenes need, in scene units (see [ScenePainting.W] and
 * [ScenePainting.H]). Colors are ARGB ints (alpha included). Implemented on Compose `DrawScope` for
 * the app and on AWT `Graphics2D` in unit tests, so the scene renders to PNG without a device.
 */
interface ZenPainter {
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int)

    fun verticalGradient(x: Float, y: Float, w: Float, h: Float, top: Int, bottom: Int)

    fun circle(cx: Float, cy: Float, r: Float, color: Int)

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Int)

    /** Radial gradient from [color] at the center to transparent at radius [r]. */
    fun glow(cx: Float, cy: Float, r: Float, color: Int)

    /** Filled polygon of the first [count] points of [points] (x0, y0, x1, y1, …). */
    fun polygon(points: FloatArray, count: Int, color: Int)

    /** Straight stroke with round caps. */
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int)
}

/** ARGB int color math shared by the scene and its palette. */
object ZenColor {
    private fun channel(c: Int, shift: Int) = (c shr shift) and 0xFF

    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (channel(a, shift) + (channel(b, shift) - channel(a, shift)) * t).toInt()
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** Per-channel multiply: [light] 0xFFFFFF leaves [c] unchanged, darker lights tint and dim it. */
    fun multiply(c: Int, light: Int): Int {
        fun ch(shift: Int) = channel(c, shift) * channel(light, shift) / 255
        return (c and 0xFF000000.toInt()) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** [c] with its alpha set to [alpha] (0..1). */
    fun alpha(c: Int, alpha: Float): Int = ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (c and 0xFFFFFF)
}

/** Deterministic integer hash, for "random" but stable placement (petals, stars, leaves). */
internal fun hash(a: Int, b: Int, c: Int = 0): Int {
    // Fixed arity (no vararg array): called hundreds of times per frame
    var h = 0x2F6B_4A1D
    h = step(h, a)
    h = step(h, b)
    h = step(h, c)
    return h and 0x7FFF_FFFF
}

private fun step(h: Int, v: Int): Int {
    var x = (h xor v) * 0x45D9F3B
    x = x xor (x ushr 16)
    return x
}
