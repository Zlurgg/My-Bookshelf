package uk.co.zlurgg.mybookshelf.sync.data.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.repository.UserPreferencesRepository
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.SyncUserPreferencesUseCase

/**
 * Implementation of SyncUserPreferencesUseCase.
 *
 * Fetches user preferences from Firestore and caches them locally.
 * Handles offline scenarios gracefully by returning success (uses local cache).
 */
class SyncUserPreferencesUseCaseImpl(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val currentUserProvider: CurrentUserProvider,
) : SyncUserPreferencesUseCase {
    override suspend fun execute(): Result<Unit, DataError.Sync> {
        val userId = currentUserProvider.getCurrentUserId()

        if (userId == null) {
            Timber.tag(TAG).d("No user signed in, skipping preferences sync")
            return Result.Success(Unit)
        }

        Timber.tag(TAG).d("=== SYNC USER PREFERENCES START ===")

        return when (val result = userPreferencesRepository.fetchAndCacheWelcomeShown(userId)) {
            is Result.Success -> {
                Timber.tag(TAG).d("=== SYNC USER PREFERENCES COMPLETE: welcomeShown=%s ===", result.data)
                Result.Success(Unit)
            }
            is Result.Error -> {
                // For network errors, return success - we'll use local cache
                // This handles offline scenarios gracefully
                when (result.error) {
                    DataError.Sync.NETWORK_ERROR,
                    DataError.Sync.UNKNOWN,
                    -> {
                        Timber.tag(TAG).w("Preferences sync failed (offline?), using local cache: %s", result.error)
                        Result.Success(Unit)
                    }
                    else -> {
                        Timber.tag(TAG).e("Preferences sync failed with critical error: %s", result.error)
                        Result.Error(result.error)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "UserPrefsSync"
    }
}
