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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
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

        // Debounce applies to the outer Library-tab local filter only; the
        // remote-search dialog uses explicit submit (no debounce).
        private const val SEARCH_DEBOUNCE_MS = 300L
        private val TIDY_MODE_KEY = booleanPreferencesKey("library_tidy_mode")
    }

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val localQueryFlow = MutableStateFlow("")

    // SharedFlow so that re-emitting the same query (e.g. after filter toggle) is not conflated.
    private val remoteQueryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)

    // Merged with remoteQueryFlow inside a single collectLatest so any new trigger
    // cancels the previous in-flight performRemoteSearch — see BookshelfViewModel
    // for the load-bearing rationale.
    private val loadMoreFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        observeSearchPreferences()
        loadTidyMode()
        observeBooks()
        observeNonRemovableBookIds()
        observeDebouncedLocalQuery()
        observeRemoteSearchTriggers()
    }

    private sealed interface SearchTrigger {
        data class Fresh(val rawQuery: String) : SearchTrigger
        data object More : SearchTrigger
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
                            // Round-trip preserve only — Library has no scope
                            // toggle and intentionally ignores this flag in
                            // behaviour. See plan §Fix F for the carve-outs in
                            // OnRemoteSearchQueryChange / OnSubmitSearch /
                            // retriggerRemoteSearchIfNeeded; the dialog's
                            // display-side gate (showLibraryScopeToggle) keeps
                            // a leaked-true value from breaking empty-state
                            // and Google attribution rendering.
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
            is LibraryAction.OnDismissSearchDialog -> resetSearchState(closeDialog = true)
            is LibraryAction.OnClearSearch -> resetSearchState(closeDialog = false)
            is LibraryAction.OnRemoteSearchQueryChange -> {
                // §Fix F carve-out: typed-only, no lastSubmittedQuery write,
                // no retrigger. Library has no scope toggle, and OnSubmitSearch
                // is the sole writer of lastSubmittedQuery on this VM.
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(query = action.query)
                    )
                }
            }
            is LibraryAction.OnSubmitSearch -> {
                // §Fix F carve-out: libraryScopeEnabled is NOT consulted here —
                // the Library tab's dialog is unambiguously remote, and a
                // leaked-true value persisted from the Bookshelf tab must not
                // turn this into a no-op (reviewer N1a).
                val q = _state.value.bookSearchState.query
                if (q.isBlank()) return@onAction
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(lastSubmittedQuery = q)
                    )
                }
                remoteQueryFlow.tryEmit(q)
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
            is LibraryAction.OnLoadMore -> {
                // The race-guard (`query == lastSubmittedQuery`) closes the
                // typed/submitted divergence window — see BookshelfViewModel.
                val s = _state.value.bookSearchState
                val canFire = s.lastSubmittedQuery.isNotBlank() &&
                    s.query.trim() == s.lastSubmittedQuery.trim() &&
                    s.canLoadMore &&
                    !s.isLoadingMore
                if (canFire) {
                    loadMoreFlow.tryEmit(Unit)
                }
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

    // TODO(phase2-controller): mirrored with BookshelfViewModel.retriggerSearchIfNeeded.
    // §Fix F: libraryScopeEnabled is NOT consulted — the Library tab ignores
    // the persisted flag for behaviour and only re-fires the last submitted query.
    private fun retriggerRemoteSearchIfNeeded() {
        val lastSubmitted = _state.value.bookSearchState.lastSubmittedQuery
        if (lastSubmitted.isBlank()) return
        remoteQueryFlow.tryEmit(lastSubmitted)
    }

    // TODO(phase2-controller): shared with BookshelfViewModel.
    private fun resetSearchState(closeDialog: Boolean) {
        _state.update {
            it.copy(
                isSearchDialogVisible = if (closeDialog) false else it.isSearchDialogVisible,
                bookSearchState = it.bookSearchState.resetForDialogClose(),
            )
        }
        // Cancels any in-flight performRemoteSearch via collectLatest.
        remoteQueryFlow.tryEmit("")
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

    // TODO(phase2-controller): mirrors BookshelfViewModel.observeSearchTriggers.
    private fun observeRemoteSearchTriggers() {
        viewModelScope.launch {
            // Merging both triggers into the same collectLatest is the load-bearing
            // cancellation primitive — see BookshelfViewModel for rationale.
            merge(
                remoteQueryFlow.map<String, SearchTrigger> { SearchTrigger.Fresh(it) },
                loadMoreFlow.map<Unit, SearchTrigger> { SearchTrigger.More },
            ).collectLatest { trigger ->
                when (trigger) {
                    is SearchTrigger.Fresh -> {
                        val query = trigger.rawQuery.trim()
                        // Cancellation emit from clear/dismiss — collectLatest above
                        // has already cancelled any in-flight performRemoteSearch.
                        if (query.isEmpty()) {
                            return@collectLatest
                        }
                        performRemoteSearch(append = false)
                    }
                    SearchTrigger.More -> performRemoteSearch(append = true)
                }
            }
        }
    }

    private suspend fun performRemoteSearch(append: Boolean) {
        val current = _state.value.bookSearchState
        if (append) {
            if (!current.canLoadMore) return
            _state.update {
                it.copy(bookSearchState = it.bookSearchState.copy(isLoadingMore = true))
            }
        } else {
            _state.update { it.copy(bookSearchState = it.bookSearchState.withFreshSearch()) }
        }

        val searchState = _state.value.bookSearchState
        val params = searchState.toSearchParams()
        val baseStartIndex = if (append) searchState.nextStartIndex else 0

        libraryUseCases.searchBooks(
            query = params.general ?: "",
            // null defers to ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS (20 —
            // Google silently caps page responses there regardless of what we ask
            // for). Post-filters trim roughly half, so a typical page yields
            // ~10 visible English titles; pagination via `startIndex` lets the
            // user request more.
            resultLimit = null,
            language = null,
            authorFilter = params.author,
            titleFilter = params.title,
            subjectFilter = params.subject,
            safeSearchEnabled = searchState.safeSearchEnabled,
            startIndex = baseStartIndex.coerceAtLeast(0).takeIf { append },
        )
            .onSuccess { searchResult ->
                val mergedBooks = if (append) {
                    val seen = HashSet(searchState.results.map { it.id })
                    searchState.results + searchResult.books.filter { seen.add(it.id) }
                } else {
                    searchResult.books
                }
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            hasSearched = true,
                            errorMessage = null,
                            results = mergedBooks,
                            filteredCount = searchResult.filteredCount,
                            nextStartIndex = baseStartIndex + searchResult.rawPageSize,
                            canLoadMore = searchResult.rawPageSize >= searchResult.pageSize,
                        )
                    )
                }
                // Accumulated list, not per-page — see BookshelfViewModel for why.
                libraryUseCases.cacheSearchPreviews(mergedBooks)
            }
            .onError { error ->
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            // After error, the user did attempt a search — the
                            // dialog should not revert to its pre-search empty state.
                            hasSearched = true,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                error,
                                "search books"
                            ),
                        )
                    )
                }
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
}
