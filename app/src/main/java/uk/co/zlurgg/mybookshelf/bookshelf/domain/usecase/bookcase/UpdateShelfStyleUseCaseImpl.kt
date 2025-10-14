package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of UpdateShelfStyleUseCase.
 * Updates the shelf's style in the repository.
 */
class UpdateShelfStyleUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository
) : UpdateShelfStyleUseCase {

    override suspend fun execute(shelfId: String, newStyle: ShelfStyle): Result<Unit, DataError.Local> {
        return try {
            // Get the shelf to update
            val shelfToUpdate = bookcaseRepository.getShelfById(shelfId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            // Update the shelf with new style
            val updatedShelf = shelfToUpdate.copy(shelfStyle = newStyle)
            bookcaseRepository.updateShelf(updatedShelf)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}
