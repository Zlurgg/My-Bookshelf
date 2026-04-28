package uk.co.zlurgg.mybookshelf.sharing.data.service

import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.CheckImportConflictUseCase
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.ExportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.ImportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.flatMap
/**
 * Refactored Android export service that delegates to use cases.
 * Maintains compatibility with existing interface while following Clean Architecture.
 */
class AndroidBookshelfExportService(
    private val exportBookshelfUseCase: ExportBookshelfUseCase,
    private val importBookshelfUseCase: ImportBookshelfUseCase,
    private val checkImportConflictUseCase: CheckImportConflictUseCase,
    private val androidShareService: AndroidShareService
) : BookshelfExportService {

    override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
        return exportBookshelfUseCase(shelfId)
            .flatMap { shareData ->
                androidShareService.shareBookshelf(shareData)
            }
    }

    override suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local> {
        return importBookshelfUseCase(jsonData)
    }

    override suspend fun checkImportNameConflict(jsonData: String): Result<String?, DataError.Local> {
        return checkImportConflictUseCase(jsonData)
    }

    override suspend fun importBookshelfWithName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
        return importBookshelfUseCase(jsonData, customName)
    }
}
