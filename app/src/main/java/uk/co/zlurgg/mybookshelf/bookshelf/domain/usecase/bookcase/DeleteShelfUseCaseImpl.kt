package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class DeleteShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val clubOperations: ClubOperations,
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

        // Delete based on shelf type
        val deleteResult = if (shelf?.clubCode != null) {
            deleteBookClubShelf(shelf.clubCode, shelfId)
        } else {
            repository.removeShelf(shelfId)
        }

        when (deleteResult) {
            is Result.Success -> {
                Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: DeleteShelf")
                syncSchedulerService.triggerImmediateSync()
            }
            is Result.Error -> return deleteResult
        }

        return Result.Success(Unit)
    }

    private suspend fun deleteBookClubShelf(clubCode: String, shelfId: String): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("Deleting associated book club: %s", clubCode)
        val deleteClubResult = clubOperations.deleteBookClub(clubCode)
        if (deleteClubResult is Result.Error) {
            Timber.tag(TAG).e("Failed to delete book club from Firestore: %s", deleteClubResult.error)
            val localError = when (deleteClubResult.error) {
                DataError.Sync.PERMISSION_DENIED -> DataError.Local.PERMISSION_DENIED
                DataError.Sync.NETWORK_ERROR -> DataError.Local.DISK_FULL
                else -> DataError.Local.UNKNOWN
            }
            return Result.Error(localError)
        }
        Timber.tag(TAG).d("Hard deleting local book club shelf: %s", shelfId)
        return repository.hardDeleteShelf(shelfId)
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
