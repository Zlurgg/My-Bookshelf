package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Represents the current state of sync for a user.
 * This is a UI-friendly representation of sync status.
 */
sealed class SyncState {
    /** User is not signed in, sync is disabled */
    data object NotSignedIn : SyncState()

    /** Sync is idle, waiting for changes or scheduled sync */
    data class Idle(
        val lastSyncTimestamp: Long,
        val pendingChangesCount: Int = 0
    ) : SyncState()

    /** Sync is currently in progress */
    data class Syncing(
        val progress: SyncProgress = SyncProgress()
    ) : SyncState()

    /** Sync completed successfully */
    data class Success(
        val result: SyncResult,
        val timestamp: Long
    ) : SyncState()

    /** Sync failed with an error */
    data class Error(
        val message: String,
        val isRetryable: Boolean = true,
        val lastAttemptTimestamp: Long = 0L
    ) : SyncState()

    /** Device is offline, sync will resume when connected */
    data object Offline : SyncState()

    /** There are unresolved conflicts requiring user attention */
    data class HasConflicts(
        val conflictCount: Int,
        val conflictIds: List<String>
    ) : SyncState()
}

/**
 * Progress information during sync.
 */
data class SyncProgress(
    val phase: SyncPhase = SyncPhase.IDLE,
    val currentItem: Int = 0,
    val totalItems: Int = 0
) {
    val progressPercent: Int
        get() = if (totalItems > 0) (currentItem * 100) / totalItems else 0
}

/**
 * Phases of a sync operation.
 */
enum class SyncPhase {
    IDLE,
    STARTING,
    PUSHING_BOOKS,
    PUSHING_SHELVES,
    PULLING_BOOKS,
    PULLING_SHELVES,
    RESOLVING_CONFLICTS,
    FINALIZING
}
