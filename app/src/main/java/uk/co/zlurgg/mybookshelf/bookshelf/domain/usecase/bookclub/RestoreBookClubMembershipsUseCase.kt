package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Restores book club memberships from Firestore after sign-in.
 *
 * When a user signs out, their local data is cleared for security.
 * When they sign back in, this use case queries Firestore to find
 * all book clubs they're a member of and recreates the local data.
 *
 * @return RestoreResult with count of restored memberships
 */
interface RestoreBookClubMembershipsUseCase {
    suspend operator fun invoke(): Result<RestoreResult, DataError.Sync>
}

/**
 * Result of restoring book club memberships.
 */
data class RestoreResult(
    val restoredCount: Int,
    val failedCount: Int,
)
