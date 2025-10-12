package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

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