package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BookEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val languages: List<String>,
    val authors: List<String>,
    val firstPublishYear: String?,
    val ratingsAverage: Double?,
    val ratingsCount: Int?,
    val numPagesMedian: Int?,
    val numEditions: Int,
    val purchased: Boolean,
    val spineColor: Int,

    // Personal metadata (NOT exported for privacy)
    val readingStatus: String = "WANT_TO_READ",
    val personalRating: Float = 0f,       // 0 = unrated, 1-5 = rated
    val personalNotes: String = "",        // "" = no notes
    val dateAdded: Long? = null,
    val purchaseDate: Long? = null,

    // Enhanced metadata from API (shareable)
    val isbn: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val internetArchiveId: String? = null,

    // Sync metadata (for Firestore cloud sync)
    val ownerId: String? = null,           // Firebase UID of owner (null = local-only/anonymous)
    val lastModifiedAt: Long = 0L,         // Timestamp for conflict resolution
    val syncStatus: String = "PENDING",    // SYNCED, PENDING, CONFLICT, DELETED
    val cloudId: String? = null,           // Firestore document ID (may differ from local ID)
    val version: Long = 1L                 // Optimistic concurrency version
)
