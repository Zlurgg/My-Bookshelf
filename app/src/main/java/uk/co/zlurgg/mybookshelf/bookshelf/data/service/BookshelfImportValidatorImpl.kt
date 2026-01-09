package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import kotlinx.coroutines.flow.first
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of BookshelfImportValidator.
 * Handles format validation and name conflict detection.
 */
class BookshelfImportValidatorImpl(
    private val bookcaseRepository: BookcaseRepository
) : BookshelfImportValidator {

    companion object {
        private const val TAG = "BookshelfImportValidator"
    }

    @Suppress("TooGenericExceptionCaught")
    override fun validateFormat(exportData: BookshelfExportData): Result<Unit, DataError.Local> {
        return try {
            // Basic structure validation
            if (exportData.bookshelf.name.isBlank()) {
                return Result.Error(DataError.Local.VALIDATION_ERROR)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "Format validation failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun checkNameConflict(shelfName: String): Result<String?, DataError.Local> {
        return try {
            val existingShelves = bookcaseRepository.getAllShelves().first()
            val conflictingShelf = existingShelves.find { it.name == shelfName }
            Result.Success(conflictingShelf?.name)
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "Name conflict check failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }
}
