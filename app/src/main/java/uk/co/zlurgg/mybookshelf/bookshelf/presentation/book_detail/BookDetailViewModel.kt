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
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.BookClubUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onError
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess

class BookDetailViewModel(
    private val bookDetailUseCases: BookDetailUseCases,
    private val bookClubUseCases: BookClubUseCases,
    private val authService: AuthService,
    private val bookId: String,
    private val shelfId: String
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()

    // Job for debounced auto-save of personal notes
    private var saveNotesJob: Job? = null

    // Job for debounced auto-save of club review
    private var saveReviewJob: Job? = null

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
                    isBookClub = bookDetails.isBookClub,
                    clubCode = bookDetails.clubCode,
                    isLoading = false
                )
            }

            // Load club reviews and comments if this is a book club book
            if (bookDetails.isBookClub && bookDetails.clubCode != null) {
                loadClubReviews(bookDetails.clubCode)
                loadClubComments(bookDetails.clubCode)
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

    private fun loadClubReviews(clubCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingReviews = true) }

            when (val reviewsResult = bookClubUseCases.getBookClubReviews(clubCode, bookId)) {
                is Result.Success -> {
                    val reviews = reviewsResult.data
                    val currentUserId = authService.getSignedInUser()?.userId

                    // Find current user's review to pre-populate their rating/text
                    val userReview = reviews.find { it.userId == currentUserId }

                    _state.update {
                        it.copy(
                            clubReviews = reviews,
                            userClubRating = userReview?.rating ?: 0f,
                            userClubReviewText = userReview?.reviewText ?: "",
                            isLoadingReviews = false
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoadingReviews = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                reviewsResult.error,
                                "load club reviews"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadClubComments(clubCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingComments = true) }

            when (val commentsResult = bookClubUseCases.getBookClubComments(clubCode, bookId)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            clubComments = commentsResult.data,
                            isLoadingComments = false
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoadingComments = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                commentsResult.error,
                                "load club comments"
                            )
                        )
                    }
                }
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
                                _state.update { it.toggleShelfStatus(false) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                _state.update { it.withError(removeResult.error, "remove book from shelf") }
                            }
                        }
                    } else {
                        when (val addResult = bookDetailUseCases.addBookToShelf.execute(book, shelfId)) {
                            is Result.Success -> {
                                _state.update { it.toggleShelfStatus(true) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                _state.update { it.withError(addResult.error, "add book to shelf") }
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
                        when (
                            val purchaseResult = bookDetailUseCases.toggleBookPurchase.execute(
                                currentBook,
                                !currentBook.purchased
                            )
                        ) {
                            is Result.Success -> {
                                // Update state immediately following renameShelf pattern
                                _state.update { it.copy(book = purchaseResult.data) }
                            }
                            is Result.Error -> {
                                _state.update { it.withError(purchaseResult.error, "toggle book purchase") }
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
                        personalNotes = state.value.book?.personalNotes
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
                            _state.update { it.toggleShelfStatus(false) }
                            onNavigateBack?.invoke()
                        }
                        is Result.Error -> {
                            _state.update { it.withError(removeResult.error, "remove book from shelf") }
                        }
                    }
                }
            }
            is BookDetailAction.OnReadingStatusChange -> {
                viewModelScope.launch {
                    when (
                        val metadataResult = bookDetailUseCases.updateBookMetadata.execute(
                            bookId = bookId,
                            readingStatus = action.status
                        )
                    ) {
                        is Result.Success -> {
                            // Update state immediately following renameShelf pattern
                            _state.update { it.updateBook { book -> book?.copy(readingStatus = action.status) } }
                        }
                        is Result.Error -> {
                            _state.update { it.withError(metadataResult.error, "update reading status") }
                        }
                    }
                }
            }
            is BookDetailAction.OnPersonalRatingChange -> {
                viewModelScope.launch {
                    when (
                        val metadataResult = bookDetailUseCases.updateBookMetadata.execute(
                            bookId = bookId,
                            personalRating = action.rating
                        )
                    ) {
                        is Result.Success -> {
                            // Update state immediately following renameShelf pattern
                            _state.update { it.updateBook { book -> book?.copy(personalRating = action.rating) } }
                        }
                        is Result.Error -> {
                            _state.update { it.withError(metadataResult.error, "update personal rating") }
                        }
                    }
                }
            }
            is BookDetailAction.OnPersonalNotesChange -> {
                // Cancel previous auto-save job if user is still typing
                saveNotesJob?.cancel()

                // Update state immediately (optimistic UI)
                _state.update { it.updateBook { book -> book?.copy(personalNotes = action.notes) } }

                // Start debounced auto-save (2 seconds after user stops typing)
                saveNotesJob = viewModelScope.launch {
                    delay(2000) // Wait 2 seconds

                    // Execute actual save to database
                    when (
                        val metadataResult = bookDetailUseCases.updateBookMetadata.execute(
                            bookId = bookId,
                            personalNotes = action.notes
                        )
                    ) {
                        is Result.Success -> {
                            // Save successful - state already updated above
                        }
                        is Result.Error -> {
                            _state.update { it.withError(metadataResult.error, "update personal notes") }
                        }
                    }
                }
            }

            // Club review actions
            is BookDetailAction.OnClubRatingChange -> {
                val clubCode = state.value.clubCode ?: return
                _state.update { it.copy(userClubRating = action.rating) }
                submitClubReview(clubCode, action.rating, state.value.userClubReviewText)
            }
            is BookDetailAction.OnClubReviewTextChange -> {
                // Cancel previous auto-save job if user is still typing
                saveReviewJob?.cancel()

                // Update state immediately (optimistic UI)
                _state.update { it.copy(userClubReviewText = action.text) }

                // Start debounced auto-save (2 seconds after user stops typing)
                saveReviewJob = viewModelScope.launch {
                    delay(2000) // Wait 2 seconds
                    val clubCode = state.value.clubCode ?: return@launch
                    submitClubReview(clubCode, state.value.userClubRating, action.text)
                }
            }
            is BookDetailAction.OnClubReviewSubmit -> {
                val clubCode = state.value.clubCode ?: return
                saveReviewJob?.cancel()
                submitClubReview(clubCode, state.value.userClubRating, state.value.userClubReviewText)
            }
            is BookDetailAction.OnClubReviewDelete -> {
                val clubCode = state.value.clubCode ?: return
                viewModelScope.launch {
                    when (val deleteResult = bookClubUseCases.deleteBookClubReview(clubCode, bookId)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    userClubRating = 0f,
                                    userClubReviewText = "",
                                    // Remove user's review from the list
                                    clubReviews = it.clubReviews.filter { review ->
                                        review.userId != authService.getSignedInUser()?.userId
                                    }
                                )
                            }
                        }
                        is Result.Error -> {
                            _state.update { it.withError(deleteResult.error, "delete club review") }
                        }
                    }
                }
            }

            // Club comment actions
            is BookDetailAction.OnCommentTextChange -> {
                _state.update { it.copy(commentText = action.text) }
            }
            is BookDetailAction.OnCommentSubmit -> {
                val clubCode = state.value.clubCode ?: return
                val text = state.value.commentText.trim()
                if (text.isEmpty()) return

                viewModelScope.launch {
                    when (val addResult = bookClubUseCases.addBookClubComment(clubCode, bookId, text)) {
                        is Result.Success -> {
                            _state.update { it.copy(commentText = "") }
                            loadClubComments(clubCode)
                        }
                        is Result.Error -> {
                            _state.update { it.withError(addResult.error, "add comment") }
                        }
                    }
                }
            }
            is BookDetailAction.OnCommentEditStart -> {
                _state.update {
                    it.copy(
                        editingCommentId = action.commentId,
                        editingCommentText = action.currentText
                    )
                }
            }
            is BookDetailAction.OnCommentEditTextChange -> {
                _state.update { it.copy(editingCommentText = action.text) }
            }
            is BookDetailAction.OnCommentEditSave -> {
                val clubCode = state.value.clubCode ?: return
                val commentId = state.value.editingCommentId ?: return
                val newText = state.value.editingCommentText.trim()
                if (newText.isEmpty()) return

                viewModelScope.launch {
                    when (val editResult = bookClubUseCases.editBookClubComment(clubCode, bookId, commentId, newText)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    editingCommentId = null,
                                    editingCommentText = ""
                                )
                            }
                            loadClubComments(clubCode)
                        }
                        is Result.Error -> {
                            _state.update { it.withError(editResult.error, "edit comment") }
                        }
                    }
                }
            }
            is BookDetailAction.OnCommentEditCancel -> {
                _state.update {
                    it.copy(
                        editingCommentId = null,
                        editingCommentText = ""
                    )
                }
            }
            is BookDetailAction.OnCommentDelete -> {
                val clubCode = state.value.clubCode ?: return
                viewModelScope.launch {
                    when (
                        val deleteResult = bookClubUseCases.deleteBookClubComment(
                            clubCode,
                            bookId,
                            action.commentId
                        )
                    ) {
                        is Result.Success -> {
                            loadClubComments(clubCode)
                        }
                        is Result.Error -> {
                            _state.update { it.withError(deleteResult.error, "delete comment") }
                        }
                    }
                }
            }
        }
    }

    private fun submitClubReview(clubCode: String, rating: Float, reviewText: String) {
        viewModelScope.launch {
            when (
                val upsertResult = bookClubUseCases.upsertBookClubReview(
                    clubCode = clubCode,
                    bookId = bookId,
                    rating = rating,
                    reviewText = reviewText
                )
            ) {
                is Result.Success -> {
                    // Reload reviews to get updated list
                    loadClubReviews(clubCode)
                }
                is Result.Error -> {
                    _state.update { it.withError(upsertResult.error, "save club review") }
                }
            }
        }
    }

    // ============================================================================
    // State Update Helpers (Private Extensions)
    // ============================================================================

    private fun BookDetailState.withError(error: DataError, operation: String): BookDetailState {
        return copy(errorMessage = ErrorFormatter.formatDataErrorMessage(error, operation))
    }

    private fun BookDetailState.updateBook(transform: (Book?) -> Book?): BookDetailState {
        return copy(book = transform(book))
    }

    private fun BookDetailState.toggleShelfStatus(onShelf: Boolean): BookDetailState {
        return copy(onShelf = onShelf)
    }
}
