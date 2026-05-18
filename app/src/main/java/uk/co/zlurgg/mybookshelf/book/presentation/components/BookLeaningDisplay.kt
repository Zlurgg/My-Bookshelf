package uk.co.zlurgg.mybookshelf.book.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.util.LeaningSpineShadow
import uk.co.zlurgg.mybookshelf.book.presentation.util.SpineHighlightStrip
import uk.co.zlurgg.mybookshelf.book.presentation.util.calculateSpineColors
import uk.co.zlurgg.mybookshelf.book.presentation.util.getBookThickness

@Composable
fun BookLeaning(
    book: Book,
    onClick: () -> Unit,
    leanAngle: Float = -5f,
    height: Int = 140
) {
    val thickness = getBookThickness(book.numPages)
    val spineColors = calculateSpineColors(book.spineColor)
    val shadow = LeaningSpineShadow

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .rotate(leanAngle)
            .height(height.dp)
            .width(thickness.dp)
            .padding(start = 2.dp, end = 2.dp, bottom = 3.dp) // Bottom padding for lean offset
            .shadow(
                elevation = shadow.elevation,
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black.copy(alpha = shadow.ambientAlpha),
                spotColor = Color.Black.copy(alpha = shadow.spotAlpha)
            )
    ) {
        // 3D spine with enhanced gradient for leaning effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(spineColors.lighter, spineColors.base, spineColors.darker),
                        startX = 0f,
                        endX = thickness * 2
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Book image integrated into spine (no separate effects)
                LoadImage(
                    imageUrl = book.imageUrl,
                    title = book.title,
                    modifier = Modifier
                        .size((thickness * 0.75f).dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                // Text integrated into spine background
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.title,
                        color = spineColors.text,
                        maxLines = 3,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 9.sp
                    )
                }
            }
        }

        SpineHighlightStrip(
            height = height - LEAN_BOTTOM_PADDING,
            topAlpha = LEANING_HIGHLIGHT_TOP_ALPHA,
            midAlpha = LEANING_HIGHLIGHT_MID_ALPHA,
            offsetX = LEANING_HIGHLIGHT_OFFSET,
        )
    }
}

private const val LEANING_HIGHLIGHT_TOP_ALPHA = 0.4f
private const val LEANING_HIGHLIGHT_MID_ALPHA = 0.2f
private val LEANING_HIGHLIGHT_OFFSET = 2.dp
private const val LEAN_BOTTOM_PADDING = 3
