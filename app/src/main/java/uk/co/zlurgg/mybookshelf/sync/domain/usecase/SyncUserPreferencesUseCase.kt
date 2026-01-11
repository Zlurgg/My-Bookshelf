package uk.co.zlurgg.mybookshelf.sync.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for syncing user preferences from cloud to local storage.
 *
 * Called after sign-in to ensure the user's preferences (like onboarding state)
 * are synchronized from the cloud before making navigation decisions.
 *
 * Handles offline scenarios gracefully - if fetch fails, local cache is used.
 */
interface SyncUserPreferencesUseCase {

    /**
     * Fetches user preferences from cloud and caches locally.
     *
     * @return Success if sync completed (or skipped for offline), Error only for critical failures
     */
    suspend operator fun invoke(): Result<Unit, DataError.Sync>
}
