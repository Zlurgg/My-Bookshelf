package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.AddShelfDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.BookcaseShelf
import uk.co.zlurgg.mybookshelf.core.presentation.bookshelves
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun BookcaseScreenRoot(
    viewModel: BookcaseViewModel = koinViewModel(),
    onBookshelfClick: (Bookshelf) -> Unit,
    onAddBookshelfClick: (String, ShelfStyle) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    BookcaseScreen(
        state = state,
        showAddBookshelfDialog = showDialog,
        onShowAddBookshelfDialogChange = { showDialog = it },
        onAction = { action ->
            when (action) {
                is BookcaseAction.OnBookshelfClick -> {
                    onBookshelfClick(action.bookshelf)
                }
                is BookcaseAction.OnAddBookshelfClick -> {
                    onAddBookshelfClick(action.name, action.style)
                }
                is BookcaseAction.ShowAddDialog -> {
                    showDialog = action.showDialog
                }
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BookcaseScreen(
    state: BookcaseState,
    showAddBookshelfDialog: Boolean,
    onShowAddBookshelfDialogChange: (Boolean) -> Unit,
    onAction: (BookcaseAction) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar if needed
    if (state.errorMessage != null) {
        LaunchedEffect(snackbarHostState) {
            snackbarHostState.showSnackbar(state.errorMessage)
            // Clear error after showing
            onAction(BookcaseAction.ResetOperationState)
        }
    }

    // Watch for operation success to close dialog
    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            onShowAddBookshelfDialogChange(false)
            onAction(BookcaseAction.ResetOperationState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onShowAddBookshelfDialogChange(true)  }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.fab_add_shelf))
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        if (!state.isLoading && state.bookshelves.isEmpty()) {
            LazyColumn(contentPadding = padding) {
                item {
                    Text(
                        text = stringResource(id = R.string.bookcase_empty_state_hint),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(
                    items = state.bookshelves,
                    key = { it.id }
                ) { shelf ->
                    BookcaseShelf(
                        shelf = shelf,
                        onRemoveBookshelf = { shelfToRemove ->
                            onAction(BookcaseAction.OnRemoveBookShelf(shelfToRemove))
                        },
                        onBookshelfClick = {
                            onAction(BookcaseAction.OnBookshelfClick(shelf))
                        },
                        modifier = Modifier.animateItem(),
                        bookCountOverride = state.bookCounts[shelf.id] ?: 0
                    )
                }
            }
        }
    }

    if (showAddBookshelfDialog) {
        AddShelfDialog(
            onDismiss = {
                if (!state.isLoading) onShowAddBookshelfDialogChange(false)
            },
            onAddShelf = { shelfName, style ->
                onAction(BookcaseAction.OnAddBookshelfClick(shelfName, style))
            },
            isLoading = state.isLoading
        )
    }
}


@Preview(showBackground = true)
@Composable
fun BookcaseScreenPreview() {
    MyBookshelfTheme {
        BookcaseScreen(
            state = BookcaseState(
                bookshelves = bookshelves,
            ),
            onAction = {},
            showAddBookshelfDialog = false,
            onShowAddBookshelfDialogChange = {}
        )
    }
}