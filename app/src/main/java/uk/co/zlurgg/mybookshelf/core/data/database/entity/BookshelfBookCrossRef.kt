package uk.co.zlurgg.mybookshelf.core.data.database.entity

import androidx.room.Entity

@Entity(primaryKeys = ["shelfId", "bookId"])
data class BookshelfBookCrossRef(
    val shelfId: String,
    val bookId: String,
    val addedAt: Long,

    // Sync metadata (for Firestore cloud sync)
    val syncStatus: String = "PENDING",    // SYNCED, PENDING, CONFLICT, DELETED
    val lastModifiedAt: Long = 0L          // Timestamp for conflict resolution
)
