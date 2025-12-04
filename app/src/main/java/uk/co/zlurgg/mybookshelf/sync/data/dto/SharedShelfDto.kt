package uk.co.zlurgg.mybookshelf.sync.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for shared shelf metadata.
 *
 * Document path: /sharedShelves/{shareCode}
 *
 * This document enables shelf discovery via share codes and tracks subscribers.
 * The actual shelf data lives in the owner's collection at:
 * /users/{ownerId}/bookshelves/{shelfId}
 */
data class SharedShelfDto(
    @DocumentId
    val shareCode: String = "",

    @get:PropertyName("owner_id")
    @set:PropertyName("owner_id")
    var ownerId: String = "",

    @get:PropertyName("shelf_id")
    @set:PropertyName("shelf_id")
    var shelfId: String = "",

    @get:PropertyName("shelf_name")
    @set:PropertyName("shelf_name")
    var shelfName: String = "",

    @get:PropertyName("subscriber_ids")
    @set:PropertyName("subscriber_ids")
    var subscriberIds: List<String> = emptyList(),

    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Date? = null,

    @get:PropertyName("book_count")
    @set:PropertyName("book_count")
    var bookCount: Int = 0
)
