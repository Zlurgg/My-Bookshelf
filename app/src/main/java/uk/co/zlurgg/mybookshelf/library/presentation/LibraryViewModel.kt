package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess
import uk.co.zlurgg.mybookshelf.library.domain.usecase.LibraryUseCases

class LibraryViewModel(
    private val libraryUseCases: LibraryUseCases,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    companion object {
        private const val TAG = "LibraryVM"
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val MIN_SEARCH_QUERY_LENGTH = 2
        private val TIDY_MODE_KEY = booleanPreferencesKey("library_tidy_mode")
    }

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val localQueryFlow = MutableStateFlow("")
    private val remoteQueryFlow = MutableStateFlow("")

    init {
        loadTidyMode()
        observeBooks()
        observeNonRemovableBookIds()
        observeDebouncedLocalQuery()
        observeDebouncedRemoteQuery()
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = action.query) }
                localQueryFlow.value = action.query
            }
            is LibraryAction.OnSortOptionSelected -> {
                _state.update { it.copy(sortOption = action.option) }
                applyFilters()
            }
            is LibraryAction.OnReadingStatusSelected -> {
                _state.update { it.copy(selectedReadingStatus = action.status) }
                applyFilters()
            }
            is LibraryAction.OnBookClick -> { /* handled by navigation callback */ }
            is LibraryAction.OnToggleTidyMode -> {
                val newMode = !_state.value.isTidyMode
                _state.update { it.copy(isTidyMode = newMode) }
                viewModelScope.launch {
                    dataStore.edit { it[TIDY_MODE_KEY] = newMode }
                }
            }

            // Remote search dialog
            is LibraryAction.OnSearchClick -> {
                _state.update { it.copy(isSearchDialogVisible = true) }
            }
            is LibraryAction.OnDismissSearchDialog -> {
                _state.update {
                    it.copy(
                        isSearchDialogVisible = false,
                        bookSearchState = BookSearchState(
                            existingBookIds = it.bookSearchState.existingBookIds
                        )
                    )
                }
                remoteQueryFlow.value = ""
            }
            is LibraryAction.OnRemoteSearchQueryChange -> {
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            query = action.query,
                            isTyping = action.query.trim().length >= MIN_SEARCH_QUERY_LENGTH
                        )
                    )
                }
                remoteQueryFlow.value = action.query
            }
            is LibraryAction.OnToggleSearchByTitle -> {
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchByTitle = !it.bookSearchState.searchByTitle
                        )
                    )
                }
                retriggerRemoteSearchIfNeeded()
            }
            is LibraryAction.OnToggleSearchByAuthor -> {
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchByAuthor = !it.bookSearchState.searchByAuthor
                        )
                    )
                }
                retriggerRemoteSearchIfNeeded()
            }
            is LibraryAction.OnAddBookToLibrary -> {
                addBookToLibrary(action.book)
            }
            is LibraryAction.OnSearchResultBookClick -> {
                viewModelScope.launch {
                    when (val cacheResult = libraryUseCases.upsertBook(action.book)) {
                        is Result.Success -> {
                            _state.update { it.copy(navigateToBook = action.book) }
                        }
                        is Result.Error -> {
                            Timber.tag(TAG).e("Failed to cache book: %s", cacheResult.error)
                            _state.update {
                                it.copy(
                                    bookSearchState = it.bookSearchState.copy(
                                        errorMessage = ErrorFormatter.formatDataErrorMessage(
                                            cacheResult.error,
                                            "open book"
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
            is LibraryAction.OnNavigationHandled -> {
                _state.update { it.copy(navigateToBook = null) }
            }
            is LibraryAction.OnDismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            // Selection mode
            is LibraryAction.OnToggleSelectionMode -> {
                _state.update {
                    it.copy(
                        isSelectionMode = !it.isSelectionMode,
                        selectedBookIds = emptySet(),
                        errorMessage = null
                    )
                }
            }
            is LibraryAction.OnToggleBookSelection -> {
                _state.update { state ->
                    val newSelection = if (action.bookId in state.selectedBookIds) {
                        state.selectedBookIds - action.bookId
                    } else {
                        state.selectedBookIds + action.bookId
                    }
                    state.copy(selectedBookIds = newSelection)
                }
            }
            is LibraryAction.OnSelectAll -> {
                _state.update { state ->
                    val visibleIds = state.deletableBooks.map { it.id }.toSet()
                    state.copy(
                        selectedBookIds = state.selectedBookIds + visibleIds
                    )
                }
            }
            is LibraryAction.OnDeselectAll -> {
                _state.update { state ->
                    val visibleIds = state.deletableBooks.map { it.id }.toSet()
                    state.copy(selectedBookIds = state.selectedBookIds - visibleIds)
                }
            }
            is LibraryAction.OnDeleteSelectedClick -> {
                _state.update { it.copy(showDeleteConfirmation = true) }
            }
            is LibraryAction.OnConfirmDelete -> {
                deleteSelectedBooks()
            }
            is LibraryAction.OnDismissDeleteDialog -> {
                _state.update { it.copy(showDeleteConfirmation = false) }
            }
        }
    }

    private fun retriggerRemoteSearchIfNeeded() {
        val currentQuery = _state.value.bookSearchState.query
        if (currentQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
            remoteQueryFlow.value = currentQuery
        }
    }

    private fun observeNonRemovableBookIds() {
        viewModelScope.launch {
            libraryUseCases.getNonRemovableBookIds().collectLatest { ids ->
                _state.update { state ->
                    val deletableIds = state.allBooks.map { it.id }.toSet() - ids
                    state.copy(
                        nonRemovableBookIds = ids,
                        selectedBookIds = state.selectedBookIds.intersect(deletableIds)
                    )
                }
            }
        }
    }

    private fun deleteSelectedBooks() {
        val selectedIds = _state.value.selectedBookIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = libraryUseCases.deleteBooks(selectedIds)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isSelectionMode = false,
                            selectedBookIds = emptySet(),
                            showDeleteConfirmation = false,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            showDeleteConfirmation = false,
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                result.error,
                                "delete books"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadTidyMode() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val tidyMode = prefs[TIDY_MODE_KEY] ?: false
            _state.update { it.copy(isTidyMode = tidyMode) }
        }
    }

    private fun observeBooks() {
        viewModelScope.launch {
            libraryUseCases.getAllLibraryBooks().collectLatest { books ->
                _state.update {
                    it.copy(
                        allBooks = books,
                        isLoading = false,
                        bookSearchState = it.bookSearchState.copy(
                            existingBookIds = books.map { book -> book.id }.toSet()
                        )
                    )
                }
                applyFilters()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDebouncedLocalQuery() {
        viewModelScope.launch {
            localQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { applyFilters() }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDebouncedRemoteQuery() {
        viewModelScope.launch {
            remoteQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { raw ->
                    val query = raw.trim()

                    if (query.length < MIN_SEARCH_QUERY_LENGTH) {
                        _state.update {
                            it.copy(
                                bookSearchState = it.bookSearchState.copy(
                                    isLoading = false,
                                    isTyping = false,
                                    errorMessage = null,
                                    results = if (query.isEmpty()) {
                                        emptyList()
                                    } else {
                                        it.bookSearchState.results
                                    }
                                )
                            )
                        }
                        return@collectLatest
                    }

                    performRemoteSearch(query)
                }
        }
    }

    private suspend fun performRemoteSearch(query: String) {
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

        val (generalQuery, titleQuery, authorQuery) = when {
            searchState.searchByTitle && searchState.searchByAuthor -> Triple(query, null, null)
            !searchState.searchByTitle && !searchState.searchByAuthor -> Triple(query, null, null)
            searchState.searchByTitle && !searchState.searchByAuthor -> Triple(null, query, null)
            else -> Triple(null, null, query)
        }

        libraryUseCases.searchBooks(
            query = generalQuery ?: "",
            resultLimit = 15,
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

    private fun addBookToLibrary(book: Book) {
        viewModelScope.launch {
            when (val result = libraryUseCases.upsertBook(book)) {
                is Result.Success -> {
                    Timber.tag(TAG).d("Book added to library: %s", book.title)
                }
                is Result.Error -> {
                    Timber.tag(TAG).e("Failed to add book: %s", result.error)
                    _state.update {
                        it.copy(
                            bookSearchState = it.bookSearchState.copy(
                                errorMessage = ErrorFormatter.formatDataErrorMessage(
                                    result.error,
                                    "add book to library"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun applyFilters() {
        val currentState = _state.value
        val query = currentState.searchQuery.trim()
        val status = currentState.selectedReadingStatus

        var result = currentState.allBooks

        // Search filter
        if (query.isNotEmpty()) {
            result = result.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                    book.authors.any { it.contains(query, ignoreCase = true) }
            }
        }

        // Reading status filter
        if (status != null) {
            result = result.filter { it.readingStatus == status }
        }

        // Sort
        result = when (currentState.sortOption) {
            LibrarySortOption.RECENTLY_ADDED -> result.sortedByDescending { it.dateAdded ?: 0L }
            LibrarySortOption.TITLE_AZ -> result.sortedBy { it.title.lowercase() }
            LibrarySortOption.AUTHOR_AZ -> result.sortedBy {
                it.authors.firstOrNull()?.lowercase() ?: ""
            }
        }

        _state.update { it.copy(filteredBooks = result) }
    }

    // State Update Helpers

    private fun LibraryState.withSearchResults(results: List<Book>): LibraryState {
        return copy(
            bookSearchState = bookSearchState.copy(
                isLoading = false,
                hasSearched = true,
                errorMessage = null,
                results = results
            )
        )
    }

    private fun LibraryState.withSearchError(error: DataError): LibraryState {
        return copy(
            bookSearchState = bookSearchState.copy(
                isLoading = false,
                hasSearched = true,
                errorMessage = ErrorFormatter.formatDataErrorMessage(error, "search books")
            )
        )
    }
}
