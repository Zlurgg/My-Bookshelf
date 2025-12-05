package uk.co.zlurgg.mybookshelf.sync.data.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto

/**
 * Interface for remote sync data operations.
 *
 * Abstracts the cloud storage backend (Firestore) for testability.
 * Uses DTOs directly since this is a data layer concern.
 */
interface RemoteSyncDataSource {

    // ==================== Books ====================

    /**
     * Uploads a book to the cloud.
     */
    suspend fun uploadBook(userId: String, book: BookFirestoreDto): Result<Unit, DataError.Sync>

    /**
     * Downloads a book from the cloud.
     */
    suspend fun downloadBook(userId: String, bookId: String): Result<BookFirestoreDto?, DataError.Sync>

    /**
     * Downloads all books modified since a given timestamp.
     */
    suspend fun downloadBooksSince(
        userId: String,
        sinceTimestamp: Long
    ): Result<List<BookFirestoreDto>, DataError.Sync>

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
        shelf: BookshelfFirestoreDto
    ): Result<Unit, DataError.Sync>

    /**
     * Downloads a bookshelf from the cloud.
     */
    suspend fun downloadBookshelf(
        userId: String,
        shelfId: String
    ): Result<BookshelfFirestoreDto?, DataError.Sync>

    /**
     * Downloads all bookshelves modified since a given timestamp.
     */
    suspend fun downloadBookshelvesSince(
        userId: String,
        sinceTimestamp: Long
    ): Result<List<BookshelfFirestoreDto>, DataError.Sync>

    /**
     * Deletes a bookshelf from the cloud.
     */
    suspend fun deleteBookshelf(userId: String, shelfId: String): Result<Unit, DataError.Sync>

    // ==================== Shared Shelves ====================

    /**
     * Registers a shelf as shared (creates entry in sharedShelves collection).
     */
    suspend fun shareShelf(sharedShelf: SharedShelfDto): Result<Unit, DataError.Sync>

    /**
     * Unshares a shelf (removes from sharedShelves collection).
     */
    suspend fun unshareShelf(shareCode: String): Result<Unit, DataError.Sync>

    /**
     * Gets shared shelf metadata by share code.
     */
    suspend fun getSharedShelf(shareCode: String): Result<SharedShelfDto?, DataError.Sync>

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
        books: List<BookFirestoreDto>
    ): Result<Int, DataError.Sync>

    /**
     * Uploads multiple bookshelves in a batch.
     */
    suspend fun uploadBookshelves(
        userId: String,
        shelves: List<BookshelfFirestoreDto>
    ): Result<Int, DataError.Sync>
}