package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

class FakeBookshelfExportService : BookshelfExportService {

    var shouldReturnError = false
    var lastExportedShelfId: String? = null
    var lastSharedShelfId: String? = null
    var lastImportedData: String? = null

    override suspend fun exportBookshelf(shelfId: String): Result<String, DataError.Local> {
        lastExportedShelfId = shelfId
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success("""{"formatVersion":1,"exportedAt":"2023-01-01T00:00:00","appName":"My Bookshelf","bookshelf":{"name":"Test Shelf","shelfStyle":"DarkWood","books":[]}}""")
        }
    }

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
}