package uk.co.zlurgg.mybookshelf.sync.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.SyncDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.data.engine.SyncEngine
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
import uk.co.zlurgg.mybookshelf.sync.domain.model.EntityType
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncConflict
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncPhase
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncResult
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncState
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository
import uk.co.zlurgg.mybookshelf.sync.domain.service.ConnectivityMonitor

/**
 * Implementation of SyncRepository that coordinates sync operations.
 *
 * This class acts as a facade over SyncEngine, providing a clean API
 * for the presentation layer while handling:
 * - State observation and transformation
 * - Connectivity awareness
 * - Conflict management
 * - Metadata persistence
 */
class SyncRepositoryImpl(
    private val syncEngine: SyncEngine,
    private val syncDao: SyncDao,
    private val bookshelfDao: BookshelfDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val timeProvider: TimeProvider,
) : SyncRepository {
    override suspend fun performSync(userId: String): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("=== FULL SYNC REQUESTED for user: %s ===", userId)

        if (!connectivityMonitor.isConnected()) {
            Timber.tag(TAG).w("Sync aborted: no network connection")
            return Result.Error(DataError.Sync.NETWORK_ERROR)
        }

        return syncEngine.performFullSync(userId)
    }

    override suspend fun pushLocalChanges(userId: String): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("Push local changes requested for user: %s", userId)

        if (!connectivityMonitor.isConnected()) {
            Timber.tag(TAG).w("Push aborted: no network connection")
            return Result.Error(DataError.Sync.NETWORK_ERROR)
        }

        return syncEngine.pushLocalChanges(userId)
    }

    override suspend fun pullRemoteChanges(userId: String): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("Pull remote changes requested for user: %s", userId)

        if (!connectivityMonitor.isConnected()) {
            Timber.tag(TAG).w("Pull aborted: no network connection")
            return Result.Error(DataError.Sync.NETWORK_ERROR)
        }

        return syncEngine.pullRemoteChanges(userId)
    }

    override suspend fun resolveConflict(
        conflict: SyncConflict,
        resolution: ConflictResolution,
    ): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d(
            "Resolving conflict for entity: %s with resolution: %s",
            conflict.entityId,
            resolution,
        )

        // For now, we just mark it as resolved by updating the sync status
        // The actual resolution logic is handled by ConflictResolver during sync
        return try {
            when (conflict.entityType) {
                EntityType.BOOK -> {
                    val newStatus =
                        when (resolution) {
                            ConflictResolution.KeepLocal -> "PENDING"
                            ConflictResolution.KeepRemote -> "SYNCED"
                            else -> "PENDING"
                        }
                    bookshelfDao.updateBookSyncStatus(
                        conflict.entityId,
                        newStatus,
                        timeProvider.currentTimeMillis(),
                    )
                }
                EntityType.BOOKSHELF -> {
                    val newStatus =
                        when (resolution) {
                            ConflictResolution.KeepLocal -> "PENDING"
                            ConflictResolution.KeepRemote -> "SYNCED"
                            else -> "PENDING"
                        }
                    bookshelfDao.updateShelfSyncStatus(
                        conflict.entityId,
                        newStatus,
                        timeProvider.currentTimeMillis(),
                    )
                }
                EntityType.CROSS_REF -> {
                    // Cross-refs are synced as part of bookshelf, no direct handling needed
                    Timber.tag(TAG).d(
                        "Cross-ref conflict for: %s - delegating to bookshelf sync",
                        conflict.entityId,
                    )
                }
            }
            Timber.tag(TAG).d("Conflict resolved successfully for: %s", conflict.entityId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to resolve conflict for: %s", conflict.entityId)
            Result.Error(DataError.Sync.UNKNOWN)
        }
    }

    override suspend fun getUnresolvedConflicts(userId: String): List<SyncConflict> {
        return syncEngine.getUnresolvedConflicts()
    }

    override fun observeSyncState(userId: String): Flow<SyncState> {
        // Combine sync metadata, connectivity, and sync progress into SyncState
        return combine(
            syncDao.observeSyncMetadata(userId),
            connectivityMonitor.observeConnectivity(),
            syncEngine.progress,
        ) { metadata, isConnected, progress ->
            when {
                // Not connected - offline state
                !isConnected -> SyncState.Offline

                // Check if currently syncing
                metadata?.syncInProgress == true || progress.phase != SyncPhase.IDLE -> {
                    SyncState.Syncing(progress = progress)
                }

                // Check for errors
                metadata?.lastSyncError != null -> {
                    SyncState.Error(
                        message = metadata.lastSyncError,
                        isRetryable = isRetryableError(metadata.lastSyncError),
                        lastAttemptTimestamp = metadata.lastSyncTimestamp,
                    )
                }

                // Check for unresolved conflicts
                syncEngine.getUnresolvedConflicts().isNotEmpty() -> {
                    val conflicts = syncEngine.getUnresolvedConflicts()
                    SyncState.HasConflicts(
                        conflictCount = conflicts.size,
                        conflictIds = conflicts.map { it.entityId },
                    )
                }

                // Successfully synced at least once
                metadata?.lastSyncTimestamp != null && metadata.lastSyncTimestamp > 0 -> {
                    SyncState.Idle(
                        lastSyncTimestamp = metadata.lastSyncTimestamp,
                        pendingChangesCount = metadata.pendingOperationsCount,
                    )
                }

                // Never synced but user is signed in
                else ->
                    SyncState.Idle(
                        lastSyncTimestamp = 0L,
                        pendingChangesCount = 0,
                    )
            }
        }
    }

    override suspend fun getLastSyncTimestamp(userId: String): Long? {
        val metadata = syncDao.getSyncMetadata(userId)
        return metadata?.lastSyncTimestamp?.takeIf { it > 0 }
    }

    override suspend fun getPendingChangesCount(userId: String): Int {
        val pendingBooks =
            bookshelfDao.getPendingSyncBooks()
                .filter { (it.ownerId == userId || it.ownerId == null) && !SystemOwnerIds.isSystemOwner(it.ownerId) }
                .size

        val pendingShelves =
            bookshelfDao.getPendingSyncShelves()
                .filter { (it.ownerId == userId || it.ownerId == null) && !SystemOwnerIds.isSystemOwner(it.ownerId) }
                .size

        return pendingBooks + pendingShelves
    }

    override suspend fun cancelSync() {
        Timber.tag(TAG).d("Cancel sync requested")
        syncEngine.cancel()
    }

    override suspend fun clearSyncData(userId: String) {
        Timber.tag(TAG).d("Clearing sync data for user: %s", userId)
        syncDao.deleteSyncMetadata(userId)
    }

    private fun isRetryableError(error: String?): Boolean {
        if (error == null) return true

        return when (error) {
            DataError.Sync.NETWORK_ERROR.name,
            DataError.Sync.SYNC_IN_PROGRESS.name,
            DataError.Sync.UNKNOWN.name,
            -> true

            DataError.Sync.NOT_SIGNED_IN.name,
            DataError.Sync.PERMISSION_DENIED.name,
            DataError.Sync.QUOTA_EXCEEDED.name,
            -> false

            else -> true
        }
    }

    companion object {
        private const val TAG = "SyncRepository"
    }
}
