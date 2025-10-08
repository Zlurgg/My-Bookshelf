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
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess

class BookshelfViewModel(
    private val bookshelfUseCases: BookshelfUseCases,
    private val bookcaseUseCases: BookcaseUseCases,
    private val shelfId: String
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 450L
        private const val MIN_SEARCH_QUERY_LENGTH = 2
    }

    // Initialize state flow with default value
    private val _state = MutableStateFlow(BookshelfState(shelfId = shelfId))
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
                _state.update { it.copy(
                    isSearchDialogVisible = false,
                    searchQuery = "",
                    searchResults = emptyList(),
                    isSearchLoading = false,
                    showAdvanced = false,
                    authorFilter = "",
                    titleFilter = ""
                ) }
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
                            _state.update { it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(cacheResult.error, "cache book")) }
                        }
                    }
                }
            }
            is BookshelfAction.OnAddBookClick -> {
                addBookToShelf(action.book)
            }
            is BookshelfAction.OnRemoveBook -> {
                viewModelScope.launch {
                    when (val removeResult = bookshelfUseCases.removeBookFromShelf.execute(action.book.id, shelfId)) {
                        is Result.Success -> {
                            // Success handled by UI update below
                        }
                        is Result.Error -> {
                            _state.update { it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(removeResult.error, "remove book from shelf")) }
                        }
                    }
                }
                _state.update { current ->
                    current.copy(
                        books = current.books.filterNot { it.id == action.book.id },
                        recentlyDeleted = action.book
                    )
                }
            }
            BookshelfAction.OnUndoRemove -> {
                _state.update { current ->
                    current.recentlyDeleted?.let {
                        current.copy(
                            books = current.books + it,
                            recentlyDeleted = null
                        )
                    } ?: current
                }
            }
            is BookshelfAction.OnSearchQueryChange -> {
                // Update UI immediately; defer actual search via debounce
                _state.update { it.copy(searchQuery = action.query) }
                queryFlow.value = action.query
            }
            BookshelfAction.OnToggleTidyMode -> {
                _state.update { it.copy(isTidyMode = !it.isTidyMode) }
            }
            BookshelfAction.OnShareShelf -> {
                shareShelf()
            }
            is BookshelfAction.OnSortChange -> {
                _state.update { it.copy(selectedSort = action.sort) }

                // Re-trigger search if there's an active query
                val currentQuery = _state.value.searchQuery.trim()
                if (currentQuery.length >= MIN_SEARCH_QUERY_LENGTH) {
                    performSearch(currentQuery)
                }
            }
            BookshelfAction.OnToggleAdvancedSearch -> {
                _state.update { it.copy(showAdvanced = !it.showAdvanced) }
            }
            is BookshelfAction.OnAuthorFilterChange -> {
                _state.update { it.copy(authorFilter = action.authorFilter) }

                // Re-trigger search if there's an active query
                val currentQuery = _state.value.searchQuery.trim()
                if (currentQuery.length >= MIN_SEARCH_QUERY_LENGTH) {
                    performSearch(currentQuery)
                }
            }
            is BookshelfAction.OnTitleFilterChange -> {
                _state.update { it.copy(titleFilter = action.titleFilter) }

                // Re-trigger search if there's an active query
                val currentQuery = _state.value.searchQuery.trim()
                if (currentQuery.length >= MIN_SEARCH_QUERY_LENGTH) {
                    performSearch(currentQuery)
                }
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
                                shelfMaterial = ShelfMaterial.fromShelfStyle(shelf.shelfStyle)
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "load shelf details")) }
                }
            }
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
                                errorMessage = null,
                                searchResults = if (query.isEmpty()) emptyList() else it.searchResults
                            )
                        }
                        return@collectLatest
                    }

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
                    _state.update {
                        it.copy(
                            errorMessage = ErrorFormatter.formatDataErrorMessage(addResult.error, "add book to shelf"),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearchLoading = true, errorMessage = null) }

            val currentState = _state.value
            bookshelfUseCases.searchBooks
                .execute(
                    query = query,
                    sortBy = currentState.selectedSort,
                    language = null,
                    authorFilter = currentState.authorFilter.takeIf { it.isNotBlank() },
                    titleFilter = currentState.titleFilter.takeIf { it.isNotBlank() }
                )
                .onSuccess { searchResults ->
                    _state.update {
                        it.copy(
                            isSearchLoading = false,
                            errorMessage = null,
                            searchResults = searchResults
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            searchResults = emptyList(),
                            isSearchLoading = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(error, "perform search")
                        )
                    }
                }
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
}
