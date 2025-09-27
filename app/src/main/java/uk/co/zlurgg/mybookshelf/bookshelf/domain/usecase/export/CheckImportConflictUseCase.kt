package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.flatMap
import uk.co.zlurgg.mybookshelf.core.domain.map

/**
 * Use case for checking if importing a bookshelf would cause name conflicts.
 * Returns the conflicting name if one exists, null otherwise.
 */
class CheckImportConflictUseCase(
    private val serializer: BookshelfSerializer,
    private val validator: BookshelfImportValidator
) {

    suspend fun execute(jsonData: String): Result<String?, DataError.Local> {
        return serializer.deserialize(jsonData)
            .flatMap { exportData ->
                validator.validateFormat(exportData).map { exportData.bookshelf.name }
            }
            .flatMap { shelfName ->
                validator.checkNameConflict(shelfName)
            }
    }
}