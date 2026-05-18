package uk.co.zlurgg.mybookshelf.core.domain.util

import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialColorGeneratorTest {

    @Test
    fun `generated color never falls below luminance floor`() {
        repeat(500) {
            val color = MaterialColorGenerator.generateMatteColor()
            val luminance = MaterialColorGenerator.wcagLuminance(color)
            assertTrue(
                "Color ${Integer.toHexString(color)} has luminance $luminance below threshold",
                luminance >= 0.05f
            )
        }
    }

    @Test
    fun `generated color is fully opaque`() {
        repeat(100) {
            val color = MaterialColorGenerator.generateMatteColor()
            val alpha = (color shr 24) and 0xFF
            assertTrue("Alpha should be 255, was $alpha", alpha == 255)
        }
    }

    @Test
    fun `wcag luminance of black is zero`() {
        val black = 0xFF000000.toInt()
        val luminance = MaterialColorGenerator.wcagLuminance(black)
        assertTrue("Black luminance should be 0", luminance == 0f)
    }

    @Test
    fun `wcag luminance of white is one`() {
        val white = 0xFFFFFFFF.toInt()
        val luminance = MaterialColorGenerator.wcagLuminance(white)
        assertTrue("White luminance should be ~1.0, was $luminance", luminance > 0.99f)
    }

    @Test
    fun `very dark color is rejected by luminance check`() {
        // DarkWood shelf background: #2B1F16 has luminance ~0.015
        val darkWood = 0xFF2B1F16.toInt()
        val luminance = MaterialColorGenerator.wcagLuminance(darkWood)
        assertTrue(
            "DarkWood-like color should be below threshold, luminance=$luminance",
            luminance < 0.05f
        )
    }
}
