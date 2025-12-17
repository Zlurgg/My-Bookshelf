package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Repository for Book Club operations.
 * Handles both local storage and Firestore sync for collaborative shelves.
 */
interface BookClubRepository {

    // ========== Club Management ==========

    /**
     * Creates a new book club from an existing shelf.
     * Uploads the shelf's books to Firestore and adds current user as first member.
     *
     * @param shelfId The local shelf ID to create the club from
     * @return The generated club code on success
     */
    suspend fun createBookClub(shelfId: String): Result<String, DataError.Sync>

    /**
     * Gets book club metadata by code.
     * @return BookClub if found, null if not found, or error
     */
    suspend fun getBookClub(code: String): Result<BookClub?, DataError.Sync>

    /**
     * Deletes a book club and cleans up local membership records.
     * Only the creator can delete a club.
     *
     * @param code The club code to delete
     */
    suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync>

    // ========== Membership ==========

    /**
     * Observes all book club memberships for the current user.
     */
    fun observeMyBookClubs(): Flow<List<BookClubMembership>>

    /**
     * Gets the local shelf associated with a book club.
     */
    suspend fun getLocalShelfForClub(code: String): Bookshelf?

    // ========== Books ==========

    /**
     * Gets all books in a book club.
     */
    suspend fun getClubBooks(code: String): Result<List<Book>, DataError.Sync>
}
