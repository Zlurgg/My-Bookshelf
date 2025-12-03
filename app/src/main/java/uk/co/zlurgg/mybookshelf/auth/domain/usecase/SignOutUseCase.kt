package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthRepository
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository

class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val authStateRepository: AuthStateRepository
) {
    suspend operator fun invoke() {
        Timber.tag(TAG).d("=== SIGN OUT ===")
        authRepository.signOut()
        authStateRepository.setSignedInState(false)
        Timber.tag(TAG).d("Sign-out complete, state cleared")
    }

    companion object {
        private const val TAG = "SignOut"
    }
}
