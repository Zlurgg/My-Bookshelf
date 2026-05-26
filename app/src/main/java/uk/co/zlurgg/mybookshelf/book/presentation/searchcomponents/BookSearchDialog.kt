package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.displayDescription
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
    // Recomputed only when the result set changes; avoids a linear scan on
    // every recomposition (typing, scroll, focus changes) of the dialog.
    val showGoogleAttribution = remember(state.results) {
        state.results.any { it.provider == BookProvider.GOOGLE_BOOKS }
    }
    AlertDialog(
        onDismissRequest = {
            if (!state.isLoading) onDismiss()
        },
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
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
                        stringResource(R.string.search_results_found, state.results.size)
                    } else {
                        null
                    }
                    resultText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 2.dp
                            )
                        )
                    }
                    // Google Books TOS attribution — shown alongside the result
                    // count so it's visible without scrolling to the end of the list.
                    if (showGoogleAttribution && state.results.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.powered_by_google),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 6.dp
                            )
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
                                text = stringResource(
                                    R.string.search_no_results_found,
                                    state.query
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.search_no_results_suggestion),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(state.results) { index, book ->
                                val isExisting = state.existingBookIds.contains(book.id)
                                val stripeColor = if (index % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(stripeColor)
                                        .clickable { onBookClick(book) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (book.imageUrl.isNotBlank()) {
                                        LoadImage(
                                            imageUrl = book.imageUrl,
                                            title = book.title,
                                            modifier = Modifier
                                                .width(56.dp)
                                                .aspectRatio(2f / 3f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = book.authors.joinToString(", "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        book.firstPublishYear?.let { year ->
                                            Text(
                                                text = year,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        book.displayDescription()?.let { preview ->
                                            Text(
                                                text = preview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    trailingContent(book, isExisting)
                                }
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
