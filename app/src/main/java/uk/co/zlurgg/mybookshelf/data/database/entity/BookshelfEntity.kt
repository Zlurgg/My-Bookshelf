package uk.co.zlurgg.mybookshelf.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BookshelfEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shelfMaterial: String,
    val position: Int = 0,
    val isTidyMode: Boolean = false,

    // Sync metadata (for Firestore cloud sync)
    val ownerId: String? = null,           // Firebase UID of owner (null = local-only/anonymous)
    val lastModifiedAt: Long = 0L,         // Timestamp for conflict resolution
    val syncStatus: String = "PENDING",    // SYNCED, PENDING, CONFLICT, DELETED
    val cloudId: String? = null,           // Firestore document ID (may differ from local ID)
    val version: Long = 1L,                // Optimistic concurrency version

    // Sharing metadata
    val isShared: Boolean = false,         // Is this shelf shared with others?
    val shareCode: String? = null          // Unique code for live sharing
)
