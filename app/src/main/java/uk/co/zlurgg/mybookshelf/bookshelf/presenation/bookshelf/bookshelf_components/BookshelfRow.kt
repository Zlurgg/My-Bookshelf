package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.bookshelf_components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.sampleBooks

@Composable
fun BookshelfRow(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    bookshelfMaterial: ShelfMaterial,
    bookSpacing: Dp,
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
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bookshelfMaterial.shelfBackground) // darker shelf background
                    .padding(top= 8.dp, start = 1.dp, end = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(bookSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                books.forEach { book ->
                    BookSpine(
                        book = book,
                        onClick = { onBookClick(book) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookshelfRowPreview() {
    BookshelfRow(
        books = sampleBooks,
        onBookClick = {},
        bookshelfMaterial = ShelfMaterial.DarkWood,
        bookSpacing = 4.dp
    )
}