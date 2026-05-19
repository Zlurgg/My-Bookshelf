package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.book.presentation.components.BookRowConfig
import uk.co.zlurgg.mybookshelf.book.presentation.util.toDisplayString
import uk.co.zlurgg.mybookshelf.book.presentation.components.BookRowDynamic
import uk.co.zlurgg.mybookshelf.book.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.book.presentation.util.calculateBookRows
import uk.co.zlurgg.mybookshelf.library.presentation.components.DeleteBooksConfirmationDialog
import uk.co.zlurgg.mybookshelf.library.presentation.searchcomponents.LibraryBookSearchDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    state: LibraryState,
    onAction: (LibraryAction) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val availableWidth = configuration.screenWidthDp.toFloat() - 40f // padding
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = state.isSelectionMode) {
        onAction(LibraryAction.OnToggleSelectionMode)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onAction(LibraryAction.OnDismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { onAction(LibraryAction.OnToggleSelectionMode) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    },
                    title = {
                        val count = state.selectedBookIds.size
                        Text(
                            pluralStringResource(
                                R.plurals.library_selected_count,
                                count,
                                count
                            )
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (state.selectedBookIds.size == state.deletableBooks.size) {
                                    onAction(LibraryAction.OnDeselectAll)
                                } else {
                                    onAction(LibraryAction.OnSelectAll)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (state.selectedBookIds.size == state.deletableBooks.size) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Outlined.CheckCircle
                                },
                                contentDescription = if (state.selectedBookIds.size == state.deletableBooks.size) {
                                    stringResource(R.string.library_deselect_all)
                                } else {
                                    stringResource(R.string.library_select_all)
                                }
                            )
                        }
                        IconButton(
                            onClick = { onAction(LibraryAction.OnDeleteSelectedClick) },
                            enabled = state.selectedBookIds.isNotEmpty() && !state.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.library_delete_selected),
                                tint = if (state.selectedBookIds.isNotEmpty()) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.library_title)) },
                    actions = {
                        if (state.allBooks.isNotEmpty() && !state.isSearchDialogVisible) {
                            IconButton(
                                onClick = { onAction(LibraryAction.OnToggleSelectionMode) }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = stringResource(R.string.library_select_books)
                                )
                            }
                        }
                        IconButton(onClick = { onAction(LibraryAction.OnToggleTidyMode) }) {
                            Icon(
                                imageVector = if (state.isTidyMode) {
                                    ImageVector.vectorResource(R.drawable.ic_untidy_books)
                                } else {
                                    ImageVector.vectorResource(R.drawable.ic_tidy_books)
                                },
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!state.isSearchDialogVisible && !state.isSelectionMode) {
                FloatingActionButton(
                    onClick = { onAction(LibraryAction.OnSearchClick) }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.library_search_books)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (state.isSelectionMode && state.deletableBooks.isEmpty()) {
            // Empty deletable state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.library_no_deletable_books),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onAction(LibraryAction.OnToggleSelectionMode) }
                ) {
                    Text(stringResource(R.string.library_exit_selection))
                }
            }
        } else if (!state.isLoading && state.allBooks.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.library_empty_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.library_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val displayBooks = if (state.isSelectionMode) {
                state.deletableBooks
            } else {
                state.filteredBooks
            }

            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                if (!state.isSelectionMode) {
                    // Search field
                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { onAction(LibraryAction.OnSearchQueryChange(it)) },
                            shape = RoundedCornerShape(100),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { Text(stringResource(R.string.library_search_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                                )
                            },
                            trailingIcon = {
                                AnimatedVisibility(visible = state.searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            onAction(LibraryAction.OnSearchQueryChange(""))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(
                                                R.string.cd_clear_search
                                            ),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(
                                    shape = RoundedCornerShape(100),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                                .minimumInteractiveComponentSize()
                        )
                    }

                    // Sort chips
                    item {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LibrarySortOption.entries.forEach { option ->
                                FilterChip(
                                    selected = state.sortOption == option,
                                    onClick = {
                                        onAction(LibraryAction.OnSortOptionSelected(option))
                                    },
                                    label = { Text(stringResource(option.labelResId)) }
                                )
                            }
                        }
                    }

                    // Reading status filter chips
                    item {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedReadingStatus == null,
                                onClick = {
                                    onAction(LibraryAction.OnReadingStatusSelected(null))
                                },
                                label = { Text(stringResource(R.string.filter_all_status)) }
                            )
                            ReadingStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = state.selectedReadingStatus == status,
                                    onClick = {
                                        onAction(LibraryAction.OnReadingStatusSelected(status))
                                    },
                                    label = { Text(status.toDisplayString()) }
                                )
                            }
                        }
                    }
                }

                // Book count
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val count = displayBooks.size
                        Text(
                            text = pluralStringResource(
                                R.plurals.library_book_count,
                                count,
                                count
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Book rows
                val rows = calculateBookRows(
                    books = displayBooks,
                    availableWidthDp = availableWidth,
                    isTidyMode = state.isTidyMode
                )

                rows.forEach { rowData ->
                    item(key = rowData.books.first().id) {
                        BookRowDynamic(
                            books = rowData.books,
                            onBookClick = { book ->
                                if (state.isSelectionMode) {
                                    onAction(LibraryAction.OnToggleBookSelection(book.id))
                                } else {
                                    onAction(LibraryAction.OnBookClick(book))
                                }
                            },
                            bookshelfMaterial = ShelfMaterial.DarkWood,
                            config = BookRowConfig(
                                showAddSlot = false,
                                isTidyMode = state.isTidyMode,
                                bookStyles = rowData.styles,
                                isSelectionMode = state.isSelectionMode,
                                selectedBookIds = state.selectedBookIds
                            )
                        )
                    }
                }
            }
        }
    }

    // Remote search dialog
    if (state.isSearchDialogVisible) {
        LibraryBookSearchDialog(
            state = state.bookSearchState,
            onQueryChange = { onAction(LibraryAction.OnRemoteSearchQueryChange(it)) },
            onToggleSearchByTitle = { onAction(LibraryAction.OnToggleSearchByTitle) },
            onToggleSearchByAuthor = { onAction(LibraryAction.OnToggleSearchByAuthor) },
            onBookClick = { book -> onAction(LibraryAction.OnSearchResultBookClick(book)) },
            onAddBook = { book -> onAction(LibraryAction.OnAddBookToLibrary(book)) },
            onDismiss = { onAction(LibraryAction.OnDismissSearchDialog) }
        )
    }

    // Delete confirmation dialog
    if (state.showDeleteConfirmation) {
        DeleteBooksConfirmationDialog(
            bookCount = state.selectedBookIds.size,
            onConfirm = { onAction(LibraryAction.OnConfirmDelete) },
            onDismiss = { onAction(LibraryAction.OnDismissDeleteDialog) }
        )
    }
}
