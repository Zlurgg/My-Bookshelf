package uk.co.zlurgg.mybookshelf.bookshelf.presenation.util

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.random.Random

fun randomReadableDarkColor(): Color {
    val hue = Random.nextFloat() * 360f
    val saturation = 0.5f + Random.nextFloat() * 0.5f  // 0.5–1.0
    val lightness = 0.2f + Random.nextFloat() * 0.2f   // 0.2–0.4 for dark tones
    return hslToColor(hue, saturation, lightness)
}

fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2 * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2 - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60 -> listOf(c, x, 0f)
        h < 120 -> listOf(x, c, 0f)
        h < 180 -> listOf(0f, c, x)
        h < 240 -> listOf(0f, x, c)
        h < 300 -> listOf(x, 0f, c)
        else -> listOf(c, 0f, x)
    }
    return Color((r1 + m), (g1 + m), (b1 + m), 1f)
}