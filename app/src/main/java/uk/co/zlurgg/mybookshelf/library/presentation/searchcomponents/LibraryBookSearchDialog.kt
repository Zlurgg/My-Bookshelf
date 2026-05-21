package uk.co.zlurgg.mybookshelf.library.presentation.searchcomponents

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchDialog
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState

@Composable
fun LibraryBookSearchDialog(
    state: BookSearchState,
    onQueryChange: (String) -> Unit,
    onToggleSearchByTitle: () -> Unit,
    onToggleSearchByAuthor: () -> Unit,
    onToggleSearchBySubject: () -> Unit,
    onToggleSafeSearch: () -> Unit,
    onBookClick: (Book) -> Unit,
    onAddBook: (Book) -> Unit,
    onDismiss: () -> Unit
) {
    BookSearchDialog(
        state = state,
        onQueryChange = onQueryChange,
        onToggleSearchByTitle = onToggleSearchByTitle,
        onToggleSearchByAuthor = onToggleSearchByAuthor,
        onToggleSearchBySubject = onToggleSearchBySubject,
        onToggleSafeSearch = onToggleSafeSearch,
        onBookClick = onBookClick,
        onDismiss = onDismiss,
        trailingContent = { book, isExisting ->
            if (isExisting) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.library_already_added),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(onClick = { onAddBook(book) }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.library_add_book)
                    )
                }
            }
        }
    )
}
