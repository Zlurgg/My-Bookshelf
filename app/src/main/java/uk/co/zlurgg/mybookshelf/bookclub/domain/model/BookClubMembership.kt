package uk.co.zlurgg.mybookshelf.bookclub.domain.model

/**
 * Domain model representing a user's membership in a Book Club.
 * Links a club code to the user's local shelf.
 */
data class BookClubMembership(
    val clubCode: String,
    val localShelfId: String,
    val joinedAt: Long,
    val lastSyncedAt: Long
)
