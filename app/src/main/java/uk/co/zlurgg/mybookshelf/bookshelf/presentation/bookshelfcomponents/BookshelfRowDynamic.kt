package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelfcomponents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.util.BookDisplayStyle
import uk.co.zlurgg.mybookshelf.book.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.book.presentation.util.getBookDisplayStyle

@Composable
fun BookshelfRowDynamic(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    bookshelfMaterial: ShelfMaterial,
    modifier: Modifier = Modifier,
    config: BookshelfRowConfig = BookshelfRowConfig()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = bookshelfMaterial.painterLarge(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp) // Ensure minimum height even when empty
                    .background(bookshelfMaterial.shelfBackground)
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
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

                if (config.showAddSlot && config.onAddClick != null) {
                    AddBookSpine(onClick = config.onAddClick)
                }
            }
        }
    }
}
