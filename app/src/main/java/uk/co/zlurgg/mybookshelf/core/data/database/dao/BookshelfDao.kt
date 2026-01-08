package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity

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
     * - System shelves (ownerId = '__system_tutorial__') - always visible
     * - Only orphan + system shelves when userId is null (guest mode)
     *
     * Note: The system ownerId is hardcoded here because Room requires compile-time constants.
     * See SystemOwnerIds.TUTORIAL for the canonical constant.
     */
    @Query(
        "SELECT * FROM BookshelfEntity WHERE (ownerId IS NULL OR ownerId = :userId OR ownerId = '__system_tutorial__') AND syncStatus != 'DELETED' ORDER BY position ASC",
    )
    fun getShelvesForUser(userId: String?): Flow<List<BookshelfEntity>>

    @Query("SELECT * FROM BookshelfEntity WHERE id = :id")
    suspend fun getShelfById(id: String): BookshelfEntity?

    @Query("SELECT * FROM BookshelfEntity WHERE name = :name LIMIT 1")
    suspend fun getShelfByName(name: String): BookshelfEntity?

    @Query("DELETE FROM BookshelfEntity WHERE id = :id")
    suspend fun deleteShelf(id: String)

    // Cross-ref
    @Upsert
    suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun deleteCrossRef(
        shelfId: String,
        bookId: String,
    )

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    suspend fun deleteAllCrossRefsForShelf(shelfId: String)

    // Queries
    @Query(
        "SELECT b.* FROM BookEntity b INNER JOIN BookshelfBookCrossRef s ON b.id = s.bookId WHERE s.shelfId = :shelfId AND s.syncStatus != 'DELETED' ORDER BY s.addedAt DESC",
    )
    fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND syncStatus != 'DELETED'")
    fun getBookCountForShelf(shelfId: String): Flow<Int>

    // Book-centric queries
    @Query("SELECT EXISTS(SELECT 1 FROM BookshelfBookCrossRef WHERE bookId = :bookId AND syncStatus != 'DELETED')")
    fun isBookInAnyShelf(bookId: String): Flow<Boolean>

    @Query("SELECT shelfId FROM BookshelfBookCrossRef WHERE bookId = :bookId AND syncStatus != 'DELETED'")
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
    suspend fun updateBookSyncStatus(
        id: String,
        status: String,
        timestamp: Long,
    )

    @Query("UPDATE BookshelfEntity SET syncStatus = :status, lastModifiedAt = :timestamp WHERE id = :id")
    suspend fun updateShelfSyncStatus(
        id: String,
        status: String,
        timestamp: Long,
    )

    @Query(
        "UPDATE BookshelfBookCrossRef SET syncStatus = :status, lastModifiedAt = :timestamp WHERE shelfId = :shelfId AND bookId = :bookId",
    )
    suspend fun updateCrossRefSyncStatus(
        shelfId: String,
        bookId: String,
        status: String,
        timestamp: Long,
    )

    @Query(
        "UPDATE BookshelfBookCrossRef SET syncStatus = :status, lastModifiedAt = :timestamp WHERE shelfId = :shelfId",
    )
    suspend fun markAllCrossRefsForShelfAs(
        shelfId: String,
        status: String,
        timestamp: Long,
    )

    // Sharing queries
    @Query("SELECT * FROM BookshelfEntity WHERE shareCode = :shareCode LIMIT 1")
    suspend fun getShelfByShareCode(shareCode: String): BookshelfEntity?

    @Query("UPDATE BookshelfEntity SET isShared = :isShared, shareCode = :shareCode WHERE id = :id")
    suspend fun updateShelfSharingStatus(
        id: String,
        isShared: Boolean,
        shareCode: String?,
    )

    // Migration queries - assign owner to orphan entities (used when user signs in)
    // Note: System entities have ownerId = '__system_tutorial__', so they're not counted as orphans
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

    // ========== Sync-aware upsert methods ==========
    // These methods properly initialize sync metadata for new entities and preserve it for updates.
    // This ensures lastModifiedAt is never 0, which would cause Firestore pull queries to miss documents.

    /**
     * Upsert a book with proper sync metadata initialization.
     *
     * For new books: Sets lastModifiedAt to initialTimestamp
     * For existing books: Preserves cloudId, increments version, updates lastModifiedAt
     *
     * This ensures local entities are never created with lastModifiedAt=0, which would
     * be filtered out by Firestore's whereGreaterThan(0) query on pull.
     */
    @Transaction
    suspend fun upsertBookWithSyncInit(
        book: BookEntity,
        initialTimestamp: Long,
    ) {
        val existing = getBookById(book.id)
        if (existing == null) {
            // New book - set lastModifiedAt to initialTimestamp
            upsert(book.copy(lastModifiedAt = initialTimestamp))
        } else {
            // Existing book - preserve sync metadata, update lastModifiedAt
            upsert(
                book.copy(
                    lastModifiedAt = initialTimestamp,
                    cloudId = existing.cloudId,
                    version = existing.version + 1,
                ),
            )
        }
    }

    /**
     * Upsert a shelf with proper sync metadata initialization.
     *
     * For new shelves: Sets lastModifiedAt to initialTimestamp
     * For existing shelves: Preserves cloudId, sharing status, increments version, updates lastModifiedAt
     */
    @Transaction
    suspend fun upsertShelfWithSyncInit(
        shelf: BookshelfEntity,
        initialTimestamp: Long,
    ) {
        val existing = getShelfById(shelf.id)
        if (existing == null) {
            // New shelf - set lastModifiedAt to initialTimestamp
            upsertShelf(shelf.copy(lastModifiedAt = initialTimestamp))
        } else {
            // Existing shelf - preserve sync metadata and local-only fields, update lastModifiedAt
            upsertShelf(
                shelf.copy(
                    lastModifiedAt = initialTimestamp,
                    cloudId = existing.cloudId,
                    version = existing.version + 1,
                    isShared = existing.isShared,
                    shareCode = existing.shareCode,
                    isBookClub = existing.isBookClub,
                    clubCode = existing.clubCode,
                ),
            )
        }
    }

    // ========== Sign-out cleanup queries ==========
    // Delete all user's data when signing out to prevent data leakage to other accounts

    @Query(
        "DELETE FROM BookshelfBookCrossRef WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :ownerId)",
    )
    suspend fun deleteAllCrossRefsForOwner(ownerId: String)

    @Query("DELETE FROM BookEntity WHERE ownerId = :ownerId")
    suspend fun deleteAllBooksForOwner(ownerId: String)

    @Query("DELETE FROM BookshelfEntity WHERE ownerId = :ownerId")
    suspend fun deleteAllShelvesForOwner(ownerId: String)
}
