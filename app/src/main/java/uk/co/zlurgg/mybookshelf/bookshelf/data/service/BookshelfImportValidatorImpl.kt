package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * Implementation of BookshelfImportValidator.
 * Handles format validation and name conflict detection.
 */
class BookshelfImportValidatorImpl(
    private val bookcaseRepository: BookcaseRepository
) : BookshelfImportValidator {

    override fun validateFormat(exportData: BookshelfExportData): Result<Unit, DataError.Local> {
        return try {
            // Validate format version
            if (exportData.formatVersion > 1) {
                return Result.Error(DataError.Local.UNSUPPORTED_FORMAT_VERSION)
            }

            // Basic structure validation
            if (exportData.bookshelf.name.isBlank()) {
                return Result.Error(DataError.Local.VALIDATION_ERROR)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }

    override suspend fun checkNameConflict(shelfName: String): Result<String?, DataError.Local> {
        return try {
            val existingShelves = bookcaseRepository.getAllShelves().first()
            val conflictingShelf = existingShelves.find { it.name == shelfName }
            Result.Success(conflictingShelf?.name)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}