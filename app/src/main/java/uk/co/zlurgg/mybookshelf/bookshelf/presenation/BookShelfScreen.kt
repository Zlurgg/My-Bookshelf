package uk.co.zlurgg.mybookshelf.bookshelf.presenation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import uk.co.zlurgg.mybookshelf.R
import kotlin.math.floor

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BookshelfScreen(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    shelfMaterial: ShelfMaterial = ShelfMaterial.Wood // customize shelf style
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val bookWidth = 48.dp + 12.dp // book width + spacing
    val booksPerRow = floor(screenWidth.value / bookWidth.value).toInt().coerceAtLeast(1)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(shelfMaterial.backgroundColor)
            .padding(vertical = 16.dp)
    ) {
        items(books.chunked(booksPerRow)) { rowBooks ->
            ShelfRow(
                books = rowBooks,
                onBookClick = onBookClick,
                shelfMaterial = shelfMaterial
            )
        }
    }
}

@Composable
fun ShelfRow(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    shelfMaterial: ShelfMaterial
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(shelfMaterial.shelfColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            books.forEach { book ->
                BookSpine(book = book, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
fun BookSpine(book: Book, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(120.dp)
            .clickable { onClick() }
            .background(Color.DarkGray, shape = RoundedCornerShape(4.dp)) // default spine bg
            .padding(4.dp, top = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = book.title,
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Left,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .rotate(-90f)
                    .width(100.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            AsyncImage(
                model = book.spineImageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error)
            )
        }

    }
}



@Preview(showBackground = true)
@Composable
fun BookshelfScreenPreview() {
    val sampleBooks = List(10) {
        Book(
            id = it.toString(),
            title = "Book Title that is longer $it",
            author = "Author $it",
            spineImageUrl = "https://via.placeholder.com/40x60.png?text=Book+$it",
            fullImageUrl = "https://via.placeholder.com/200x300.png?text=Book+$it",
            blurb = "This is a sample blurb for book $it.",
            purchased = it % 2 == 0,
            affiliateLink = "https://example.com/book$it"
        )
    }
    BookshelfScreen(
        books = sampleBooks,
        onBookClick = {},
        shelfMaterial = ShelfMaterial.Wood
    )
}


// --- Models and Styles ---

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val spineImageUrl: String,
    val fullImageUrl: String,
    val blurb: String,
    val purchased: Boolean,
    val affiliateLink: String
)

enum class ShelfMaterial(val backgroundColor: Color, val shelfColor: Color) {
    Wood(Color(0xFFFAF3E0), Color(0xFF8B5E3C)),
    Steel(Color(0xFFEFEFEF), Color(0xFF888888)),
    DarkWood(Color(0xFF3B2F2F), Color(0xFF5C4033))
}
