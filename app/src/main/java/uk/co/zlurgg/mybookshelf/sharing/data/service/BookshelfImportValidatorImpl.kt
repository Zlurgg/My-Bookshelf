package uk.co.zlurgg.mybookshelf.sharing.data.service

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.sharing.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfImportValidator
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

    override fun validateFormat(exportData: BookshelfExportData): Result<Unit, DataError.Local> {
        if (exportData.bookshelf.name.isBlank()) {
            return Result.Error(DataError.Local.VALIDATION_ERROR)
        }
        return Result.Success(Unit)
    }

    override suspend fun checkNameConflict(shelfName: String): Result<String?, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val existingShelves = bookcaseRepository.getAllShelves().first()
            val conflictingShelf = existingShelves.find { it.name == shelfName }
            conflictingShelf?.name
        }
    }
}
