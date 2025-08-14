package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.bookshelf_components.ShelfRow
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.search_components.SearchBar
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.search_components.SearchResultsDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.sampleBooks
import kotlin.math.floor

@Composable
fun BookshelfScreenRoot(
    viewModel: BookshelfViewModel = koinViewModel(),
    onBookClick: (Book) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BookshelfScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is BookshelfAction.OnBookClick -> onBookClick(action.book)
                is BookshelfAction.OnBackClick -> onBackClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
        shelfMaterial = ShelfMaterial.Wood,
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BookshelfScreen(
    state: BookshelfState,
    onAction: (BookshelfAction) -> Unit,
    shelfMaterial: ShelfMaterial,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val bookWidth = 62.dp
    val bookSpacing = 4.dp
    val shelfSpacing = 8.dp
    val booksPerRow = floor((screenWidth) / (bookWidth + bookSpacing + shelfSpacing))
        .toInt().coerceAtLeast(1)

    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            SearchBar(
                searchQuery = state.searchQuery,
                onSearchQueryChange = { onAction(BookshelfAction.OnSearchQueryChange(it)) },
                onSearchClick = { onAction(BookshelfAction.OnSearchClick) },
                onImeSearch = {
                    keyboardController?.hide()
                },
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(state.books.chunked(booksPerRow)) { rowBooks ->
                ShelfRow(
                    books = rowBooks,
                    onBookClick = { book -> onAction(BookshelfAction.OnBookClick(book)) },
                    shelfMaterial = shelfMaterial,
                    bookSpacing = bookSpacing,
                )
            }
        }
    }

    // Search dialog
    if (state.isSearchDialogVisible) {
        SearchResultsDialog(
            results = state.searchResults,
            isLoading = state.isSearchLoading,
            onAddBook = { book -> onAction(BookshelfAction.OnAddBookFromSearch(book)) },
            onDismiss = { onAction(BookshelfAction.OnDismissSearchDialog) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookshelfScreenPreview() {
    BookshelfScreen(
        state = BookshelfState(
            books = sampleBooks
        ),
        onAction = {},
        shelfMaterial = ShelfMaterial.Wood,
    )
}