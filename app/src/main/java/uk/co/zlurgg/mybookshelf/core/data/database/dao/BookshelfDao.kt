package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Dao
import androidx.room.Transaction

/**
 * Composite DAO providing access to all bookshelf-related database operations.
 * Extends focused interfaces for better separation of concerns while maintaining
 * a single injection point for consumers that need multiple entity types.
 *
 * Individual interfaces:
 * - [BookDao]: Book entity CRUD, sync, owner operations (11 functions)
 * - [ShelfDao]: Shelf entity CRUD, sync, sharing, owner operations (16 functions)
 * - [CrossRefDao]: Book-shelf relationship operations (11 functions)
 */
@Dao
interface BookshelfDao : BookDao, ShelfDao, CrossRefDao {

    @Transaction
    suspend fun revertAllUserDataToGuest(userId: String) {
        // Delete club shelves entirely — the clubs are already deleted from Firestore
        deleteCrossRefsForClubShelves(userId)
        deleteClubShelvesForOwner(userId)
        // Revert remaining user data to guest ownership
        resetCrossRefSyncStatusForOwner(userId)
        revertBooksToGuest(userId)
        revertShelvesToGuest(userId)
    }

    @Transaction
    suspend fun revertOrphanedDataToGuest(): Boolean {
        val orphanedUserId = findOrphanedOwnerId() ?: return false
        revertAllUserDataToGuest(orphanedUserId)
        return true
    }
}
