package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

/**
 * Pure mapping logic for converting between Bookshelf domain models and ExportedBookshelf DTOs.
 * Contains no business rules - only data transformation.
 */
object BookshelfMapper {

    fun toExportedBookshelf(shelf: Bookshelf): ExportedBookshelf {
        return ExportedBookshelf(
            name = shelf.name,
            shelfStyle = shelf.shelfStyle,
            books = shelf.books.map { BookMapper.toExportedBook(it) }
        )
    }

    fun fromExportedBookshelf(exportedShelf: ExportedBookshelf, newId: String): Bookshelf {
        return Bookshelf(
            id = newId,
            name = exportedShelf.name,
            books = exportedShelf.books.map { BookMapper.fromExportedBook(it) },
            shelfStyle = exportedShelf.shelfStyle
        )
    }
}