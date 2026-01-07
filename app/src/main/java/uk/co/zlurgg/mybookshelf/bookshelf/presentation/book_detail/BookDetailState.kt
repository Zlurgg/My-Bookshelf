package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview

data class BookDetailState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val onShelf: Boolean = false,
    val errorMessage: String? = null,
    // Book Club fields
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    val clubReviews: List<BookClubReview> = emptyList(),
    val isLoadingReviews: Boolean = false,
    val userClubRating: Float = 0f,
    val userClubReviewText: String = ""
)
