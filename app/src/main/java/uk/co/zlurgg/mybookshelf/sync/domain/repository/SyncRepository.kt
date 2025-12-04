package uk.co.zlurgg.mybookshelf.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncConflict
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncResult
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncState

/**
 * Repository interface for sync operations.
 *
 * This is the main entry point for all sync functionality.
 * Implementations handle the complexity of coordinating local and remote data.
 */
interface SyncRepository {

    /**
     * Performs a full sync for the given user.
     * Pushes local changes to cloud, then pulls remote changes.
     *
     * @param userId Firebase UID of the user
     * @return Result containing sync statistics or error
     */
    suspend fun performSync(userId: String): Result<SyncResult, DataError.Sync>

    /**
     * Pushes local changes to the cloud.
     * Only uploads entities with PENDING or CONFLICT status.
     *
     * @param userId Firebase UID of the user
     * @return Result containing push statistics or error
     */
    suspend fun pushLocalChanges(userId: String): Result<SyncResult, DataError.Sync>

    /**
     * Pulls remote changes from the cloud.
     * Downloads entities modified since last sync.
     *
     * @param userId Firebase UID of the user
     * @return Result containing pull statistics or error
     */
    suspend fun pullRemoteChanges(userId: String): Result<SyncResult, DataError.Sync>

    /**
     * Resolves a specific conflict.
     *
     * @param conflict The conflict to resolve
     * @param resolution The chosen resolution strategy
     * @return Result indicating success or error
     */
    suspend fun resolveConflict(
        conflict: SyncConflict,
        resolution: ConflictResolution
    ): Result<Unit, DataError.Sync>

    /**
     * Gets all unresolved conflicts.
     *
     * @param userId Firebase UID of the user
     * @return List of unresolved conflicts
     */
    suspend fun getUnresolvedConflicts(userId: String): List<SyncConflict>

    /**
     * Observes the current sync state.
     * Emits updates when sync status changes.
     *
     * @param userId Firebase UID of the user
     * @return Flow of sync state updates
     */
    fun observeSyncState(userId: String): Flow<SyncState>

    /**
     * Gets the timestamp of the last successful sync.
     *
     * @param userId Firebase UID of the user
     * @return Timestamp in milliseconds, or null if never synced
     */
    suspend fun getLastSyncTimestamp(userId: String): Long?

    /**
     * Gets the count of pending local changes.
     *
     * @param userId Firebase UID of the user
     * @return Number of entities pending sync
     */
    suspend fun getPendingChangesCount(userId: String): Int

    /**
     * Cancels any ongoing sync operation.
     */
    suspend fun cancelSync()

    /**
     * Clears all sync metadata for a user (used on sign out).
     *
     * @param userId Firebase UID of the user
     */
    suspend fun clearSyncData(userId: String)
}
