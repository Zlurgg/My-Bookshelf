package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.bookshelf_components.BookshelfRow
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.search_components.SearchResultsDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.sampleBooks
import kotlin.math.floor

@Composable
fun BookshelfScreenRoot(
    viewModel: BookshelfViewModel = koinViewModel(),
    onAddBookClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    BookshelfScreen(
        state = state,
        showBookSearchDialog = showDialog,
        onShowBookSearchDialogChange = { showDialog = it },
        onAction = { action ->
            when (action) {
                is BookshelfAction.OnBookClick -> {
                    onBookClick(action.book)
                }
                is BookshelfAction.OnAddBookClick -> {
                    onAddBookClick(action.book)
                }
                is BookshelfAction.OnBackClick -> {
                    onBackClick()
                }
                else -> viewModel.onAction(action)
            }
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BookshelfScreen(
    state: BookshelfState,
    showBookSearchDialog: Boolean,
    onShowBookSearchDialogChange: (Boolean) -> Unit,
    onAction: (BookshelfAction) -> Unit,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val bookWidth = 62.dp
    val bookSpacing = 4.dp
    val shelfSpacing = 8.dp
    val booksPerRow = floor((screenWidth) / (bookWidth + bookSpacing + shelfSpacing))
        .toInt().coerceAtLeast(1)

//    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onShowBookSearchDialogChange(true)  }) {
                Icon(Icons.Default.Add, contentDescription = "Add Shelf")
            }
        },
        /*        topBar = {
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
                            .statusBarsPadding()
                    )
                }*/
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(state.books.chunked(booksPerRow)) { rowBooks ->
                BookshelfRow(
                    books = rowBooks,
                    onBookClick = { book -> onAction(BookshelfAction.OnBookClick(book)) },
                    bookshelfMaterial = state.shelfMaterial,
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
            onAddBook = { book -> onAction(BookshelfAction.OnAddBookClick(book)) },
            onDismiss = { onAction(BookshelfAction.OnDismissSearchDialog) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookshelfScreenPreview() {
    BookshelfScreen(
        state = BookshelfState(
            books = sampleBooks,
            shelfId = "1"
        ),
        onAction = {},
        showBookSearchDialog = false,
        onShowBookSearchDialogChange = {}
    )
}