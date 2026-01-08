package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class DeleteShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val bookClubRepository: BookClubRepository,
    private val syncSchedulerService: SyncSchedulerService,
) : DeleteShelfUseCase {
    companion object {
        private const val TAG = "DeleteShelf"
    }

    override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Get shelf to check if it has a book club
            val shelf = repository.getShelfById(shelfId)

            // Delete associated book club if exists (creator must delete from Firestore first)
            if (shelf?.clubCode != null) {
                Timber.tag(TAG).d("Deleting associated book club: %s", shelf.clubCode)
                val deleteClubResult = bookClubRepository.deleteBookClub(shelf.clubCode)
                if (deleteClubResult is Result.Error) {
                    Timber.tag(TAG).e("Failed to delete book club from Firestore: %s", deleteClubResult.error)
                    // Map Sync errors to Local errors for proper UI feedback
                    val localError =
                        when (deleteClubResult.error) {
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
                repository.hardDeleteShelf(shelfId)
            } else {
                // Regular shelf - soft delete for sync
                repository.removeShelf(shelfId)
            }

            // Trigger sync after successful shelf deletion
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: DeleteShelf")
            syncSchedulerService.triggerImmediateSync()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }

    override suspend fun restore(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return try {
            repository.addShelf(shelf)

            // Trigger sync after successful shelf restoration
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: RestoreShelf")
            syncSchedulerService.triggerImmediateSync()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}
