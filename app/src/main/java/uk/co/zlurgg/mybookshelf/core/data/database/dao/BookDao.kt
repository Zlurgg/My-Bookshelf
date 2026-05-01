package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity

/**
 * Book entity operations: CRUD, sync status, owner management.
 */
interface BookDao {
    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM BookEntity WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("DELETE FROM BookEntity WHERE id = :id")
    suspend fun deleteBook(id: String)

    @Query("SELECT * FROM BookEntity WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncBooks(): List<BookEntity>

    @Query("UPDATE BookEntity SET syncStatus = :status, lastModifiedAt = :timestamp WHERE id = :id")
    suspend fun updateBookSyncStatus(id: String, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM BookEntity WHERE ownerId IS NULL")
    suspend fun countOrphanBooks(): Int

    @Query("UPDATE BookEntity SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun assignOwnerToOrphanBooks(userId: String)

    @Query("SELECT * FROM BookEntity WHERE ownerId = :ownerId")
    suspend fun getBooksByOwner(ownerId: String): List<BookEntity>

    @Query("UPDATE BookEntity SET syncStatus = 'PENDING' WHERE ownerId = :ownerId")
    suspend fun markAllBooksPending(ownerId: String)

    @Query("DELETE FROM BookEntity WHERE ownerId = :ownerId")
    suspend fun deleteAllBooksForOwner(ownerId: String)

    @Query("UPDATE BookEntity SET ownerId = NULL, syncStatus = 'SYNCED' WHERE ownerId = :userId")
    suspend fun revertBooksToGuest(userId: String)

    @Transaction
    suspend fun upsertBookWithSyncInit(book: BookEntity, initialTimestamp: Long) {
        val existing = getBookById(book.id)
        if (existing == null) {
            upsert(book.copy(lastModifiedAt = initialTimestamp))
        } else {
            upsert(
                book.copy(
                    lastModifiedAt = initialTimestamp,
                    cloudId = existing.cloudId,
                    version = existing.version + 1
                )
            )
        }
    }
}
