package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

/**
 * Data class containing information needed for sharing a bookshelf.
 */
data class ShareData(
    val token: String,
    val shelfName: String
)
