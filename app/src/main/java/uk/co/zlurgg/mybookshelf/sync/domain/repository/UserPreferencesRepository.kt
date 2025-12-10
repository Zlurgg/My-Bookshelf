package uk.co.zlurgg.mybookshelf.sync.domain.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Repository for syncing user preferences between local storage and cloud.
 *
 * Handles the synchronization of user-level preferences (like onboarding state)
 * so they persist across devices for the same account.
 */
interface UserPreferencesRepository {

    /**
     * Fetches welcome shown state from cloud and caches it locally.
     *
     * @param userId The Firebase user ID
     * @return The welcome shown state from cloud, or error if fetch fails
     */
    suspend fun fetchAndCacheWelcomeShown(userId: String): Result<Boolean, DataError.Sync>

    /**
     * Sets welcome shown state in both local cache and cloud.
     *
     * @param userId The Firebase user ID
     * @param shown Whether the welcome screen has been shown
     * @return Success or error
     */
    suspend fun setWelcomeShown(userId: String, shown: Boolean): Result<Unit, DataError.Sync>
}
