package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.components.ShelfRow
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.sampleBooks
import kotlin.math.floor

@Composable
fun BookshelfScreenRoot(
    navController: NavController,
    viewModel: BookshelfViewModel = koinViewModel(),
    ) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BookshelfScreen(
        state = state,
        onBookClick = {}, // onAction...
        shelfMaterial = ShelfMaterial.Wood,
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BookshelfScreen(
    state: BookshelfState,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    shelfMaterial: ShelfMaterial, // customize shelf style
) {
    /* TODO:
         - Add search bar to add new book to shelf
         - delete shelf / add shelf from bookcase navigates here
         - Send book shelf to other users
         - go to book page via clicking on book
    */

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val bookWidth = 62.dp
    val bookSpacing = 4.dp
    val shelfSpacing = 8.dp
    val booksPerRow = floor((screenWidth) / (bookWidth + bookSpacing + shelfSpacing)).toInt().coerceAtLeast(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
    ) {
        items(
            state.books.chunked(booksPerRow)) { rowBooks ->
            ShelfRow(
                books = rowBooks,
                onBookClick = onBookClick,
                shelfMaterial = shelfMaterial,
                bookSpacing = bookSpacing,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookshelfScreenPreview() {
    BookshelfScreen(
        state = BookshelfState(
            books = sampleBooks
        ),
        onBookClick = {},
        shelfMaterial = ShelfMaterial.Wood,
    )
}