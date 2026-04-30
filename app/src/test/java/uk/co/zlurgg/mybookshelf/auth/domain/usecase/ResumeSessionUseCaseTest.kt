package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.SyncUserPreferencesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

class ResumeSessionUseCaseTest {

    private val mockSyncScheduler = MockSyncSchedulerService()

    private var syncPreferencesCallCount = 0
    private val mockSyncUserPreferences = object : SyncUserPreferencesUseCase {
        override suspend fun invoke(): Result<Unit, DataError.Sync> {
            syncPreferencesCallCount++
            return Result.Success(Unit)
        }
    }

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
        mockSyncUserPreferences,
        mockRestoreBookClubMemberships,
        mockSyncScheduler,
    )

    @After
    fun tearDown() {
        syncPreferencesCallCount = 0
        restoreCallCount = 0
        mockRestoreResult = Result.Success(RestoreResult(restoredCount = 0, failedCount = 0))
        mockSyncScheduler.reset()
    }

    @Test
    fun `invoke - calls syncUserPreferences`() = runTest {
        useCase()

        assertEquals(1, syncPreferencesCallCount)
    }

    @Test
    fun `invoke - calls restoreBookClubMemberships`() = runTest {
        useCase()

        assertEquals(1, restoreCallCount)
    }

    @Test
    fun `invoke - calls schedulePeriodicSync and triggerImmediateSync`() = runTest {
        useCase()

        assertEquals(1, mockSyncScheduler.schedulePeriodicSyncCallCount)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `invoke - club restoration failure does not prevent sync`() = runTest {
        mockRestoreResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)

        useCase()

        assertEquals(1, mockSyncScheduler.schedulePeriodicSyncCallCount)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }
}
