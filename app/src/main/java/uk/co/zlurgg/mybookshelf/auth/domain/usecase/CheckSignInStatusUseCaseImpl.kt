package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class CheckSignInStatusUseCaseImpl(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
    private val bookcaseRepository: BookcaseRepository,
) : CheckSignInStatusUseCase {
    companion object {
        private const val TAG = "AuthStatus"
    }

    override suspend operator fun invoke(): Boolean {
        val localState = when (val result = authStateRepository.isSignedIn()) {
            is Result.Success -> result.data
            is Result.Error -> {
                Timber.tag(TAG).w("Failed to check local auth state: %s", result.error)
                false // Default to not signed in on error
            }
        }
        val firebaseUser = authService.getSignedInUser()

        val isSignedIn = localState && firebaseUser != null

        // Recovery: if not signed in, revert any orphaned user data to guest.
        // This catches the case where the process was killed between auth deletion
        // and finalizeLocalCleanup in DeleteAccountUseCaseImpl.
        // Assumes Firebase Auth is fully initialized by this point — this runs from
        // SignInViewModel.init after Koin and FirebaseEmulatorConfig setup.
        if (!isSignedIn) {
            when (val revertResult = bookcaseRepository.revertOrphanedDataToGuest()) {
                is Result.Success -> Timber.tag(TAG).d("Orphaned data reverted to guest")
                is Result.Error -> Timber.tag(TAG).e("Failed to revert orphaned data: %s", revertResult.error)
            }
        }

        Timber.tag(TAG).d(
            "Auth status - Local: %s, Firebase: %s, Result: %s",
            localState,
            firebaseUser != null,
            isSignedIn
        )

        return isSignedIn
    }
}
