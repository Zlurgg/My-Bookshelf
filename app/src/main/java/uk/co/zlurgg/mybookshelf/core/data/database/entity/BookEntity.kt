package uk.co.zlurgg.mybookshelf.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BookEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val title: String,
    val subtitle: String? = null,
    val description: String?,
    val imageUrl: String,
    val languages: List<String>,
    val authors: List<String>,
    val firstPublishYear: String?,
    val numPagesMedian: Int?,
    val purchased: Boolean,
    val spineColor: Int,

    // Provider tracking
    val provider: String = "GOOGLE_BOOKS",

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
    val subjects: List<String> = emptyList(),

    // Google Books metadata
    val previewLink: String? = null,
    val infoLink: String? = null,
    val maturityRating: String = "UNKNOWN",
    val printType: String = "UNKNOWN",

    // Per-user search artifact (Google `searchInfo.textSnippet`); not synced.
    val searchSnippet: String? = null,
)
