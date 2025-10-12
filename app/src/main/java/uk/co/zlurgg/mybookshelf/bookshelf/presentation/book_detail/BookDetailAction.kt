package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus

sealed interface BookDetailAction {
    data class OnRateBookDetailClick(val rating: Int) : BookDetailAction
    data object OnPurchaseClick : BookDetailAction
    data class OnAddBookClick(val book: Book) : BookDetailAction
    data class OnRemoveBookClick(val book: Book) : BookDetailAction
    data object OnBackClick : BookDetailAction

    // Personal metadata actions (NOT exported for privacy)
    data class OnReadingStatusChange(val status: ReadingStatus) : BookDetailAction
    data class OnPersonalRatingChange(val rating: Float?) : BookDetailAction
    data class OnPersonalNotesChange(val notes: String?) : BookDetailAction
}