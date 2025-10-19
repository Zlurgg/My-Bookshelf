package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.flatMap
import uk.co.zlurgg.mybookshelf.core.domain.result.map

/**
 * Implementation of ImportBookshelfUseCase.
 * Orchestrates the import workflow using pure Result chains.
 */
class ImportBookshelfUseCaseImpl(
    private val serializer: BookshelfSerializer,
    private val validator: BookshelfImportValidator,
    private val dataOrchestrator: BookshelfDataOrchestrator,
    private val exportMapper: BookshelfExportMapper
) : ImportBookshelfUseCase {

    override suspend fun execute(jsonData: String, customName: String?): Result<Unit, DataError.Local> {
        return serializer.deserialize(jsonData)
            .flatMap { exportData ->
                validator.validateFormat(exportData).map { exportData }
            }
            .flatMap { exportData ->
                val shelf = exportMapper.fromExportData(exportData, customName)
                dataOrchestrator.importShelfToDatabase(shelf)
            }
    }
}