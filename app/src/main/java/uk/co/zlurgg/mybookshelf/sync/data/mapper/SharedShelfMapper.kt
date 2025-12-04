package uk.co.zlurgg.mybookshelf.sync.data.mapper

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto
import java.util.Date

/**
 * Maps between BookshelfEntity and SharedShelfDto for shared shelf management.
 */

/**
 * Creates a SharedShelfDto from a BookshelfEntity for registering a shared shelf.
 *
 * @param ownerId The Firebase UID of the shelf owner
 * @param bookCount Number of books currently in the shelf
 */
fun BookshelfEntity.toSharedShelfDto(
    ownerId: String,
    bookCount: Int
): SharedShelfDto {
    requireNotNull(shareCode) { "Cannot create SharedShelfDto without shareCode" }

    return SharedShelfDto(
        shareCode = shareCode,
        ownerId = ownerId,
        shelfId = id,
        shelfName = name,
        subscriberIds = emptyList(),
        createdAt = Date(),
        bookCount = bookCount
    )
}

/**
 * Converts a Firestore document snapshot map to SharedShelfDto.
 * Useful when working directly with DocumentSnapshot.data
 */
fun Map<String, Any?>.toSharedShelfDto(documentId: String): SharedShelfDto {
    @Suppress("UNCHECKED_CAST")
    return SharedShelfDto(
        shareCode = documentId,
        ownerId = this["owner_id"] as? String ?: "",
        shelfId = this["shelf_id"] as? String ?: "",
        shelfName = this["shelf_name"] as? String ?: "",
        subscriberIds = this["subscriber_ids"] as? List<String> ?: emptyList(),
        createdAt = this["created_at"] as? Date,
        bookCount = (this["book_count"] as? Number)?.toInt() ?: 0
    )
}
