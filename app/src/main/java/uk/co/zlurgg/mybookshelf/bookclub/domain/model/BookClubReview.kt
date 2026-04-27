package uk.co.zlurgg.mybookshelf.bookclub.domain.model

/**
 * Domain model representing a review for a book in a Book Club.
 * Each user can have one review per book, containing their rating and/or review text.
 */
data class BookClubReview(
    val id: String,
    val bookId: String,
    val userId: String,
    val displayName: String,
    val rating: Float, // 0 = no rating, 1-5 = rated
    val reviewText: String, // "" = no review text
    val createdAt: Long,
    val updatedAt: Long
)
