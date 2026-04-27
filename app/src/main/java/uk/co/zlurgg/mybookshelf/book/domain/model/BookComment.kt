package uk.co.zlurgg.mybookshelf.book.domain.model

/**
 * Slim comment model for displaying comments in book detail screens.
 * Consumed by BookDetailViewModel via BookReviewProvider.
 * The bookclub module maps its richer BookClubComment to this at the boundary.
 */
data class BookComment(
    val id: String,
    val bookId: String,
    val userId: String,
    val displayName: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)
