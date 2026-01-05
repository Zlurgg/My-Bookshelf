package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case to validate all local book club memberships against Firestore.
 *
 * Checks each local book club membership to verify the club still exists.
 * If a club has been deleted, cleans up the local data and returns the club name
 * so the UI can notify the user.
 *
 * This should be called when the Bookcase screen loads to detect clubs that
 * were deleted by their creators while this user was offline/away.
 *
 * @return Success with list of deleted club names (empty if all valid), or Error if validation failed
 */
interface ValidateBookClubMembershipsUseCase {
    suspend operator fun invoke(): Result<List<String>, DataError.Sync>
}
