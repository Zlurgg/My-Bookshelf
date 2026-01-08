package uk.co.zlurgg.mybookshelf.sync.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for Book documents.
 *
 * Document path: /users/{userId}/books/{bookId}
 *
 * Note: Firestore requires a no-arg constructor for deserialization,
 * hence the default values for all properties.
 */
data class BookFirestoreDto(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    @get:PropertyName("image_url")
    @set:PropertyName("image_url")
    var imageUrl: String = "",
    val description: String? = null,
    val languages: List<String> = emptyList(),
    @get:PropertyName("first_publish_year")
    @set:PropertyName("first_publish_year")
    var firstPublishYear: String? = null,
    @get:PropertyName("average_rating")
    @set:PropertyName("average_rating")
    var averageRating: Double? = null,
    @get:PropertyName("rating_count")
    @set:PropertyName("rating_count")
    var ratingCount: Int? = null,
    @get:PropertyName("num_pages")
    @set:PropertyName("num_pages")
    var numPages: Int? = null,
    @get:PropertyName("num_editions")
    @set:PropertyName("num_editions")
    var numEditions: Int = 0,
    val purchased: Boolean = false,
    @get:PropertyName("spine_color")
    @set:PropertyName("spine_color")
    var spineColor: Int = 0,
    // Personal metadata (synced but private to user)
    @get:PropertyName("reading_status")
    @set:PropertyName("reading_status")
    var readingStatus: String = "WANT_TO_READ",
    @get:PropertyName("personal_rating")
    @set:PropertyName("personal_rating")
    var personalRating: Float = 0f,
    @get:PropertyName("personal_notes")
    @set:PropertyName("personal_notes")
    var personalNotes: String = "",
    @get:PropertyName("date_added")
    @set:PropertyName("date_added")
    var dateAdded: Long? = null,
    @get:PropertyName("purchase_date")
    @set:PropertyName("purchase_date")
    var purchaseDate: Long? = null,
    // Enhanced metadata
    val isbn: String? = null,
    val publisher: String? = null,
    @get:PropertyName("publish_date")
    @set:PropertyName("publish_date")
    var publishDate: String? = null,
    @get:PropertyName("internet_archive_id")
    @set:PropertyName("internet_archive_id")
    var internetArchiveId: String? = null,
    // Sync metadata
    val version: Long = 1L,
    @get:PropertyName("last_modified_at")
    @set:PropertyName("last_modified_at")
    var lastModifiedAt: Long = 0L,
)
