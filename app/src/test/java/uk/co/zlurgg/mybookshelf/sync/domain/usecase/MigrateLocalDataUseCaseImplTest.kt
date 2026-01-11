package uk.co.zlurgg.mybookshelf.sync.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.MigrationResult
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncRepository

class MigrateLocalDataUseCaseImplTest {

    private lateinit var mockSyncRepository: MockSyncRepository
    private lateinit var fakeSyncScheduler: FakeSyncScheduler
    private lateinit var fakeCurrentUserProvider: FakeCurrentUserProvider
    private lateinit var useCase: MigrateLocalDataUseCaseImpl

    @Before
    fun setup() {
        mockSyncRepository = MockSyncRepository()
        fakeSyncScheduler = FakeSyncScheduler()
        fakeCurrentUserProvider = FakeCurrentUserProvider()
        useCase = MigrateLocalDataUseCaseImpl(
            mockSyncRepository,
            fakeSyncScheduler,
            fakeCurrentUserProvider
        )
    }

    // ==================== No Migration Needed Tests ====================

    @Test
    fun `returns NO_MIGRATION_NEEDED when no orphan data exists`() = runTest {
        mockSyncRepository.migrateOrphanDataResult = Result.Success(MigrationResult.NO_MIGRATION_NEEDED)

        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(0, migration.booksAssigned)
        assertEquals(0, migration.shelvesAssigned)
        assertFalse("Should not have data to migrate", migration.hadDataToMigrate)
        assertFalse("Sync should not be triggered", migration.syncTriggered)
    }

    @Test
    fun `does not trigger sync when no orphan data exists`() = runTest {
        mockSyncRepository.migrateOrphanDataResult = Result.Success(MigrationResult.NO_MIGRATION_NEEDED)

        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        assertFalse("Sync should not be triggered", fakeSyncScheduler.syncTriggered)
    }

    @Test
    fun `calls repository with current user ID`() = runTest {
        fakeCurrentUserProvider.userId = "test-user-456"

        useCase()

        assertTrue("Repository should be called", mockSyncRepository.migrateOrphanDataCalled)
        assertEquals("test-user-456", mockSyncRepository.lastMigrateOrphanDataUserId)
    }

    // ==================== Migration With Data Tests ====================

    @Test
    fun `returns correct migration counts from repository`() = runTest {
        mockSyncRepository.migrateOrphanDataResult = Result.Success(
            MigrationResult(
                booksAssigned = 5,
                shelvesAssigned = 3,
                hadDataToMigrate = true,
                syncTriggered = false
            )
        )

        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(5, migration.booksAssigned)
        assertEquals(3, migration.shelvesAssigned)
        assertEquals(8, migration.totalMigrated)
        assertTrue("Should have data to migrate", migration.hadDataToMigrate)
    }

    @Test
    fun `triggers immediate sync after successful migration with data`() = runTest {
        mockSyncRepository.migrateOrphanDataResult = Result.Success(
            MigrationResult(
                booksAssigned = 1,
                shelvesAssigned = 0,
                hadDataToMigrate = true,
                syncTriggered = false
            )
        )

        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertTrue("Sync should be triggered flag", migration.syncTriggered)
        assertTrue("Scheduler should have triggered sync", fakeSyncScheduler.syncTriggered)
    }

    @Test
    fun `sets syncTriggered flag to true when data was migrated`() = runTest {
        mockSyncRepository.migrateOrphanDataResult = Result.Success(
            MigrationResult(
                booksAssigned = 2,
                shelvesAssigned = 1,
                hadDataToMigrate = true,
                syncTriggered = false // Repository returns false
            )
        )

        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertTrue("UseCase should set syncTriggered to true", migration.syncTriggered)
    }

    // ==================== Error Cases ====================

    @Test
    fun `returns error when user is not signed in`() = runTest {
        fakeCurrentUserProvider.userId = null

        val result = useCase()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Sync.MIGRATION_FAILED, (result as Result.Error).error)
        assertFalse("Repository should not be called", mockSyncRepository.migrateOrphanDataCalled)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        mockSyncRepository.migrateOrphanDataResult = Result.Error(DataError.Sync.MIGRATION_FAILED)

        val result = useCase()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Sync.MIGRATION_FAILED, (result as Result.Error).error)
        assertFalse("Sync should not be triggered on error", fakeSyncScheduler.syncTriggered)
    }

    // ==================== Fakes ====================

    private class FakeCurrentUserProvider : CurrentUserProvider {
        var userId: String? = "user-123"

        override fun getCurrentUserId(): String? = userId
    }

    private class FakeSyncScheduler : SyncSchedulerService {
        var syncTriggered = false

        override fun schedulePeriodicSync() = Unit
        override fun triggerImmediateSync() {
            syncTriggered = true
        }
        override fun cancelAllSync() = Unit
    }
}
