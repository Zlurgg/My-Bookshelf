package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import kotlinx.coroutines.flow.first
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
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
        Timber.tag(TAG).d("Attempting to join book club: %s", code)

        val validationError = validateJoinPreconditions()
        if (validationError != null) return validationError

        val existingResult = checkExistingMembership(code)
        if (existingResult != null) return existingResult

        return verifyClubAndJoin(code)
    }

    private suspend fun validateJoinPreconditions(): Result<JoinResult, DataError.Sync>? {
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).d("User not signed in, cannot join club")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        val currentBookClubs = bookClubRepository.observeMyBookClubs().first()
        if (currentBookClubs.size >= BookClub.MAX_BOOK_CLUBS) {
            Timber.tag(TAG).w("User has reached max book clubs limit: %d", BookClub.MAX_BOOK_CLUBS)
            return Result.Error(DataError.Sync.MAX_BOOK_CLUBS_REACHED)
        }

        return null
    }

    private suspend fun checkExistingMembership(code: String): Result<JoinResult, DataError.Sync>? {
        when (val memberCheckResult = bookClubRepository.isMemberOfClub(code)) {
            is Result.Error -> return Result.Error(memberCheckResult.error)
            is Result.Success -> {
                if (memberCheckResult.data) {
                    Timber.tag(TAG).d("User is already a member of club %s", code)
                    val existingShelf = bookClubRepository.getLocalShelfForClub(code)
                    return if (existingShelf != null) {
                        Result.Success(JoinResult.AlreadyMember(existingShelf.id))
                    } else {
                        Timber.tag(TAG).w("Member in Firestore but no local shelf, proceeding with join")
                        performJoin(code)
                    }
                }
            }
        }
        return null
    }

    private suspend fun verifyClubAndJoin(code: String): Result<JoinResult, DataError.Sync> {
        val clubResult = bookClubRepository.getBookClub(code)
        when (clubResult) {
            is Result.Error -> return Result.Error(clubResult.error)
            is Result.Success -> {
                if (clubResult.data == null) {
                    Timber.tag(TAG).d("Club not found: %s", code)
                    return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
                }
            }
        }
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

                Timber.tag(TAG).d("Successfully joined club %s, local shelf: %s", code, localShelfId)
                Result.Success(JoinResult.Success(localShelfId, shelfName))
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to join club: %s", joinResult.error)
                Result.Error(joinResult.error)
            }
        }
    }

    companion object {
        private const val TAG = "JoinBookClub"
    }
}
