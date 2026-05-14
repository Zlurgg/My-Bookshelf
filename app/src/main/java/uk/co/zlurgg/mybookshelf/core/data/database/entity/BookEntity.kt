package uk.co.zlurgg.mybookshelf.core.data.database.entity

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
    val readingStatus: String = "NOT_READ",
    val personalRating: Float = 0f, // 0 = unrated, 1-5 = rated
    val personalNotes: String = "", // "" = no notes
    val dateAdded: Long? = null,
    val purchaseDate: Long? = null,

    // Enhanced metadata from API (shareable)
    val isbn: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val internetArchiveId: String? = null,

    // Subjects/categories from API
    val subjects: List<String> = emptyList(),
)
