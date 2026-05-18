package uk.co.zlurgg.mybookshelf.book.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpineEffectsTest {

    @Test
    fun `lighter color is brighter than base`() {
        val colors = calculateSpineColors(0xFF804020.toInt())
        assertTrue("Lighter red should be >= base", colors.lighter.red >= colors.base.red)
        assertTrue("Lighter green should be >= base", colors.lighter.green >= colors.base.green)
        assertTrue("Lighter blue should be >= base", colors.lighter.blue >= colors.base.blue)
    }

    @Test
    fun `darker color is dimmer than base`() {
        val colors = calculateSpineColors(0xFF804020.toInt())
        assertTrue("Darker red should be <= base", colors.darker.red <= colors.base.red)
        assertTrue("Darker green should be <= base", colors.darker.green <= colors.base.green)
        assertTrue("Darker blue should be <= base", colors.darker.blue <= colors.base.blue)
    }

    @Test
    fun `lighter color channels are clamped to 1`() {
        // Near-white color where ×1.15 would exceed 1.0
        val colors = calculateSpineColors(0xFFF0F0F0.toInt())
        assertTrue("Red should not exceed 1.0", colors.lighter.red <= 1f)
        assertTrue("Green should not exceed 1.0", colors.lighter.green <= 1f)
        assertTrue("Blue should not exceed 1.0", colors.lighter.blue <= 1f)
    }

    @Test
    fun `base color preserves alpha`() {
        val colors = calculateSpineColors(0xFF804020.toInt())
        assertEquals("Alpha should be 1.0", 1f, colors.base.alpha)
        assertEquals("Lighter alpha should be 1.0", 1f, colors.lighter.alpha)
        assertEquals("Darker alpha should be 1.0", 1f, colors.darker.alpha)
    }
}
