package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
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

    override suspend operator fun invoke(bookId: String, shelfId: String): Result<Unit, DataError.Local> {
        // Check if this is a book club shelf BEFORE removing
        val shelf = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }
        // Remove the book-shelf association
        when (val removeResult = bookshelfRepository.removeBookFromShelf(shelfId, bookId)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return removeResult
        }

        // If this is a book club shelf, also remove from Firestore club collection
        shelf?.takeIf { it.isBookClub }?.clubCode?.takeIf { it.isNotEmpty() }?.let { code ->
            Timber.tag(TAG).d("Removing book %s from book club %s", bookId, code)
            val syncResult = bookClubRepository.removeBookFromClub(code, bookId)
            if (syncResult is Result.Error) {
                Timber.tag(TAG).w("Failed to remove book from club: %s", syncResult.error)
                // Don't fail the whole operation - local remove succeeded
            }
        }

        // Trigger sync after successful book removal
        Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: RemoveBookFromShelf")
        syncSchedulerService.triggerImmediateSync()

        return Result.Success(Unit)
    }

    companion object {
        private const val TAG = "RemoveBookFromShelf"
    }
}
