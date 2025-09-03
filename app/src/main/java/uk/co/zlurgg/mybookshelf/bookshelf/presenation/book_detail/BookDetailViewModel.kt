package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.onError
import uk.co.zlurgg.mybookshelf.core.domain.onSuccess

class BookDetailViewModel(
    private val bookshelfRepository: BookshelfRepository,
    private val bookId: String,
    private val shelfId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailState())
    val state = _state
        .onStart {
            viewModelScope.launch {
                // Load base book from local DB if present
                val base = bookshelfRepository.getBookById(bookId)
                _state.update { it.copy(book = base, isLoading = false) }

                // Observe shelf membership for this shelf and reflect in UI
                launch {
                    bookshelfRepository.getBooksForShelf(shelfId).collect { list ->
                        val inShelf = list.any { it.id == bookId }
                        _state.update { s -> s.copy(book = s.book?.copy(onShelf = inShelf)) }
                    }
                }

                // Fetch description from remote and merge
                bookshelfRepository.getBookDescription(bookId)
                    .onSuccess { description ->
                        _state.update { s -> s.copy(book = s.book?.copy(description = description)) }
                        // Optionally persist back
                        _state.value.book?.let { bookshelfRepository.upsertBook(it) }
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
                        val onShelf = state.value.book?.onShelf == true
                        val book: Book = action.book
                        if (onShelf) {
                            bookshelfRepository.removeBookFromShelf(shelfId, book.id)
                            _state.update { it.copy(book = it.book?.copy(onShelf = false)) }
                        } else {
                            bookshelfRepository.addBookToShelf(shelfId, book.copy(onShelf = true))
                            _state.update { it.copy(book = it.book?.copy(onShelf = true)) }
                        }
                        _events.emit(BookDetailEvent.NavigateBack)
                    } catch (_: Exception) { }
                }
            }
            is BookDetailAction.OnPurchaseClick -> {
                // Not implemented - placeholder for future deep link purchase
            }
            is BookDetailAction.OnRateBookDetailClick -> {
                _state.update { it.copy(book = it.book?.copy(ratingCount = it.rating)) }
            }
            is BookDetailAction.OnRemoveBookClick -> {
                viewModelScope.launch {
                    try {
                        bookshelfRepository.removeBookFromShelf(shelfId, bookId)
                        _state.update { it.copy(book = it.book?.copy(onShelf = false)) }
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