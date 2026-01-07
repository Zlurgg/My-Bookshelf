package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview
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

    /**
     * Renames a book club. Only the creator can rename.
     * Updates both local shelf and Firestore metadata.
     *
     * @param code The club code
     * @param newName The new name for the club
     */
    suspend fun renameBookClub(code: String, newName: String): Result<Unit, DataError.Sync>

    /**
     * Updates a book club's style. Only the creator can update.
     * Updates Firestore metadata.
     *
     * @param code The club code
     * @param style The new style name
     */
    suspend fun updateClubStyle(code: String, style: String): Result<Unit, DataError.Sync>

    /**
     * Leaves a book club. Removes the user from the club's member list,
     * decrements member count, cleans up user settings, and deletes local data.
     * The creator cannot leave - they must delete the club instead.
     *
     * @param code The club code to leave
     */
    suspend fun leaveBookClub(code: String): Result<Unit, DataError.Sync>

    /**
     * Converts a book club shelf to a personal shelf.
     * Used when a club is deleted by its creator - the member keeps their books
     * but the shelf is no longer linked to the club.
     *
     * - Removes club properties from the shelf (isBookClub, clubCode, clubCreatorId)
     * - Deletes the local membership record
     * - Removes from user's remote preferences
     * - Keeps all books intact
     *
     * @param code The club code of the deleted club
     */
    suspend fun convertClubToPersonalShelf(code: String): Result<Unit, DataError.Sync>

    // ========== Membership ==========

    /**
     * Observes all book club memberships for the current user.
     */
    fun observeMyBookClubs(): Flow<List<BookClubMembership>>

    /**
     * Gets the local shelf associated with a book club.
     */
    suspend fun getLocalShelfForClub(code: String): Bookshelf?

    /**
     * Checks if the current user is a member of the specified book club.
     *
     * @param code The club code to check
     * @return true if user is a member, false otherwise
     */
    suspend fun isMemberOfClub(code: String): Result<Boolean, DataError.Sync>

    /**
     * Joins an existing book club.
     * Creates a local shelf linked to the club, adds user as member,
     * and downloads all club books.
     *
     * @param code The club code to join
     * @return The local shelf ID on success
     */
    suspend fun joinBookClub(code: String): Result<String, DataError.Sync>

    /**
     * Gets all book club codes where the user is a member from Firestore.
     * Used to restore memberships after sign-out/sign-in.
     *
     * @param userId The user ID to query memberships for
     * @return List of club codes the user is a member of
     */
    suspend fun getRemoteClubMemberships(userId: String): Result<List<String>, DataError.Sync>

    /**
     * Restores a book club membership by recreating local data from Firestore.
     * Creates local shelf and downloads books if needed.
     *
     * @param code The club code to restore
     * @return The local shelf ID on success
     */
    suspend fun restoreClubMembership(code: String): Result<String, DataError.Sync>

    // ========== Books ==========

    /**
     * Gets all books in a book club.
     */
    suspend fun getClubBooks(code: String): Result<List<Book>, DataError.Sync>

    /**
     * Syncs a book to a book club in Firestore.
     * Called when a book is added to a book club shelf.
     *
     * @param code The club code
     * @param book The book to sync
     */
    suspend fun syncBookToClub(code: String, book: Book): Result<Unit, DataError.Sync>

    /**
     * Removes a book from a book club in Firestore.
     * Called when a book is removed from a book club shelf.
     *
     * @param code The club code
     * @param bookId The book ID to remove
     */
    suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync>

    /**
     * Syncs books FROM the club to the local shelf.
     * Fetches all books from Firestore and adds any missing ones locally.
     * Also removes local books that were deleted from the club.
     *
     * @param code The club code
     * @param localShelfId The local shelf ID to sync books to
     * @return Number of books added/removed
     */
    suspend fun syncBooksFromClub(code: String, localShelfId: String): Result<SyncResult, DataError.Sync>

    // ========== Reviews ==========

    /**
     * Gets all reviews for a book in a book club.
     *
     * @param code The club code
     * @param bookId The book ID to get reviews for
     * @return List of reviews from all members
     */
    suspend fun getBookReviews(code: String, bookId: String): Result<List<BookClubReview>, DataError.Sync>

    /**
     * Creates or updates the current user's review for a book.
     *
     * @param code The club code
     * @param bookId The book ID being reviewed
     * @param rating The rating (0 = no rating, 1-5 = rated)
     * @param reviewText The review text (empty = no text)
     */
    suspend fun upsertBookReview(
        code: String,
        bookId: String,
        rating: Float,
        reviewText: String
    ): Result<Unit, DataError.Sync>

    /**
     * Deletes the current user's review for a book.
     *
     * @param code The club code
     * @param bookId The book ID to delete review for
     */
    suspend fun deleteBookReview(code: String, bookId: String): Result<Unit, DataError.Sync>

    // ========== Comments ==========

    /**
     * Gets all comments for a book in a book club.
     * Comments are ordered by created_at ascending (oldest first).
     *
     * @param code The club code
     * @param bookId The book ID to get comments for
     * @return List of comments from all members
     */
    suspend fun getBookComments(code: String, bookId: String): Result<List<BookClubComment>, DataError.Sync>

    /**
     * Adds a new comment for the current user.
     * Unlike reviews, users can add multiple comments.
     *
     * @param code The club code
     * @param bookId The book ID being commented on
     * @param text The comment text
     * @return The generated comment ID on success
     */
    suspend fun addBookComment(
        code: String,
        bookId: String,
        text: String
    ): Result<String, DataError.Sync>

    /**
     * Edits an existing comment's text.
     * Only the comment author can edit their comment.
     *
     * @param code The club code
     * @param bookId The book ID
     * @param commentId The comment ID to edit
     * @param newText The new text for the comment
     */
    suspend fun editBookComment(
        code: String,
        bookId: String,
        commentId: String,
        newText: String
    ): Result<Unit, DataError.Sync>

    /**
     * Deletes a comment.
     * Only the comment author can delete their comment.
     *
     * @param code The club code
     * @param bookId The book ID
     * @param commentId The comment ID to delete
     */
    suspend fun deleteBookComment(
        code: String,
        bookId: String,
        commentId: String
    ): Result<Unit, DataError.Sync>
}

/**
 * Result of syncing books from a club.
 */
data class SyncResult(
    val booksAdded: Int,
    val booksRemoved: Int
)
