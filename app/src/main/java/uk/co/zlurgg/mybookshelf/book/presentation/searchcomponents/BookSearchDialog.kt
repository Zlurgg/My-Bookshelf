package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.components.LoadImage

@Composable
fun BookSearchDialog(
    state: BookSearchState,
    onQueryChange: (String) -> Unit,
    onToggleSearchByTitle: () -> Unit,
    onToggleSearchByAuthor: () -> Unit,
    onToggleSearchBySubject: () -> Unit,
    onToggleSafeSearch: () -> Unit,
    onBookClick: (Book) -> Unit,
    onDismiss: () -> Unit,
    trailingContent: @Composable (book: Book, isExisting: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isLoading) onDismiss()
        },
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
            ) {
                BookSearchBar(
                    searchQuery = state.query,
                    onSearchQueryChange = onQueryChange,
                    onImeSearch = { /* handled by onQueryChange as user types */ }
                )

                Spacer(modifier = Modifier.height(4.dp))

                SearchFilters(
                    searchByTitle = state.searchByTitle,
                    searchByAuthor = state.searchByAuthor,
                    searchBySubject = state.searchBySubject,
                    titleEnabled = state.canToggleTitle,
                    authorEnabled = state.canToggleAuthor,
                    subjectEnabled = state.canToggleSubject,
                    safeSearchEnabled = state.safeSearchEnabled,
                    onToggleTitle = onToggleSearchByTitle,
                    onToggleAuthor = onToggleSearchByAuthor,
                    onToggleSubject = onToggleSearchBySubject,
                    onToggleSafeSearch = onToggleSafeSearch
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.isTyping || state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Inline error message (distinct from empty results)
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (!state.isLoading && !state.isTyping && state.hasSearched) {
                    val resultText = if (state.filteredCount > 0) {
                        stringResource(
                            R.string.safe_search_filtered,
                            state.results.size,
                            state.filteredCount
                        )
                    } else if (state.results.isNotEmpty()) {
                        "${state.results.size} results found"
                    } else {
                        null
                    }
                    resultText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                when {
                    state.results.isEmpty() && state.query.isNotBlank() &&
                        !state.isTyping && !state.isLoading &&
                        state.hasSearched && state.errorMessage == null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No books found for \"${state.query}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try different keywords or check your spelling",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.results) { book ->
                                val isExisting = state.existingBookIds.contains(book.id)
                                ListItem(
                                    leadingContent = {
                                        LoadImage(
                                            imageUrl = book.imageUrl,
                                            title = book.title,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = book.title,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Column {
                                            Text(
                                                text = book.authors.joinToString(", "),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            book.firstPublishYear?.let { year ->
                                                Text(
                                                    text = year,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        trailingContent(book, isExisting)
                                    },
                                    modifier = Modifier.clickable { onBookClick(book) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(id = R.string.action_close)) }
        }
    )
}
