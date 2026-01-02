package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.handlers.BookClubOperationsHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess

class BookshelfViewModel(
    private val bookshelfUseCases: BookshelfUseCases,
    private val bookcaseUseCases: BookcaseUseCases,
    private val bookClubOperations: BookClubOperationsHandler,
    private val shelfId: String
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 250L  // Reduced from 450ms for faster perceived response
        private const val MIN_SEARCH_QUERY_LENGTH = 2
    }

    // Initialize state flow with default value
    private val _state = MutableStateFlow(BookshelfState(
        shelfId = shelfId,
        isTutorialShelf = shelfId == SystemOwnerIds.TUTORIAL_SHELF_ID
    ))
    val state: StateFlow<BookshelfState> = _state.asStateFlow()

    // Debounced query flow
    private val queryFlow = MutableStateFlow("")

    init {
        observeDebouncedQuery()
        loadBooks()
        loadShelfDetails()
    }

    fun onAction(action: BookshelfAction) {
        when (action) {
            is BookshelfAction.OnSearchClick -> {
                _state.update { it.copy(isSearchDialogVisible = true) }
            }
            is BookshelfAction.OnDismissSearchDialog -> {
                _state.update { it.closeSearchDialog() }
                // Reset query to cancel any pending search
                queryFlow.value = ""
            }
            is BookshelfAction.OnBookClick -> {
                // Persist clicked book so details screen can load it by ID safely
                viewModelScope.launch {
                    when (val cacheResult = bookshelfUseCases.upsertBook.execute(action.book)) {
                        is Result.Success -> {
                            // Success - book cached successfully
                        }
                        is Result.Error -> {
                            _state.update { it.withError(cacheResult.error, "cache book") }
                        }
                    }
                }
            }
            is BookshelfAction.OnAddBookClick -> {
                addBookToShelf(action.book)
            }
            is BookshelfAction.OnRemoveBook -> {
                // Optimistic UI update
                _state.update { it.withBookRemoved(action.book) }

                viewModelScope.launch {
                    when (val removeResult = bookshelfUseCases.removeBookFromShelf.execute(action.book.id, shelfId)) {
                        is Result.Success -> {
                            // Success - optimistic update already applied
                        }
                        is Result.Error -> {
                            _state.update { it.withError(removeResult.error, "remove book from shelf") }
                        }
                    }
                }
            }
            BookshelfAction.OnUndoRemove -> {
                _state.update { it.withBookRestored() }
            }
            is BookshelfAction.OnSearchQueryChange -> {
                // Update UI immediately with typing indicator; defer actual search via debounce
                _state.update { it.copy(
                    searchQuery = action.query,
                    isTyping = action.query.trim().length >= MIN_SEARCH_QUERY_LENGTH
                ) }
                queryFlow.value = action.query
            }
            BookshelfAction.OnToggleTidyMode -> {
                val newTidyMode = !_state.value.isTidyMode
                _state.update { it.copy(isTidyMode = newTidyMode) }
                persistTidyMode(newTidyMode)
            }
            BookshelfAction.OnShareShelf -> {
                shareShelf()
            }
            BookshelfAction.OnToggleSearchByTitle -> {
                _state.update { it.copy(searchByTitle = !it.searchByTitle) }

                // Re-trigger search via debounced flow for consistency
                val currentQuery = _state.value.searchQuery
                if (currentQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
                    queryFlow.value = currentQuery  // Triggers debounced search
                }
            }
            BookshelfAction.OnToggleSearchByAuthor -> {
                _state.update { it.copy(searchByAuthor = !it.searchByAuthor) }

                // Re-trigger search via debounced flow for consistency
                val currentQuery = _state.value.searchQuery
                if (currentQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
                    queryFlow.value = currentQuery  // Triggers debounced search
                }
            }

            // Book Club Actions
            BookshelfAction.OnShowShareOptions -> {
                _state.update { it.copy(isShareOptionsVisible = true) }
            }
            BookshelfAction.OnDismissShareOptions -> {
                _state.update { it.copy(isShareOptionsVisible = false) }
            }
            BookshelfAction.OnShareCopy -> {
                _state.update { it.copy(isShareOptionsVisible = false) }
                shareShelf()
            }
            BookshelfAction.OnCreateBookClub -> {
                _state.update { it.copy(isShareOptionsVisible = false) }
                createBookClub()
            }
            BookshelfAction.OnDismissInviteLink -> {
                _state.update { it.copy(bookClubInviteLink = null, bookClubCode = null) }
            }
            BookshelfAction.OnCopyInviteLink -> {
                // Copy is handled by the UI, just dismiss
                _state.update { it.copy(bookClubInviteLink = null, bookClubCode = null) }
            }
            else -> Unit
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            bookshelfUseCases.getShelfBooks.execute(shelfId).collect { books ->
                _state.update { it.copy(books = books, isLoading = false) }
            }
        }
    }

    private fun loadShelfDetails() {
        viewModelScope.launch {
            when (val result = bookcaseUseCases.getShelfById.execute(shelfId)) {
                is Result.Success -> {
                    result.data?.let { shelf ->
                        _state.update {
                            it.copy(
                                shelfName = shelf.name,
                                shelfMaterial = ShelfMaterial.fromShelfStyle(shelf.shelfStyle),
                                isTidyMode = shelf.isTidyMode,
                                isBookClub = shelf.isBookClub,
                                clubCode = shelf.clubCode
                            )
                        }
                        // If this is a book club, sync books from Firestore
                        if (shelf.isBookClub && !shelf.clubCode.isNullOrEmpty()) {
                            syncBookClubBooks(shelf.clubCode, shelfId)
                        }
                    }
                }
                is Result.Error -> {
                    _state.update { it.withError(result.error, "load shelf details") }
                }
            }
        }
    }

    private fun syncBookClubBooks(clubCode: String, shelfId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            when (val syncResult = bookClubOperations.syncBooksFromClub(clubCode, shelfId)) {
                is Result.Success -> {
                    val result = syncResult.data
                    _state.update { it.copy(
                        isSyncing = false,
                        syncMessage = if (result.booksAdded > 0 || result.booksRemoved > 0) {
                            "Synced: +${result.booksAdded} / -${result.booksRemoved} books"
                        } else null
                    ) }
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        isSyncing = false,
                        errorMessage = ErrorFormatter.formatDataErrorMessage(syncResult.error, "sync book club")
                    ) }
                }
            }
        }
    }

    private fun persistTidyMode(isTidyMode: Boolean) {
        viewModelScope.launch {
            bookshelfUseCases.updateShelfTidyMode.execute(shelfId, isTidyMode)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDebouncedQuery() {
        viewModelScope.launch {
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { raw ->
                    val query = raw.trim()

                    if (query.length < MIN_SEARCH_QUERY_LENGTH) {
                        _state.update {
                            it.copy(
                                isSearchLoading = false,
                                isTyping = false,
                                errorMessage = null,
                                searchResults = if (query.isEmpty()) emptyList() else it.searchResults
                            )
                        }
                        return@collectLatest
                    }

                    // Perform search directly in collectLatest so it can be cancelled
                    performSearch(query)
                }
        }
    }

    private fun addBookToShelf(book: Book) {
        viewModelScope.launch {
            when (val addResult = bookshelfUseCases.addBookToShelf.execute(book, shelfId)) {
                is Result.Success -> {
                    // Success - book added successfully
                }
                is Result.Error -> {
                    _state.update { it.withError(addResult.error, "add book to shelf") }
                }
            }
        }
    }

    private suspend fun performSearch(query: String) {
        // Execute search directly (not in a new coroutine)
        // This allows collectLatest to cancel in-flight searches
        _state.update { it.copy(
            isSearchLoading = true,
            isTyping = false,  // Debounce period complete, now actively searching
            errorMessage = null
        ) }

        val currentState = _state.value

        // Map checkbox states to OpenLibrary API parameters:
        // - Both checked OR both unchecked → use general q= parameter (smart search)
        // - Only title checked → use title= parameter
        // - Only author checked → use author= parameter
        val (generalQuery, titleQuery, authorQuery) = when {
            currentState.searchByTitle && currentState.searchByAuthor -> Triple(query, null, null)
            !currentState.searchByTitle && !currentState.searchByAuthor -> Triple(query, null, null)
            currentState.searchByTitle && !currentState.searchByAuthor -> Triple(null, query, null)
            // Fallback (should never happen)
            else -> Triple(null, null, query)
        }

        bookshelfUseCases.searchBooks
            .execute(
                query = generalQuery ?: "",
                resultLimit = 15,  // First 15 results for performance
                language = null,
                authorFilter = authorQuery,
                titleFilter = titleQuery
            )
            .onSuccess { searchResults ->
                _state.update { it.withSearchResults(searchResults) }
            }
            .onError { error ->
                _state.update { it.withSearchError(error) }
            }
    }

    private fun shareShelf() {
        viewModelScope.launch {
            _state.update { it.copy(isShareLoading = true, errorMessage = null) }

            when (val shareResult = bookshelfUseCases.shareBookshelf.execute(shelfId)) {
                is Result.Success -> {
                    // Share sheet opened successfully - no success dialog needed
                    _state.update { it.copy(isShareLoading = false) }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isShareLoading = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(shareResult.error, "share bookshelf")
                        )
                    }
                }
            }
        }
    }

    private fun createBookClub() {
        viewModelScope.launch {
            _state.update { it.copy(isCreatingBookClub = true, errorMessage = null) }

            when (val createResult = bookClubOperations.createBookClub(shelfId, _state.value.shelfName)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isCreatingBookClub = false,
                            bookClubCode = createResult.data.clubCode,
                            bookClubInviteLink = createResult.data.inviteLink
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isCreatingBookClub = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(createResult.error, "create book club")
                        )
                    }
                }
            }
        }
    }

    // ============================================================================
    // State Update Helpers (Private Extensions)
    // ============================================================================

    private fun BookshelfState.withError(error: DataError, operation: String): BookshelfState {
        return copy(
            isLoading = false,
            isSearchLoading = false,
            errorMessage = ErrorFormatter.formatDataErrorMessage(error, operation)
        )
    }

    private fun BookshelfState.withSearchResults(results: List<Book>): BookshelfState {
        return copy(
            isSearchLoading = false,
            hasSearched = true,
            errorMessage = null,
            searchResults = results
        )
    }

    private fun BookshelfState.withSearchError(error: DataError): BookshelfState {
        return copy(
            searchResults = emptyList(),
            isSearchLoading = false,
            hasSearched = true,
            errorMessage = ErrorFormatter.formatDataErrorMessage(error, "perform search")
        )
    }

    private fun BookshelfState.withBookRemoved(book: Book): BookshelfState {
        return copy(
            books = books.filterNot { it.id == book.id },
            recentlyDeleted = book
        )
    }

    private fun BookshelfState.withBookRestored(): BookshelfState {
        return recentlyDeleted?.let {
            copy(books = books + it, recentlyDeleted = null)
        } ?: this
    }

    private fun BookshelfState.closeSearchDialog(): BookshelfState {
        return copy(
            isSearchDialogVisible = false,
            searchQuery = "",
            searchResults = emptyList(),
            isSearchLoading = false,
            isTyping = false,
            hasSearched = false,
            searchByTitle = true,
            searchByAuthor = true
        )
    }
}
