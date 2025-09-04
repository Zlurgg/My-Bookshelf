package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.onError
import uk.co.zlurgg.mybookshelf.core.domain.onSuccess

class BookshelfViewModel(
    private val bookshelfRepository: BookshelfRepository,
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
                    isSearchLoading = false
                ) }
                // Reset query to cancel any pending search
                queryFlow.value = ""
            }
            is BookshelfAction.OnBookClick -> {
                // Persist clicked book so details screen can load it by ID safely
                viewModelScope.launch {
                    try {
                        bookshelfRepository.upsertBook(action.book)
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = "Failed to cache book: ${e.message}") }
                    }
                }
            }
            is BookshelfAction.OnAddBookClick -> {
                addBookToShelf(action.book)
            }
            is BookshelfAction.OnRemoveBook -> {
                viewModelScope.launch {
                    try {
                        bookshelfRepository.removeBookFromShelf(shelfId, action.book.id)
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = "Failed to remove book: ${e.message}") }
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
            else -> Unit
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            try {
                bookshelfRepository.getBooksForShelf(shelfId).collect { books ->
                    _state.update { it.copy(books = books, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = "Failed to load books: ${e.message}",
                        isLoading = false
                    )
                }
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

                    _state.update { it.copy(isSearchLoading = true, errorMessage = null) }

                    bookshelfRepository
                        .searchBooks(query)
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
    }

    private fun addBookToShelf(book: Book) {
        viewModelScope.launch {
            try {
                bookshelfRepository.addBookToShelf(shelfId, book)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = "Failed to add book: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
}
