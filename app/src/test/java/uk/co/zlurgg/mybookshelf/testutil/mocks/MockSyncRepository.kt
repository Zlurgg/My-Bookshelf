package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
import uk.co.zlurgg.mybookshelf.sync.domain.model.GuestDataInfo
import uk.co.zlurgg.mybookshelf.sync.domain.model.MigrationResult
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncConflict
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncResult
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncState
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository

/**
 * Mock implementation of SyncRepository for testing.
 */
class MockSyncRepository : SyncRepository {
    var clearedSyncDataForUserId: String? = null
        private set

    override suspend fun performSync(userId: String): Result<SyncResult, DataError.Sync> =
        Result.Success(SyncResult())

    override suspend fun pushLocalChanges(userId: String): Result<SyncResult, DataError.Sync> =
        Result.Success(SyncResult())

    override suspend fun pullRemoteChanges(userId: String): Result<SyncResult, DataError.Sync> =
        Result.Success(SyncResult())

    override suspend fun resolveConflict(
        conflict: SyncConflict,
        resolution: ConflictResolution
    ): Result<Unit, DataError.Sync> = Result.Success(Unit)

    override suspend fun getUnresolvedConflicts(userId: String): List<SyncConflict> = emptyList()

    override fun observeSyncState(userId: String): Flow<SyncState> = flowOf(SyncState.Idle(lastSyncTimestamp = 0L))

    override suspend fun getLastSyncTimestamp(userId: String): Long? = null

    override suspend fun getPendingChangesCount(userId: String): Int = 0

    var hasRemoteDataResult: Boolean = false
    var deleteAllRemoteDataResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var deleteAllRemoteDataCalled = false

    override suspend fun hasRemoteData(userId: String): Boolean = hasRemoteDataResult

    override suspend fun deleteAllRemoteData(userId: String): Result<Unit, DataError.Sync> {
        deleteAllRemoteDataCalled = true
        return deleteAllRemoteDataResult
    }

    override suspend fun cancelSync() = Unit

    override suspend fun clearSyncData(userId: String) {
        clearedSyncDataForUserId = userId
    }

    // Migration support
    var migrateOrphanDataResult: Result<MigrationResult, DataError.Sync> =
        Result.Success(MigrationResult.NO_MIGRATION_NEEDED)
    var migrateOrphanDataCalled = false
    var lastMigrateOrphanDataUserId: String? = null

    override suspend fun migrateOrphanData(userId: String): Result<MigrationResult, DataError.Sync> {
        migrateOrphanDataCalled = true
        lastMigrateOrphanDataUserId = userId
        return migrateOrphanDataResult
    }

    // Orphan data counts support
    var getOrphanDataCountsResult: Result<GuestDataInfo, DataError.Local> =
        Result.Success(GuestDataInfo(bookCount = 0, shelfCount = 0))
    var getOrphanDataCountsCalled = false

    override suspend fun getOrphanDataCounts(): Result<GuestDataInfo, DataError.Local> {
        getOrphanDataCountsCalled = true
        return getOrphanDataCountsResult
    }

    fun reset() {
        clearedSyncDataForUserId = null
        migrateOrphanDataResult = Result.Success(MigrationResult.NO_MIGRATION_NEEDED)
        migrateOrphanDataCalled = false
        lastMigrateOrphanDataUserId = null
        getOrphanDataCountsResult = Result.Success(GuestDataInfo(bookCount = 0, shelfCount = 0))
        getOrphanDataCountsCalled = false
        hasRemoteDataResult = false
        deleteAllRemoteDataResult = Result.Success(Unit)
        deleteAllRemoteDataCalled = false
    }
}
