package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components.BookshelfRowDynamic
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components.BookSearchDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util.BookDisplayStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util.getBookDisplayStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util.getBookWidth
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.core.presentation.sampleBooks

@Composable
fun BookshelfScreenRoot(
    viewModel: BookshelfViewModel = koinViewModel(),
    onAddBookClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBackClick: () -> Unit,
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
                else -> viewModel.onAction(action)
            }
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    state: BookshelfState,
    onAction: (BookshelfAction) -> Unit,
) {
    // Use books in their original order (no forced sorting)
    val books = state.books
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val availableWidth = screenWidth - 24.dp - 16.dp // margins and padding

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.shelfName.ifBlank { stringResource(id = R.string.app_name) }, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { onAction(BookshelfAction.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_close)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(BookshelfAction.OnToggleTidyMode) }) {
                        Icon(
                            imageVector = if (state.isTidyMode) Icons.Filled.Star else Icons.Filled.Menu,
                            contentDescription = if (state.isTidyMode) "Switch to natural arrangement" else "Tidy shelf"
                        )
                    }
                    IconButton(onClick = { onAction(BookshelfAction.OnSearchClick) }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(id = R.string.search_hint)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!state.isLoading && books.isEmpty()) {
            LazyColumn(contentPadding = paddingValues) {
                item {
                    Text(
                        text = stringResource(id = R.string.bookshelf_empty_state_hint),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                item {
                    BookshelfRowDynamic(
                        books = emptyList(),
                        onBookClick = { /* no-op */ },
                        bookshelfMaterial = state.shelfMaterial,
                        showAddSlot = true,
                        onAddClick = { onAction(BookshelfAction.OnSearchClick) },
                        isTidyMode = state.isTidyMode
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = paddingValues) {
                var bookIndex = 0
                
                while (bookIndex < books.size) {
                    // Calculate how many books fit in a row based on their individual styles
                    var currentRowWidth = 0f
                    var booksInRow = 0
                    val addButtonWidth = 60f + 6f // add button space if this is the last row
                    val rowBookStyles = mutableListOf<BookDisplayStyle>()
                    
                    // First pass: determine how many books fit using simpler non-position-dependent styling
                    while (bookIndex + booksInRow < books.size) {
                        val book = books[bookIndex + booksInRow]
                        val tempIsLast = (bookIndex + booksInRow + 1) >= books.size
                        
                        // Use basic style for width estimation (avoids circular dependency)
                        val bookStyle = if (state.isTidyMode) {
                            BookDisplayStyle.VERTICAL
                        } else {
                            getBookDisplayStyle(book) // Use simple hash-based style
                        }
                        
                        val bookWidth = getBookWidth(book, bookStyle) + 6f // width + spacing
                        val potentialRowWidth = currentRowWidth + bookWidth
                        val totalNeededWidth = potentialRowWidth + if (tempIsLast) addButtonWidth else 0f
                        
                        if (totalNeededWidth <= availableWidth.value) {
                            currentRowWidth = potentialRowWidth
                            booksInRow++
                        } else {
                            break
                        }
                    }
                    
                    // Ensure at least one book per row
                    if (booksInRow == 0) booksInRow = 1
                    
                    // Second pass: apply position-aware styling with consistent parameters
                    val endIndex = minOf(bookIndex + booksInRow, books.size)
                    val rowBooks = books.subList(bookIndex, endIndex)
                    val isLastRow = endIndex >= books.size
                    val totalAvailableWidth = availableWidth.value - if (isLastRow) addButtonWidth else 0f
                    
                    // Apply final styling with proper position context
                    rowBooks.forEachIndexed { index, book ->
                        val bookStyle = if (state.isTidyMode) {
                            BookDisplayStyle.VERTICAL
                        } else {
                            // Start with base style from first pass
                            val baseStyle = getBookDisplayStyle(book)
                            // Apply position-based refinements
                            when {
                                // First book in row: can't lean left (no support)
                                index == 0 && baseStyle == BookDisplayStyle.LEANING_LEFT -> 
                                    BookDisplayStyle.VERTICAL
                                
                                // Last book in row: check if there's enough space for right lean
                                index == rowBooks.size - 1 && baseStyle == BookDisplayStyle.LEANING_RIGHT -> {
                                    val widthSoFar = rowBookStyles.mapIndexed { styleIndex, style ->
                                        getBookWidth(rowBooks[styleIndex], style) + 6f
                                    }.sum()
                                    val remainingSpace = totalAvailableWidth - widthSoFar
                                    if (remainingSpace > 30f) BookDisplayStyle.VERTICAL else baseStyle
                                }
                                
                                // All other cases: use base style
                                else -> baseStyle
                            }
                        }
                        rowBookStyles.add(bookStyle)
                    }
                    
                    item(key = rowBooks.first().id) {
                        BookshelfRowDynamic(
                            books = rowBooks,
                            bookStyles = rowBookStyles,
                            onBookClick = { book -> onAction(BookshelfAction.OnBookClick(book)) },
                            bookshelfMaterial = state.shelfMaterial,
                            showAddSlot = isLastRow,
                            onAddClick = { onAction(BookshelfAction.OnSearchClick) },
                            isTidyMode = state.isTidyMode
                        )
                    }
                    
                    bookIndex = endIndex
                }
            }
        }
    }

    // Search dialog
    if (state.isSearchDialogVisible) {
        BookSearchDialog(
            query = state.searchQuery,
            onQueryChange = { onAction(BookshelfAction.OnSearchQueryChange(it)) },
            results = state.searchResults,
            isLoading = state.isSearchLoading,
            inShelfIds = state.books.map { it.id }.toSet(),
            onAddBook = { book ->
                onAction(BookshelfAction.OnAddBookClick(book))
                onAction(BookshelfAction.OnDismissSearchDialog)
            },
            onRemoveBook = { book ->
                onAction(BookshelfAction.OnRemoveBook(book))
                onAction(BookshelfAction.OnDismissSearchDialog)
            },
            onBookClick = { book -> onAction(BookshelfAction.OnBookClick(book)) },
            onDismiss = {
                onAction(BookshelfAction.OnDismissSearchDialog)
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