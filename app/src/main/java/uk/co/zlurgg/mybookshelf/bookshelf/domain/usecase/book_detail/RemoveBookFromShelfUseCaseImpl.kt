package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Implementation of RemoveBookFromShelfUseCase that handles removing book-shelf associations.
 * The book remains in the database; only the relationship to the shelf is removed.
 * Also syncs to Firestore if the shelf is a book club.
 */
class RemoveBookFromShelfUseCaseImpl(
    private val bookshelfRepository: BookshelfRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val bookClubRepository: BookClubRepository,
    private val syncSchedulerService: SyncSchedulerService
) : RemoveBookFromShelfUseCase {

    override suspend fun execute(bookId: String, shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Check if this is a book club shelf BEFORE removing
            val shelf = bookcaseRepository.getShelfById(shelfId)
            val clubCode = shelf?.clubCode?.takeIf { it.isNotEmpty() }
            val isBookClub = shelf?.isBookClub == true && clubCode != null

            // Remove the book-shelf association
            bookshelfRepository.removeBookFromShelf(shelfId, bookId)

            // If this is a book club shelf, also remove from Firestore club collection
            if (isBookClub) {
                Timber.tag(TAG).d("Removing book %s from book club %s", bookId, clubCode!!)
                val syncResult = bookClubRepository.removeBookFromClub(clubCode, bookId)
                if (syncResult is Result.Error) {
                    Timber.tag(TAG).w("Failed to remove book from club: %s", syncResult.error)
                    // Don't fail the whole operation - local remove succeeded
                }
            }

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

    companion object {
        private const val TAG = "RemoveBookFromShelf"
    }
}