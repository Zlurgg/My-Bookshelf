package uk.co.zlurgg.mybookshelf.account.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class DeleteAccountUseCaseImpl(
    private val currentUserProvider: CurrentUserProvider,
    private val clubOperations: ClubOperations,
    private val authService: AuthService,
    private val bookcaseRepository: BookcaseRepository,
    private val authStateRepository: AuthStateRepository,
) : DeleteAccountUseCase {

    @Suppress("ReturnCount")
    override suspend operator fun invoke(): Result<Unit, DataError> {
        val userId = currentUserProvider.getCurrentUserId()
            ?: return Result.Error(DataError.Local.AUTH_FAILED)

        // Delete clubs created by this user
        val createdResult = clubOperations.getClubsCreatedByUser(userId)
        if (createdResult is Result.Error) return createdResult
        val createdCodes = (createdResult as Result.Success).data

        for (code in createdCodes) {
            val deleteResult = clubOperations.deleteBookClub(code)
            if (deleteResult is Result.Error) return deleteResult
        }

        // Remove user from clubs they're a member of (excluding ones they created)
        val memberResult = clubOperations.getClubMembershipsForUser(userId)
        if (memberResult is Result.Error) return memberResult
        val memberCodes = (memberResult as Result.Success).data - createdCodes.toSet()

        for (code in memberCodes) {
            val removeResult = clubOperations.removeUserFromClub(code, userId)
            if (removeResult is Result.Error) return removeResult
        }

        // Delete user's Firestore document (must be before auth deletion — rules require auth)
        val deleteDocResult = clubOperations.deleteUserDocument(userId)
        if (deleteDocResult is Result.Error) {
            Timber.tag(TAG).w("Failed to delete user document: %s", deleteDocResult.error)
        }

        // Delete Firebase Auth account
        val authDeleteResult = authService.deleteAccount()
        if (authDeleteResult is Result.Error) return authDeleteResult

        // Auth succeeded — now safe to clean up local club data
        finalizeLocalCleanup(userId)

        return Result.Success(Unit)
    }

    override suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError> {
        // Capture userId before auth deletion nukes currentUser
        val userId = currentUserProvider.getCurrentUserId()
            ?: return Result.Error(DataError.Local.AUTH_FAILED)

        val reAuthResult = authService.reauthenticate(idToken)
        if (reAuthResult is Result.Error) return reAuthResult

        val deleteResult = authService.deleteAccount()
        if (deleteResult is Result.Error) return deleteResult

        // Auth succeeded — now safe to clean up local club data
        finalizeLocalCleanup(userId)

        return Result.Success(Unit)
    }

    private suspend fun finalizeLocalCleanup(userId: String) {
        when (val result = bookcaseRepository.deleteClubShelves(userId)) {
            is Result.Success -> Timber.tag(TAG).d("Club shelves deleted")
            is Result.Error -> Timber.tag(TAG).e("Failed to delete club shelves: %s", result.error)
        }
        when (val authResult = authStateRepository.setSignedInState(false)) {
            is Result.Success -> Unit
            is Result.Error -> Timber.tag(TAG).e("Failed to clear signed-in state: %s", authResult.error)
        }
    }

    companion object {
        private const val TAG = "DeleteAccount"
    }
}
