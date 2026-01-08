package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Book(
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
    val spineColor: Int, // ARGB color as Int - generated once and persisted for consistency
    // Personal metadata (NOT exported for privacy)
    val readingStatus: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val personalRating: Float = 0f, // 0 = unrated, 1-5 = rated
    val personalNotes: String = "", // "" = no notes
    val dateAdded: Long? = null,
    val purchaseDate: Long? = null,
    // Enhanced metadata from API (shareable)
    val isbn: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val internetArchiveId: String? = null,
)
