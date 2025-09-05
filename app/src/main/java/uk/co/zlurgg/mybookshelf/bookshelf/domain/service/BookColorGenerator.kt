package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import kotlin.math.abs
import kotlin.random.Random

/**
 * Generates appropriate colors for book spines.
 * Creates dark, readable colors that work well for book visualization.
 */
object BookColorGenerator {

    /**
     * Generates a random dark color suitable for book spines.
     * Returns an ARGB color as Int.
     */
    fun generateSpineColor(): Int {
        val hue = Random.Default.nextFloat() * 360f
        val saturation = 0.5f + Random.Default.nextFloat() * 0.5f  // 0.5–1.0 for vibrant colors
        val lightness = 0.2f + Random.Default.nextFloat() * 0.2f   // 0.2–0.4 for dark tones
        return hslToArgb(hue, saturation, lightness)
    }

    /**
     * Converts HSL color values to ARGB integer.
     */
    private fun hslToArgb(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2 * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2 - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val alpha = 255
        val red = ((r1 + m) * 255).toInt().coerceIn(0, 255)
        val green = ((g1 + m) * 255).toInt().coerceIn(0, 255)
        val blue = ((b1 + m) * 255).toInt().coerceIn(0, 255)

        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}