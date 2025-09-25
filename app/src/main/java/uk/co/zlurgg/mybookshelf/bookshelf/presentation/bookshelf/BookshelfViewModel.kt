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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.onError
import uk.co.zlurgg.mybookshelf.core.domain.onSuccess

class BookshelfViewModel(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookshelfExportService: BookshelfExportService,
    private val searchBooksUseCase: SearchBooksUseCase,
    private val addBookToShelfUseCase: AddBookToShelfUseCase,
    private val removeBookFromShelfUseCase: RemoveBookFromShelfUseCase,
    private val getShelfBooksUseCase: GetShelfBooksUseCase,
    private val shelfId: String
) : ViewModel() {

    // Initialize state flow with default value
    private val _state = MutableStateFlow(BookshelfState(shelfId = shelfId))
    val state: StateFlow<BookshelfState> = _state.asStateFlow()

    // Debounced query flow
    private val queryFlow = MutableStateFlow("")

    init {
        observeDebouncedQuery()
        loadBooks()
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
                    // Simple operation - keep as direct repository call for now
                    // This could be moved to a UseCase in Phase 4 if needed
                    try {
                        bookRepository.upsertBook(action.book)
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = "Failed to cache book") }
                    }
                }
            }
            is BookshelfAction.OnAddBookClick -> {
                addBookToShelf(action.book)
            }
            is BookshelfAction.OnRemoveBook -> {
                viewModelScope.launch {
                    when (removeBookFromShelfUseCase.execute(action.book.id, shelfId)) {
                        is Result.Success -> {
                            // Success handled by UI update below
                        }
                        is Result.Error -> {
                            _state.update { it.copy(errorMessage = "Failed to remove book from shelf") }
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
            BookshelfAction.OnDismissShareSuccess -> {
                _state.update { it.copy(shareSuccess = false) }
            }
            is BookshelfAction.OnSortChange -> {
                _state.update { it.copy(selectedSort = action.sort) }

                // Re-trigger search if there's an active query
                val currentQuery = _state.value.searchQuery.trim()
                if (currentQuery.length >= 2) {
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
                if (currentQuery.length >= 2) {
                    performSearch(currentQuery)
                }
            }
            is BookshelfAction.OnTitleFilterChange -> {
                _state.update { it.copy(titleFilter = action.titleFilter) }

                // Re-trigger search if there's an active query
                val currentQuery = _state.value.searchQuery.trim()
                if (currentQuery.length >= 2) {
                    performSearch(currentQuery)
                }
            }
            else -> Unit
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            getShelfBooksUseCase.execute(shelfId).collect { books ->
                _state.update { it.copy(books = books, isLoading = false) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDebouncedQuery() {
        viewModelScope.launch {
            queryFlow
                .debounce(450)
                .distinctUntilChanged()
                .collectLatest { raw ->
                    val query = raw.trim()

                    if (query.length < 2) {
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
            when (addBookToShelfUseCase.execute(book, shelfId)) {
                is Result.Success -> {
                    // Success - book added successfully
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            errorMessage = "Failed to add book to shelf",
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
            searchBooksUseCase
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
                            errorMessage = error.toString()
                        )
                    }
                }
        }
    }

    private fun shareShelf() {
        viewModelScope.launch {
            _state.update { it.copy(isShareLoading = true, errorMessage = null, shareSuccess = false) }

            bookshelfExportService.shareBookshelf(shelfId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isShareLoading = false,
                            shareSuccess = true
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            isShareLoading = false,
                            errorMessage = ErrorFormatter.formatOperationError("share bookshelf", Exception(error.toString()))
                        )
                    }
                }
        }
    }
}
