package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.core.domain.onError
import uk.co.zlurgg.mybookshelf.core.domain.onSuccess
import uk.co.zlurgg.mybookshelf.core.domain.Result

class BookDetailViewModel(
    private val bookDetailUseCases: BookDetailUseCases,
    private val bookId: String,
    private val shelfId: String
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()

    init {
        loadBookDetails()
    }

    private fun loadBookDetails() {
        viewModelScope.launch {
            // Use GetBookDetailsUseCase to get reactive book details and shelf status
            bookDetailUseCases.getBookDetails.execute(bookId, shelfId).collect { bookDetails ->
                _state.update { currentState ->
                    currentState.copy(
                        book = bookDetails.book,
                        onShelf = bookDetails.isOnShelf,
                        isLoading = false
                    )
                }
            }
        }

        // Load book description separately (non-blocking)
        viewModelScope.launch {
            bookDetailUseCases.getBookDetails.loadBookDescription(bookId)
                .onSuccess {
                    // Description loading handled by the UseCase
                    // State will update via the reactive flow above
                }
                .onError {
                    // ignore, keep UI usable
                }
        }
    }

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
                        when (bookDetailUseCases.removeBookFromShelf.execute(book.id, shelfId)) {
                            is Result.Success -> {
                                _state.update { it.copy(onShelf = false) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                // Handle error silently as in original implementation
                            }
                        }
                    } else {
                        when (bookDetailUseCases.addBookToShelf.execute(book, shelfId)) {
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
                    val currentBook = state.value.book
                    if (currentBook != null) {
                        when (val result = bookDetailUseCases.toggleBookPurchase.execute(currentBook, true)) {
                            is Result.Success -> {
                                _state.update { it.copy(book = result.data) }
                            }
                            is Result.Error -> {
                                // Silent failure as in original implementation
                            }
                        }
                    }
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
                    when (bookDetailUseCases.removeBookFromShelf.execute(bookId, shelfId)) {
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

