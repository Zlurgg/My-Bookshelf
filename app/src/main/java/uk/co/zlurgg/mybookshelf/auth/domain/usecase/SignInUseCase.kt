package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import android.content.Context
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.model.SignInResult
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthRepository
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository

class SignInUseCase(
    private val authRepository: AuthRepository,
    private val authStateRepository: AuthStateRepository
) {
    suspend operator fun invoke(context: Context): SignInResult {
        Timber.tag(TAG).d("=== SIGN IN ATTEMPT ===")

        val result = authRepository.signIn(context)

        if (result.data != null) {
            Timber.tag(TAG).d("Sign-in successful for user: %s", result.data.userId)
            authStateRepository.setSignedInState(true)
        } else {
            Timber.tag(TAG).w("Sign-in failed: %s", result.errorMessage)
        }

        return result
    }

    companion object {
        private const val TAG = "SignIn"
    }
}
