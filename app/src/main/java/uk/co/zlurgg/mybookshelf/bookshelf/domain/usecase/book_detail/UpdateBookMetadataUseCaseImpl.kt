package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * Implementation of UpdateBookMetadataUseCase.
 *
 * Updates a book's personal metadata with validation.
 * Personal metadata includes reading status, personal rating, notes, and dates.
 * This data is NOT exported/shared - it stays local to the user's device.
 */
class UpdateBookMetadataUseCaseImpl(
    private val bookRepository: BookRepository,
    private val timeProvider: TimeProvider
) : UpdateBookMetadataUseCase {

    override suspend fun execute(
        bookId: String,
        readingStatus: ReadingStatus?,
        personalRating: Float?,
        personalNotes: String?,
        purchaseDate: Long?
    ): Result<Unit, DataError> {
        return try {
            // Validate personal rating (1.0-5.0)
            if (personalRating != null && (personalRating < 1.0f || personalRating > 5.0f)) {
                return Result.Error(DataError.Validation.INVALID_FORMAT)
            }

            // Validate personal notes length (≤5000 characters)
            if (personalNotes != null && personalNotes.length > 5000) {
                return Result.Error(DataError.Validation.TOO_LONG)
            }

            // Get existing book
            val existingBook = bookRepository.getBookById(bookId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            // Update book with new metadata
            val updatedBook = existingBook.copy(
                readingStatus = readingStatus ?: existingBook.readingStatus,
                personalRating = personalRating ?: existingBook.personalRating,
                personalNotes = personalNotes ?: existingBook.personalNotes,
                purchaseDate = purchaseDate ?: existingBook.purchaseDate,
                // Auto-set dateAdded if not already set
                dateAdded = existingBook.dateAdded ?: timeProvider.currentTimeMillis()
            )

            // Save updated book
            bookRepository.upsertBook(updatedBook)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                    ?: DataError.Local.UNKNOWN
            )
        }
    }
}