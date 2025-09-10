package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import kotlin.math.abs
import kotlin.random.Random

/**
 * Generates appropriate colors for book spines.
 * Creates matte, realistic colors that simulate real book materials like cloth, leather, and aged paper.
 */
object BookColorGenerator {

    /**
     * Generates a matte, realistic color suitable for book spines.
     * Returns an ARGB color as Int that mimics real book materials.
     */
    fun generateSpineColor(): Int {
        val hue = Random.Default.nextFloat() * 360f
        val saturation = 0.25f + Random.Default.nextFloat() * 0.35f  // 0.25–0.6 for muted colors
        val lightness = 0.15f + Random.Default.nextFloat() * 0.25f   // 0.15–0.4 for realistic darkness
        
        val baseColor = hslToArgb(hue, saturation, lightness)
        return applyMatteBookFinish(baseColor)
    }
    
    /**
     * Applies realistic matte finishing to a base color to simulate book spine materials.
     * Desaturates and ages the color to look like cloth, leather, or paper.
     */
    private fun applyMatteBookFinish(argbColor: Int): Int {
        // Extract RGB components
        val alpha = (argbColor shr 24) and 0xFF
        val red = (argbColor shr 16) and 0xFF
        val green = (argbColor shr 8) and 0xFF
        val blue = argbColor and 0xFF
        
        // Convert to 0-1 range
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f
        
        // Calculate grayscale for desaturation (standard luminance weights)
        val gray = r * 0.299f + g * 0.587f + b * 0.114f
        
        // Desaturate by blending with grayscale (60% original, 40% gray)
        val desaturatedR = r * 0.6f + gray * 0.4f
        val desaturatedG = g * 0.6f + gray * 0.4f
        val desaturatedB = b * 0.6f + gray * 0.4f
        
        // Add subtle aged paper/cloth tint and reduce brightness
        val matteR = (desaturatedR * 0.85f + 0.05f).coerceIn(0f, 1f) // Add warmth
        val matteG = (desaturatedG * 0.85f + 0.03f).coerceIn(0f, 1f) 
        val matteB = (desaturatedB * 0.80f + 0.02f).coerceIn(0f, 1f) // Reduce blue for aged look
        
        // Convert back to 0-255 range
        val finalRed = (matteR * 255).toInt().coerceIn(0, 255)
        val finalGreen = (matteG * 255).toInt().coerceIn(0, 255)
        val finalBlue = (matteB * 255).toInt().coerceIn(0, 255)
        
        return (alpha shl 24) or (finalRed shl 16) or (finalGreen shl 8) or finalBlue
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