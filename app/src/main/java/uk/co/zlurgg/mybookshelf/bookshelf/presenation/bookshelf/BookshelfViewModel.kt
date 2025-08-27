package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.repository.BookshelfRepository

class BookshelfViewModel(
    private val repository: BookshelfRepository,
    private val shelfId: String
) : ViewModel() {

    // Initialize state flow with default value
    private val _state = MutableStateFlow(BookshelfState(shelfId = shelfId))
    val state: StateFlow<BookshelfState> = _state.asStateFlow()

    init {
        loadBooks()
    }

    fun onAction(action: BookshelfAction) {
        when (action) {
            is BookshelfAction.OnSearchClick -> {
                _state.update { it.copy(isSearchDialogVisible = true) }
            }
            is BookshelfAction.OnDismissSearchDialog -> {
                _state.update { it.copy(isSearchDialogVisible = false, searchQuery = "") }
            }
            is BookshelfAction.OnAddBookFromSearch -> {
                addBookToShelf(action.book)
            }
            is BookshelfAction.OnRemoveBook -> {
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
                _state.update { it.copy(searchQuery = action.query, isSearchLoading = true) }
                viewModelScope.launch {
                    try {
                        val results = repository.searchBooks(action.query)
                        _state.update {
                            it.copy(
                                searchResults = results,
                                isSearchLoading = false
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                isSearchLoading = false,
                                errorMessage = "Search failed: ${e.message}"
                            )
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            try {
                repository.getBooksForShelf(shelfId).collect { books ->
                    _state.update { it.copy(books = books) }
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

    private fun addBookToShelf(book: Book) {
        viewModelScope.launch {
            try {
                repository.addBookToShelf(shelfId, book)
                _state.update { it.copy(books = it.books + book) }
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
