package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity

/**
 * Shelf entity operations: CRUD, sync status, sharing, owner management.
 */
interface ShelfDao {
    @Upsert
    suspend fun upsertShelf(shelf: BookshelfEntity)

    @Query("SELECT * FROM BookshelfEntity ORDER BY position ASC")
    fun getAllShelves(): Flow<List<BookshelfEntity>>

    @Query(
        """
        SELECT * FROM BookshelfEntity
        WHERE (ownerId IS NULL OR ownerId = :userId OR ownerId = '__system_tutorial__')
        AND syncStatus != 'DELETED'
        ORDER BY position ASC
        """
    )
    fun getShelvesForUser(userId: String?): Flow<List<BookshelfEntity>>

    @Query("SELECT * FROM BookshelfEntity WHERE id = :id")
    suspend fun getShelfById(id: String): BookshelfEntity?

    @Query("SELECT * FROM BookshelfEntity WHERE name = :name LIMIT 1")
    suspend fun getShelfByName(name: String): BookshelfEntity?

    @Query("DELETE FROM BookshelfEntity WHERE id = :id")
    suspend fun deleteShelf(id: String)

    @Query("SELECT * FROM BookshelfEntity WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncShelves(): List<BookshelfEntity>

    @Query("UPDATE BookshelfEntity SET syncStatus = :status, lastModifiedAt = :timestamp WHERE id = :id")
    suspend fun updateShelfSyncStatus(id: String, status: String, timestamp: Long)

    @Query("SELECT * FROM BookshelfEntity WHERE shareCode = :shareCode LIMIT 1")
    suspend fun getShelfByShareCode(shareCode: String): BookshelfEntity?

    @Query("UPDATE BookshelfEntity SET isShared = :isShared, shareCode = :shareCode WHERE id = :id")
    suspend fun updateShelfSharingStatus(id: String, isShared: Boolean, shareCode: String?)

    @Query("SELECT COUNT(*) FROM BookshelfEntity WHERE ownerId IS NULL")
    suspend fun countOrphanShelves(): Int

    @Query("UPDATE BookshelfEntity SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun assignOwnerToOrphanShelves(userId: String)

    @Query("SELECT * FROM BookshelfEntity WHERE ownerId = :ownerId ORDER BY position ASC")
    suspend fun getShelvesByOwner(ownerId: String): List<BookshelfEntity>

    @Query("UPDATE BookshelfEntity SET syncStatus = 'PENDING' WHERE ownerId = :ownerId")
    suspend fun markAllShelvesPending(ownerId: String)

    @Query("DELETE FROM BookshelfEntity WHERE ownerId = :ownerId")
    suspend fun deleteAllShelvesForOwner(ownerId: String)

    @Transaction
    suspend fun upsertShelfWithSyncInit(shelf: BookshelfEntity, initialTimestamp: Long) {
        val existing = getShelfById(shelf.id)
        if (existing == null) {
            upsertShelf(shelf.copy(lastModifiedAt = initialTimestamp))
        } else {
            upsertShelf(
                shelf.copy(
                    lastModifiedAt = initialTimestamp,
                    cloudId = existing.cloudId,
                    version = existing.version + 1,
                    isShared = existing.isShared,
                    shareCode = existing.shareCode,
                    isBookClub = existing.isBookClub,
                    clubCode = existing.clubCode
                )
            )
        }
    }
}
