package uk.co.zlurgg.mybookshelf.bookclub.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for Book Club metadata.
 *
 * Document path: /bookClubs/{clubCode}
 */
data class BookClubMetadataDto(
    @DocumentId
    val code: String = "",

    val name: String = "",

    @get:PropertyName("shelf_style")
    @set:PropertyName("shelf_style")
    var shelfStyle: String = "DARK_WOOD",

    @get:PropertyName("created_by")
    @set:PropertyName("created_by")
    var createdBy: String = "",

    @get:PropertyName("created_by_name")
    @set:PropertyName("created_by_name")
    var createdByName: String = "",

    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Date? = null,

    @get:PropertyName("last_modified_at")
    @set:PropertyName("last_modified_at")
    var lastModifiedAt: Long = 0L,

    @get:PropertyName("book_count")
    @set:PropertyName("book_count")
    var bookCount: Int = 0,

    @get:PropertyName("member_count")
    @set:PropertyName("member_count")
    var memberCount: Int = 0
)
