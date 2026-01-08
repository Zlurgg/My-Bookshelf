package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for a book club comment.
 * Unlike reviews (one per user per book), comments allow multiple per user.
 */
@Immutable
data class BookClubComment(
    val id: String,
    val bookId: String,
    val userId: String,
    val displayName: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)
