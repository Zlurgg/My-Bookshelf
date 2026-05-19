package uk.co.zlurgg.mybookshelf.bookshelf.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.book.presentation.components.BookRowConfig
import uk.co.zlurgg.mybookshelf.book.presentation.components.BookRowDynamic
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.searchcomponents.BookSearchCallbacks
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.searchcomponents.ShelfBookSearchDialog
import uk.co.zlurgg.mybookshelf.auth.presentation.components.SignInRequiredDialog
import uk.co.zlurgg.mybookshelf.book.presentation.preview.sampleBooks
import uk.co.zlurgg.mybookshelf.book.presentation.util.ADD_SLOT_RESERVED_WIDTH
import uk.co.zlurgg.mybookshelf.book.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.book.presentation.util.calculateBookRows

@Composable
fun BookshelfScreenRoot(
    viewModel: BookshelfViewModel = koinViewModel(),
    onAddBookClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBackClick: () -> Unit,
    onCreateBookClub: () -> Unit = {},
    onSignIn: () -> Unit,
    shelfName: String? = null,
    shelfMaterial: ShelfMaterial? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val uiState = state.copy(
        shelfName = shelfName ?: state.shelfName,
        shelfMaterial = shelfMaterial ?: state.shelfMaterial
    )

    BookshelfScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                is BookshelfAction.OnBookClick -> onBookClick(action.book)
                is BookshelfAction.OnAddBookClick -> onAddBookClick(action.book)
                is BookshelfAction.OnBackClick -> onBackClick()
                BookshelfAction.OnCreateBookClub -> onCreateBookClub()
                else -> viewModel.onAction(action)
            }
        },
        onSignIn = onSignIn,
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    state: BookshelfState,
    onAction: (BookshelfAction) -> Unit,
    onSignIn: () -> Unit = {},
) {
    var showCreateBookClubDialog by remember { mutableStateOf(false) }
    var showSignInRequiredDialog by remember { mutableStateOf(false) }

    // Use books in their original order (no forced sorting)
    val books = state.books
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val availableWidth = screenWidth - 24.dp - 16.dp // margins and padding

    // Add slot visibility: hidden for tutorial, hidden at capacity, auth-gated on club shelves
    val showAddSlot = !state.isTutorialShelf &&
        books.size < BookshelfConstants.MAX_BOOKS_PER_SHELF &&
        (!state.isBookClub || state.isSignedIn)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.shelfName.ifBlank { stringResource(id = R.string.app_name) },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (state.isBookClub) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.bookshelf_book_club_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(BookshelfAction.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_close)
                        )
                    }
                },
                actions = {
                    if (books.isNotEmpty()) {
                        IconButton(onClick = { onAction(BookshelfAction.OnToggleTidyMode) }) {
                            Icon(
                                imageVector = if (state.isTidyMode) {
                                    ImageVector.vectorResource(
                                        R.drawable.ic_untidy_books
                                    )
                                } else {
                                    ImageVector.vectorResource(R.drawable.ic_tidy_books)
                                },
                                contentDescription = if (state.isTidyMode) {
                                    stringResource(R.string.cd_switch_to_natural_arrangement)
                                } else {
                                    stringResource(R.string.cd_tidy_shelf)
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Create Book Club FAB: hidden for tutorial shelves, guests, and on club shelves
            if (!state.isTutorialShelf && !state.isBookClub && state.isSignedIn) {
                FloatingActionButton(
                    onClick = { showCreateBookClubDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = stringResource(R.string.cd_create_book_club)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (!state.isLoading && books.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Book count counter (hidden for tutorial shelf)
                if (!state.isTutorialShelf) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(
                                R.string.shelf_book_count,
                                books.size,
                                BookshelfConstants.MAX_BOOKS_PER_SHELF
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                BookRowDynamic(
                    books = emptyList(),
                    onBookClick = { /* no-op */ },
                    bookshelfMaterial = state.shelfMaterial,
                    config = BookRowConfig(
                        showAddSlot = showAddSlot,
                        isTidyMode = state.isTidyMode,
                        onAddClick = { onAction(BookshelfAction.OnSearchClick) }
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.bookshelf_empty_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.bookshelf_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = paddingValues) {
                // Book count counter (hidden for tutorial shelf)
                if (!state.isTutorialShelf) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.shelf_book_count,
                                    books.size,
                                    BookshelfConstants.MAX_BOOKS_PER_SHELF
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val rows = calculateBookRows(
                    books = books,
                    availableWidthDp = availableWidth.value,
                    isTidyMode = state.isTidyMode,
                    reservedLeadingWidthDp = if (showAddSlot) ADD_SLOT_RESERVED_WIDTH else 0f
                )

                rows.forEachIndexed { index, rowData ->
                    item(key = rowData.books.first().id) {
                        val isFirstRow = index == 0
                        BookRowDynamic(
                            books = rowData.books,
                            onBookClick = { book -> onAction(BookshelfAction.OnBookClick(book)) },
                            bookshelfMaterial = state.shelfMaterial,
                            config = BookRowConfig(
                                showAddSlot = showAddSlot && isFirstRow,
                                isTidyMode = state.isTidyMode,
                                bookStyles = rowData.styles,
                                onAddClick = if (isFirstRow) {
                                    { onAction(BookshelfAction.OnSearchClick) }
                                } else {
                                    null
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    // Create Book Club confirmation dialog
    if (showCreateBookClubDialog) {
        AlertDialog(
            onDismissRequest = { showCreateBookClubDialog = false },
            title = { Text(stringResource(R.string.create_book_club_title)) },
            text = { Text(stringResource(R.string.create_book_club_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateBookClubDialog = false
                        onAction(BookshelfAction.OnCreateBookClub)
                    }
                ) {
                    Text(stringResource(R.string.create_book_club_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBookClubDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Sign-in required dialog (shown when guest user tries to create a book club)
    if (showSignInRequiredDialog) {
        SignInRequiredDialog(
            title = stringResource(R.string.sign_in_required_book_clubs_title),
            message = stringResource(R.string.sign_in_required_book_clubs_message),
            onSignIn = {
                showSignInRequiredDialog = false
                onSignIn()
            },
            onDismiss = { showSignInRequiredDialog = false }
        )
    }

    // Search dialog
    if (state.isSearchDialogVisible) {
        ShelfBookSearchDialog(
            state = state.bookSearchState.copy(
                existingBookIds = state.books.map { it.id }.toSet()
            ),
            callbacks = object : BookSearchCallbacks {
                override val onQueryChange: (String) -> Unit = { query ->
                    onAction(BookshelfAction.OnSearchQueryChange(query))
                }
                override val onToggleSearchByTitle: () -> Unit = {
                    onAction(BookshelfAction.OnToggleSearchByTitle)
                }
                override val onToggleSearchByAuthor: () -> Unit = {
                    onAction(BookshelfAction.OnToggleSearchByAuthor)
                }
                override val onAddBook: (Book) -> Unit = { book ->
                    onAction(BookshelfAction.OnAddBookClick(book))
                    // Keep dialog open for bulk adding (e.g., multiple books from same series)
                }
                override val onRemoveBook: (Book) -> Unit = { book ->
                    onAction(BookshelfAction.OnRemoveBook(book))
                    // Keep dialog open for bulk removing
                }
                override val onBookClick: (Book) -> Unit = { book ->
                    onAction(BookshelfAction.OnBookClick(book))
                }
                override val onDismiss: () -> Unit = {
                    onAction(BookshelfAction.OnDismissSearchDialog)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookshelfScreenPreview() {
    BookshelfScreen(
        state = BookshelfState(
            books = sampleBooks,
            shelfId = "1",
            shelfName = "Fiction"
        ),
        onAction = {},
    )
}
