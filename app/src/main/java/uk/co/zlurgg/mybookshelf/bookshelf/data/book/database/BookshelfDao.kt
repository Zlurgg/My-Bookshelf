package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookshelfDao {
    // Books
    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM BookEntity WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("DELETE FROM BookEntity WHERE id = :id")
    suspend fun deleteBook(id: String)

    // Shelves
    @Upsert
    suspend fun upsertShelf(shelf: BookshelfEntity)

    @Query("SELECT * FROM BookshelfEntity ORDER BY position ASC")
    fun getAllShelves(): Flow<List<BookshelfEntity>>

    /**
     * Get shelves filtered by owner. Returns:
     * - User's shelves (ownerId = userId) when signed in
     * - Orphan shelves (ownerId IS NULL) for migration/backwards compatibility
     * - Only orphan shelves when userId is null (guest mode)
     */
    @Query("SELECT * FROM BookshelfEntity WHERE ownerId IS NULL OR ownerId = :userId ORDER BY position ASC")
    fun getShelvesForUser(userId: String?): Flow<List<BookshelfEntity>>

    @Query("SELECT * FROM BookshelfEntity WHERE id = :id")
    suspend fun getShelfById(id: String): BookshelfEntity?

    @Query("DELETE FROM BookshelfEntity WHERE id = :id")
    suspend fun deleteShelf(id: String)

    // Cross-ref
    @Upsert
    suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun deleteCrossRef(shelfId: String, bookId: String)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    suspend fun deleteAllCrossRefsForShelf(shelfId: String)

    // Queries
    @Query(
        "SELECT b.* FROM BookEntity b INNER JOIN BookshelfBookCrossRef s ON b.id = s.bookId WHERE s.shelfId = :shelfId ORDER BY s.addedAt DESC"
    )
    fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    fun getBookCountForShelf(shelfId: String): Flow<Int>

    // Book-centric queries
    @Query("SELECT EXISTS(SELECT 1 FROM BookshelfBookCrossRef WHERE bookId = :bookId)")
    fun isBookInAnyShelf(bookId: String): Flow<Boolean>

    @Query("SELECT shelfId FROM BookshelfBookCrossRef WHERE bookId = :bookId")
    fun getShelvesForBook(bookId: String): Flow<List<String>>

    // ========== Sync-related queries ==========

    // Get entities pending sync (syncStatus != 'SYNCED')
    @Query("SELECT * FROM BookEntity WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncBooks(): List<BookEntity>

    @Query("SELECT * FROM BookshelfEntity WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncShelves(): List<BookshelfEntity>

    @Query("SELECT * FROM BookshelfBookCrossRef WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncCrossRefs(): List<BookshelfBookCrossRef>

    // Update sync status
    @Query("UPDATE BookEntity SET syncStatus = :status, lastModifiedAt = :timestamp WHERE id = :id")
    suspend fun updateBookSyncStatus(id: String, status: String, timestamp: Long)

    @Query("UPDATE BookshelfEntity SET syncStatus = :status, lastModifiedAt = :timestamp WHERE id = :id")
    suspend fun updateShelfSyncStatus(id: String, status: String, timestamp: Long)

    @Query("UPDATE BookshelfBookCrossRef SET syncStatus = :status, lastModifiedAt = :timestamp WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun updateCrossRefSyncStatus(shelfId: String, bookId: String, status: String, timestamp: Long)

    // Sharing queries
    @Query("SELECT * FROM BookshelfEntity WHERE shareCode = :shareCode LIMIT 1")
    suspend fun getShelfByShareCode(shareCode: String): BookshelfEntity?

    @Query("UPDATE BookshelfEntity SET isShared = :isShared, shareCode = :shareCode WHERE id = :id")
    suspend fun updateShelfSharingStatus(id: String, isShared: Boolean, shareCode: String?)

    // Migration queries - assign owner to orphan entities (used when user signs in)
    @Query("SELECT COUNT(*) FROM BookEntity WHERE ownerId IS NULL")
    suspend fun countOrphanBooks(): Int

    @Query("SELECT COUNT(*) FROM BookshelfEntity WHERE ownerId IS NULL")
    suspend fun countOrphanShelves(): Int

    @Query("UPDATE BookEntity SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun assignOwnerToOrphanBooks(userId: String)

    @Query("UPDATE BookshelfEntity SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun assignOwnerToOrphanShelves(userId: String)

    // Query entities by owner
    @Query("SELECT * FROM BookEntity WHERE ownerId = :ownerId")
    suspend fun getBooksByOwner(ownerId: String): List<BookEntity>

    @Query("SELECT * FROM BookshelfEntity WHERE ownerId = :ownerId ORDER BY position ASC")
    suspend fun getShelvesByOwner(ownerId: String): List<BookshelfEntity>

    // Mark all entities as pending sync for a user (triggers full sync)
    @Query("UPDATE BookEntity SET syncStatus = 'PENDING' WHERE ownerId = :ownerId")
    suspend fun markAllBooksPending(ownerId: String)

    @Query("UPDATE BookshelfEntity SET syncStatus = 'PENDING' WHERE ownerId = :ownerId")
    suspend fun markAllShelvesPending(ownerId: String)

    // ========== Sign-out cleanup queries ==========
    // Delete all user's data when signing out to prevent data leakage to other accounts

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :ownerId)")
    suspend fun deleteAllCrossRefsForOwner(ownerId: String)

    @Query("DELETE FROM BookEntity WHERE ownerId = :ownerId")
    suspend fun deleteAllBooksForOwner(ownerId: String)

    @Query("DELETE FROM BookshelfEntity WHERE ownerId = :ownerId")
    suspend fun deleteAllShelvesForOwner(ownerId: String)
}