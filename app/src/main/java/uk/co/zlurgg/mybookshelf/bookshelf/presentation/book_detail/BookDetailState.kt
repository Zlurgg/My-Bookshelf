package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.compose.runtime.Stable
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview

@Stable
data class BookDetailState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val onShelf: Boolean = false,
    val errorMessage: String? = null,
    // Book Club fields
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    // Reviews (for ratings)
    val clubReviews: List<BookClubReview> = emptyList(),
    val isLoadingReviews: Boolean = false,
    val userClubRating: Float = 0f,
    val userClubReviewText: String = "",
    // Comments (for discussion)
    val clubComments: List<BookClubComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentText: String = "",
    val editingCommentId: String? = null,
    val editingCommentText: String = "",
)
