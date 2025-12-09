package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Implementation of RemoveBookFromShelfUseCase that handles removing book-shelf associations.
 * The book remains in the database; only the relationship to the shelf is removed.
 */
class RemoveBookFromShelfUseCaseImpl(
    private val bookshelfRepository: BookshelfRepository,
    private val syncSchedulerService: SyncSchedulerService
) : RemoveBookFromShelfUseCase {

    override suspend fun execute(bookId: String, shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Remove the book-shelf association
            bookshelfRepository.removeBookFromShelf(shelfId, bookId)

            // Trigger sync after successful book removal
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: RemoveBookFromShelf")
            syncSchedulerService.triggerImmediateSync()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                    ?: DataError.Local.UNKNOWN
            )
        }
    }
}