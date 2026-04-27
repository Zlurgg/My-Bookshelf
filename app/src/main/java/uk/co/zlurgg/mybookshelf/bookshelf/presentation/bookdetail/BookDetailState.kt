package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookdetail

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookComment
import uk.co.zlurgg.mybookshelf.book.domain.model.BookReview

data class BookDetailState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val onShelf: Boolean = false,
    val errorMessage: String? = null,
    // Book Club fields
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    // Reviews (for ratings)
    val clubReviews: List<BookReview> = emptyList(),
    val isLoadingReviews: Boolean = false,
    val userClubRating: Float = 0f,
    val userClubReviewText: String = "",
    // Comments (for discussion)
    val clubComments: List<BookComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentText: String = "",
    val editingCommentId: String? = null,
    val editingCommentText: String = ""
)
