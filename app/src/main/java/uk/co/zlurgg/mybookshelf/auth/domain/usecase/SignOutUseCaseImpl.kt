package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class SignOutUseCaseImpl(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
    private val syncScheduler: SyncSchedulerService,
    private val clearUserData: ClearUserDataUseCase,
    private val currentUserProvider: CurrentUserProvider,
    private val syncRepository: SyncRepository
) : SignOutUseCase {
    companion object {
        private const val TAG = "SignOut"
    }

    override suspend operator fun invoke(): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN-OUT START ===")

        // Cancel all sync work before signing out
        Timber.tag(TAG).d("Cancelling sync work")
        syncScheduler.cancelAllSync()

        // Clear user data before signing out (preserves guest and system data)
        val userId = currentUserProvider.getCurrentUserId()
        if (userId != null) {
            Timber.tag(TAG).d("Clearing user data for: %s", userId)
            when (val clearResult = clearUserData(userId)) {
                is Result.Success -> {
                    Timber.tag(TAG).d("Cleared %d items", clearResult.data)
                }
                is Result.Error -> {
                    // Log but don't fail sign-out if clearing fails
                    Timber.tag(TAG).w("Failed to clear user data: %s", clearResult.error)
                }
            }

            // Clear sync metadata so next sign-in triggers a full sync
            Timber.tag(TAG).d("Clearing sync metadata for: %s", userId)
            syncRepository.clearSyncData(userId)
        } else {
            Timber.tag(TAG).d("No user signed in, skipping data clearing")
        }

        return when (val result = authService.signOut()) {
            is Result.Success -> {
                when (val stateResult = authStateRepository.setSignedInState(false)) {
                    is Result.Success -> { /* State saved successfully */ }
                    is Result.Error -> {
                        Timber.tag(TAG).w("Failed to save auth state: %s", stateResult.error)
                        // Continue anyway - Firebase sign-out succeeded
                    }
                }
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
