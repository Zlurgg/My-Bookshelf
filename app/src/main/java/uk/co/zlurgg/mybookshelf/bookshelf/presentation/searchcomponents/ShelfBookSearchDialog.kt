package uk.co.zlurgg.mybookshelf.bookshelf.presentation.searchcomponents

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.preview.sampleBooks
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchDialog
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState

@Composable
fun ShelfBookSearchDialog(
    state: BookSearchState,
    callbacks: BookSearchCallbacks
) {
    BookSearchDialog(
        state = state,
        onQueryChange = callbacks.onQueryChange,
        onToggleSearchByTitle = callbacks.onToggleSearchByTitle,
        onToggleSearchByAuthor = callbacks.onToggleSearchByAuthor,
        onToggleSearchBySubject = callbacks.onToggleSearchBySubject,
        onToggleSafeSearch = callbacks.onToggleSafeSearch,
        onBookClick = callbacks.onBookClick,
        onDismiss = callbacks.onDismiss,
        trailingContent = { book, isExisting ->
            if (isExisting) {
                IconButton(onClick = { callbacks.onRemoveBook(book) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.action_remove_short),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(onClick = { callbacks.onAddBook(book) }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.action_add_short),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun ShelfBookSearchDialogPreview() {
    ShelfBookSearchDialog(
        state = BookSearchState(
            query = "",
            results = sampleBooks,
            existingBookIds = emptySet()
        ),
        callbacks = object : BookSearchCallbacks {
            override val onQueryChange: (String) -> Unit = {}
            override val onToggleSearchByTitle: () -> Unit = {}
            override val onToggleSearchByAuthor: () -> Unit = {}
            override val onToggleSearchBySubject: () -> Unit = {}
            override val onToggleSafeSearch: () -> Unit = {}
            override val onAddBook: (Book) -> Unit = {}
            override val onRemoveBook: (Book) -> Unit = {}
            override val onBookClick: (Book) -> Unit = {}
            override val onDismiss: () -> Unit = {}
        }
    )
}
