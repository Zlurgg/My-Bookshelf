package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.MigrateLocalDataUseCase

class SignInUseCase(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
    private val syncScheduler: SyncSchedulerService,
    private val migrateLocalDataUseCase: MigrateLocalDataUseCase
) {
    companion object {
        private const val TAG = "SignIn"
    }

    suspend fun execute(): Result<UserData, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN-IN START ===")

        return when (val signInResult = authService.signIn()) {
            is Result.Success -> {
                Timber.tag(TAG).d("Sign-in successful, saving state")
                authStateRepository.setSignedInState(true)

                // Migrate local data to the signed-in user
                val userId = signInResult.data.userId
                Timber.tag(TAG).d("Running migration for user: %s", userId)
                when (val migrationResult = migrateLocalDataUseCase.execute(userId)) {
                    is Result.Success -> {
                        Timber.tag(TAG).d(
                            "Migration complete - Books: %d, Shelves: %d",
                            migrationResult.data.booksAssigned,
                            migrationResult.data.shelvesAssigned
                        )
                    }
                    is Result.Error -> {
                        // Log migration error but don't fail sign-in
                        Timber.tag(TAG).w("Migration failed: %s (continuing with sign-in)", migrationResult.error)
                    }
                }

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
