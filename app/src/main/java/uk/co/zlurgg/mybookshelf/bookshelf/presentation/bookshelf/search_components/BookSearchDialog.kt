package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components.LoadImage
import uk.co.zlurgg.mybookshelf.core.presentation.sampleBooks

@Composable
fun BookSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Book>,
    isLoading: Boolean,
    inShelfIds: Set<String>,
    onAddBook: (Book) -> Unit,
    onRemoveBook: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // Only allow dismiss if not loading
            if (!isLoading) onDismiss()
        },
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            ) {
                SearchBar(
                    searchQuery = query,
                    onSearchQueryChange = onQueryChange,
                    onImeSearch = { /* handled by onQueryChange as user types */ },
                )
            }
        },
        text = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                results.isEmpty() && query.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(id = R.string.search_no_results))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(results) { book ->
                            val isInShelf = inShelfIds.contains(book.id)
                            ListItem(
                                leadingContent = {
                                    LoadImage(
                                        imageUrl = book.imageUrl,
                                        title = book.title,
                                        modifier = Modifier.size(48.dp)
                                    )
                                },
                                headlineContent = { Text(book.title) },
                                supportingContent = { Text(book.authors.joinToString()) },
                                trailingContent = {
                                    if (isInShelf) {
                                        IconButton(onClick = { onRemoveBook(book) }) {
                                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.action_remove_short))
                                        }
                                    } else {
                                        IconButton(onClick = { onAddBook(book) }) {
                                            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.action_add_short))
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { onBookClick(book) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_close)) }
        }
    )
}

@Preview
@Composable
private fun BookSearchScreenPreview() {
    BookSearchDialog(
        query = "",
        onQueryChange = {},
        results = sampleBooks,
        isLoading = false,
        inShelfIds = emptySet(),
        onAddBook = {},
        onRemoveBook = {},
        onBookClick = {},
        onDismiss = {},
    )
}