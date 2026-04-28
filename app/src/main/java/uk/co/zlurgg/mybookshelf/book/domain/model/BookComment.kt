package uk.co.zlurgg.mybookshelf.book.domain.model

/**
 * Comment model for displaying comments in book detail screens.
 * Consumed by BookDetailViewModel via BookReviewProvider.
 * The bookclub module maps BookClubComment to this at the boundary.
 *
 * Fields are currently identical to BookClubComment — the separate type exists
 * as a decoupling boundary so bookshelf never imports from bookclub domain.
 * If bookclub adds club-specific metadata, this type remains stable.
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
