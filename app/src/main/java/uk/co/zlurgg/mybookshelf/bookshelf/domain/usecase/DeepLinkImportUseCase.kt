package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * Use case for handling deep link bookshelf imports.
 * Orchestrates token validation and bookshelf import process.
 */
class DeepLinkImportUseCase(
    private val shareTokenService: ShareTokenService,
    private val bookshelfExportService: BookshelfExportService
) {

    suspend fun importBookshelfFromToken(token: String): Result<ImportResult, DataError.Local> {
        return when (val tokenResult = shareTokenService.getShelfDataByToken(token)) {
            is Result.Success -> {
                // Check for name conflicts first
                when (val conflictResult = bookshelfExportService.checkImportNameConflict(tokenResult.data)) {
                    is Result.Success -> {
                        if (conflictResult.data != null) {
                            // Name conflict exists, return conflict info
                            Result.Success(ImportResult.NameConflict(conflictResult.data, tokenResult.data))
                        } else {
                            // No conflict, proceed with import
                            when (val importResult = bookshelfExportService.importBookshelf(tokenResult.data)) {
                                is Result.Success -> Result.Success(ImportResult.Success)
                                is Result.Error -> Result.Error(importResult.error)
                            }
                        }
                    }
                    is Result.Error -> Result.Error(conflictResult.error)
                }
            }
            is Result.Error -> Result.Error(tokenResult.error)
        }
    }

    suspend fun importBookshelfWithCustomName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
        return when (val importResult = bookshelfExportService.importBookshelfWithName(jsonData, customName)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(importResult.error)
        }
    }
}