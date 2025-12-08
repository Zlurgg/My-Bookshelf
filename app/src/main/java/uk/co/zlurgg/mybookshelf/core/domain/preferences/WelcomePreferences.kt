package uk.co.zlurgg.mybookshelf.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing welcome screen preferences.
 * Part of the core domain layer - defines the contract for preference storage.
 *
 * Welcome state is stored per-user, so each user sees the welcome screen
 * on their first sign-in, even if another user has already seen it on the same device.
 */
interface WelcomePreferences {
    /**
     * Marks that the welcome screen has been shown to the specified user.
     *
     * @param userId The user ID (Firebase UID), or null for guest mode
     */
    suspend fun setWelcomeShown(userId: String?)

    /**
     * Returns a flow indicating whether the welcome screen has been shown to the specified user.
     *
     * @param userId The user ID (Firebase UID), or null for guest mode
     * @return Flow<Boolean> - true if welcome has been shown to this user, false otherwise
     */
    fun hasShownWelcome(userId: String?): Flow<Boolean>
}
