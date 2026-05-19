package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Dao
import androidx.room.Transaction

/**
 * Composite DAO providing access to all bookshelf-related database operations.
 * Extends focused interfaces for better separation of concerns while maintaining
 * a single injection point for consumers that need multiple entity types.
 *
 * Individual interfaces:
 * - [BookDao]: Book entity CRUD
 * - [ShelfDao]: Shelf entity CRUD, owner operations
 * - [CrossRefDao]: Book-shelf relationship operations
 */
@Dao
interface BookshelfDao : BookDao, ShelfDao, CrossRefDao {

    // Cross-cutting transaction: coordinates BookDao.deleteBooksById + CrossRefDao.deleteAllCrossRefsForBooks.
    // Lives on the composite DAO because it spans both focused DAOs.
    // Must be a default method with body — Room doesn't support abstract @Transaction.
    @Transaction
    suspend fun deleteBooks(bookIds: List<String>) {
        deleteAllCrossRefsForBooks(bookIds)
        deleteBooksById(bookIds)
    }
}
