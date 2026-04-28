package uk.co.zlurgg.mybookshelf.book.domain.model

/**
 * Review model for displaying reviews in book detail screens.
 * Consumed by BookDetailViewModel via BookReviewProvider.
 * The bookclub module maps BookClubReview to this at the boundary.
 *
 * Fields are currently identical to BookClubReview — the separate type exists
 * as a decoupling boundary so bookshelf never imports from bookclub domain.
 * If bookclub adds club-specific metadata (e.g. clubCode, memberRole),
 * this type remains stable for bookshelf consumers.
 */
data class BookReview(
    val id: String,
    val bookId: String,
    val userId: String,
    val displayName: String,
    val rating: Float,
    val reviewText: String,
    val createdAt: Long,
    val updatedAt: Long
)
