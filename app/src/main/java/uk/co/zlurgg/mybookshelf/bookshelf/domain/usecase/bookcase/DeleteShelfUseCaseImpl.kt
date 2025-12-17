package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
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

    override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Get shelf to check if it has a book club
            val shelf = repository.getShelfById(shelfId)

            // Delete associated book club if exists
            if (shelf?.clubCode != null) {
                Timber.tag(TAG).d("Deleting associated book club: %s", shelf.clubCode)
                val deleteClubResult = bookClubRepository.deleteBookClub(shelf.clubCode)
                if (deleteClubResult is Result.Error) {
                    Timber.tag(TAG).w("Failed to delete book club, continuing with shelf deletion: %s", deleteClubResult.error)
                    // Continue with shelf deletion even if club deletion fails
                }
            }

            repository.removeShelf(shelfId)

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