package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

interface BookshelfExportService {
    suspend fun exportBookshelf(shelfId: String): Result<String, DataError.Local>
    suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local>
    suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local>
}