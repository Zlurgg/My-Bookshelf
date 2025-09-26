package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ExportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.flatMap

/**
 * Refactored Android export service that delegates to use cases.
 * Maintains compatibility with existing interface while following Clean Architecture.
 */
class AndroidBookshelfExportService(
    private val exportBookshelfUseCase: ExportBookshelfUseCase,
    private val importBookshelfUseCase: ImportBookshelfUseCase,
    private val checkImportConflictUseCase: CheckImportConflictUseCase,
    private val androidShareService: AndroidShareService,
    private val context: Context
) : BookshelfExportService {


    override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
        return exportBookshelfUseCase.execute(shelfId)
            .flatMap { shareData ->
                androidShareService.shareBookshelf(shareData)
            }
    }

    override suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local> {
        return importBookshelfUseCase.execute(jsonData)
    }

    override suspend fun checkImportNameConflict(jsonData: String): Result<String?, DataError.Local> {
        return checkImportConflictUseCase.execute(jsonData)
    }

    override suspend fun importBookshelfWithName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
        return importBookshelfUseCase.execute(jsonData, customName)
    }

}