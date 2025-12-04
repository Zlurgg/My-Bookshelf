package uk.co.zlurgg.mybookshelf.auth.domain.service

/**
 * Provides the current signed-in user's ID.
 * Used by repositories to filter data by owner.
 */
interface CurrentUserProvider {
    /**
     * Returns the current user's ID, or null if no user is signed in (guest mode).
     */
    fun getCurrentUserId(): String?
}
