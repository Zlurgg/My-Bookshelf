package uk.co.zlurgg.mybookshelf.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks user's membership in book clubs.
 * Each membership links a club code to a local shelf.
 */
@Entity(
    tableName = "book_club_memberships",
    indices = [
        Index(value = ["clubCode"], unique = true),
        Index(value = ["localShelfId"]),
    ],
)
data class BookClubMembershipEntity(
    @PrimaryKey val id: String,
    val clubCode: String, // Firestore book club code
    val localShelfId: String, // Local BookshelfEntity.id
    val joinedAt: Long, // When user joined the club
    val lastSyncedAt: Long, // Last successful sync timestamp
    val syncStatus: String = "PENDING", // SYNCED, PENDING, DELETED
)
