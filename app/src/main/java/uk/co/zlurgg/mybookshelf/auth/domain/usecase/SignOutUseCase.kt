package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SignOutUseCase(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository
) {
    companion object {
        private const val TAG = "SignOut"
    }

    suspend fun execute(): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN-OUT START ===")

        return when (val result = authService.signOut()) {
            is Result.Success -> {
                authStateRepository.setSignedInState(false)
                Timber.tag(TAG).d("=== SIGN-OUT COMPLETE ===")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Sign-out failed: %s", result.error)
                result
            }
        }
    }
}
