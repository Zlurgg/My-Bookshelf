package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService

class CheckSignInStatusUseCase(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository
) {
    companion object {
        private const val TAG = "AuthStatus"
    }

    suspend fun execute(): Boolean {
        val localState = authStateRepository.isSignedIn()
        val firebaseUser = authService.getSignedInUser()

        val isSignedIn = localState && firebaseUser != null

        Timber.tag(TAG).d("Auth status - Local: %s, Firebase: %s, Result: %s",
            localState, firebaseUser != null, isSignedIn)

        return isSignedIn
    }
}
