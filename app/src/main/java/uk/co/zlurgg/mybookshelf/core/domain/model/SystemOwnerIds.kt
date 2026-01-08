package uk.co.zlurgg.mybookshelf.core.domain.model

/**
 * System-level owner IDs for entities that should be visible to all users
 * and excluded from cloud sync.
 *
 * These are used to identify "system" entities like the tutorial shelf
 * that should:
 * - Always be visible regardless of which user is signed in
 * - Never be synced to Firestore
 * - Never be deleted on sign-out
 */
object SystemOwnerIds {
    /**
     * Owner ID for the tutorial shelf and book.
     * Entities with this ownerId are always visible to all users.
     */
    const val TUTORIAL = "__system_tutorial__"

    /**
     * Fixed ID for the tutorial shelf (not random UUID).
     * Using a fixed ID allows us to find/update the tutorial shelf reliably.
     */
    const val TUTORIAL_SHELF_ID = "shelf-tutorial"

    /**
     * Checks if an ownerId represents a system entity.
     * System entities should:
     * - Be included in all user queries
     * - Be excluded from cloud sync
     * - Be preserved on sign-out
     *
     * @param ownerId The owner ID to check
     * @return true if this is a system owner ID
     */
    fun isSystemOwner(ownerId: String?): Boolean {
        return ownerId == TUTORIAL
    }
}
