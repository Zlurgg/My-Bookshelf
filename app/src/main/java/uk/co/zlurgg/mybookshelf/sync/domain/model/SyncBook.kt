package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Domain model for book sync operations.
 *
 * This represents a book in the sync context - pure domain without
 * any Firestore or database annotations.
 */
data class SyncBook(
    val id: String,
    val title: String,
    val authors: List<String>,
    val imageUrl: String,
    val description: String?,
    val languages: List<String>,
    val firstPublishYear: String?,
    val averageRating: Double?,
    val ratingCount: Int?,
    val numPages: Int?,
    val numEditions: Int,
    val purchased: Boolean,
    val spineColor: Int,

    // Personal metadata
    val readingStatus: String,
    val personalRating: Float,
    val personalNotes: String,
    val dateAdded: Long?,
    val purchaseDate: Long?,

    // Enhanced metadata
    val isbn: String?,
    val publisher: String?,
    val publishDate: String?,
    val internetArchiveId: String?,

    // Sync metadata
    val version: Long,
    val lastModifiedAt: Long
)
