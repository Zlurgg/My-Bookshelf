package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of UpdateShelfTidyModeUseCase.
 * Updates only the isTidyMode field while preserving all other shelf properties.
 */
class UpdateShelfTidyModeUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository
) : UpdateShelfTidyModeUseCase {

    @Suppress("TooGenericExceptionCaught") // Intentional: converts all exceptions to Result.Error with logging
    override suspend fun execute(shelfId: String, isTidyMode: Boolean): Result<Unit, DataError> {
        return try {
            // Get current shelf
            val shelf = bookcaseRepository.getShelfById(shelfId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            // Update with new tidy mode
            val updatedShelf = shelf.copy(isTidyMode = isTidyMode)
            bookcaseRepository.updateShelf(updatedShelf)

            Result.Success(Unit)
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e)
            Timber.tag(TAG).e(e, "Update tidy mode failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }

    companion object {
        private const val TAG = "UpdateShelfTidyMode"
    }
}
