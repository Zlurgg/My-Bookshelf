package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Represents a conflict between local and remote versions of an entity.
 */
data class SyncConflict(
    /** ID of the conflicting entity */
    val entityId: String,

    /** Type of entity (book or shelf) */
    val entityType: EntityType,

    /** Local version timestamp */
    val localTimestamp: Long,

    /** Remote version timestamp */
    val remoteTimestamp: Long,

    /** Local version number */
    val localVersion: Long,

    /** Remote version number */
    val remoteVersion: Long,

    /** Human-readable description of local changes */
    val localChangeDescription: String? = null,

    /** Human-readable description of remote changes */
    val remoteChangeDescription: String? = null
)

/**
 * Types of entities that can be synced.
 */
enum class EntityType {
    BOOK,
    BOOKSHELF,
    CROSS_REF
}

/**
 * Resolution strategy for a conflict.
 */
sealed class ConflictResolution {
    /** Keep the local version, overwrite remote */
    data object KeepLocal : ConflictResolution()

    /** Keep the remote version, overwrite local */
    data object KeepRemote : ConflictResolution()

    /** Use the version with the latest timestamp (last-write-wins) */
    data object LastWriteWins : ConflictResolution()

    /** Merge changes (where possible) */
    data object Merge : ConflictResolution()

    /** Skip this conflict, leave unresolved */
    data object Skip : ConflictResolution()
}

/**
 * Strategy for automatic conflict resolution.
 */
enum class ConflictStrategy {
    /** Always use local version */
    LOCAL_WINS,

    /** Always use remote version */
    REMOTE_WINS,

    /** Use version with latest timestamp */
    LAST_WRITE_WINS,

    /** Ask user for each conflict */
    ASK_USER
}
