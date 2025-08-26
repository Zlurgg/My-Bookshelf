package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.repository.BookshelfRepository

class BookshelfViewModel(
    private val repository: BookshelfRepository,
    private val shelfId: String
) : ViewModel() {

    private val _state = MutableStateFlow(BookshelfState(shelfId = shelfId))
    val state = _state.asStateFlow()

    fun onAction(action: BookshelfAction) {
        when (action) {
            is BookshelfAction.OnSearchClick -> {
                _state.update { it.copy(isSearchDialogVisible = true) }
            }
            is BookshelfAction.OnDismissSearchDialog -> {
                _state.update { it.copy(isSearchDialogVisible = false, searchQuery = "") }
            }
            is BookshelfAction.OnAddBookFromSearch -> {
                viewModelScope.launch {
                    repository.addBookToShelf(shelfId, action.book)
                }
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
                    val results = repository.searchBooks(action.query)
                    _state.update {
                        it.copy(
                            searchResults = results,
                            isSearchLoading = false
                        )
                    }
                }
            }
            else -> Unit
        }
    }

    private fun searchBooks(query: String) {
        viewModelScope.launch {
            val results = repository.searchBooks(query)
            _state.update { it.copy(searchResults = results) }
        }
    }

    private fun addBookToShelf(book: Book) {
        viewModelScope.launch {
            repository.addBookToShelf(shelfId, book)
            _state.update { it.copy(books = it.books + book) }
        }
    }
}
