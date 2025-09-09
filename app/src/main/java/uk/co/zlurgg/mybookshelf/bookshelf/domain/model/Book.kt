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
    val affiliateLink: String,
    val spineColor: Int // ARGB color as Int - generated once and persisted for consistency
)