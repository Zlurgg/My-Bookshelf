package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book

@Composable
fun BookVertical(
    book: Book,
    onClick: () -> Unit,
    height: Int = 150
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .height(height.dp)
            .width(60.dp)
            .background(Color(book.spineColor), shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LoadImage(
                imageUrl = book.imageUrl,
                title = book.title,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize(),
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
}

@Composable
fun BookLeaning(
    book: Book,
    onClick: () -> Unit,
    leanAngle: Float = -5f,
    height: Int = 140
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .rotate(leanAngle)
            .height(height.dp)
            .width(55.dp)
            .background(Color(book.spineColor), shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LoadImage(
                imageUrl = book.imageUrl,
                title = book.title,
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.title,
                    color = Color.White,
                    maxLines = 3,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

@Composable
fun BookHorizontalStack(
    books: List<Book>,
    onClick: (Book) -> Unit,
    maxStack: Int = 3
) {
    val stackedBooks = books.take(maxStack)
    
    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(4.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        stackedBooks.forEachIndexed { index, book ->
            Box(
                modifier = Modifier
                    .clickable { onClick(book) }
                    .width(150.dp - (index * 5).dp)
                    .height(25.dp)
                    .background(Color(book.spineColor), shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        maxLines = 1,
                        fontSize = 10.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    LoadImage(
                        imageUrl = book.imageUrl,
                        title = book.title,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(90f)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

enum class BookDisplayStyle {
    VERTICAL,
    LEANING_LEFT,
    LEANING_RIGHT,
    HORIZONTAL_STACK
}

fun getBookDisplayStyle(book: Book): BookDisplayStyle {
    // Use book ID hash to consistently assign display style to each book
    val bookHash = book.id.hashCode()
    return when (bookHash % 4) {
        0 -> BookDisplayStyle.VERTICAL
        1 -> BookDisplayStyle.LEANING_LEFT
        2 -> BookDisplayStyle.LEANING_RIGHT
        else -> BookDisplayStyle.VERTICAL // Avoid horizontal stacks for individual books
    }
}

fun getBookWidth(style: BookDisplayStyle): Float {
    return when (style) {
        BookDisplayStyle.VERTICAL -> 60f
        BookDisplayStyle.LEANING_LEFT, BookDisplayStyle.LEANING_RIGHT -> 70f
        BookDisplayStyle.HORIZONTAL_STACK -> 150f // only used for stacks
    }
}

data class ShelfLayoutTemplate(
    val name: String,
    val pattern: List<BookDisplayStyle>,
    val booksPerShelf: Int
)

val shelfLayoutTemplates = listOf(
    ShelfLayoutTemplate(
        name = "Classic",
        pattern = listOf(
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL
        ),
        booksPerShelf = 5
    ),
    ShelfLayoutTemplate(
        name = "Mixed Vertical",
        pattern = listOf(
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.LEANING_LEFT,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.LEANING_RIGHT
        ),
        booksPerShelf = 5
    ),
    ShelfLayoutTemplate(
        name = "Stack and Stand",
        pattern = listOf(
            BookDisplayStyle.HORIZONTAL_STACK,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.LEANING_RIGHT
        ),
        booksPerShelf = 6
    ),
    ShelfLayoutTemplate(
        name = "Casual",
        pattern = listOf(
            BookDisplayStyle.LEANING_LEFT,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.HORIZONTAL_STACK,
            BookDisplayStyle.LEANING_RIGHT
        ),
        booksPerShelf = 6
    ),
    ShelfLayoutTemplate(
        name = "Library Style",
        pattern = listOf(
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL,
            BookDisplayStyle.VERTICAL
        ),
        booksPerShelf = 6
    ),
    ShelfLayoutTemplate(
        name = "Artsy",
        pattern = listOf(
            BookDisplayStyle.HORIZONTAL_STACK,
            BookDisplayStyle.LEANING_LEFT,
            BookDisplayStyle.LEANING_RIGHT,
            BookDisplayStyle.HORIZONTAL_STACK
        ),
        booksPerShelf = 7
    )
)