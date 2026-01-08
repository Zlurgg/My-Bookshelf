package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.SyncMetadataEntity

/**
 * Data Access Object for sync metadata operations.
 */
@Dao
interface SyncDao {
    @Upsert
    suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM SyncMetadataEntity WHERE userId = :userId")
    suspend fun getSyncMetadata(userId: String): SyncMetadataEntity?

    @Query("SELECT * FROM SyncMetadataEntity WHERE userId = :userId")
    fun observeSyncMetadata(userId: String): Flow<SyncMetadataEntity?>

    @Query("UPDATE SyncMetadataEntity SET lastSyncTimestamp = :timestamp WHERE userId = :userId")
    suspend fun updateLastSyncTimestamp(
        userId: String,
        timestamp: Long,
    )

    @Query("UPDATE SyncMetadataEntity SET syncInProgress = :inProgress WHERE userId = :userId")
    suspend fun updateSyncInProgress(
        userId: String,
        inProgress: Boolean,
    )

    @Query("UPDATE SyncMetadataEntity SET lastSyncError = :error WHERE userId = :userId")
    suspend fun updateLastSyncError(
        userId: String,
        error: String?,
    )

    @Query("UPDATE SyncMetadataEntity SET pendingOperationsCount = :count WHERE userId = :userId")
    suspend fun updatePendingOperationsCount(
        userId: String,
        count: Int,
    )

    @Query("DELETE FROM SyncMetadataEntity WHERE userId = :userId")
    suspend fun deleteSyncMetadata(userId: String)
}
