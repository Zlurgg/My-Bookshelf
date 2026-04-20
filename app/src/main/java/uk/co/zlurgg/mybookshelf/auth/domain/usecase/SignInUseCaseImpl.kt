package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class SignInUseCaseImpl(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
    private val syncScheduler: SyncSchedulerService
) : SignInUseCase {
    companion object {
        private const val TAG = "SignIn"
    }

    override suspend operator fun invoke(idToken: String): Result<UserData, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN-IN START ===")

        return when (val signInResult = authService.signIn(idToken)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Sign-in successful, saving state")
                when (val stateResult = authStateRepository.setSignedInState(true)) {
                    is Result.Success -> { /* State saved successfully */ }
                    is Result.Error -> {
                        Timber.tag(TAG).w("Failed to save auth state: %s", stateResult.error)
                        // Continue anyway - Firebase auth succeeded
                    }
                }

                // Note: Guest data migration is handled separately by the ViewModel
                // after asking the user if they want to import guest data

                // Start background sync after successful sign-in
                Timber.tag(TAG).d("Scheduling sync after sign-in")
                syncScheduler.schedulePeriodicSync()
                syncScheduler.triggerImmediateSync()

                Timber.tag(TAG).d("=== SIGN-IN COMPLETE ===")
                signInResult
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Sign-in failed: %s", signInResult.error)
                signInResult
            }
        }
    }
}
