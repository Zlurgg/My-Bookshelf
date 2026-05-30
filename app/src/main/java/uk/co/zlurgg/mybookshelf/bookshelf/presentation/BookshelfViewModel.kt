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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState
import uk.co.zlurgg.mybookshelf.book.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.GetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferenceState
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferences
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess

class BookshelfViewModel(
    private val bookshelfUseCases: BookshelfUseCases,
    private val getShelfById: GetShelfByIdUseCase,
    private val bookClubOperations: ClubOperations,
    private val checkSignInStatus: CheckSignInStatusUseCase,
    private val searchPreferences: SearchPreferences,
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

    // Merged with queryFlow inside a single collectLatest so any new trigger
    // (typed query, filter toggle, library-scope toggle, or another load-more)
    // cancels the in-flight load-more. Tracking a separate Job would re-introduce
    // the race where a filter-toggle reset and a load-more result interleave.
    private val loadMoreFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        observeSearchPreferences()
        observeDebouncedQuery()
        loadBooks()
        loadShelfDetails()
        checkSignInStatus()
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
                            libraryScopeEnabled = prefs.libraryScopeEnabled,
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
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
            is BookshelfAction.OnSearchResultBookClick -> {
                _state.update { it.copy(navigateToBook = action.book) }
            }
            is BookshelfAction.OnNavigationHandled -> {
                _state.update { it.copy(navigateToBook = null) }
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
                persistSearchPreferences()
                retriggerSearchIfNeeded()
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
                persistSearchPreferences()
                retriggerSearchIfNeeded()
            }
            BookshelfAction.OnToggleSearchBySubject -> {
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
                retriggerSearchIfNeeded()
            }
            BookshelfAction.OnToggleSafeSearch -> {
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            safeSearchEnabled = !it.bookSearchState.safeSearchEnabled
                        )
                    )
                }
                persistSearchPreferences()
                retriggerSearchIfNeeded()
            }
            BookshelfAction.OnToggleLibraryScope -> {
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            libraryScopeEnabled = !it.bookSearchState.libraryScopeEnabled
                        )
                    )
                }
                persistSearchPreferences()
                retriggerSearchIfNeeded()
            }
            BookshelfAction.OnLoadMore -> {
                // The min-length guard handles an in-flight tap landing after the
                // user deleted the query below MIN — canLoadMore is still true
                // from the prior page but the upcoming below-min reset invalidates it.
                val s = _state.value.bookSearchState
                val canFire = s.query.trim().length >= MIN_SEARCH_QUERY_LENGTH &&
                    s.canLoadMore &&
                    !s.isLoadingMore &&
                    !s.libraryScopeEnabled
                if (canFire) loadMoreFlow.tryEmit(Unit)
            }
            // Navigation actions handled by the UI layer
            is BookshelfAction.OnBookClick,
            BookshelfAction.OnBackClick,
            BookshelfAction.OnCreateBookClub -> Unit
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
                    libraryScopeEnabled = searchState.libraryScopeEnabled,
                )
            )
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
            // Merging both triggers into the same collectLatest is the load-bearing
            // cancellation primitive — any new trigger cancels the previous in-flight
            // performSearch regardless of source. Refactoring to per-call Job tracking
            // re-opens the toggle-during-load-more race.
            merge(
                queryFlow.debounce(SEARCH_DEBOUNCE_MS).map<String, SearchTrigger> {
                    SearchTrigger.Fresh(it)
                },
                loadMoreFlow.map<Unit, SearchTrigger> { SearchTrigger.More },
            ).collectLatest { trigger ->
                when (trigger) {
                    is SearchTrigger.Fresh -> {
                        val query = trigger.rawQuery.trim()
                        val libraryScope = _state.value.bookSearchState.libraryScopeEnabled

                        // Skip the min-length gate when library scope is on —
                        // empty query is "show me my whole library."
                        if (query.length < MIN_SEARCH_QUERY_LENGTH && !libraryScope) {
                            _state.update {
                                it.copy(bookSearchState = it.bookSearchState.withBelowMinLength())
                            }
                            return@collectLatest
                        }
                        performSearch(append = false)
                    }
                    SearchTrigger.More -> performSearch(append = true)
                }
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

    private fun retriggerSearchIfNeeded() {
        val searchState = _state.value.bookSearchState
        val isLongEnough = searchState.query.trim().length >= MIN_SEARCH_QUERY_LENGTH
        // Library scope reads from local memory — the min-length gate exists
        // to throttle network calls, not local filters, so an empty query is
        // legitimately "show me everything I own."
        if (isLongEnough || searchState.libraryScopeEnabled) {
            queryFlow.tryEmit(searchState.query)
        }
    }

    private suspend fun performSearch(append: Boolean) {
        val current = _state.value.bookSearchState
        if (append) {
            if (!current.canLoadMore || current.libraryScopeEnabled) return
            _state.update {
                it.copy(bookSearchState = it.bookSearchState.copy(isLoadingMore = true))
            }
        } else {
            _state.update { it.copy(bookSearchState = it.bookSearchState.withFreshSearch()) }
        }

        val searchState = _state.value.bookSearchState

        if (searchState.libraryScopeEnabled) {
            performLibrarySearch(searchState)
            return
        }

        val params = searchState.toSearchParams()
        val baseStartIndex = if (append) searchState.nextStartIndex else 0

        bookshelfUseCases.searchBooks(
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
            // Only send startIndex on load-more so page 1 still hits the fallback path.
            startIndex = baseStartIndex.coerceAtLeast(0).takeIf { append },
        )
            .onSuccess { searchResult ->
                // HashSet keeps dedupe at O(n+m). Google occasionally returns the
                // same volume ID across adjacent pages of a popular query.
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
                            // Advance by pre-filter raw count — post-filter
                            // books.size would re-fetch dropped rows on Google.
                            nextStartIndex = baseStartIndex + searchResult.rawPageSize,
                            // Provider-aware end-detection — result.pageSize is
                            // what the data source actually asked for (Google 40,
                            // OL 100, possibly different if page 1 fell back).
                            canLoadMore = searchResult.rawPageSize >= searchResult.pageSize,
                        )
                    )
                }
                // Cache the ACCUMULATED list. The repo's `cacheSearchPreviews`
                // clears then writes, so passing just the page-2 batch would
                // invalidate page-1 entries for tap-into-detail.
                bookshelfUseCases.cacheSearchPreviews(mergedBooks)
            }
            .onError { error ->
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            // Preserve results on load-more error (fresh-search
                            // already cleared them via withFreshSearch). hasSearched
                            // stays true so the dialog doesn't revert to its
                            // pre-search empty state on a fresh-search failure.
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

    private suspend fun performLibrarySearch(searchState: BookSearchState) {
        // Subject is intentionally omitted — local books don't carry the same
        // subject-qualifier semantics as the remote `subject:` field. The use
        // case applies a title-default fallback when neither title nor author
        // is checked, so the user always gets results.
        bookshelfUseCases.searchLibraryBooks(
            query = searchState.query,
            searchByTitle = searchState.searchByTitle,
            searchByAuthor = searchState.searchByAuthor,
        )
            .onSuccess { books ->
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            hasSearched = true,
                            errorMessage = null,
                            results = books,
                            filteredCount = 0,
                            // Zero pagination explicitly — toggling into library
                            // scope from a paginated remote result mustn't leave
                            // nextStartIndex / canLoadMore stale.
                            nextStartIndex = 0,
                            canLoadMore = false,
                        )
                    )
                }
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
        // Preserve search preferences — the DataStore observer won't re-emit because
        // the persisted value hasn't changed, so a fresh BookSearchState would lose them.
        return copy(
            isSearchDialogVisible = false,
            bookSearchState = BookSearchState(
                existingBookIds = bookSearchState.existingBookIds,
                searchByTitle = bookSearchState.searchByTitle,
                searchByAuthor = bookSearchState.searchByAuthor,
                searchBySubject = bookSearchState.searchBySubject,
                safeSearchEnabled = bookSearchState.safeSearchEnabled,
                libraryScopeEnabled = bookSearchState.libraryScopeEnabled,
            )
        )
    }
}
