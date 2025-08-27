package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.components.AddShelfDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.components.BookcaseShelf
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.bookshelves
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun BookcaseScreenRoot(
    viewModel: BookcaseViewModel = koinViewModel(),
    onBookshelfClick: (Bookshelf) -> Unit,
    onAddBookshelfClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BookcaseScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is BookcaseAction.OnBookshelfClick -> onBookshelfClick(action.bookshelf)
                is BookcaseAction.OnAddBookshelfClick -> onAddBookshelfClick(action.name)
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun BookcaseScreen(
    state: BookcaseState,
    onAction: (BookcaseAction) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Watch for operation success to close dialog
    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            showDialog = false
            onAction(BookcaseAction.ResetOperationState)
        }
    }

    // Show error snackbar if needed
    if (state.errorMessage != null) {
        LaunchedEffect(snackbarHostState) {
            snackbarHostState.showSnackbar(state.errorMessage)
            // Clear error after showing
            onAction(BookcaseAction.ResetOperationState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Shelf")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(
                items = state.bookshelves,
                key = { it.id }
            ) { shelf ->
                BookcaseShelf(
                    shelf = shelf,
                    onRemove = { shelfToRemove ->
                        onAction(BookcaseAction.OnRemoveBookShelf(shelfToRemove))
                    },
                    onClick = {
                        onAction(BookcaseAction.OnBookshelfClick(it))
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (showDialog) {
        AddShelfDialog(
            onDismiss = { showDialog = false },
            onAddShelf = { shelfName ->
                onAction(BookcaseAction.OnAddBookshelfClick(shelfName))
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
            onAction = {}
        )
    }
}