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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AndroidBookshelfExportService(
    private val bookcaseRepository: BookcaseRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookRepository: BookRepository,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val shareTokenService: ShareTokenService,
    private val context: Context
) : BookshelfExportService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    companion object {
        private const val SHARE_BASE_URL = "https://zlurgg.github.io/My-Bookshelf/share"
    }

    override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Get shelf once and use it for both export and share
            val allShelves = bookcaseRepository.getAllShelves().first()
            val shelf = allShelves.find { it.id == shelfId }
                ?: return Result.Error(DataError.Local.UNKNOWN)

            // Load books and create export data
            val books = bookshelfRepository.getBooksForShelf(shelfId).first()
            val shelfWithBooks = shelf.copy(books = books)
            val exportData = createExportData(shelfWithBooks)
            val jsonString = json.encodeToString(BookshelfExportData.serializer(), exportData)

            // Generate token with the JSON data
            when (val tokenResult = shareTokenService.generateToken(jsonString)) {
                is Result.Success -> {
                    // Generate web URL for sharing with bookshelf name as parameter
                    val encodedName = java.net.URLEncoder.encode(shelf.name, "UTF-8")
                    val shareUrl = "$SHARE_BASE_URL/?name=${encodedName}#${tokenResult.data}"
                    val message = "Check out my ${shelf.name}!\n${shareUrl}"

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        putExtra(Intent.EXTRA_SUBJECT, "My Bookshelf: ${shelf.name}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooserIntent = Intent.createChooser(shareIntent, "Share Bookshelf")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    context.startActivity(chooserIntent)
                    Result.Success(Unit)
                }
                is Result.Error -> Result.Error(tokenResult.error)
            }
        } catch (_: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
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

            // Link each book to the imported shelf
            importedShelf.books.forEach { book ->
                bookshelfRepository.addBookToShelf(importedShelf.id, book.id)
            }

            Result.Success(Unit)
        } catch (_: SerializationException) {
            Result.Error(DataError.Local.UNKNOWN) // Invalid JSON format
        } catch (_: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun checkImportNameConflict(jsonData: String): Result<String?, DataError.Local> {
        return try {
            val exportData = json.decodeFromString<BookshelfExportData>(jsonData)

            // Validate format version
            if (exportData.formatVersion > 1) {
                return Result.Error(DataError.Local.UNKNOWN) // Unsupported version
            }

            val existingShelves = bookcaseRepository.getAllShelves().first()
            val conflictingShelf = existingShelves.find { it.name == exportData.bookshelf.name }

            Result.Success(conflictingShelf?.name)
        } catch (_: SerializationException) {
            Result.Error(DataError.Local.UNKNOWN) // Invalid JSON format
        } catch (_: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun importBookshelfWithName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
        return try {
            val exportData = json.decodeFromString<BookshelfExportData>(jsonData)

            // Validate format version
            if (exportData.formatVersion > 1) {
                return Result.Error(DataError.Local.UNKNOWN) // Unsupported version
            }

            val importedShelf = createBookshelfFromImport(exportData.bookshelf).copy(name = customName)

            // Add all books to the repository first
            importedShelf.books.forEach { book ->
                bookRepository.upsertBook(book)
            }

            // Then add the shelf
            bookcaseRepository.addShelf(importedShelf)

            // Link each book to the imported shelf
            importedShelf.books.forEach { book ->
                bookshelfRepository.addBookToShelf(importedShelf.id, book.id)
            }

            Result.Success(Unit)
        } catch (_: SerializationException) {
            Result.Error(DataError.Local.UNKNOWN) // Invalid JSON format
        } catch (_: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    private fun createExportData(shelf: Bookshelf): BookshelfExportData {
        return BookshelfExportData(
            exportedAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timeProvider.currentTimeMillis()),
                java.time.ZoneId.systemDefault()
            ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            bookshelf = shelf.toExportedBookshelf()
        )
    }

    private fun createBookshelfFromImport(exportedShelf: ExportedBookshelf): Bookshelf {
        return Bookshelf(
            id = idGenerator.generateId(),
            name = exportedShelf.name,
            books = exportedShelf.books.map { it.toBook() },
            shelfStyle = exportedShelf.shelfStyle
        )
    }

    private fun Bookshelf.toExportedBookshelf(): ExportedBookshelf {
        return ExportedBookshelf(
            name = name,
            shelfStyle = shelfStyle,
            books = books.map { it.toExportedBook() }
        )
    }

    private fun Book.toExportedBook(): ExportedBook {
        return ExportedBook(
            id = id,
            title = title,
            authors = authors,
            imageUrl = imageUrl,
            description = description,
            languages = languages,
            firstPublishYear = firstPublishYear,
            averageRating = averageRating,
            ratingCount = ratingCount,
            numPages = numPages,
            numEditions = numEditions,
            purchased = purchased,
            spineColor = spineColor
        )
    }

    private fun ExportedBook.toBook(): Book {
        return Book(
            id = id,
            title = title,
            authors = authors,
            imageUrl = imageUrl,
            description = description,
            languages = languages,
            firstPublishYear = firstPublishYear,
            averageRating = averageRating,
            ratingCount = ratingCount,
            numPages = numPages,
            numEditions = numEditions,
            purchased = purchased,
            spineColor = spineColor
        )
    }
}