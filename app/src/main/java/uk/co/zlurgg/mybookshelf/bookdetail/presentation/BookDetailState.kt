package uk.co.zlurgg.mybookshelf.bookdetail.presentation

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookComment
import uk.co.zlurgg.mybookshelf.book.domain.model.BookReview
import uk.co.zlurgg.mybookshelf.book.domain.util.BookDetailConstants

data class BookDetailState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val onShelf: Boolean = false,
    val hasShelfContext: Boolean = true,
    val isInLibrary: Boolean = false,
    val errorMessage: String? = null,
    // Auth
    val currentUserId: String? = null,
    // Book Club fields
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    val clubCreatorId: String? = null,
    val addedByUserId: String? = null,
    // Reviews (for ratings)
    val clubReviews: List<BookReview> = emptyList(),
    val isLoadingReviews: Boolean = false,
    val userClubRating: Float = 0f,
    val userClubReviewText: String = "",
    // Club computed fields
    val clubAverageRating: Float = 0f,
    val clubReviewsWithText: List<BookReview> = emptyList(),
    val userHasExistingReview: Boolean = false,
    // Comments (for discussion)
    val clubComments: List<BookComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentText: String = "",
    val editingCommentId: String? = null,
    val editingCommentText: String = ""
) {
    val isSignedIn: Boolean get() = currentUserId != null
    val isClubOwner: Boolean get() = isBookClub && currentUserId != null && currentUserId == clubCreatorId
    val isAddedByCurrentUser: Boolean
        get() = isBookClub && currentUserId != null &&
            addedByUserId != null && currentUserId == addedByUserId
    val canRemoveFromShelf: Boolean get() = when {
        !isBookClub -> true
        isClubOwner -> true
        isAddedByCurrentUser -> true
        else -> false
    }
    val isTutorialBook: Boolean get() = book?.id == BookDetailConstants.TUTORIAL_BOOK_ID
}
