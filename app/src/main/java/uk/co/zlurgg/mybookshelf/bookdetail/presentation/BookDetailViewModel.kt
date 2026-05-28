package uk.co.zlurgg.mybookshelf.bookdetail.presentation

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
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookDetailsWithShelfStatus
import uk.co.zlurgg.mybookshelf.book.domain.service.BookReviewProvider
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants.DebounceDelayMs

class BookDetailViewModel(
    private val bookDetailUseCases: BookDetailUseCases,
    private val bookReviewProvider: BookReviewProvider,
    private val authUseCases: AuthUseCases,
    private val bookId: String,
    private val shelfId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailState(hasShelfContext = shelfId != null))
    val state: StateFlow<BookDetailState> = _state.asStateFlow()

    // Job for debounced auto-save of club review (Firestore-bound — debounced
    // to batch network writes). Personal-notes writes are no longer debounced;
    // column-scoped UPDATEs make per-keystroke saves cheap.
    private var saveReviewJob: Job? = null

    init {
        loadBookDetails()
        loadCurrentUserId()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            val userId = authUseCases.getCurrentUserId()
            _state.update { it.copy(currentUserId = userId) }
        }
    }

    private fun loadBookDetails() {
        viewModelScope.launch {
            val details = loadInitialBookState()
            if (details.isBookClub && details.clubCode != null) {
                loadClubReviews(details.clubCode)
                loadClubComments(details.clubCode)
            }
            details.book?.let { maybeFetchDescription(it) }
        }
    }

    /**
     * Loads book + shelf + club metadata once and pushes it into state.
     *
     * Returns the details so the orchestrator can route on them directly,
     * avoiding a "did the state actually update?" follow-up read.
     */
    private suspend fun loadInitialBookState(): BookDetailsWithShelfStatus {
        val details = bookDetailUseCases.getBookDetails(bookId, shelfId).first()
        _state.update { currentState ->
            currentState.copy(
                book = details.book,
                onShelf = details.isOnShelf,
                isInLibrary = details.isInLibrary,
                isBookClub = details.isBookClub,
                clubCode = details.clubCode,
                clubCreatorId = details.clubCreatorId,
                addedByUserId = details.addedByUserId,
                isLoading = false
            )
        }
        return details
    }

    /**
     * Fetches and persists the book's description when the cached value is blank.
     *
     * Persist-then-merge ordering is intentional: persistence is the source of
     * truth for "we don't need to re-fetch this." Merging into state without
     * persisting would reproduce the original 1.4 bug on the next screen open.
     *
     * Errors are intentionally ignored — the UI stays usable without a description.
     */
    private suspend fun maybeFetchDescription(book: Book) {
        if (!book.description.isNullOrBlank()) return
        bookDetailUseCases.getBookDescription(book.id, book.provider)
            .onSuccess { fetched ->
                bookDetailUseCases.updateBookDescription(book.id, fetched)
                _state.update { it.copy(book = it.book?.copy(description = fetched)) }
            }
    }

    private fun loadClubReviews(clubCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingReviews = true) }

            when (val reviewsResult = bookReviewProvider.getReviews(clubCode, bookId)) {
                is Result.Success -> {
                    val reviews = reviewsResult.data
                    val currentUserId = authUseCases.getCurrentUserId()

                    // Find current user's review to pre-populate their rating/text
                    val userReview = reviews.find { it.userId == currentUserId }

                    // Pre-compute club review aggregations
                    val ratedReviews = reviews.filter { it.rating > 0 }
                    val averageRating = if (ratedReviews.isNotEmpty()) {
                        ratedReviews.map { it.rating }.average().toFloat()
                    } else {
                        0f
                    }
                    val reviewsWithText = reviews.filter { it.reviewText.isNotBlank() }
                    val hasExistingReview = reviews.any {
                        it.userId == currentUserId && it.reviewText.isNotBlank()
                    }

                    _state.update {
                        it.copy(
                            clubReviews = reviews,
                            userClubRating = userReview?.rating ?: 0f,
                            userClubReviewText = userReview?.reviewText ?: "",
                            clubAverageRating = averageRating,
                            clubReviewsWithText = reviewsWithText,
                            userHasExistingReview = hasExistingReview,
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

            when (val commentsResult = bookReviewProvider.getComments(clubCode, bookId)) {
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
                val currentShelfId = shelfId ?: return
                viewModelScope.launch {
                    val onShelf = state.value.onShelf
                    val book: Book = action.book

                    if (onShelf) {
                        when (val removeResult = bookDetailUseCases.removeBookFromShelf(book.id, currentShelfId)) {
                            is Result.Success -> {
                                _state.update { it.toggleShelfStatus(false) }
                                onNavigateBack?.invoke()
                            }
                            is Result.Error -> {
                                _state.update { it.withError(removeResult.error, "remove book from shelf") }
                            }
                        }
                    } else {
                        when (val addResult = bookDetailUseCases.addBookToShelf(book, currentShelfId)) {
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
            is BookDetailAction.OnAddToLibraryClick -> {
                viewModelScope.launch {
                    when (val upsertResult = bookDetailUseCases.upsertBook(action.book)) {
                        is Result.Success -> {
                            _state.update { it.copy(isInLibrary = true) }
                        }
                        is Result.Error -> {
                            _state.update { it.withError(upsertResult.error, "add book to library") }
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
                            val purchaseResult = bookDetailUseCases.toggleBookPurchase(
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
            BookDetailAction.OnBackClick -> onNavigateBack?.invoke()
            is BookDetailAction.OnRemoveBookClick -> {
                val currentShelfId = shelfId ?: return
                viewModelScope.launch {
                    when (val removeResult = bookDetailUseCases.removeBookFromShelf(bookId, currentShelfId)) {
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
                        val metadataResult = bookDetailUseCases.updateBookMetadata(
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
                        val metadataResult = bookDetailUseCases.updateBookMetadata(
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
                // Optimistic UI update, then immediate write — the underlying
                // DAO call is a single column-scoped UPDATE, so per-keystroke
                // I/O is cheap (no read, no full-row write).
                _state.update { it.updateBook { book -> book?.copy(personalNotes = action.notes) } }
                viewModelScope.launch {
                    when (
                        val metadataResult = bookDetailUseCases.updateBookMetadata(
                            bookId = bookId,
                            personalNotes = action.notes
                        )
                    ) {
                        is Result.Success -> Unit
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
                    delay(DebounceDelayMs)
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
                    when (val deleteResult = bookReviewProvider.deleteReview(clubCode, bookId)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    userClubRating = 0f,
                                    userClubReviewText = "",
                                    // Remove user's review from the list
                                    clubReviews = it.clubReviews.filter { review ->
                                        review.userId != authUseCases.getCurrentUserId()
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
                    when (val addResult = bookReviewProvider.addComment(clubCode, bookId, text)) {
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
                    when (
                        val editResult = bookReviewProvider.editComment(
                            clubCode,
                            bookId,
                            commentId,
                            newText,
                        )
                    ) {
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
                        val deleteResult = bookReviewProvider.deleteComment(
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
                val upsertResult = bookReviewProvider.upsertReview(
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
