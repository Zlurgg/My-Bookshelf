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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.ShelfMaterial
import kotlin.random.Random

@Composable
fun BookshelfRowCustom(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    bookshelfMaterial: ShelfMaterial,
    showAddSlot: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    rowIndex: Int
) {
    // Use combination of first book ID and row position for stable but varied layout
    val layoutSeed = remember(books.firstOrNull()?.id, rowIndex) {
        val bookIdHash = books.firstOrNull()?.id?.hashCode() ?: 0
        (bookIdHash * 31 + rowIndex).toLong()
    }
    
    val template = remember(layoutSeed) {
        shelfLayoutTemplates.random(Random(layoutSeed))
    }
    
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
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                var bookIndex = 0
                var patternIndex = 0
                
                while (bookIndex < books.size && patternIndex < template.pattern.size) {
                    when (template.pattern[patternIndex]) {
                        BookDisplayStyle.VERTICAL -> {
                            if (bookIndex < books.size) {
                                BookVertical(
                                    book = books[bookIndex],
                                    onClick = { onBookClick(books[bookIndex]) },
                                    height = 150
                                )
                                bookIndex++
                            }
                        }
                        BookDisplayStyle.LEANING_LEFT -> {
                            if (bookIndex < books.size) {
                                BookLeaning(
                                    book = books[bookIndex],
                                    onClick = { onBookClick(books[bookIndex]) },
                                    leanAngle = -5f,
                                    height = 145
                                )
                                bookIndex++
                            }
                        }
                        BookDisplayStyle.LEANING_RIGHT -> {
                            if (bookIndex < books.size) {
                                BookLeaning(
                                    book = books[bookIndex],
                                    onClick = { onBookClick(books[bookIndex]) },
                                    leanAngle = 5f,
                                    height = 145
                                )
                                bookIndex++
                            }
                        }
                        BookDisplayStyle.HORIZONTAL_STACK -> {
                            val stackSize = minOf(3, books.size - bookIndex)
                            if (stackSize > 0) {
                                val stackBooks = books.subList(bookIndex, bookIndex + stackSize)
                                BookHorizontalStack(
                                    books = stackBooks,
                                    onClick = onBookClick,
                                    maxStack = 3
                                )
                                bookIndex += stackSize
                            }
                        }
                    }
                    patternIndex++
                }
                
                if (showAddSlot && onAddClick != null) {
                    AddBookSpine(onClick = onAddClick)
                }
            }
        }
    }
}

fun calculateBooksPerRow(template: ShelfLayoutTemplate): Int {
    var capacity = 0
    template.pattern.forEach { style ->
        capacity += when (style) {
            BookDisplayStyle.HORIZONTAL_STACK -> 3
            else -> 1
        }
    }
    return capacity
}

fun calculateRowWidth(template: ShelfLayoutTemplate, includeAddButton: Boolean = true): Float {
    var totalWidth = 0f
    template.pattern.forEach { style ->
        totalWidth += when (style) {
            BookDisplayStyle.VERTICAL -> 60f + 8f // width + spacing
            BookDisplayStyle.LEANING_LEFT, BookDisplayStyle.LEANING_RIGHT -> 70f + 8f // wider due to angle + spacing
            BookDisplayStyle.HORIZONTAL_STACK -> 150f + 8f // much wider + spacing
        }
    }
    
    // Add space for the add button (same size as vertical book)
    if (includeAddButton) {
        totalWidth += 60f + 8f // add button width + spacing
    }
    
    return totalWidth - 8f // remove last spacing
}