package uk.co.zlurgg.mybookshelf.sync.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for Book Club member.
 *
 * Document path: /bookClubs/{clubCode}/members/{userId}
 *
 * Note: user_id is stored both as document ID AND as a field to enable
 * collection group queries (Firestore can't query by document ID in collection groups).
 */
data class BookClubMemberDto(
    @DocumentId
    val id: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String = "",

    @ServerTimestamp
    @get:PropertyName("joined_at")
    @set:PropertyName("joined_at")
    var joinedAt: Date? = null
)
