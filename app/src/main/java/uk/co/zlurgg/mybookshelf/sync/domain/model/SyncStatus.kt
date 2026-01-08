package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Represents the sync status of an entity.
 */
enum class SyncStatus {
    /** Entity is synced with cloud */
    SYNCED,

    /** Entity has local changes pending upload */
    PENDING,

    /** Entity has conflicting changes between local and cloud */
    CONFLICT,

    /** Entity is marked for deletion (soft delete for sync) */
    DELETED,

    ;

    companion object {
        fun fromString(value: String): SyncStatus {
            return entries.find { it.name == value } ?: PENDING
        }
    }
}
