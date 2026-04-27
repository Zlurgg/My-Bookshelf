package uk.co.zlurgg.mybookshelf.book.domain.model

/**
 * Slim review model for displaying reviews in book detail screens.
 * Consumed by BookDetailViewModel via BookReviewProvider.
 * The bookclub module maps its richer BookClubReview to this at the boundary.
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
