package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.book.domain.model.Book

@Composable
fun LibraryScreenRoot(
    viewModel: LibraryViewModel = koinViewModel(),
    onBookClick: (Book) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateToBook) {
        state.navigateToBook?.let { book ->
            onBookClick(book)
            viewModel.onAction(LibraryAction.OnNavigationHandled)
        }
    }

    LibraryScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is LibraryAction.OnBookClick -> onBookClick(action.book)
                is LibraryAction.OnSearchResultBookClick -> viewModel.onAction(action)
                else -> viewModel.onAction(action)
            }
        }
    )
}
