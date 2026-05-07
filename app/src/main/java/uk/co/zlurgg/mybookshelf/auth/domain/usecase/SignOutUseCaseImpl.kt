package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SignOutUseCaseImpl(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val clubOperations: ClubOperations,
    private val bookcaseRepository: BookcaseRepository,
) : SignOutUseCase {

    override suspend operator fun invoke(): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN-OUT START ===")

        // Capture userId before sign-out (Firebase clears auth state on sign-out)
        val userId = currentUserProvider.getCurrentUserId()

        // Step 1: Firebase sign-out first
        val signOutResult = authService.signOut()
        if (signOutResult is Result.Error) {
            Timber.tag(TAG).e("Sign-out failed: %s", signOutResult.error)
            return signOutResult
        }

        when (val stateResult = authStateRepository.setSignedInState(false)) {
            is Result.Success -> { /* State saved successfully */ }
            is Result.Error -> Timber.tag(TAG).w("Failed to save auth state: %s", stateResult.error)
        }

        // Step 2: Clear club memberships
        if (userId != null) {
            when (val clearResult = clubOperations.clearAllMemberships()) {
                is Result.Success -> Timber.tag(TAG).d("Club memberships cleared")
                is Result.Error -> Timber.tag(TAG).w("Failed to clear memberships: %s", clearResult.error)
            }

            // Step 3: Delete club shelves
            when (val deleteResult = bookcaseRepository.deleteClubShelves(userId)) {
                is Result.Success -> Timber.tag(TAG).d("Club shelves deleted")
                is Result.Error -> Timber.tag(TAG).w("Failed to delete club shelves: %s", deleteResult.error)
            }
        }

        Timber.tag(TAG).d("=== SIGN-OUT COMPLETE ===")
        return Result.Success(Unit)
    }

    companion object {
        private const val TAG = "SignOut"
    }
}
