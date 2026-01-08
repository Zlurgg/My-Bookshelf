package uk.co.zlurgg.mybookshelf.sync.data.mappers

import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto
import java.util.Date

/**
 * Maps between Room entities and Firestore DTOs.
 *
 * These mappers are used by SyncEngine to convert between
 * local database entities and Firestore DTOs for cloud sync.
 */

// ==================== BookEntity ↔ BookFirestoreDto ====================

/**
 * Converts a BookEntity to a BookFirestoreDto for Firestore upload.
 */
fun BookEntity.toFirestoreDto(): BookFirestoreDto {
    return BookFirestoreDto(
        id = id,
        title = title,
        authors = authors,
        imageUrl = imageUrl,
        description = description,
        languages = languages,
        firstPublishYear = firstPublishYear,
        averageRating = ratingsAverage,
        ratingCount = ratingsCount,
        numPages = numPagesMedian,
        numEditions = numEditions,
        purchased = purchased,
        spineColor = spineColor,
        readingStatus = readingStatus,
        personalRating = personalRating,
        personalNotes = personalNotes,
        dateAdded = dateAdded,
        purchaseDate = purchaseDate,
        isbn = isbn,
        publisher = publisher,
        publishDate = publishDate,
        internetArchiveId = internetArchiveId,
        version = version,
        lastModifiedAt = lastModifiedAt,
    )
}

/**
 * Converts a BookFirestoreDto to a BookEntity for local storage.
 *
 * @param ownerId The Firebase UID of the owner
 * @param cloudId The cloud document ID (same as id for user's own books)
 */
fun BookFirestoreDto.toEntity(
    ownerId: String,
    cloudId: String = id,
): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        description = description,
        imageUrl = imageUrl,
        languages = languages,
        authors = authors,
        firstPublishYear = firstPublishYear,
        ratingsAverage = averageRating,
        ratingsCount = ratingCount,
        numPagesMedian = numPages,
        numEditions = numEditions,
        purchased = purchased,
        spineColor = spineColor,
        readingStatus = readingStatus,
        personalRating = personalRating,
        personalNotes = personalNotes,
        dateAdded = dateAdded,
        purchaseDate = purchaseDate,
        isbn = isbn,
        publisher = publisher,
        publishDate = publishDate,
        internetArchiveId = internetArchiveId,
        ownerId = ownerId,
        lastModifiedAt = lastModifiedAt,
        syncStatus = "SYNCED",
        cloudId = cloudId,
        version = version,
    )
}

// ==================== BookshelfEntity ↔ BookshelfFirestoreDto ====================

/**
 * Converts a BookshelfEntity to a BookshelfFirestoreDto for Firestore upload.
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
        lastModifiedAt = lastModifiedAt,
    )
}

/**
 * Converts a BookshelfFirestoreDto to a BookshelfEntity for local storage.
 *
 * Note: bookIds from the DTO need to be handled separately
 * to create BookshelfBookCrossRef entries.
 *
 * @param ownerId The Firebase UID of the owner
 * @param cloudId The cloud document ID (same as id for user's own shelves)
 */
fun BookshelfFirestoreDto.toEntity(
    ownerId: String,
    cloudId: String = id,
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
        shareCode = shareCode,
    )
}

// ==================== Firestore Map to DTO ====================

/**
 * Converts a Firestore document snapshot map to BookFirestoreDto.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toBookFirestoreDto(documentId: String): BookFirestoreDto {
    return BookFirestoreDto(
        id = documentId,
        title = this["title"] as? String ?: "",
        authors = this["authors"] as? List<String> ?: emptyList(),
        imageUrl = this["image_url"] as? String ?: "",
        description = this["description"] as? String,
        languages = this["languages"] as? List<String> ?: emptyList(),
        firstPublishYear = this["first_publish_year"] as? String,
        averageRating = (this["average_rating"] as? Number)?.toDouble(),
        ratingCount = (this["rating_count"] as? Number)?.toInt(),
        numPages = (this["num_pages"] as? Number)?.toInt(),
        numEditions = (this["num_editions"] as? Number)?.toInt() ?: 0,
        purchased = this["purchased"] as? Boolean ?: false,
        spineColor = (this["spine_color"] as? Number)?.toInt() ?: 0,
        readingStatus = this["reading_status"] as? String ?: "WANT_TO_READ",
        personalRating = (this["personal_rating"] as? Number)?.toFloat() ?: 0f,
        personalNotes = this["personal_notes"] as? String ?: "",
        dateAdded = (this["date_added"] as? Number)?.toLong(),
        purchaseDate = (this["purchase_date"] as? Number)?.toLong(),
        isbn = this["isbn"] as? String,
        publisher = this["publisher"] as? String,
        publishDate = this["publish_date"] as? String,
        internetArchiveId = this["internet_archive_id"] as? String,
        version = (this["version"] as? Number)?.toLong() ?: 1L,
        lastModifiedAt = (this["last_modified_at"] as? Number)?.toLong() ?: 0L,
    )
}

/**
 * Converts a Firestore document snapshot map to BookshelfFirestoreDto.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toBookshelfFirestoreDto(documentId: String): BookshelfFirestoreDto {
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
        lastModifiedAt = (this["last_modified_at"] as? Number)?.toLong() ?: 0L,
    )
}

/**
 * Converts a Firestore document snapshot map to SharedShelfDto.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toSharedShelfDto(documentId: String): SharedShelfDto {
    return SharedShelfDto(
        shareCode = documentId,
        ownerId = this["owner_id"] as? String ?: "",
        shelfId = this["shelf_id"] as? String ?: "",
        shelfName = this["shelf_name"] as? String ?: "",
        subscriberIds = this["subscriber_ids"] as? List<String> ?: emptyList(),
        createdAt = this["created_at"] as? Date,
        bookCount = (this["book_count"] as? Number)?.toInt() ?: 0,
    )
}
