package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SignOutUseCaseImpl(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
) : SignOutUseCase {

    override suspend operator fun invoke(): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN-OUT START ===")

        val signOutResult = authService.signOut()
        if (signOutResult is Result.Error) {
            Timber.tag(TAG).e("Sign-out failed: %s", signOutResult.error)
            return signOutResult
        }

        when (val stateResult = authStateRepository.setSignedInState(false)) {
            is Result.Success -> { /* State saved successfully */ }
            is Result.Error -> Timber.tag(TAG).w("Failed to save auth state: %s", stateResult.error)
        }

        Timber.tag(TAG).d("=== SIGN-OUT COMPLETE ===")
        return Result.Success(Unit)
    }

    companion object {
        private const val TAG = "SignOut"
    }
}
