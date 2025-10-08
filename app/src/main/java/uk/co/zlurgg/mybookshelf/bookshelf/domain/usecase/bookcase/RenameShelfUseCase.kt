package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for renaming a bookshelf.
 * Validates the new name and updates the shelf in the repository.
 */
interface RenameShelfUseCase {
    /**
     * Renames a bookshelf with validation.
     *
     * @param shelfId ID of the shelf to rename
     * @param newName New name for the shelf (will be trimmed)
     * @return Result.Success if renamed, Result.Error if validation fails or shelf not found
     */
    suspend fun execute(shelfId: String, newName: String): Result<Unit, DataError.Local>
}
