package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Joins a user to an existing book club.
 *
 * This use case handles the complete join flow:
 * 1. Validates user is signed in
 * 2. Checks if already a member
 * 3. Creates a local shelf linked to the club
 * 4. Adds the user as a member in Firestore
 * 5. Downloads all club books to the local shelf
 *
 * @return JoinResult indicating success or that user is already a member
 */
interface JoinBookClubUseCase {
    suspend operator fun invoke(code: String): Result<JoinResult, DataError.Sync>
}

/**
 * Result of attempting to join a book club.
 */
sealed class JoinResult {
    /**
     * Successfully joined the book club.
     * @param localShelfId The ID of the newly created local shelf
     * @param shelfName The name of the local shelf (includes "(Book Club)" suffix)
     */
    data class Success(
        val localShelfId: String,
        val shelfName: String
    ) : JoinResult()

    /**
     * User is already a member of this book club.
     * @param localShelfId The ID of the existing local shelf for this club
     */
    data class AlreadyMember(val localShelfId: String) : JoinResult()
}
