package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

class FakeBookshelfExportService : BookshelfExportService {

    var shouldReturnError = false
    var lastSharedShelfId: String? = null
    var lastImportedData: String? = null

    override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
        lastSharedShelfId = shelfId
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local> {
        lastImportedData = jsonData
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun checkImportNameConflict(jsonData: String): Result<String?, DataError.Local> {
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(null) // No conflict by default
        }
    }

    override suspend fun importBookshelfWithName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
        lastImportedData = jsonData
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(Unit)
        }
    }
}