package uk.co.zlurgg.mybookshelf.bookclub.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for a book in a Book Club.
 *
 * Document path: /bookClubs/{clubCode}/books/{bookId}
 *
 * Note: Personal notes and ratings are NOT included here.
 * Those are stored in user-specific reviews.
 */
data class BookClubBookDto(
    @DocumentId
    val id: String = "",

    val title: String = "",

    val subtitle: String? = null,

    val authors: List<String> = emptyList(),

    @get:PropertyName("cover_url")
    @set:PropertyName("cover_url")
    var coverUrl: String? = null,

    val isbn: String? = null,

    val provider: String = "GOOGLE_BOOKS",

    @get:PropertyName("first_publish_year")
    @set:PropertyName("first_publish_year")
    var firstPublishYear: Int? = null,

    @get:PropertyName("page_count")
    @set:PropertyName("page_count")
    var pageCount: Int? = null,

    @get:PropertyName("spine_color")
    @set:PropertyName("spine_color")
    var spineColor: Int = 0,

    @get:PropertyName("added_by")
    @set:PropertyName("added_by")
    var addedBy: String = "",

    @get:PropertyName("added_by_name")
    @set:PropertyName("added_by_name")
    var addedByName: String = "",

    @ServerTimestamp
    @get:PropertyName("added_at")
    @set:PropertyName("added_at")
    var addedAt: Date? = null
)
