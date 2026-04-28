package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Reusable mock implementation of BookshelfExportService for testing.
 * Provides configurable behavior and tracking for test scenarios.
 *
 * Following the pattern established in MockBookcaseRepository and MockUseCases.
 */
class MockBookshelfExportService : BookshelfExportService {

    // Tracking properties
    var shareBookshelfCalled = false
    var importBookshelfCalled = false
    var checkImportNameConflictCalled = false
    var importBookshelfWithNameCalled = false

    var lastShareShelfId: String? = null
    var lastImportJsonData: String? = null
    var lastCheckConflictJsonData: String? = null
    var lastImportWithNameJsonData: String? = null
    var lastCustomName: String? = null

    // Configuration properties
    var shareResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    var importResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    var checkConflictResult: Result<String?, DataError.Local> = Result.Success(null)
    var importWithNameResult: Result<Unit, DataError.Local> = Result.Success(Unit)

    override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
        shareBookshelfCalled = true
        lastShareShelfId = shelfId
        return shareResult
    }

    override suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local> {
        importBookshelfCalled = true
        lastImportJsonData = jsonData
        return importResult
    }

    override suspend fun checkImportNameConflict(jsonData: String): Result<String?, DataError.Local> {
        checkImportNameConflictCalled = true
        lastCheckConflictJsonData = jsonData
        return checkConflictResult
    }

    override suspend fun importBookshelfWithName(
        jsonData: String,
        customName: String
    ): Result<Unit, DataError.Local> {
        importBookshelfWithNameCalled = true
        lastImportWithNameJsonData = jsonData
        lastCustomName = customName
        return importWithNameResult
    }

    /**
     * Reset all tracking and configuration to initial state.
     * Call this in @After tearDown() for test isolation.
     */
    fun reset() {
        shareBookshelfCalled = false
        importBookshelfCalled = false
        checkImportNameConflictCalled = false
        importBookshelfWithNameCalled = false

        lastShareShelfId = null
        lastImportJsonData = null
        lastCheckConflictJsonData = null
        lastImportWithNameJsonData = null
        lastCustomName = null

        shareResult = Result.Success(Unit)
        importResult = Result.Success(Unit)
        checkConflictResult = Result.Success(null)
        importWithNameResult = Result.Success(Unit)
    }
}
