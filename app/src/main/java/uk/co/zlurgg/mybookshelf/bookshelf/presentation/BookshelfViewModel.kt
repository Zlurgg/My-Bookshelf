package uk.co.zlurgg.mybookshelf.bookshelf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.GetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess

class BookshelfViewModel(
    private val bookshelfUseCases: BookshelfUseCases,
    private val getShelfById: GetShelfByIdUseCase,
    private val bookClubOperations: ClubOperations,
    private val checkSignInStatus: CheckSignInStatusUseCase,
    private val shelfId: String
) : ViewModel() {

    companion object {
        private const val TAG = "BookshelfViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val MIN_SEARCH_QUERY_LENGTH = 2
    }

    // Initialize state flow with default value
    private val _state = MutableStateFlow(
        BookshelfState(
            shelfId = shelfId,
            isTutorialShelf = shelfId == SystemOwnerIds.TUTORIAL_SHELF_ID
        )
    )
    val state: StateFlow<BookshelfState> = _state.asStateFlow()

    // SharedFlow so that re-emitting the same query (e.g. after filter toggle) is not conflated.
    private val queryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)

    init {
        observeDebouncedQuery()
        loadBooks()
        loadShelfDetails()
        checkSignInStatus()
    }

    private fun checkSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = checkSignInStatus.invoke()
            _state.update { it.copy(isSignedIn = isSignedIn) }
        }
    }

    fun onAction(action: BookshelfAction) {
        when (action) {
            is BookshelfAction.OnSearchClick -> {
                _state.update { it.copy(isSearchDialogVisible = true) }
            }
            is BookshelfAction.OnDismissSearchDialog -> {
                _state.update { it.closeSearchDialog() }
                // Reset query to cancel any pending search
                queryFlow.tryEmit("")
            }
            is BookshelfAction.OnBookClick -> {
                // Persist clicked book so details screen can load it by ID safely
                viewModelScope.launch {
                    when (val cacheResult = bookshelfUseCases.upsertBook(action.book)) {
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
                    when (val removeResult = bookshelfUseCases.removeBookFromShelf(action.book.id, shelfId)) {
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
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            query = action.query,
                            isTyping = action.query.trim().length >= MIN_SEARCH_QUERY_LENGTH
                        )
                    )
                }
                queryFlow.tryEmit(action.query)
            }
            BookshelfAction.OnToggleTidyMode -> {
                val newTidyMode = !_state.value.isTidyMode
                _state.update { it.copy(isTidyMode = newTidyMode) }
                persistTidyMode(newTidyMode)
            }
            BookshelfAction.OnToggleSearchByTitle -> {
                val current = _state.value.bookSearchState
                if (!current.canToggleTitle) return@onAction

                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchByTitle = !it.bookSearchState.searchByTitle
                        )
                    )
                }

                // Re-trigger search via debounced flow for consistency
                val currentQuery = _state.value.bookSearchState.query
                if (currentQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
                    queryFlow.tryEmit(currentQuery)
                }
            }
            BookshelfAction.OnToggleSearchByAuthor -> {
                val current = _state.value.bookSearchState
                if (!current.canToggleAuthor) return@onAction

                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchByAuthor = !it.bookSearchState.searchByAuthor
                        )
                    )
                }

                // Re-trigger search via debounced flow for consistency
                val currentQuery = _state.value.bookSearchState.query
                if (currentQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
                    queryFlow.tryEmit(currentQuery)
                }
            }
            // Navigation actions handled by the UI layer
            BookshelfAction.OnBackClick,
            BookshelfAction.OnCreateBookClub -> Unit
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            bookshelfUseCases.getShelfBooks(shelfId).collect { books ->
                _state.update {
                    it.copy(
                        books = books,
                        isLoading = false,
                        // existingBookIds may lag book mutations by one Flow emission
                        bookSearchState = it.bookSearchState.copy(
                            existingBookIds = books.map { book -> book.id }.toSet()
                        )
                    )
                }
            }
        }
    }

    private fun loadShelfDetails() {
        viewModelScope.launch {
            Timber.tag(TAG).d("Loading shelf details for: %s", shelfId)
            when (val result = getShelfById(shelfId)) {
                is Result.Success -> {
                    result.data?.let { shelf ->
                        Timber.tag(TAG).d(
                            "Shelf loaded: name=%s, isBookClub=%s, clubCode=%s",
                            shelf.name,
                            shelf.isBookClub,
                            shelf.clubCode
                        )
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
                            Timber.tag(TAG).d("Triggering book club sync for: %s", shelf.clubCode)
                            syncBookClubBooks(shelf.clubCode, shelfId)
                        } else {
                            Timber.tag(TAG).d("Not a book club, skipping sync")
                        }
                    }
                }
                is Result.Error -> {
                    Timber.tag(TAG).e("Failed to load shelf: %s", result.error)
                    _state.update { it.withError(result.error, "load shelf details") }
                }
            }
        }
    }

    private fun syncBookClubBooks(clubCode: String, shelfId: String) {
        viewModelScope.launch {
            when (val syncResult = bookClubOperations.syncBooksFromClub(clubCode, shelfId)) {
                // No state update needed — the book list refreshes via Flow collection
                is Result.Success -> Unit
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            errorMessage = ErrorFormatter.formatDataErrorMessage(syncResult.error, "sync book club")
                        )
                    }
                }
            }
        }
    }

    private fun persistTidyMode(isTidyMode: Boolean) {
        viewModelScope.launch {
            bookshelfUseCases.updateShelfTidyMode(shelfId, isTidyMode)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDebouncedQuery() {
        viewModelScope.launch {
            // queryFlow is a SharedFlow (not StateFlow) so filter toggles can re-emit the
            // same query string. distinctUntilChanged is omitted for the same reason.
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { raw ->
                    val query = raw.trim()

                    if (query.length < MIN_SEARCH_QUERY_LENGTH) {
                        _state.update {
                            it.copy(
                                bookSearchState = it.bookSearchState.copy(
                                    isLoading = false,
                                    isTyping = false,
                                    errorMessage = null,
                                    results = if (query.isEmpty()) emptyList() else it.bookSearchState.results
                                )
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
            when (val addResult = bookshelfUseCases.addBookToShelf(book, shelfId)) {
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
        _state.update {
            it.copy(
                bookSearchState = it.bookSearchState.copy(
                    isLoading = true,
                    isTyping = false,
                    errorMessage = null
                )
            )
        }

        val searchState = _state.value.bookSearchState

        // Map checkbox states to OpenLibrary API parameters:
        // - Both checked (default) → general q= parameter
        // - Only title checked → title= parameter
        // - Only author checked → author= parameter
        val (generalQuery, titleQuery, authorQuery) = when {
            searchState.searchByTitle && searchState.searchByAuthor -> Triple(query, null, null)
            searchState.searchByTitle -> Triple(null, query, null)
            searchState.searchByAuthor -> Triple(null, null, query)
            else -> {
                Timber.w("Unexpected: no search filter checked, falling back to general search")
                Triple(query, null, null)
            }
        }

        bookshelfUseCases.searchBooks(
            query = generalQuery ?: "",
            resultLimit = 15, // First 15 results for performance
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

    // ============================================================================
    // State Update Helpers (Private Extensions)
    // ============================================================================

    private fun BookshelfState.withError(error: DataError, operation: String): BookshelfState {
        return copy(
            isLoading = false,
            bookSearchState = bookSearchState.copy(isLoading = false),
            errorMessage = ErrorFormatter.formatDataErrorMessage(error, operation)
        )
    }

    private fun BookshelfState.withSearchResults(results: List<Book>): BookshelfState {
        return copy(
            bookSearchState = bookSearchState.copy(
                isLoading = false,
                hasSearched = true,
                errorMessage = null,
                results = results
            )
        )
    }

    private fun BookshelfState.withSearchError(error: DataError): BookshelfState {
        return copy(
            bookSearchState = bookSearchState.copy(
                // results intentionally preserved — error banner is sufficient feedback
                isLoading = false,
                hasSearched = true,
                errorMessage = ErrorFormatter.formatDataErrorMessage(error, "search books")
            )
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
            bookSearchState = BookSearchState(
                existingBookIds = bookSearchState.existingBookIds
            )
        )
    }
}
