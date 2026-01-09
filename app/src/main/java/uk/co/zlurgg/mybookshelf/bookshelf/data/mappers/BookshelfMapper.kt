package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookIdentifier
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

/**
 * Pure mapping logic for converting Bookshelf domain models to ExportedBookshelf DTOs.
 * Contains no business rules - only data transformation.
 *
 * Note: Import logic (ExportedBookshelf → Bookshelf) is handled by BookshelfExportMapper
 * since it requires API calls to fetch book details.
 */
object BookshelfMapper {

    fun toExportedBookshelf(shelf: Bookshelf): ExportedBookshelf {
        return ExportedBookshelf(
            name = shelf.name,
            shelfStyle = shelf.shelfStyle,
            bookIds = shelf.books.map { book ->
                BookIdentifier(workId = book.id)
            }
        )
    }
}
