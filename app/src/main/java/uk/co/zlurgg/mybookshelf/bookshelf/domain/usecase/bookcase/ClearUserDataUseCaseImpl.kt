package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation that clears all local data for a user during sign-out.
 * This prevents data leakage when switching between accounts.
 */
class ClearUserDataUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val bookClubRepository: BookClubRepository
) : ClearUserDataUseCase {

    override suspend operator fun invoke(userId: String): Result<Int, DataError.Local> {
        Timber.tag(TAG).d("=== CLEARING USER DATA ===")
        Timber.tag(TAG).d("User ID: %s", userId)

        // Clear bookshelf data (shelves, books, cross-refs)
        val totalItems = when (val clearResult = bookcaseRepository.clearUserData(userId)) {
            is Result.Success -> clearResult.data
            is Result.Error -> return clearResult
        }

        Timber.tag(TAG).d("Cleared %d bookshelf items", totalItems)

        // Clear book club memberships
        when (val clearMembershipsResult = bookClubRepository.clearAllMemberships()) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return clearMembershipsResult
        }

        Timber.tag(TAG).d("Cleared book club memberships")
        Timber.tag(TAG).d("=== USER DATA CLEARED: %d items ===", totalItems)

        return Result.Success(totalItems)
    }

    companion object {
        private const val TAG = "ClearUserData"
    }
}
