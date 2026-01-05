package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of UpdateShelfStyleUseCase.
 * Updates the shelf's style in the repository.
 * For book clubs, only the creator can change the style.
 */
class UpdateShelfStyleUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val authService: AuthService
) : UpdateShelfStyleUseCase {

    override suspend fun execute(shelfId: String, newStyle: ShelfStyle): Result<Unit, DataError.Local> {
        return try {
            // Get the shelf to update
            val shelfToUpdate = bookcaseRepository.getShelfById(shelfId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            // Permission check for book clubs - only creator can change style
            if (shelfToUpdate.isBookClub) {
                val currentUser = authService.getSignedInUser()
                if (currentUser == null || shelfToUpdate.clubCreatorId != currentUser.userId) {
                    return Result.Error(DataError.Local.PERMISSION_DENIED)
                }
            }

            // Update the shelf with new style
            val updatedShelf = shelfToUpdate.copy(shelfStyle = newStyle)
            bookcaseRepository.updateShelf(updatedShelf)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}
