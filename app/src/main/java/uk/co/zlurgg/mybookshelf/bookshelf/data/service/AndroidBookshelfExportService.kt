package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfIdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AndroidBookshelfExportService(
    private val bookcaseRepository: BookcaseRepository,
    private val bookRepository: BookRepository,
    private val bookshelfIdGenerator: BookshelfIdGenerator,
    private val timeProvider: TimeProvider,
    private val context: Context
) : BookshelfExportService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportBookshelf(shelfId: String): Result<String, DataError.Local> {
        return try {
            val allShelves = bookcaseRepository.getAllShelves().first()
            val shelf = allShelves.find { it.id == shelfId }
                ?: return Result.Error(DataError.Local.UNKNOWN)

            val exportData = createExportData(shelf)
            val jsonString = json.encodeToString(exportData)
            Result.Success(jsonString)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
        return when (val exportResult = exportBookshelf(shelfId)) {
            is Result.Success -> {
                try {
                    val allShelves = bookcaseRepository.getAllShelves().first()
                    val shelf = allShelves.find { it.id == shelfId }
                        ?: return Result.Error(DataError.Local.UNKNOWN)

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, exportResult.data)
                        putExtra(Intent.EXTRA_SUBJECT, "My Bookshelf: ${shelf.name}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooserIntent = Intent.createChooser(shareIntent, "Share Bookshelf")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    context.startActivity(chooserIntent)
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(DataError.Local.UNKNOWN)
                }
            }
            is Result.Error -> Result.Error(exportResult.error)
        }
    }

    override suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local> {
        return try {
            val exportData = json.decodeFromString<BookshelfExportData>(jsonData)

            // Validate format version
            if (exportData.formatVersion > 1) {
                return Result.Error(DataError.Local.UNKNOWN) // Unsupported version
            }

            val importedShelf = createBookshelfFromImport(exportData.bookshelf)

            // Add all books to the repository first
            importedShelf.books.forEach { book ->
                bookRepository.upsertBook(book)
            }

            // Then add the shelf
            bookcaseRepository.addShelf(importedShelf)

            Result.Success(Unit)
        } catch (e: SerializationException) {
            Result.Error(DataError.Local.UNKNOWN) // Invalid JSON format
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    private fun createExportData(shelf: Bookshelf): BookshelfExportData {
        val exportedBooks = shelf.books.map { book ->
            ExportedBook(
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
                spineColor = book.spineColor
            )
        }

        val exportedBookshelf = ExportedBookshelf(
            name = shelf.name,
            shelfStyle = shelf.shelfStyle,
            books = exportedBooks
        )

        return BookshelfExportData(
            exportedAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timeProvider.currentTimeMillis()),
                java.time.ZoneId.systemDefault()
            ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            bookshelf = exportedBookshelf
        )
    }

    private fun createBookshelfFromImport(exportedShelf: ExportedBookshelf): Bookshelf {
        val books = exportedShelf.books.map { exportedBook ->
            Book(
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
                spineColor = exportedBook.spineColor
            )
        }

        return Bookshelf(
            id = bookshelfIdGenerator.generateId(),
            name = exportedShelf.name,
            books = books,
            shelfStyle = exportedShelf.shelfStyle
        )
    }
}