package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import timber.log.Timber
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

    companion object {
        private const val TAG = "BookshelfImport"
    }

    override suspend fun execute(jsonData: String, customName: String?): Result<Unit, DataError.Local> {
        Timber.tag(
            TAG
        ).d("Starting bookshelf import%s", if (customName != null) " with custom name: $customName" else "")

        val result = serializer.deserialize(jsonData)
            .flatMap { exportData ->
                Timber.tag(TAG).d("Deserialization successful, validating format...")
                validator.validateFormat(exportData).map { exportData }
            }
            .flatMap { exportData ->
                Timber.tag(TAG).d("Validation successful, converting to domain model...")
                when (val shelfResult = exportMapper.fromExportData(exportData, customName)) {
                    is Result.Success -> {
                        Timber.tag(TAG).d("Conversion successful, importing to database...")
                        dataOrchestrator.importShelfToDatabase(shelfResult.data)
                    }
                    is Result.Error -> {
                        // Convert remote errors to local errors for consistency
                        val localError = when (shelfResult.error) {
                            is DataError.Remote -> DataError.Local.UNKNOWN
                            is DataError.Local -> shelfResult.error
                            else -> DataError.Local.UNKNOWN
                        }
                        Timber.tag(TAG).e("Export mapper error: %s (converted to %s)", shelfResult.error, localError)
                        Result.Error(localError)
                    }
                }
            }

        return when (result) {
            is Result.Success -> {
                Timber.tag(TAG).d("Bookshelf import completed successfully")
                result
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Bookshelf import failed: %s", result.error)
                result
            }
        }
    }
}
