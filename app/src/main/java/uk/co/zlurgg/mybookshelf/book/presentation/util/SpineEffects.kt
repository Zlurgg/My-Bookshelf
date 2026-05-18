package uk.co.zlurgg.mybookshelf.book.presentation.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared 3D spine effect data and composables used by BookVertical, BookLeaning, and BookHorizontal.
 */

@Immutable
data class SpineColors(
    val base: Color,
    val lighter: Color,
    val darker: Color
)

fun calculateSpineColors(spineColor: Int): SpineColors {
    val base = Color(spineColor)
    val lighter = base.copy(
        red = (base.red * LIGHTER_MULTIPLIER).coerceAtMost(1f),
        green = (base.green * LIGHTER_MULTIPLIER).coerceAtMost(1f),
        blue = (base.blue * LIGHTER_MULTIPLIER).coerceAtMost(1f)
    )
    val darker = base.copy(
        red = base.red * DARKER_MULTIPLIER,
        green = base.green * DARKER_MULTIPLIER,
        blue = base.blue * DARKER_MULTIPLIER
    )
    return SpineColors(base = base, lighter = lighter, darker = darker)
}

@Immutable
data class SpineShadowConfig(
    val elevation: Dp,
    val ambientAlpha: Float,
    val spotAlpha: Float
)

val DefaultSpineShadow = SpineShadowConfig(
    elevation = 2.dp,
    ambientAlpha = 0.2f,
    spotAlpha = 0.3f
)

val LeaningSpineShadow = SpineShadowConfig(
    elevation = 3.dp,
    ambientAlpha = 0.25f,
    spotAlpha = 0.4f
)

@Composable
fun SpineHighlightStrip(
    height: Int,
    topAlpha: Float = DEFAULT_HIGHLIGHT_TOP_ALPHA,
    midAlpha: Float = DEFAULT_HIGHLIGHT_MID_ALPHA,
    offsetX: Dp = DEFAULT_HIGHLIGHT_OFFSET,
) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(height.dp)
            .offset(x = offsetX)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = topAlpha),
                        Color.White.copy(alpha = midAlpha),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(0.5.dp)
            )
    )
}

private const val LIGHTER_MULTIPLIER = 1.15f
private const val DARKER_MULTIPLIER = 0.6f
private const val DEFAULT_HIGHLIGHT_TOP_ALPHA = 0.3f
private const val DEFAULT_HIGHLIGHT_MID_ALPHA = 0.1f
private val DEFAULT_HIGHLIGHT_OFFSET = 3.dp
