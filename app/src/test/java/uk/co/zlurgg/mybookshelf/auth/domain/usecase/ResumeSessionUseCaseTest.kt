package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ResumeSessionUseCaseTest {

    private var restoreCallCount = 0
    private var mockRestoreResult: Result<RestoreResult, DataError.Sync> =
        Result.Success(RestoreResult(restoredCount = 0, failedCount = 0))
    private val mockRestoreBookClubMemberships = object : RestoreBookClubMembershipsUseCase {
        override suspend fun invoke(): Result<RestoreResult, DataError.Sync> {
            restoreCallCount++
            return mockRestoreResult
        }
    }

    private val useCase = ResumeSessionUseCaseImpl(
        mockRestoreBookClubMemberships,
    )

    @After
    fun tearDown() {
        restoreCallCount = 0
        mockRestoreResult = Result.Success(RestoreResult(restoredCount = 0, failedCount = 0))
    }

    @Test
    fun `invoke - calls restoreBookClubMemberships`() = runTest {
        useCase()

        assertEquals(1, restoreCallCount)
    }
}
