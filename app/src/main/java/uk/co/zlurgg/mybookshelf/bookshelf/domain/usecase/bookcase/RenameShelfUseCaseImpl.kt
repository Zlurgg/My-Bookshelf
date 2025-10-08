package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of RenameShelfUseCase.
 * Validates new name against existing shelves and updates the repository.
 */
class RenameShelfUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository
) : RenameShelfUseCase {

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

            // Get all shelves to check for name conflicts
            val allShelves = bookcaseRepository.getAllShelves().first()

            // Check for name conflict (case insensitive, excluding self)
            val hasConflict = allShelves.any { shelf ->
                shelf.id != shelfId && shelf.name.equals(trimmedName, ignoreCase = true)
            }

            if (hasConflict) {
                return Result.Error(DataError.Local.NAME_CONFLICT)
            }

            // Update the shelf with new name
            val updatedShelf = shelfToRename.copy(name = trimmedName)
            bookcaseRepository.updateShelf(updatedShelf)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}
