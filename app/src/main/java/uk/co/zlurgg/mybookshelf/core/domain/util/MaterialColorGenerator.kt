package uk.co.zlurgg.mybookshelf.core.domain.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/**
 * Generates appropriate colors for visual elements requiring realistic material appearances.
 * Creates matte, realistic colors that simulate materials like cloth, leather, and aged paper.
 */
@Suppress("MagicNumber")
object MaterialColorGenerator {

    /**
     * Generates a matte, realistic color suitable for material surfaces.
     * Returns an ARGB color as Int that mimics real material finishes.
     *
     * Guarantees the final color has sufficient WCAG relative luminance to remain
     * visible against dark shelf backgrounds (DarkWood, DarkGreyMetal).
     */
    fun generateMatteColor(): Int {
        repeat(MAX_REGENERATION_ATTEMPTS) {
            val hue = Random.Default.nextFloat() * HUE_RANGE
            val saturation = MIN_SATURATION + Random.Default.nextFloat() * SATURATION_RANGE
            val lightness = MIN_LIGHTNESS + Random.Default.nextFloat() * LIGHTNESS_RANGE

            val baseColor = hslToArgb(hue, saturation, lightness)
            val matteColor = applyMatteFinish(baseColor)

            if (wcagLuminance(matteColor) >= MIN_LUMINANCE) {
                return matteColor
            }
        }
        return FALLBACK_COLOR
    }

    /**
     * Applies realistic matte finishing to a base color to simulate material surfaces.
     * Desaturates and ages the color to look like cloth, leather, or paper.
     */
    private fun applyMatteFinish(argbColor: Int): Int {
        val alpha = (argbColor shr ALPHA_SHIFT) and CHANNEL_MASK
        val red = (argbColor shr RED_SHIFT) and CHANNEL_MASK
        val green = (argbColor shr GREEN_SHIFT) and CHANNEL_MASK
        val blue = argbColor and CHANNEL_MASK

        val r = red / MAX_CHANNEL_F
        val g = green / MAX_CHANNEL_F
        val b = blue / MAX_CHANNEL_F

        // Calculate grayscale for desaturation (standard luminance weights)
        val gray = r * GRAY_RED_WEIGHT + g * GRAY_GREEN_WEIGHT + b * GRAY_BLUE_WEIGHT

        // Desaturate by blending with grayscale (60% original, 40% gray)
        val desaturatedR = r * ORIGINAL_BLEND + gray * GRAY_BLEND
        val desaturatedG = g * ORIGINAL_BLEND + gray * GRAY_BLEND
        val desaturatedB = b * ORIGINAL_BLEND + gray * GRAY_BLEND

        // Add subtle aged material tint and reduce brightness
        val matteR = (desaturatedR * BRIGHTNESS_RED + WARMTH_RED).coerceIn(0f, 1f)
        val matteG = (desaturatedG * BRIGHTNESS_GREEN + WARMTH_GREEN).coerceIn(0f, 1f)
        val matteB = (desaturatedB * BRIGHTNESS_BLUE + WARMTH_BLUE).coerceIn(0f, 1f)

        val finalRed = (matteR * MAX_CHANNEL_F).toInt().coerceIn(0, MAX_CHANNEL)
        val finalGreen = (matteG * MAX_CHANNEL_F).toInt().coerceIn(0, MAX_CHANNEL)
        val finalBlue = (matteB * MAX_CHANNEL_F).toInt().coerceIn(0, MAX_CHANNEL)

        return (alpha shl ALPHA_SHIFT) or (finalRed shl RED_SHIFT) or
            (finalGreen shl GREEN_SHIFT) or finalBlue
    }

    /**
     * Calculates WCAG 2.0 relative luminance using proper sRGB linearization.
     * https://www.w3.org/TR/WCAG20/#relativeluminancedef
     */
    internal fun wcagLuminance(argbColor: Int): Float {
        val r = linearize(((argbColor shr RED_SHIFT) and CHANNEL_MASK) / MAX_CHANNEL_F)
        val g = linearize(((argbColor shr GREEN_SHIFT) and CHANNEL_MASK) / MAX_CHANNEL_F)
        val b = linearize((argbColor and CHANNEL_MASK) / MAX_CHANNEL_F)
        return WCAG_RED_WEIGHT * r + WCAG_GREEN_WEIGHT * g + WCAG_BLUE_WEIGHT * b
    }

    /**
     * Applies sRGB inverse transfer function to linearize a channel value.
     */
    private fun linearize(srgb: Float): Float {
        return if (srgb <= SRGB_LINEAR_THRESHOLD) {
            srgb / SRGB_LINEAR_DIVISOR
        } else {
            ((srgb + SRGB_OFFSET) / SRGB_DIVISOR).pow(SRGB_GAMMA)
        }
    }

    private fun hslToArgb(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2 * l - 1f)) * s
        val x = c * (1f - abs((h / HSL_SECTOR_SIZE) % 2 - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            h < HSL_SECTOR_SIZE -> Triple(c, x, 0f)
            h < HSL_SECTOR_SIZE * 2 -> Triple(x, c, 0f)
            h < HSL_SECTOR_SIZE * 3 -> Triple(0f, c, x)
            h < HSL_SECTOR_SIZE * 4 -> Triple(0f, x, c)
            h < HSL_SECTOR_SIZE * 5 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val finalRed = ((r1 + m) * MAX_CHANNEL_F).toInt().coerceIn(0, MAX_CHANNEL)
        val finalGreen = ((g1 + m) * MAX_CHANNEL_F).toInt().coerceIn(0, MAX_CHANNEL)
        val finalBlue = ((b1 + m) * MAX_CHANNEL_F).toInt().coerceIn(0, MAX_CHANNEL)

        return (MAX_CHANNEL shl ALPHA_SHIFT) or (finalRed shl RED_SHIFT) or
            (finalGreen shl GREEN_SHIFT) or finalBlue
    }

    // HSL generation ranges
    private const val HUE_RANGE = 360f
    private const val MIN_SATURATION = 0.25f
    private const val SATURATION_RANGE = 0.35f
    private const val MIN_LIGHTNESS = 0.15f
    private const val LIGHTNESS_RANGE = 0.25f

    // ARGB bit shifts and masks
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val CHANNEL_MASK = 0xFF
    private const val MAX_CHANNEL = 255
    private const val MAX_CHANNEL_F = 255f

    // Grayscale weights (Rec. 601)
    private const val GRAY_RED_WEIGHT = 0.299f
    private const val GRAY_GREEN_WEIGHT = 0.587f
    private const val GRAY_BLUE_WEIGHT = 0.114f

    // Desaturation blend ratios
    private const val ORIGINAL_BLEND = 0.6f
    private const val GRAY_BLEND = 0.4f

    // Aged material tint: brightness reduction and warm offset per channel
    private const val BRIGHTNESS_RED = 0.85f
    private const val WARMTH_RED = 0.05f
    private const val BRIGHTNESS_GREEN = 0.85f
    private const val WARMTH_GREEN = 0.03f
    private const val BRIGHTNESS_BLUE = 0.80f
    private const val WARMTH_BLUE = 0.02f

    // WCAG 2.0 relative luminance coefficients
    private const val WCAG_RED_WEIGHT = 0.2126f
    private const val WCAG_GREEN_WEIGHT = 0.7152f
    private const val WCAG_BLUE_WEIGHT = 0.0722f

    // sRGB linearization constants
    private const val SRGB_LINEAR_THRESHOLD = 0.04045f
    private const val SRGB_LINEAR_DIVISOR = 12.92f
    private const val SRGB_OFFSET = 0.055f
    private const val SRGB_DIVISOR = 1.055f
    private const val SRGB_GAMMA = 2.4f

    // HSL sector size in degrees
    private const val HSL_SECTOR_SIZE = 60f

    // Luminance floor and retry config
    private const val MIN_LUMINANCE = 0.05f
    private const val MAX_REGENERATION_ATTEMPTS = 10

    // Warm medium-brown: safe against all shelf materials
    private const val FALLBACK_COLOR = 0xFF5C4033.toInt()
}
