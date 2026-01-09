package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Implementation of RenameShelfUseCase.
 * Validates new name is not blank and updates the repository.
 * Duplicate names are allowed - users can have multiple shelves with the same name.
 */
class RenameShelfUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val syncSchedulerService: SyncSchedulerService
) : RenameShelfUseCase {

    @Suppress("TooGenericExceptionCaught") // Intentional: converts all exceptions to Result.Error with logging
    override suspend fun execute(shelfId: String, newName: String): Result<Unit, DataError.Local> {
        return try {
            // Trim whitespace from new name
            val trimmedName = newName.trim()

            // Validate: Name cannot be blank
            if (trimmedName.isBlank()) {
                return Result.Error(DataError.Local.VALIDATION_ERROR)
            }

            // Get the shelf to rename
            val shelfToRename = bookcaseRepository.getShelfById(shelfId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            // Note: Duplicate names are allowed - users can have multiple shelves with the same name

            // Update the shelf with new name
            val updatedShelf = shelfToRename.copy(name = trimmedName)
            bookcaseRepository.updateShelf(updatedShelf)

            // Trigger sync after successful shelf rename
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: RenameShelf")
            syncSchedulerService.triggerImmediateSync()

            Result.Success(Unit)
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "Rename shelf failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }

    companion object {
        private const val TAG = "RenameShelf"
    }
}
