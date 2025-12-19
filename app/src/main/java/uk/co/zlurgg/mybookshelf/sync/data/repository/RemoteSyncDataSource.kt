package uk.co.zlurgg.mybookshelf.sync.data.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubBookDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMetadataDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.UserPreferencesFirestoreDto

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

    // ==================== User Preferences ====================

    /**
     * Gets user preferences from the cloud.
     * Returns null if no preferences document exists.
     */
    suspend fun getUserPreferences(userId: String): Result<UserPreferencesFirestoreDto?, DataError.Sync>

    /**
     * Sets user preferences in the cloud.
     * Creates the document if it doesn't exist, updates if it does.
     */
    suspend fun setUserPreferences(
        userId: String,
        preferences: UserPreferencesFirestoreDto
    ): Result<Unit, DataError.Sync>

    // ==================== Book Clubs ====================

    /**
     * Creates a new book club with the given metadata.
     */
    suspend fun createBookClub(
        code: String,
        metadata: BookClubMetadataDto
    ): Result<Unit, DataError.Sync>

    /**
     * Gets book club metadata by code.
     * Returns null if club doesn't exist.
     */
    suspend fun getBookClubMetadata(code: String): Result<BookClubMetadataDto?, DataError.Sync>

    /**
     * Adds a member to a book club.
     */
    suspend fun addBookClubMember(
        code: String,
        member: BookClubMemberDto
    ): Result<Unit, DataError.Sync>

    /**
     * Gets all members of a book club.
     */
    suspend fun getBookClubMembers(code: String): Result<List<BookClubMemberDto>, DataError.Sync>

    /**
     * Checks if a user is a member of a book club.
     */
    suspend fun isMember(code: String, userId: String): Result<Boolean, DataError.Sync>

    /**
     * Adds a book to a book club.
     */
    suspend fun addBookToClub(
        code: String,
        book: BookClubBookDto
    ): Result<Unit, DataError.Sync>

    /**
     * Removes a book from a book club.
     */
    suspend fun removeBookFromClub(
        code: String,
        bookId: String
    ): Result<Unit, DataError.Sync>

    /**
     * Gets all books in a book club.
     */
    suspend fun getClubBooks(code: String): Result<List<BookClubBookDto>, DataError.Sync>

    /**
     * Updates the book/member counts in club metadata.
     */
    suspend fun updateBookClubCounts(
        code: String,
        bookCount: Int,
        memberCount: Int
    ): Result<Unit, DataError.Sync>

    /**
     * Deletes a book club and all its subcollections (members, books).
     */
    suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync>

    /**
     * Adds a club code to the user's membership list in their preferences.
     * This enables restoring book club shelves after sign-out/sign-in.
     */
    suspend fun addClubMembership(userId: String, clubCode: String): Result<Unit, DataError.Sync>

    /**
     * Removes a club code from the user's membership list in their preferences.
     */
    suspend fun removeClubMembership(userId: String, clubCode: String): Result<Unit, DataError.Sync>
}