package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class BookDetailViewModel(
    private val bookDetailUseCases: BookDetailUseCases,
    private val bookId: String,
    private val shelfId: String
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()

    // Job for debounced auto-save of personal notes
    private var saveNotesJob: Job? = null

    init {
        loadBookDetails()
    }

    private fun loadBookDetails() {
        viewModelScope.launch {
            // Load book details once - no continuous collection to avoid race conditions
            // Manual state updates in action handlers keep UI synchronized
            val bookDetails = bookDetailUseCases.getBookDetails.execute(bookId, shelfId).first()
            _state.update { currentState ->
                currentState.copy(
                    book = bookDetails.book,
                    onShelf = bookDetails.isOnShelf,
                    isLoading = false,
                    // Initialize personal metadata from loaded book
                    readingStatus = bookDetails.book?.readingStatus ?: currentState.readingStatus,
                    personalRating = bookDetails.book?.personalRating ?: 0f,
                    personalNotes = bookDetails.book?.personalNotes ?: "",
                    isPurchased = bookDetails.book?.purchased ?: currentState.isPurchased
                )
            }
        }

        // Load book description separately (non-blocking)
        viewModelScope.launch {
            bookDetailUseCases.getBookDetails.loadBookDescription(bookId)
                .onSuccess {
                    // Reload book data once to get updated description
                    val bookDetails = bookDetailUseCases.getBookDetails.execute(bookId, shelfId).first()
                    _state.update { it.copy(book = bookDetails.book) }
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
                        when (val removeResult = bookDetailUseCases.removeBookFromShelf.execute(book.id, shelfId)) {
                            is Result.Success -> {
                                _state.update { it.copy(onShelf = false) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                _state.update {
                                    it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(removeResult.error, "remove book from shelf"))
                                }
                            }
                        }
                    } else {
                        when (val addResult = bookDetailUseCases.addBookToShelf.execute(book, shelfId)) {
                            is Result.Success -> {
                                _state.update { it.copy(onShelf = true) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                _state.update {
                                    it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(addResult.error, "add book to shelf"))
                                }
                            }
                        }
                    }
                }
            }
            is BookDetailAction.OnPurchaseClick -> {
                viewModelScope.launch {
                    val currentBook = state.value.book
                    if (currentBook != null) {
                        // Toggle: pass opposite of current purchased status
                        when (val purchaseResult = bookDetailUseCases.toggleBookPurchase.execute(currentBook, !currentBook.purchased)) {
                            is Result.Success -> {
                                // Update state immediately following renameShelf pattern
                                _state.update { it.copy(
                                    book = purchaseResult.data,
                                    isPurchased = purchaseResult.data.purchased
                                ) }
                            }
                            is Result.Error -> {
                                _state.update {
                                    it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(purchaseResult.error, "toggle book purchase"))
                                }
                            }
                        }
                    }
                }
            }
            BookDetailAction.OnBackClick -> {
                viewModelScope.launch {
                    // Cancel any pending debounced save
                    saveNotesJob?.cancel()

                    // Always save current notes state before navigating (idempotent operation)
                    bookDetailUseCases.updateBookMetadata.execute(
                        bookId = bookId,
                        personalNotes = state.value.personalNotes
                    )
                    // Note: Ignoring result - this is best-effort save before navigation

                    // Navigate back after save completes
                    onNavigateBack?.invoke()
                }
            }
            is BookDetailAction.OnRemoveBookClick -> {
                viewModelScope.launch {
                    when (val removeResult = bookDetailUseCases.removeBookFromShelf.execute(bookId, shelfId)) {
                        is Result.Success -> {
                            _state.update { it.copy(onShelf = false) }
                            onNavigateBack?.invoke()
                        }
                        is Result.Error -> {
                            _state.update {
                                it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(removeResult.error, "remove book from shelf"))
                            }
                        }
                    }
                }
            }
            is BookDetailAction.OnReadingStatusChange -> {
                viewModelScope.launch {
                    when (val metadataResult = bookDetailUseCases.updateBookMetadata.execute(
                        bookId = bookId,
                        readingStatus = action.status
                    )) {
                        is Result.Success -> {
                            // Update state immediately following renameShelf pattern
                            _state.update { it.copy(readingStatus = action.status) }
                        }
                        is Result.Error -> {
                            _state.update {
                                it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(metadataResult.error, "update reading status"))
                            }
                        }
                    }
                }
            }
            is BookDetailAction.OnPersonalRatingChange -> {
                viewModelScope.launch {
                    when (val metadataResult = bookDetailUseCases.updateBookMetadata.execute(
                        bookId = bookId,
                        personalRating = action.rating
                    )) {
                        is Result.Success -> {
                            // Update state immediately following renameShelf pattern
                            _state.update { it.copy(personalRating = action.rating) }
                        }
                        is Result.Error -> {
                            _state.update {
                                it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(metadataResult.error, "update personal rating"))
                            }
                        }
                    }
                }
            }
            is BookDetailAction.OnPersonalNotesChange -> {
                // Cancel previous auto-save job if user is still typing
                saveNotesJob?.cancel()

                // Update state immediately (optimistic UI)
                _state.update { it.copy(personalNotes = action.notes) }

                // Start debounced auto-save (2 seconds after user stops typing)
                saveNotesJob = viewModelScope.launch {
                    delay(2000) // Wait 2 seconds

                    // Execute actual save to database
                    when (val metadataResult = bookDetailUseCases.updateBookMetadata.execute(
                        bookId = bookId,
                        personalNotes = action.notes
                    )) {
                        is Result.Success -> {
                            // Save successful - state already updated above
                        }
                        is Result.Error -> {
                            _state.update {
                                it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(metadataResult.error, "update personal notes"))
                            }
                        }
                    }
                }
            }
        }
    }
}

