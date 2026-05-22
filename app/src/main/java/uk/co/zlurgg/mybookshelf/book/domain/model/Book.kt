package uk.co.zlurgg.mybookshelf.book.domain.model

data class Book(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String>,
    val imageUrl: String,
    val description: String?,
    val languages: List<String>,
    val firstPublishYear: String?,
    val numPages: Int?,
    val purchased: Boolean,
    val spineColor: Int, // ARGB color as Int - generated once and persisted for consistency

    // Provider tracking
    val provider: BookProvider = BookProvider.GOOGLE_BOOKS,

    // Personal metadata (NOT exported for privacy)
    val readingStatus: ReadingStatus = ReadingStatus.NOT_READ,
    val personalRating: Float = 0f, // 0 = unrated, 1-5 = rated
    val personalNotes: String = "", // "" = no notes
    val dateAdded: Long? = null,
    val purchaseDate: Long? = null,

    // Enhanced metadata from API (shareable)
    val isbn: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val subjects: List<String> = emptyList(),

    // Google Books metadata (null for OL-sourced books)
    val previewLink: String? = null,
    val infoLink: String? = null,
    val maturityRating: MaturityRating = MaturityRating.UNKNOWN,
    val printType: PrintType = PrintType.UNKNOWN,
)
