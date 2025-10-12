package uk.co.zlurgg.mybookshelf.bookshelf.data.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportedBook(
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

    // Enhanced metadata (shareable)
    val isbn: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val internetArchiveId: String? = null
)