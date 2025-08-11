package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun BookSearchScreenRoot(
    navController: NavController,
    viewModel: BookSearchViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BookSearchScreen(
        state = state,
        onAction = { action ->

        }
    )
}

@Composable
fun BookSearchScreen(
    state: BookSearchState,
    onAction: (BookSearchAction) -> Unit
) {

}
