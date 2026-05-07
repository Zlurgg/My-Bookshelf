package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity

/**
 * Shelf entity operations: CRUD, owner management.
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

    @Query("DELETE FROM BookshelfEntity WHERE ownerId = :userId AND isBookClub = 1")
    suspend fun deleteClubShelvesForOwner(userId: String)
}
