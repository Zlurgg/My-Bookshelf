package uk.co.zlurgg.mybookshelf.sync.data.mapper

import uk.co.zlurgg.mybookshelf.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto

/**
 * Maps between BookshelfEntity and BookshelfFirestoreDto for Firestore sync.
 */

/**
 * Converts a BookshelfEntity to a BookshelfFirestoreDto for uploading to Firestore.
 *
 * @param bookIds List of book IDs in this shelf (retrieved separately from cross-refs)
 */
fun BookshelfEntity.toFirestoreDto(bookIds: List<String>): BookshelfFirestoreDto {
    return BookshelfFirestoreDto(
        id = id,
        name = name,
        shelfMaterial = shelfMaterial,
        position = position,
        isTidyMode = isTidyMode,
        bookIds = bookIds,
        isShared = isShared,
        shareCode = shareCode,
        version = version,
        lastModifiedAt = lastModifiedAt
    )
}

/**
 * Converts a BookshelfFirestoreDto from Firestore to a BookshelfEntity for local storage.
 *
 * Note: bookIds from the DTO need to be handled separately to create BookshelfBookCrossRef entries.
 *
 * @param ownerId The Firebase UID of the owner
 * @param cloudId The Firestore document ID (same as id for user's own shelves)
 */
fun BookshelfFirestoreDto.toEntity(
    ownerId: String,
    cloudId: String = id
): BookshelfEntity {
    return BookshelfEntity(
        id = id,
        name = name,
        shelfMaterial = shelfMaterial,
        position = position,
        isTidyMode = isTidyMode,
        ownerId = ownerId,
        lastModifiedAt = lastModifiedAt,
        syncStatus = "SYNCED",
        cloudId = cloudId,
        version = version,
        isShared = isShared,
        shareCode = shareCode
    )
}

/**
 * Converts a Firestore document snapshot map to BookshelfFirestoreDto.
 * Useful when working directly with DocumentSnapshot.data
 */
fun Map<String, Any?>.toBookshelfFirestoreDto(documentId: String): BookshelfFirestoreDto {
    @Suppress("UNCHECKED_CAST")
    return BookshelfFirestoreDto(
        id = documentId,
        name = this["name"] as? String ?: "",
        shelfMaterial = this["shelf_material"] as? String ?: "DARK_WOOD",
        position = (this["position"] as? Number)?.toInt() ?: 0,
        isTidyMode = this["is_tidy_mode"] as? Boolean ?: false,
        bookIds = this["book_ids"] as? List<String> ?: emptyList(),
        isShared = this["is_shared"] as? Boolean ?: false,
        shareCode = this["share_code"] as? String,
        version = (this["version"] as? Number)?.toLong() ?: 1L,
        lastModifiedAt = (this["last_modified_at"] as? Number)?.toLong() ?: 0L
    )
}
