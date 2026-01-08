package uk.co.zlurgg.mybookshelf.bookshelf.data.export

import kotlinx.serialization.Serializable

/**
 * Minimal export data structure for sharing bookshelves.
 * Simplified to reduce URL size - only contains essential bookshelf data.
 */
@Serializable
data class BookshelfExportData(
    val bookshelf: ExportedBookshelf,
)
