package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBook
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

/**
 * Pure mapping logic for converting between Book domain models and ExportedBook DTOs.
 * Contains no business rules - only data transformation.
 */
object BookMapper {

    fun toExportedBook(book: Book): ExportedBook {
        return ExportedBook(
            id = book.id,
            title = book.title,
            authors = book.authors,
            imageUrl = book.imageUrl,
            description = book.description,
            languages = book.languages,
            firstPublishYear = book.firstPublishYear,
            averageRating = book.averageRating,
            ratingCount = book.ratingCount,
            numPages = book.numPages,
            numEditions = book.numEditions,
            purchased = book.purchased,
            spineColor = book.spineColor,
            // Enhanced metadata (shareable)
            isbn = book.isbn,
            publisher = book.publisher,
            publishDate = book.publishDate,
            internetArchiveId = book.internetArchiveId
            // NOTE: Personal metadata (readingStatus, personalRating, personalNotes, etc.)
            // is intentionally NOT exported for privacy
        )
    }

    fun fromExportedBook(exportedBook: ExportedBook): Book {
        return Book(
            id = exportedBook.id,
            title = exportedBook.title,
            authors = exportedBook.authors,
            imageUrl = exportedBook.imageUrl,
            description = exportedBook.description,
            languages = exportedBook.languages,
            firstPublishYear = exportedBook.firstPublishYear,
            averageRating = exportedBook.averageRating,
            ratingCount = exportedBook.ratingCount,
            numPages = exportedBook.numPages,
            numEditions = exportedBook.numEditions,
            purchased = exportedBook.purchased,
            spineColor = exportedBook.spineColor,
            // Enhanced metadata
            isbn = exportedBook.isbn,
            publisher = exportedBook.publisher,
            publishDate = exportedBook.publishDate,
            internetArchiveId = exportedBook.internetArchiveId
            // NOTE: Personal metadata defaults to null/WANT_TO_READ on import (fresh start)
        )
    }
}