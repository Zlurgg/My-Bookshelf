package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.flatMap
import uk.co.zlurgg.mybookshelf.core.domain.result.map

/**
 * Implementation of CheckImportConflictUseCase.
 * Checks for name conflicts during bookshelf import.
 */
class CheckImportConflictUseCaseImpl(
    private val serializer: BookshelfSerializer,
    private val validator: BookshelfImportValidator,
) : CheckImportConflictUseCase {
    override suspend fun execute(jsonData: String): Result<String?, DataError.Local> {
        return serializer.deserialize(jsonData)
            .flatMap { exportData ->
                validator.validateFormat(exportData).map { exportData.bookshelf.name }
            }
            .flatMap { shelfName ->
                validator.checkNameConflict(shelfName)
            }
    }

    /**
     * Check if a specific shelf name conflicts with existing shelves.
     * Used when validating custom names during conflict resolution.
     * Returns the conflicting name if one exists, null otherwise.
     */
    override suspend fun checkShelfName(shelfName: String): Result<String?, DataError.Local> {
        return validator.checkNameConflict(shelfName)
    }
}
