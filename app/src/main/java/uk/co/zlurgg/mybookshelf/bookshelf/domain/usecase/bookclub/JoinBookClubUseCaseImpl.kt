package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of JoinBookClubUseCase.
 *
 * Coordinates the join flow by:
 * 1. Validating user is signed in
 * 2. Checking if already a member
 * 3. Getting club metadata for the shelf name
 * 4. Delegating the actual join operation to the repository
 */
class JoinBookClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository,
    private val authService: AuthService
) : JoinBookClubUseCase {

    override suspend fun invoke(code: String): Result<JoinResult, DataError.Sync> {
        Timber.tag(TAG).d("Attempting to join book club: $code")

        // 1. Validate user is signed in
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).d("User not signed in, cannot join club")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        // 2. Check if already a member
        val memberCheckResult = bookClubRepository.isMemberOfClub(code)
        when (memberCheckResult) {
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to check membership: ${memberCheckResult.error}")
                return Result.Error(memberCheckResult.error)
            }
            is Result.Success -> {
                if (memberCheckResult.data) {
                    // User is already a member - get the existing shelf
                    Timber.tag(TAG).d("User is already a member of club $code")
                    val existingShelf = bookClubRepository.getLocalShelfForClub(code)
                    return if (existingShelf != null) {
                        Result.Success(JoinResult.AlreadyMember(existingShelf.id))
                    } else {
                        // Edge case: member in Firestore but no local shelf
                        // Proceed with join to recreate local data
                        Timber.tag(TAG).w("Member in Firestore but no local shelf, proceeding with join")
                        performJoin(code)
                    }
                }
            }
        }

        // 3. Get club metadata to verify it exists
        val clubResult = bookClubRepository.getBookClub(code)
        when (clubResult) {
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get club metadata: ${clubResult.error}")
                return Result.Error(clubResult.error)
            }
            is Result.Success -> {
                if (clubResult.data == null) {
                    Timber.tag(TAG).d("Club not found: $code")
                    return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
                }
            }
        }

        // 4. Perform the join
        return performJoin(code)
    }

    private suspend fun performJoin(code: String): Result<JoinResult, DataError.Sync> {
        val joinResult = bookClubRepository.joinBookClub(code)

        return when (joinResult) {
            is Result.Success -> {
                val localShelfId = joinResult.data
                // Get the shelf name for the result
                val shelf = bookClubRepository.getLocalShelfForClub(code)
                val shelfName = shelf?.name ?: "Book Club"

                Timber.tag(TAG).d("Successfully joined club $code, local shelf: $localShelfId")
                Result.Success(JoinResult.Success(localShelfId, shelfName))
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to join club: ${joinResult.error}")
                Result.Error(joinResult.error)
            }
        }
    }

    companion object {
        private const val TAG = "JoinBookClub"
    }
}
