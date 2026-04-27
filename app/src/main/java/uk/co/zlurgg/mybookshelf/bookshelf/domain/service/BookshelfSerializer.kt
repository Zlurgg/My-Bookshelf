package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Domain service interface for serializing and deserializing bookshelf data.
 * Pure business logic without implementation details.
 */
interface BookshelfSerializer {
    fun serialize(shelf: Bookshelf): Result<String, DataError.Local>
    fun deserialize(jsonData: String): Result<BookshelfExportData, DataError.Local>
}
