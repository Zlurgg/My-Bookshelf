package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.onError
import uk.co.zlurgg.mybookshelf.core.domain.onSuccess

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookId: String,
    private val shelfId: String
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailState())
    val state = _state
        .onStart {
            viewModelScope.launch {
                // Load base book from local DB if present
                val base = bookRepository.getBookById(bookId)
                _state.update { it.copy(book = base, isLoading = false) }

                // Observe shelf membership and reflect in UI
                launch {
                    bookshelfRepository.isBookOnShelf(bookId, shelfId).collect { onShelf ->
                        _state.update { s -> s.copy(onShelf = onShelf) }
                    }
                }

                // Fetch description from remote and merge
                bookRepository.getBookDescription(bookId)
                    .onSuccess { description ->
                        _state.update { s -> s.copy(book = s.book?.copy(description = description)) }
                        // Optionally persist back
                        _state.value.book?.let { bookRepository.upsertBook(it) }
                    }
                    .onError {
                        // ignore, keep UI usable
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )

    // Navigation callback
    private var onNavigateBack: (() -> Unit)? = null
    
    fun setNavigationCallback(onBack: () -> Unit) {
        onNavigateBack = onBack
    }

    fun onAction(action: BookDetailAction) {
        when (action) {
            is BookDetailAction.OnAddBookClick -> {
                viewModelScope.launch {
                    try {
                        val onShelf = state.value.onShelf
                        val book: Book = action.book
                        
                        if (onShelf) {
                            bookshelfRepository.removeBookFromShelf(shelfId, book.id)
                            _state.update { it.copy(onShelf = false) }
                        } else {
                            // Ensure book is saved first
                            bookRepository.upsertBook(book)
                            bookshelfRepository.addBookToShelf(shelfId, book.id)
                            _state.update { it.copy(onShelf = true) }
                        }
                        onNavigateBack?.invoke()
                    } catch (_: Exception) { }
                }
            }
            is BookDetailAction.OnPurchaseClick -> {
                // TODO: Implement purchase functionality
                // Should open affiliate link in browser or Custom Tab
                // See BookRepository interface for planned purchase integration
            }
            is BookDetailAction.OnRateBookDetailClick -> {
                _state.update { current ->
                    current.copy(
                        rating = action.rating,
                        book = current.book?.copy(ratingCount = action.rating)
                    )
                }
            }
            BookDetailAction.OnBackClick -> {
                onNavigateBack?.invoke()
            }
            is BookDetailAction.OnRemoveBookClick -> {
                viewModelScope.launch {
                    try {
                        bookshelfRepository.removeBookFromShelf(shelfId, bookId)
                        _state.update { it.copy(onShelf = false) }
                        onNavigateBack?.invoke()
                    } catch (_: Exception) { }
                }
            }
        }
    }
}

