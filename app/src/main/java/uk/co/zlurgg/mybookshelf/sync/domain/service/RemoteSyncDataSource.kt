package uk.co.zlurgg.mybookshelf.sync.domain.service

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.SharedShelf
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncBook
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncBookshelf

/**
 * Interface for remote sync data operations.
 *
 * Abstracts the cloud storage backend (Firestore) for testability.
 * Uses domain models to maintain Clean Architecture principles.
 */
interface RemoteSyncDataSource {

    // ==================== Books ====================

    /**
     * Uploads a book to the cloud.
     */
    suspend fun uploadBook(userId: String, book: SyncBook): Result<Unit, DataError.Sync>

    /**
     * Downloads a book from the cloud.
     */
    suspend fun downloadBook(userId: String, bookId: String): Result<SyncBook?, DataError.Sync>

    /**
     * Downloads all books modified since a given timestamp.
     */
    suspend fun downloadBooksSince(
        userId: String,
        sinceTimestamp: Long
    ): Result<List<SyncBook>, DataError.Sync>

    /**
     * Deletes a book from the cloud.
     */
    suspend fun deleteBook(userId: String, bookId: String): Result<Unit, DataError.Sync>

    // ==================== Bookshelves ====================

    /**
     * Uploads a bookshelf to the cloud.
     */
    suspend fun uploadBookshelf(
        userId: String,
        shelf: SyncBookshelf
    ): Result<Unit, DataError.Sync>

    /**
     * Downloads a bookshelf from the cloud.
     */
    suspend fun downloadBookshelf(
        userId: String,
        shelfId: String
    ): Result<SyncBookshelf?, DataError.Sync>

    /**
     * Downloads all bookshelves modified since a given timestamp.
     */
    suspend fun downloadBookshelvesSince(
        userId: String,
        sinceTimestamp: Long
    ): Result<List<SyncBookshelf>, DataError.Sync>

    /**
     * Deletes a bookshelf from the cloud.
     */
    suspend fun deleteBookshelf(userId: String, shelfId: String): Result<Unit, DataError.Sync>

    // ==================== Shared Shelves ====================

    /**
     * Registers a shelf as shared (creates entry in sharedShelves collection).
     */
    suspend fun shareShelf(sharedShelf: SharedShelf): Result<Unit, DataError.Sync>

    /**
     * Unshares a shelf (removes from sharedShelves collection).
     */
    suspend fun unshareShelf(shareCode: String): Result<Unit, DataError.Sync>

    /**
     * Gets shared shelf metadata by share code.
     */
    suspend fun getSharedShelf(shareCode: String): Result<SharedShelf?, DataError.Sync>

    /**
     * Adds current user as subscriber to a shared shelf.
     */
    suspend fun subscribeToShelf(shareCode: String, userId: String): Result<Unit, DataError.Sync>

    /**
     * Removes current user from subscribers of a shared shelf.
     */
    suspend fun unsubscribeFromShelf(shareCode: String, userId: String): Result<Unit, DataError.Sync>

    // ==================== Batch Operations ====================

    /**
     * Uploads multiple books in a batch.
     */
    suspend fun uploadBooks(
        userId: String,
        books: List<SyncBook>
    ): Result<Int, DataError.Sync>

    /**
     * Uploads multiple bookshelves in a batch.
     */
    suspend fun uploadBookshelves(
        userId: String,
        shelves: List<SyncBookshelf>
    ): Result<Int, DataError.Sync>
}
