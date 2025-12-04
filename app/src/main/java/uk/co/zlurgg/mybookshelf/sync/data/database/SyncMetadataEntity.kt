package uk.co.zlurgg.mybookshelf.sync.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores sync state metadata for each user.
 * Tracks when sync was last performed and any pending operations.
 */
@Entity
data class SyncMetadataEntity(
    @PrimaryKey val userId: String,
    val lastSyncTimestamp: Long,
    val syncInProgress: Boolean = false,
    val lastSyncError: String? = null,
    val pendingOperationsCount: Int = 0
)
