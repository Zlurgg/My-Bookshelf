package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.searchcomponents

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelfcomponents.LoadImage
import uk.co.zlurgg.mybookshelf.book.presentation.preview.sampleBooks

@Composable
fun BookSearchDialog(
    state: BookSearchState,
    callbacks: BookSearchCallbacks
) {
    AlertDialog(
        onDismissRequest = {
            // Only allow dismiss if not loading
            if (!state.isLoading) callbacks.onDismiss()
        },
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
            ) {
                BookSearchBar(
                    searchQuery = state.query,
                    onSearchQueryChange = callbacks.onQueryChange,
                    onImeSearch = { /* handled by onQueryChange as user types */ }
                )

                Spacer(modifier = Modifier.height(4.dp))

                SearchFilters(
                    searchByTitle = state.searchByTitle,
                    searchByAuthor = state.searchByAuthor,
                    onToggleTitle = callbacks.onToggleSearchByTitle,
                    onToggleAuthor = callbacks.onToggleSearchByAuthor
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Progress indicator (shows during typing AND searching)
                if (state.isTyping || state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Result count (show when we have results and not loading)
                if (state.results.isNotEmpty() && !state.isLoading && !state.isTyping) {
                    Text(
                        text = "${state.results.size} results found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                when {
                    state.results.isEmpty() && state.query.isNotBlank() &&
                        !state.isTyping && !state.isLoading &&
                        state.hasSearched -> {
                        // Enhanced empty state with icon and helpful messaging
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
                                val isInShelf = state.inShelfIds.contains(book.id)
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
                                        if (isInShelf) {
                                            IconButton(onClick = { callbacks.onRemoveBook(book) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(
                                                        id = R.string.action_remove_short
                                                    )
                                                )
                                            }
                                        } else {
                                            IconButton(onClick = { callbacks.onAddBook(book) }) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = stringResource(id = R.string.action_add_short)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable { callbacks.onBookClick(book) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { callbacks.onDismiss() }) { Text(stringResource(id = R.string.action_close)) }
        }
    )
}

@Preview
@Composable
private fun BookSearchScreenPreview() {
    BookSearchDialog(
        state = BookSearchState(
            query = "",
            results = sampleBooks,
            isLoading = false,
            isTyping = false,
            inShelfIds = emptySet(),
            searchByTitle = true,
            searchByAuthor = true
        ),
        callbacks = object : BookSearchCallbacks {
            override val onQueryChange: (String) -> Unit = {}
            override val onToggleSearchByTitle: () -> Unit = {}
            override val onToggleSearchByAuthor: () -> Unit = {}
            override val onAddBook: (Book) -> Unit = {}
            override val onRemoveBook: (Book) -> Unit = {}
            override val onBookClick: (Book) -> Unit = {}
            override val onDismiss: () -> Unit = {}
        }
    )
}
