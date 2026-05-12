package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.compose.runtime.Composable
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

    LibraryScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is LibraryAction.OnBookClick -> onBookClick(action.book)
                else -> viewModel.onAction(action)
            }
        }
    )
}
