package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
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

    override suspend fun cancelSync() = Unit

    override suspend fun clearSyncData(userId: String) {
        clearedSyncDataForUserId = userId
    }

    fun reset() {
        clearedSyncDataForUserId = null
    }
}
