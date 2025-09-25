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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.core.domain.onError
import uk.co.zlurgg.mybookshelf.core.domain.onSuccess
import uk.co.zlurgg.mybookshelf.core.domain.Result

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val addBookToShelfUseCase: AddBookToShelfUseCase,
    private val removeBookFromShelfUseCase: RemoveBookFromShelfUseCase,
    private val getBookDetailsUseCase: GetBookDetailsUseCase,
    private val bookId: String,
    private val shelfId: String
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailState())
    val state = _state
        .onStart {
            viewModelScope.launch {
                // Use GetBookDetailsUseCase to get reactive book details and shelf status
                getBookDetailsUseCase.execute(bookId, shelfId).collect { bookDetails ->
                    _state.update { currentState ->
                        currentState.copy(
                            book = bookDetails.book,
                            onShelf = bookDetails.isOnShelf,
                            isLoading = false
                        )
                    }
                }

                // Load book description separately (non-blocking)
                launch {
                    getBookDetailsUseCase.loadBookDescription(bookId)
                        .onSuccess {
                            // Description loading handled by the UseCase
                            // State will update via the reactive flow above
                        }
                        .onError {
                            // ignore, keep UI usable
                        }
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
                    val onShelf = state.value.onShelf
                    val book: Book = action.book

                    if (onShelf) {
                        when (removeBookFromShelfUseCase.execute(book.id, shelfId)) {
                            is Result.Success -> {
                                _state.update { it.copy(onShelf = false) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                // Handle error silently as in original implementation
                            }
                        }
                    } else {
                        when (addBookToShelfUseCase.execute(book, shelfId)) {
                            is Result.Success -> {
                                _state.update { it.copy(onShelf = true) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                // Handle error silently as in original implementation
                            }
                        }
                    }
                }
            }
            is BookDetailAction.OnPurchaseClick -> {
                viewModelScope.launch {
                    try {
                        val currentBook = state.value.book
                        if (currentBook != null) {
                            val updatedBook = currentBook.copy(purchased = true)
                            bookRepository.upsertBook(updatedBook)
                            _state.update { it.copy(book = updatedBook) }
                        }
                    } catch (_: Exception) { }
                }
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
                    when (removeBookFromShelfUseCase.execute(bookId, shelfId)) {
                        is Result.Success -> {
                            _state.update { it.copy(onShelf = false) }
                            onNavigateBack?.invoke()
                        }
                        is Result.Error -> {
                            // Handle error silently as in original implementation
                        }
                    }
                }
            }
        }
    }
}

