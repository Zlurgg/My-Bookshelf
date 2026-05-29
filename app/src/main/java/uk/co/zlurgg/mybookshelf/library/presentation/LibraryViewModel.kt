package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferenceState
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferences
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess
import uk.co.zlurgg.mybookshelf.library.domain.usecase.LibraryUseCases

class LibraryViewModel(
    private val libraryUseCases: LibraryUseCases,
    private val dataStore: DataStore<Preferences>,
    private val searchPreferences: SearchPreferences,
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

    // SharedFlow so that re-emitting the same query (e.g. after filter toggle) is not conflated.
    private val remoteQueryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)

    init {
        observeSearchPreferences()
        loadTidyMode()
        observeBooks()
        observeNonRemovableBookIds()
        observeDebouncedLocalQuery()
        observeDebouncedRemoteQuery()
    }

    private fun observeSearchPreferences() {
        searchPreferences.observe()
            .onEach { prefs ->
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchByTitle = prefs.searchByTitle,
                            searchByAuthor = prefs.searchByAuthor,
                            searchBySubject = prefs.searchBySubject,
                            safeSearchEnabled = prefs.safeSearchEnabled,
                            // Read for state parity only — the Library dialog
                            // doesn't surface the "My library only" toggle (the
                            // Library tab IS the user's library), so this flag
                            // is silently inert here. Stored so persistence
                            // round-trips preserve whatever the Shelf dialog set.
                            libraryScopeEnabled = prefs.libraryScopeEnabled,
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
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
                // Preserve search preferences — the DataStore observer won't re-emit because
                // the persisted value hasn't changed, so a fresh BookSearchState would lose them.
                _state.update {
                    it.copy(
                        isSearchDialogVisible = false,
                        bookSearchState = BookSearchState(
                            existingBookIds = it.bookSearchState.existingBookIds,
                            searchByTitle = it.bookSearchState.searchByTitle,
                            searchByAuthor = it.bookSearchState.searchByAuthor,
                            searchBySubject = it.bookSearchState.searchBySubject,
                            safeSearchEnabled = it.bookSearchState.safeSearchEnabled,
                            libraryScopeEnabled = it.bookSearchState.libraryScopeEnabled,
                        )
                    )
                }
                remoteQueryFlow.tryEmit("")
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
                remoteQueryFlow.tryEmit(action.query)
            }
            is LibraryAction.OnToggleSearchByTitle -> {
                val current = _state.value.bookSearchState
                if (!current.canToggleTitle) return@onAction

                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchByTitle = !it.bookSearchState.searchByTitle
                        )
                    )
                }
                persistSearchPreferences()
                retriggerRemoteSearchIfNeeded()
            }
            is LibraryAction.OnToggleSearchByAuthor -> {
                val current = _state.value.bookSearchState
                if (!current.canToggleAuthor) return@onAction

                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchByAuthor = !it.bookSearchState.searchByAuthor
                        )
                    )
                }
                persistSearchPreferences()
                retriggerRemoteSearchIfNeeded()
            }
            is LibraryAction.OnToggleSearchBySubject -> {
                val current = _state.value.bookSearchState
                if (!current.canToggleSubject) return@onAction

                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            searchBySubject = !it.bookSearchState.searchBySubject
                        )
                    )
                }
                persistSearchPreferences()
                retriggerRemoteSearchIfNeeded()
            }
            is LibraryAction.OnToggleSafeSearch -> {
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            safeSearchEnabled = !it.bookSearchState.safeSearchEnabled
                        )
                    )
                }
                persistSearchPreferences()
                retriggerRemoteSearchIfNeeded()
            }
            is LibraryAction.OnAddBookToLibrary -> {
                addBookToLibrary(action.book)
            }
            is LibraryAction.OnSearchResultBookClick -> {
                _state.update { it.copy(navigateToBook = action.book) }
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

    private fun persistSearchPreferences() {
        val searchState = _state.value.bookSearchState
        viewModelScope.launch {
            searchPreferences.update(
                SearchPreferenceState(
                    searchByTitle = searchState.searchByTitle,
                    searchByAuthor = searchState.searchByAuthor,
                    searchBySubject = searchState.searchBySubject,
                    safeSearchEnabled = searchState.safeSearchEnabled,
                    // Round-trip preserve — Library doesn't toggle this flag,
                    // so write whatever we observed back unchanged.
                    libraryScopeEnabled = searchState.libraryScopeEnabled,
                )
            )
        }
    }

    private fun retriggerRemoteSearchIfNeeded() {
        val currentQuery = _state.value.bookSearchState.query
        if (currentQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
            remoteQueryFlow.tryEmit(currentQuery)
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
            // distinctUntilChanged is correct here: title/author checkboxes only affect
            // remote OpenLibrary search. Local applyFilters() always matches both fields.
            localQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { applyFilters() }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDebouncedRemoteQuery() {
        viewModelScope.launch {
            // remoteQueryFlow is a SharedFlow (not StateFlow) so filter toggles can re-emit
            // the same query string. distinctUntilChanged is omitted for the same reason.
            remoteQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { raw ->
                    val query = raw.trim()

                    if (query.length < MIN_SEARCH_QUERY_LENGTH) {
                        _state.update {
                            it.copy(bookSearchState = it.bookSearchState.withBelowMinLength())
                        }
                        return@collectLatest
                    }

                    performRemoteSearch()
                }
        }
    }

    private suspend fun performRemoteSearch() {
        _state.update { it.copy(bookSearchState = it.bookSearchState.withLoading()) }

        val searchState = _state.value.bookSearchState
        val params = searchState.toSearchParams()

        libraryUseCases.searchBooks(
            query = params.general ?: "",
            // null defers to ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS (40).
            // The Google Books post-fetch language filter trims roughly half of
            // those, so a 40-result cap typically yields ~20 visible English
            // titles — better coverage at the same per-request quota cost.
            resultLimit = null,
            language = null,
            authorFilter = params.author,
            titleFilter = params.title,
            subjectFilter = params.subject,
            safeSearchEnabled = searchState.safeSearchEnabled
        )
            .onSuccess { searchResult ->
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            isLoading = false,
                            hasSearched = true,
                            errorMessage = null,
                            results = searchResult.books,
                            filteredCount = searchResult.filteredCount
                        )
                    )
                }
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
