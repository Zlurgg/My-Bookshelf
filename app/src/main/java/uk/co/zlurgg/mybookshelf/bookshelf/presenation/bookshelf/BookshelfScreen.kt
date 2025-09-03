package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.bookshelf_components.BookshelfRow
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.search_components.BookSearchDialog
import uk.co.zlurgg.mybookshelf.core.presentation.sampleBooks
import kotlin.math.floor

@Composable
fun BookshelfScreenRoot(
    viewModel: BookshelfViewModel = koinViewModel(),
    onAddBookClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BookshelfScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is BookshelfAction.OnBookClick -> onBookClick(action.book)
                is BookshelfAction.OnAddBookClick -> onAddBookClick(action.book)
                is BookshelfAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BookshelfScreen(
    state: BookshelfState,
    onAction: (BookshelfAction) -> Unit,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val bookWidth = 62.dp
    val bookSpacing = 4.dp
    val shelfSpacing = 8.dp
    val booksPerRow = floor((screenWidth) / (bookWidth + bookSpacing + shelfSpacing))
        .toInt().coerceAtLeast(1)

    Scaffold(
    ) { paddingValues ->
        if (!state.isLoading && state.books.isEmpty()) {
            androidx.compose.material3.Text(
                text = "This shelf is empty. Tap + to add a book",
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(24.dp)
            )
        } else {
            LazyColumn(contentPadding = paddingValues) {
                val rows = state.books.chunked(booksPerRow)
                items(rows.size) { index ->
                    val rowBooks = rows[index]
                    val isLastRow = index == rows.lastIndex
                    BookshelfRow(
                        books = rowBooks,
                        onBookClick = { book -> onAction(BookshelfAction.OnBookClick(book)) },
                        bookshelfMaterial = state.shelfMaterial,
                        bookSpacing = bookSpacing,
                        showAddSlot = isLastRow,
                        onAddClick = { onAction(BookshelfAction.OnSearchClick) }
                    )
                }
            }
        }
    }

    // Search dialog
    if (state.isSearchDialogVisible) {
        BookSearchDialog(
            query = state.searchQuery,
            onQueryChange = { onAction(BookshelfAction.OnSearchQueryChange(it)) },
            results = state.searchResults,
            isLoading = state.isSearchLoading,
            inShelfIds = state.books.map { it.id }.toSet(),
            onAddBook = { book ->
                onAction(BookshelfAction.OnAddBookClick(book))
                onAction(BookshelfAction.OnDismissSearchDialog)
            },
            onRemoveBook = { book ->
                onAction(BookshelfAction.OnRemoveBook(book))
                onAction(BookshelfAction.OnDismissSearchDialog)
            },
            onBookClick = { book -> onAction(BookshelfAction.OnBookClick(book)) },
            onDismiss = {
                onAction(BookshelfAction.OnDismissSearchDialog)
            }
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
    )
}