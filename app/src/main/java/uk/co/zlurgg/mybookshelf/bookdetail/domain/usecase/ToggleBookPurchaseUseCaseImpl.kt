package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class ToggleBookPurchaseUseCaseImpl(
    private val bookRepository: BookRepository,
    private val syncSchedulerService: SyncSchedulerService,
) : ToggleBookPurchaseUseCase {

    override suspend operator fun invoke(book: Book, purchased: Boolean): Result<Book, DataError.Local> {
        // Check if book already exists to preserve personal metadata
        val existingBook = when (val getResult = bookRepository.getBookById(book.id)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        val updatedBook = if (existingBook != null) {
            // Book exists - preserve personal metadata, update purchased status and other API data
            book.copy(
                purchased = purchased,
                readingStatus = existingBook.readingStatus,
                personalRating = existingBook.personalRating,
                personalNotes = existingBook.personalNotes,
                dateAdded = existingBook.dateAdded,
                purchaseDate = existingBook.purchaseDate
            )
        } else {
            // New book - use as-is with purchased status
            book.copy(purchased = purchased)
        }

        return when (val upsertResult = bookRepository.upsertBook(updatedBook)) {
            is Result.Success -> {
                Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: ToggleBookPurchase")
                syncSchedulerService.triggerImmediateSync()
                Result.Success(updatedBook)
            }
            is Result.Error -> upsertResult
        }
    }
}
