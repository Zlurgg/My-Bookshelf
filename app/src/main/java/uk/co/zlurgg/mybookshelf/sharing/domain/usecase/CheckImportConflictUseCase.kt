package uk.co.zlurgg.mybookshelf.sharing.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for checking if importing a bookshelf would cause name conflicts.
 * Returns the conflicting name if one exists, null otherwise.
 */
interface CheckImportConflictUseCase {
    suspend operator fun invoke(jsonData: String): Result<String?, DataError.Local>

    /**
     * Check if a specific shelf name conflicts with existing shelves.
     * Used when validating custom names during conflict resolution.
     * Returns the conflicting name if one exists, null otherwise.
     */
    suspend fun checkShelfName(shelfName: String): Result<String?, DataError.Local>
}
