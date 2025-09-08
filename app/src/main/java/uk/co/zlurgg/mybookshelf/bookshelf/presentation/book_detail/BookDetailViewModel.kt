package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.onError
import uk.co.zlurgg.mybookshelf.core.domain.onSuccess

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookId: String,
    private val shelfId: String? = null, // Optional shelf context
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
                    if (shelfId != null) {
                        // Show status for specific shelf
                        bookshelfRepository.isBookOnShelf(bookId, shelfId).collect { onShelf ->
                            _state.update { s -> s.copy(onShelf = onShelf) }
                        }
                    } else {
                        // Show general library membership
                        bookshelfRepository.isBookInAnyShelf(bookId).collect { inLibrary ->
                            _state.update { s -> s.copy(onShelf = inLibrary) }
                        }
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

    // One-shot UI events
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<BookDetailEvent>(extraBufferCapacity = 1)
    val events: kotlinx.coroutines.flow.SharedFlow<BookDetailEvent> = _events

    fun onAction(action: BookDetailAction) {
        when (action) {
            is BookDetailAction.OnAddBookClick -> {
                viewModelScope.launch {
                    try {
                        val onShelf = state.value.onShelf
                        val book: Book = action.book
                        
                        // TODO: For now, require shelfId context for add/remove operations
                        // Future: Show shelf selection dialog when shelfId is null
                        if (shelfId != null) {
                            if (onShelf) {
                                bookshelfRepository.removeBookFromShelf(shelfId, book.id)
                                _state.update { it.copy(onShelf = false) }
                            } else {
                                // Ensure book is saved first
                                bookRepository.upsertBook(book)
                                bookshelfRepository.addBookToShelf(shelfId, book.id)
                                _state.update { it.copy(onShelf = true) }
                            }
                        }
                        _events.emit(BookDetailEvent.NavigateBack)
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
            is BookDetailAction.OnRemoveBookClick -> {
                viewModelScope.launch {
                    try {
                        if (shelfId != null) {
                            bookshelfRepository.removeBookFromShelf(shelfId, bookId)
                            _state.update { it.copy(onShelf = false) }
                        }
                        _events.emit(BookDetailEvent.NavigateBack)
                    } catch (_: Exception) { }
                }
            }
        }
    }
}

sealed interface BookDetailEvent {
    data object NavigateBack : BookDetailEvent
}