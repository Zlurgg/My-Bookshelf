package uk.co.zlurgg.mybookshelf.book.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.util.BookDisplayStyle
import uk.co.zlurgg.mybookshelf.book.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.book.presentation.util.getBookDisplayStyle

@Composable
fun BookRowDynamic(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    bookshelfMaterial: ShelfMaterial,
    modifier: Modifier = Modifier,
    config: BookRowConfig = BookRowConfig()
) {
    val shelfBg = bookshelfMaterial.shelfBackground
    val lipHighlight = shelfBg.copy(
        red = (shelfBg.red * SHELF_LIP_LIGHTER).coerceAtMost(1f),
        green = (shelfBg.green * SHELF_LIP_LIGHTER).coerceAtMost(1f),
        blue = (shelfBg.blue * SHELF_LIP_LIGHTER).coerceAtMost(1f)
    )
    val lipShadow = shelfBg.copy(
        red = shelfBg.red * SHELF_LIP_DARKER,
        green = shelfBg.green * SHELF_LIP_DARKER,
        blue = shelfBg.blue * SHELF_LIP_DARKER
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = bookshelfMaterial.painterLarge(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            // Book row area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(shelfBg)
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if (config.showAddSlot && config.onAddClick != null) {
                    AddBookSpine(onClick = config.onAddClick)
                }

                // Render each book with appropriate style based on mode
                books.forEachIndexed { index, book ->
                    val bookStyle = config.bookStyles?.getOrNull(index)
                        ?: if (config.isTidyMode) BookDisplayStyle.VERTICAL else getBookDisplayStyle(book)
                    when (bookStyle) {
                        BookDisplayStyle.VERTICAL -> {
                            BookVertical(
                                book = book,
                                onClick = { onBookClick(book) },
                                height = 150
                            )
                        }
                        BookDisplayStyle.LEANING_LEFT -> {
                            BookLeaning(
                                book = book,
                                onClick = { onBookClick(book) },
                                leanAngle = -5f,
                                height = 145
                            )
                        }
                        BookDisplayStyle.LEANING_RIGHT -> {
                            BookLeaning(
                                book = book,
                                onClick = { onBookClick(book) },
                                leanAngle = 5f,
                                height = 145
                            )
                        }
                        BookDisplayStyle.HORIZONTAL_STACK -> {
                            BookHorizontal(
                                book = book,
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }
            }

            // Shelf lip — front edge that catches light
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SHELF_LIP_HEIGHT.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(lipHighlight, shelfBg, lipShadow)
                        )
                    )
            )
        }
    }
}

private const val SHELF_LIP_HEIGHT = 6
private const val SHELF_LIP_LIGHTER = 1.4f
private const val SHELF_LIP_DARKER = 0.5f
