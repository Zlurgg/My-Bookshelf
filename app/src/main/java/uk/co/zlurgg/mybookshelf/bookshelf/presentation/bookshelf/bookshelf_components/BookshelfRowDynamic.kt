package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util.BookDisplayStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util.getBookDisplayStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.ShelfMaterial

@Composable
fun BookshelfRowDynamic(
    books: List<Book>,
    bookStyles: List<BookDisplayStyle>? = null,
    onBookClick: (Book) -> Unit,
    bookshelfMaterial: ShelfMaterial,
    showAddSlot: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    isTidyMode: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = bookshelfMaterial.painter(),
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
                    .background(bookshelfMaterial.shelfBackground)
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Render each book with appropriate style based on mode
                books.forEachIndexed { index, book ->
                    val bookStyle = bookStyles?.getOrNull(index) 
                        ?: if (isTidyMode) BookDisplayStyle.VERTICAL else getBookDisplayStyle(book)
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
                
                if (showAddSlot && onAddClick != null) {
                    AddBookSpine(onClick = onAddClick)
                }
            }
        }
    }
}

