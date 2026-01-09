package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of UpdateShelfTidyModeUseCase.
 * Updates only the isTidyMode field while preserving all other shelf properties.
 */
class UpdateShelfTidyModeUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository
) : UpdateShelfTidyModeUseCase {

    override suspend fun execute(shelfId: String, isTidyMode: Boolean): Result<Unit, DataError> {
        // Get current shelf
        val shelf = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
            is Result.Error -> return getResult
        }

        // Update with new tidy mode
        val updatedShelf = shelf.copy(isTidyMode = isTidyMode)
        return bookcaseRepository.updateShelf(updatedShelf)
    }
}
