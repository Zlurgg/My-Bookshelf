package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.flatMap
import uk.co.zlurgg.mybookshelf.core.domain.map

/**
 * Implementation of DeepLinkImportUseCase that orchestrates token validation and bookshelf import.
 * Refactored to use dedicated use cases following Clean Architecture principles.
 */
class DeepLinkImportUseCaseImpl(
    private val shareTokenService: ShareTokenService,
    private val checkImportConflictUseCase: CheckImportConflictUseCase,
    private val importBookshelfUseCase: ImportBookshelfUseCase
) : DeepLinkImportUseCase {

    override suspend fun importBookshelfFromToken(token: String): Result<ImportResult, DataError.Local> {
        return shareTokenService.getShelfDataByToken(token)
            .flatMap { jsonData ->
                checkImportConflictUseCase.execute(jsonData)
                    .flatMap { conflictingName ->
                        if (conflictingName != null) {
                            // Name conflict exists, return conflict info
                            Result.Success(ImportResult.NameConflict(conflictingName, jsonData))
                        } else {
                            // No conflict, proceed with import
                            importBookshelfUseCase.execute(jsonData)
                                .map { ImportResult.Success }
                        }
                    }
            }
    }

    override suspend fun importBookshelfWithCustomName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
        return importBookshelfUseCase.execute(jsonData, customName)
    }
}