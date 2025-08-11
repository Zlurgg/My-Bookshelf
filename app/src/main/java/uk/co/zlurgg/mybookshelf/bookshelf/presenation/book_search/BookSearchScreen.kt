package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search.components.BookList
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search.components.SearchBar
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.sampleBooks

@Composable
fun BookSearchScreenRoot(
    viewModel: BookSearchViewModel = koinViewModel(),
    onBookClick: (Book) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BookSearchScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is BookSearchAction.OnBookClick -> onBookClick(action.book)
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun BookSearchScreen(
    state: BookSearchState,
    onAction: (BookSearchAction) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchResultsListState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(
            searchQuery = state.searchQuery,
            onSearchQueryChange = {
                onAction(BookSearchAction.OnSearchQueryChange(it))
            },
            onImeSearch = {
                keyboardController?.hide()
            },
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp)
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(
                topStart = 32.dp,
                topEnd = 32.dp
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        when {
                            state.searchResults.isEmpty() -> {
                                Text(
                                    text = "No Search Results",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {
                                BookList(
                                    books = state.searchResults,
                                    onBookClick = {
                                        onAction(BookSearchAction.OnBookClick(it))
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    scrollState = searchResultsListState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BookSearchScreenPreview() {
    BookSearchScreen(
        state = BookSearchState(
            searchResults = sampleBooks
        ),
        onAction = {}
    )
}