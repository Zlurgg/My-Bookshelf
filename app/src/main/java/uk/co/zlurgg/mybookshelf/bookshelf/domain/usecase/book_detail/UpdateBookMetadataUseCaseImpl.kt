package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Implementation of UpdateBookMetadataUseCase.
 *
 * Updates a book's personal metadata with validation.
 * Personal metadata includes reading status, personal rating, notes, and dates.
 * This data is NOT exported/shared - it stays local to the user's device.
 */
class UpdateBookMetadataUseCaseImpl(
    private val bookRepository: BookRepository,
    private val timeProvider: TimeProvider,
    private val syncSchedulerService: SyncSchedulerService
) : UpdateBookMetadataUseCase {

    companion object {
        private const val MAX_RATING = 5.0f
        private const val MAX_NOTES_LENGTH = 5000
    }

    override suspend operator fun invoke(
        bookId: String,
        readingStatus: ReadingStatus?,
        personalRating: Float?,
        personalNotes: String?,
        purchaseDate: Long?
    ): Result<Unit, DataError> {
        // Validate personal rating (0.0-5.0, where 0 = unrated)
        if (personalRating != null && (personalRating !in 0f..MAX_RATING)) {
            return Result.Error(DataError.Validation.INVALID_FORMAT)
        }

        // Validate personal notes length
        if (personalNotes != null && personalNotes.length > MAX_NOTES_LENGTH) {
            return Result.Error(DataError.Validation.TOO_LONG)
        }

        // Get existing book
        val existingBook = when (val getResult = bookRepository.getBookById(bookId)) {
            is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
            is Result.Error -> return getResult
        }

        // Update book with new metadata
        // null parameter = "don't change this field"
        // explicit value (including 0f/"") = "update to this value"
        val updatedBook = existingBook.copy(
            readingStatus = readingStatus ?: existingBook.readingStatus,
            personalRating = personalRating ?: existingBook.personalRating,
            personalNotes = personalNotes ?: existingBook.personalNotes,
            purchaseDate = purchaseDate ?: existingBook.purchaseDate,
            // Auto-set dateAdded if not already set
            dateAdded = existingBook.dateAdded ?: timeProvider.currentTimeMillis()
        )

        // Save updated book
        return when (val upsertResult = bookRepository.upsertBook(updatedBook)) {
            is Result.Success -> {
                // Trigger sync after successful metadata update
                Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: UpdateBookMetadata")
                syncSchedulerService.triggerImmediateSync()
                Result.Success(Unit)
            }
            is Result.Error -> upsertResult
        }
    }
}
