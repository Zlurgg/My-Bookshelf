package uk.co.zlurgg.mybookshelf.sync.data.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.data.dto.UserPreferencesFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.domain.repository.UserPreferencesRepository

/**
 * Implementation of UserPreferencesRepository that syncs between
 * local DataStore and Firestore cloud storage.
 */
class UserPreferencesRepositoryImpl(
    private val remoteDataSource: RemoteSyncDataSource,
    private val welcomePreferences: WelcomePreferences,
    private val timeProvider: TimeProvider,
) : UserPreferencesRepository {
    override suspend fun fetchAndCacheWelcomeShown(userId: String): Result<Boolean, DataError.Sync> {
        Timber.tag(TAG).d("Fetching user preferences from cloud for user: %s", userId)

        return when (val result = remoteDataSource.getUserPreferences(userId)) {
            is Result.Success -> {
                val prefs = result.data
                val welcomeShown = prefs?.welcomeShown ?: false

                Timber.tag(TAG).d("Cloud preferences fetched: welcomeShown=%s", welcomeShown)

                // Cache to local DataStore
                if (welcomeShown) {
                    welcomePreferences.setWelcomeShown(userId)
                    Timber.tag(TAG).d("Cached welcomeShown=true to local DataStore")
                }

                Result.Success(welcomeShown)
            }
            is Result.Error -> {
                Timber.tag(TAG).w("Failed to fetch cloud preferences: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun setWelcomeShown(
        userId: String,
        shown: Boolean,
    ): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Setting welcomeShown=%s for user: %s", shown, userId)

        // Always write to local first (immediate UX)
        if (shown) {
            welcomePreferences.setWelcomeShown(userId)
            Timber.tag(TAG).d("Wrote welcomeShown to local DataStore")
        }

        // Then write to cloud
        val dto =
            UserPreferencesFirestoreDto(
                id = PREFERENCES_DOC_ID,
                welcomeShown = shown,
                lastModifiedAt = timeProvider.currentTimeMillis(),
            )

        return when (val result = remoteDataSource.setUserPreferences(userId, dto)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Successfully wrote preferences to cloud")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).w("Failed to write preferences to cloud: %s", result.error)
                // Note: Local write already succeeded, so user won't see welcome again on this device
                Result.Error(result.error)
            }
        }
    }

    companion object {
        private const val TAG = "UserPrefsSync"
        private const val PREFERENCES_DOC_ID = "preferences"
    }
}
