package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class DeleteShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val bookClubRepository: BookClubRepository,
    private val syncSchedulerService: SyncSchedulerService
) : DeleteShelfUseCase {

    companion object {
        private const val TAG = "DeleteShelf"
    }

    override suspend operator fun invoke(shelfId: String): Result<Unit, DataError.Local> {
        // Get shelf to check if it has a book club
        val shelf = when (val getResult = repository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        // Delete associated book club if exists (creator must delete from Firestore first)
        if (shelf?.clubCode != null) {
            Timber.tag(TAG).d("Deleting associated book club: %s", shelf.clubCode)
            val deleteClubResult = bookClubRepository.deleteBookClub(shelf.clubCode)
            if (deleteClubResult is Result.Error) {
                Timber.tag(TAG).e("Failed to delete book club from Firestore: %s", deleteClubResult.error)
                // Map Sync errors to Local errors for proper UI feedback
                val localError = when (deleteClubResult.error) {
                    DataError.Sync.PERMISSION_DENIED -> DataError.Local.PERMISSION_DENIED
                    DataError.Sync.NETWORK_ERROR -> DataError.Local.DISK_FULL // No better mapping available
                    else -> DataError.Local.UNKNOWN
                }
                // Don't continue with local deletion if Firestore deletion failed
                // This prevents orphaned clubs in Firestore
                return Result.Error(localError)
            }
            // Book club deleted from Firestore - hard delete locally (no sync needed)
            Timber.tag(TAG).d("Hard deleting local book club shelf: %s", shelfId)
            when (val hardDeleteResult = repository.hardDeleteShelf(shelfId)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return hardDeleteResult
            }
        } else {
            // Regular shelf - soft delete for sync
            when (val removeResult = repository.removeShelf(shelfId)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return removeResult
            }
        }

        // Trigger sync after successful shelf deletion
        Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: DeleteShelf")
        syncSchedulerService.triggerImmediateSync()

        return Result.Success(Unit)
    }

    override suspend fun restore(shelf: Bookshelf): Result<Unit, DataError.Local> {
        when (val addResult = repository.addShelf(shelf)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return addResult
        }

        // Trigger sync after successful shelf restoration
        Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: RestoreShelf")
        syncSchedulerService.triggerImmediateSync()

        return Result.Success(Unit)
    }
}
