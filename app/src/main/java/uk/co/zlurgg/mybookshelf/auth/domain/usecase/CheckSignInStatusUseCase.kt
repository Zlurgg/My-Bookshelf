package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthRepository
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository

class CheckSignInStatusUseCase(
    private val authRepository: AuthRepository,
    private val authStateRepository: AuthStateRepository
) {
    suspend operator fun invoke(): Boolean {
        val localState = authStateRepository.isSignedIn()
        val currentUser = authRepository.getSignedInUser()

        val isSignedIn = localState && currentUser != null

        Timber.tag(TAG).d(
            "Auth status check - Local: %s, Firebase user: %s, Result: %s",
            localState,
            currentUser != null,
            isSignedIn
        )

        return isSignedIn
    }

    companion object {
        private const val TAG = "AuthStatus"
    }
}
