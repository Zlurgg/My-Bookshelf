package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelfcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.util.getBookThickness

@Composable
fun BookVertical(
    book: Book,
    onClick: () -> Unit,
    height: Int = 150
) {
    val thickness = getBookThickness(book.numPages)
    val baseColor = Color(book.spineColor) // Already matte from BookColorGenerator
    val lighterColor = baseColor.copy(
        red = (baseColor.red * 1.15f).coerceAtMost(1f),
        green = (baseColor.green * 1.15f).coerceAtMost(1f),
        blue = (baseColor.blue * 1.15f).coerceAtMost(1f)
    )
    val darkerColor = baseColor.copy(
        red = baseColor.red * 0.6f,
        green = baseColor.green * 0.6f,
        blue = baseColor.blue * 0.6f
    )

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .height(height.dp)
            .width(thickness.dp)
            .padding(horizontal = 1.dp) // Contain shadow within bounds
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
    ) {
        // 3D spine with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(lighterColor, baseColor, darkerColor),
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
                // Book image integrated into spine (no separate shadow/background)
                LoadImage(
                    imageUrl = book.imageUrl,
                    title = book.title,
                    modifier = Modifier
                        .size((thickness * 0.8f).dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                // Text integrated into spine background
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        maxLines = 4,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 9.sp
                    )
                }
            }
        }

        // Subtle highlight strip for 3D depth
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(height.dp)
                .offset(x = 3.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(0.5.dp)
                )
        )
    }
}
