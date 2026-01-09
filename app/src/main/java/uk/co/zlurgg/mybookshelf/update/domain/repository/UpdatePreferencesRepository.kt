package uk.co.zlurgg.mybookshelf.update.domain.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Repository interface for update-related preferences.
 * Minimal interface following Interface Segregation Principle.
 *
 * This allows the update package to be reused without depending
 * on the full PreferencesRepository interface.
 */
interface UpdatePreferencesRepository {
    /**
     * Get the version the user has dismissed (opted out of updating to).
     *
     * @return Result containing version string that was dismissed, or null if none
     */
    suspend fun getDismissedVersion(): Result<String?, DataError.Local>

    /**
     * Set the version the user has dismissed.
     *
     * @param version the version string to mark as dismissed
     * @return Result indicating success or failure
     */
    suspend fun setDismissedVersion(version: String): Result<Unit, DataError.Local>
}
