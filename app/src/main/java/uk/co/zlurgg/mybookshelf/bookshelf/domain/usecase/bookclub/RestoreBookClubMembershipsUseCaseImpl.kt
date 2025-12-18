package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of RestoreBookClubMembershipsUseCase.
 *
 * Queries Firestore for all book clubs where the user is a member,
 * then recreates local shelves and downloads books for each one.
 */
class RestoreBookClubMembershipsUseCaseImpl(
    private val bookClubRepository: BookClubRepository,
    private val authService: AuthService
) : RestoreBookClubMembershipsUseCase {

    override suspend fun invoke(): Result<RestoreResult, DataError.Sync> {
        Timber.tag(TAG).d("=== RESTORING BOOK CLUB MEMBERSHIPS ===")

        // 1. Validate user is signed in
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).d("User not signed in, cannot restore memberships")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        Timber.tag(TAG).d("Restoring memberships for user: %s", user.userId)

        // 2. Query Firestore for user's club memberships
        val membershipsResult = bookClubRepository.getRemoteClubMemberships(user.userId)
        when (membershipsResult) {
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get remote memberships: %s", membershipsResult.error)
                return Result.Error(membershipsResult.error)
            }
            is Result.Success -> {
                val clubCodes = membershipsResult.data
                Timber.tag(TAG).d("Found %d club memberships in Firestore", clubCodes.size)

                if (clubCodes.isEmpty()) {
                    Timber.tag(TAG).d("No club memberships to restore")
                    return Result.Success(RestoreResult(0, 0))
                }

                // 3. Restore each membership
                var restoredCount = 0
                var failedCount = 0

                for (code in clubCodes) {
                    val restoreResult = bookClubRepository.restoreClubMembership(code)
                    when (restoreResult) {
                        is Result.Success -> {
                            Timber.tag(TAG).d("Restored club: %s -> shelf: %s", code, restoreResult.data)
                            restoredCount++
                        }
                        is Result.Error -> {
                            Timber.tag(TAG).w("Failed to restore club %s: %s", code, restoreResult.error)
                            failedCount++
                        }
                    }
                }

                Timber.tag(TAG).d("=== RESTORE COMPLETE: %d restored, %d failed ===", restoredCount, failedCount)
                return Result.Success(RestoreResult(restoredCount, failedCount))
            }
        }
    }

    companion object {
        private const val TAG = "RestoreBookClubs"
    }
}
