package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

/**
 * Data class representing complete book details including shelf membership
 */
data class BookDetailsWithShelfStatus(
    val book: Book?,
    val isOnShelf: Boolean
)
