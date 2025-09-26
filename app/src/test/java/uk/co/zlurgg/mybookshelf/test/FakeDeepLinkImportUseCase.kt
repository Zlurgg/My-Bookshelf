package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.ImportResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Fake implementation of DeepLinkImportUseCase for testing.
 * Allows tests to control import results and verify interactions.
 */
class FakeDeepLinkImportUseCase : DeepLinkImportUseCase {

    var importFromTokenResult: Result<ImportResult, DataError.Local> = Result.Success(ImportResult.Success)
    var importWithCustomNameResult: Result<Unit, DataError.Local> = Result.Success(Unit)

    // Track method calls for verification
    var lastTokenInput: String? = null
    var lastJsonData: String? = null
    var lastCustomName: String? = null
    var importFromTokenCallCount = 0
    var importWithCustomNameCallCount = 0

    override suspend fun importBookshelfFromToken(token: String): Result<ImportResult, DataError.Local> {
        lastTokenInput = token
        importFromTokenCallCount++
        return importFromTokenResult
    }

    override suspend fun importBookshelfWithCustomName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
        lastJsonData = jsonData
        lastCustomName = customName
        importWithCustomNameCallCount++
        return importWithCustomNameResult
    }

    fun reset() {
        importFromTokenResult = Result.Success(ImportResult.Success)
        importWithCustomNameResult = Result.Success(Unit)
        lastTokenInput = null
        lastJsonData = null
        lastCustomName = null
        importFromTokenCallCount = 0
        importWithCustomNameCallCount = 0
    }
}