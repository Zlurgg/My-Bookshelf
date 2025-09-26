package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Mapper for converting between Bookshelf domain models and BookshelfExportData.
 * Handles export data creation with timestamps and metadata.
 */
class BookshelfExportMapper(
    private val timeProvider: TimeProvider,
    private val idGenerator: IdGenerator
) {

    fun toExportData(shelf: Bookshelf): BookshelfExportData {
        return BookshelfExportData(
            exportedAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timeProvider.currentTimeMillis()),
                java.time.ZoneId.systemDefault()
            ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            bookshelf = BookshelfMapper.toExportedBookshelf(shelf)
        )
    }

    fun fromExportData(
        exportData: BookshelfExportData,
        customName: String? = null
    ): Bookshelf {
        val exportedShelf = exportData.bookshelf
        val finalName = customName ?: exportedShelf.name

        return BookshelfMapper.fromExportedBookshelf(
            exportedShelf.copy(name = finalName),
            newId = generateNewId()
        )
    }

    private fun generateNewId(): String {
        return idGenerator.generateId()
    }
}