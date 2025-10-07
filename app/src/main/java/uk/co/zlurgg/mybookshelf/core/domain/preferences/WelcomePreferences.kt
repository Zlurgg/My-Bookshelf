package uk.co.zlurgg.mybookshelf.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing welcome screen preferences.
 * Part of the core domain layer - defines the contract for preference storage.
 */
interface WelcomePreferences {
    /**
     * Marks that the welcome screen has been shown to the user.
     */
    suspend fun setWelcomeShown()

    /**
     * Returns a flow indicating whether the welcome screen has been shown.
     * @return Flow<Boolean> - true if welcome has been shown, false otherwise
     */
    fun hasShownWelcome(): Flow<Boolean>
}
