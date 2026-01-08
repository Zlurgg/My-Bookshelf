package uk.co.zlurgg.mybookshelf.bookshelf.data.export

import kotlinx.serialization.Serializable

/**
 * Minimal book identifier for sharing.
 * Contains only the OpenLibrary work ID needed to fetch full book details.
 */
@Serializable
data class BookIdentifier(
    val workId: String,
)
