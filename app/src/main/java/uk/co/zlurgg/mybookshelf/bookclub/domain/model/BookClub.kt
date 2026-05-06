package uk.co.zlurgg.mybookshelf.bookclub.domain.model

import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle

/**
 * Domain model representing a Book Club.
 * A collaborative collection of books that multiple users can view and manage together.
 */
data class BookClub(
    val code: String,
    val name: String,
    val style: ShelfStyle,
    val createdAt: Long,
    val createdBy: String,
    val createdByName: String,
    val bookCount: Int,
    val memberCount: Int
) {
    companion object {
        const val MAX_BOOK_CLUBS = 5
    }
}
