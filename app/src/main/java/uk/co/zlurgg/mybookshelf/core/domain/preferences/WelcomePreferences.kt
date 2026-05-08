package uk.co.zlurgg.mybookshelf.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing welcome screen preferences.
 * Part of the core domain layer - defines the contract for preference storage.
 *
 * Welcome state is per-device: once any user has seen the welcome screen,
 * it won't show again regardless of which account signs in.
 */
interface WelcomePreferences {
    /**
     * Marks that the welcome screen has been shown on this device.
     */
    suspend fun setWelcomeShown()

    /**
     * Returns a flow indicating whether the welcome screen has been shown on this device.
     */
    fun hasShownWelcome(): Flow<Boolean>
}
