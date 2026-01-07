package uk.co.zlurgg.mybookshelf.sync.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for a review in a Book Club.
 *
 * Document path: /bookClubs/{clubCode}/books/{bookId}/reviews/{userId}
 */
data class BookClubReviewDto(
    @DocumentId
    val id: String = "",

    @get:PropertyName("book_id")
    @set:PropertyName("book_id")
    var bookId: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String = "",

    val rating: Float = 0f,         // 0 = no rating, 1-5 = rated

    @get:PropertyName("review_text")
    @set:PropertyName("review_text")
    var reviewText: String = "",    // "" = no review text

    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Date? = null
)
