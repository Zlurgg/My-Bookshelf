package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class CheckSignInStatusUseCaseImpl(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
) : CheckSignInStatusUseCase {

    override suspend operator fun invoke(): Boolean {
        val localState = when (val result = authStateRepository.isSignedIn()) {
            is Result.Success -> result.data
            is Result.Error -> {
                Timber.tag(TAG).w("Failed to check local auth state: %s", result.error)
                false
            }
        }
        val firebaseUser = authService.getSignedInUser()

        val isSignedIn = localState && firebaseUser != null

        Timber.tag(TAG).d(
            "Auth status - Local: %s, Firebase: %s, Result: %s",
            localState,
            firebaseUser != null,
            isSignedIn
        )

        return isSignedIn
    }

    companion object {
        private const val TAG = "AuthStatus"
    }
}
