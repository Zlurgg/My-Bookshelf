package uk.co.zlurgg.mybookshelf.book.domain.model

/**
 * Data class representing complete book details including shelf membership
 */
data class BookDetailsWithShelfStatus(
    val book: Book?,
    val isOnShelf: Boolean,
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    val clubCreatorId: String? = null,
    val addedByUserId: String? = null,
)
