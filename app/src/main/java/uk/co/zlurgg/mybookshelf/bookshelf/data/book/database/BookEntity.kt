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
    val personalRating: Float? = null,
    val personalNotes: String? = null,
    val dateAdded: Long? = null,
    val purchaseDate: Long? = null,

    // Enhanced metadata from API (shareable)
    val isbn: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val internetArchiveId: String? = null
)
