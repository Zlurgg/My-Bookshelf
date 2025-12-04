package uk.co.zlurgg.mybookshelf.sync.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for Bookshelf documents.
 *
 * Document path: /users/{userId}/bookshelves/{shelfId}
 *
 * Note: Books are stored separately in /users/{userId}/books/{bookId}.
 * This document contains bookIds list referencing those books.
 */
data class BookshelfFirestoreDto(
    @DocumentId
    val id: String = "",

    val name: String = "",

    @get:PropertyName("shelf_material")
    @set:PropertyName("shelf_material")
    var shelfMaterial: String = "DARK_WOOD",

    val position: Int = 0,

    @get:PropertyName("is_tidy_mode")
    @set:PropertyName("is_tidy_mode")
    var isTidyMode: Boolean = false,

    // References to books in this shelf (ordered by addedAt)
    @get:PropertyName("book_ids")
    @set:PropertyName("book_ids")
    var bookIds: List<String> = emptyList(),

    // Sharing metadata
    @get:PropertyName("is_shared")
    @set:PropertyName("is_shared")
    var isShared: Boolean = false,

    @get:PropertyName("share_code")
    @set:PropertyName("share_code")
    var shareCode: String? = null,

    // Sync metadata
    val version: Long = 1L,

    @get:PropertyName("last_modified_at")
    @set:PropertyName("last_modified_at")
    var lastModifiedAt: Long = 0L
)
