package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Dao

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
interface BookshelfDao : BookDao, ShelfDao, CrossRefDao
