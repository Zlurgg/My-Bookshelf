package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SignInUseCase(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository
) {
    companion object {
        private const val TAG = "SignIn"
    }

    suspend fun execute(): Result<UserData, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN-IN START ===")

        return when (val result = authService.signIn()) {
            is Result.Success -> {
                Timber.tag(TAG).d("Sign-in successful, saving state")
                authStateRepository.setSignedInState(true)
                Timber.tag(TAG).d("=== SIGN-IN COMPLETE ===")
                result
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Sign-in failed: %s", result.error)
                result
            }
        }
    }
}
