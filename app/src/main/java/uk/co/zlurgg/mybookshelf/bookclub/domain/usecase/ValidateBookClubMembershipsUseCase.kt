package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Result of validating book club memberships.
 *
 * @param deletedClubNames Names of clubs that were deleted by their creator (converted to personal shelves)
 * @param memberCounts Map of clubCode to memberCount for existing clubs
 */
data class ValidationResult(
    val deletedClubNames: List<String>,
    val memberCounts: Map<String, Int>
)

/**
 * Use case to validate all local book club memberships against Firestore.
 *
 * Checks each local book club membership to verify the club still exists.
 * If a club has been deleted, cleans up the local data and returns the club name
 * so the UI can notify the user. Also collects member counts from existing clubs.
 *
 * This should be called when the Bookcase screen loads to detect clubs that
 * were deleted by their creators while this user was offline/away.
 *
 * @return Success with validation result, or Error if validation failed
 */
interface ValidateBookClubMembershipsUseCase {
    suspend operator fun invoke(): Result<ValidationResult, DataError.Sync>
}
