package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
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

    override suspend operator fun invoke(shelfId: String, newName: String): Result<Unit, DataError.Local> {
        if (newName.isBlank()) {
            return Result.Error(DataError.Local.VALIDATION_ERROR)
        }
        val trimmedName = newName.trim()

        val shelfToRename = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
            is Result.Error -> return getResult
        }

        val updatedShelf = shelfToRename.copy(name = trimmedName)
        when (val updateResult = bookcaseRepository.updateShelf(updatedShelf)) {
            is Result.Success -> {
                Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: RenameShelf")
                syncSchedulerService.triggerImmediateSync()
            }
            is Result.Error -> return updateResult
        }

        return Result.Success(Unit)
    }
}
