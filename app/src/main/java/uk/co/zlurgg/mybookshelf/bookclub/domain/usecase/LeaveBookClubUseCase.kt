package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Leaves a book club.
 *
 * This use case handles the complete leave flow:
 * 1. Validates user is signed in
 * 2. Verifies user is a member (and not the creator)
 * 3. Removes user from Firestore members collection
 * 4. Decrements member count
 * 5. Removes club from user's membership settings
 * 6. Deletes local membership and shelf
 *
 * The creator cannot leave - they must delete the club instead.
 */
interface LeaveBookClubUseCase {
    suspend operator fun invoke(shelfId: String): Result<Unit, DataError.Sync>
}
