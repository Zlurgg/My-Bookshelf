package uk.co.zlurgg.mybookshelf.bookshelf.presenation

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.placeholder
import kotlinx.coroutines.Dispatchers
import uk.co.zlurgg.mybookshelf.R
import kotlin.math.floor
import kotlin.random.Random

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BookshelfScreen(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    shelfMaterial: ShelfMaterial, // customize shelf style
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val bookWidth = 60.dp
    val bookSpacing = 4.dp
    val booksPerRow = floor((screenWidth + bookSpacing) / (bookWidth + bookSpacing)).toInt().coerceAtLeast(1) -1

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
//            .background(shelfMaterial)
            .padding(vertical = 16.dp)
    ) {
        items(books.chunked(booksPerRow)) { rowBooks ->
            ShelfRow(
                books = rowBooks,
                onBookClick = onBookClick,
                shelfMaterial = shelfMaterial,
                bookSpacing = bookSpacing,
            )
        }
    }
}

@Composable
fun ShelfRow(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    shelfMaterial: ShelfMaterial,
    bookSpacing: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = shelfMaterial.painter(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .padding(8.dp),
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(shelfMaterial.shelfBackground) // darker shelf background
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

@Composable
fun BookSpine(
    book: Book,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(book.spineColor, shape = RoundedCornerShape(4.dp)) // default spine bg
            .padding(4.dp, top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .width(58.dp)
                .height(150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

//          Book image at the top
            LoadImage(
                model = book.spineImageUrl,
                title = book.title,
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

//             Rotated title below the image
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.title,
                    color = Color.White,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Left,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .rotate(-90f)
                )
            }
        }
    }
}

@Composable
fun LoadImage(
    model: String,
    title: String,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = model,
        loading = {
            CircularProgressIndicator(modifier = Modifier.requiredSize(40.dp))
        },
        contentDescription = title,
    )
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
            affiliateLink = "https://example.com/book$it",
            spineColor = randomReadableDarkColor()
        )
    }
    BookshelfScreen(
        books = sampleBooks,
        onBookClick = {},
        shelfMaterial = ShelfMaterial.Wood,
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
    val affiliateLink: String,
    val spineColor: Color
)

enum class ShelfMaterial(val shelfColor: Int, val shelfBackground: Color) {
    Wood(R.drawable.wood_texture_brown, Color(0xFF2B1F16));
    @Composable
    fun painter(): Painter = painterResource(shelfColor)
}

fun randomReadableDarkColor(): Color {
    val hue = Random.nextFloat() * 360f
    val saturation = 0.5f + Random.nextFloat() * 0.5f  // 0.5–1.0
    val lightness = 0.2f + Random.nextFloat() * 0.2f   // 0.2–0.4 for dark tones
    return hslToColor(hue, saturation, lightness)
}

fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2 * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2 - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60 -> listOf(c, x, 0f)
        h < 120 -> listOf(x, c, 0f)
        h < 180 -> listOf(0f, c, x)
        h < 240 -> listOf(0f, x, c)
        h < 300 -> listOf(x, 0f, c)
        else -> listOf(c, 0f, x)
    }
    return Color((r1 + m), (g1 + m), (b1 + m), 1f)
}