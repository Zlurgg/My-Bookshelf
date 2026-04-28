package uk.co.zlurgg.mybookshelf.sharing.domain.service

import uk.co.zlurgg.mybookshelf.sharing.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Domain service interface for validating bookshelf import data.
 * Handles format validation and name conflict detection.
 */
interface BookshelfImportValidator {
    fun validateFormat(exportData: BookshelfExportData): Result<Unit, DataError.Local>
    suspend fun checkNameConflict(shelfName: String): Result<String?, DataError.Local>
}
