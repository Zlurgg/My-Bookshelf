package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef

/**
 * Book-shelf cross-reference operations: relationships, queries, sync status.
 */
interface CrossRefDao {
    @Upsert
    suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun deleteCrossRef(shelfId: String, bookId: String)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    suspend fun deleteAllCrossRefsForShelf(shelfId: String)

    @Query(
        """
        SELECT b.* FROM BookEntity b
        INNER JOIN BookshelfBookCrossRef s ON b.id = s.bookId
        WHERE s.shelfId = :shelfId AND s.syncStatus != 'DELETED'
        ORDER BY s.addedAt DESC
        """
    )
    fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND syncStatus != 'DELETED'")
    fun getBookCountForShelf(shelfId: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM BookshelfBookCrossRef WHERE bookId = :bookId AND syncStatus != 'DELETED')")
    fun isBookInAnyShelf(bookId: String): Flow<Boolean>

    @Query("SELECT shelfId FROM BookshelfBookCrossRef WHERE bookId = :bookId AND syncStatus != 'DELETED'")
    fun getShelvesForBook(bookId: String): Flow<List<String>>

    @Query("SELECT * FROM BookshelfBookCrossRef WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncCrossRefs(): List<BookshelfBookCrossRef>

    @Query(
        """
        UPDATE BookshelfBookCrossRef
        SET syncStatus = :status, lastModifiedAt = :timestamp
        WHERE shelfId = :shelfId AND bookId = :bookId
        """
    )
    suspend fun updateCrossRefSyncStatus(shelfId: String, bookId: String, status: String, timestamp: Long)

    @Query(
        "UPDATE BookshelfBookCrossRef SET syncStatus = :status, lastModifiedAt = :timestamp WHERE shelfId = :shelfId"
    )
    suspend fun markAllCrossRefsForShelfAs(shelfId: String, status: String, timestamp: Long)

    @Query(
        "DELETE FROM BookshelfBookCrossRef WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :ownerId)"
    )
    suspend fun deleteAllCrossRefsForOwner(ownerId: String)

    @Query(
        """
        DELETE FROM BookshelfBookCrossRef
        WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :userId AND isBookClub = 1)
        """
    )
    suspend fun deleteCrossRefsForClubShelves(userId: String)

    @Query(
        """
        UPDATE BookshelfBookCrossRef SET syncStatus = 'SYNCED'
        WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :userId)
        """
    )
    suspend fun resetCrossRefSyncStatusForOwner(userId: String)
}
