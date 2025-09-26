package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface BookshelfExportService {
    suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local>
    suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local>
    suspend fun checkImportNameConflict(jsonData: String): Result<String?, DataError.Local>
    suspend fun importBookshelfWithName(jsonData: String, customName: String): Result<Unit, DataError.Local>
}