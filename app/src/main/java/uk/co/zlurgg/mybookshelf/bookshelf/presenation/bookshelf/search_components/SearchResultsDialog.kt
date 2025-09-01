package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.search_components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.sampleBooks

@Composable
fun SearchResultsDialog(
    results: List<Book>,
    isLoading: Boolean,
    onAddBook: (Book) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Results") },
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
                results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No results found")
                    }
                }
                else -> {
                    LazyColumn {
                        items(results) { book ->
                            ListItem(
                                headlineContent = { Text(book.title) },
                                supportingContent = { Text(book.authors.joinToString()) },
                                trailingContent = {
                                    IconButton(onClick = { onAddBook(book) }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Preview
@Composable
private fun BookSearchScreenPreview() {
    SearchResultsDialog(
        results = sampleBooks,
        isLoading = false,
        onAddBook = {},
        onDismiss = {},
    )
}