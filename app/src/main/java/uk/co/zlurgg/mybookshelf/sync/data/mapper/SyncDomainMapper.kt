package uk.co.zlurgg.mybookshelf.sync.data.mapper

import uk.co.zlurgg.mybookshelf.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto
import uk.co.zlurgg.mybookshelf.sync.domain.model.SharedShelf
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncBook
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncBookshelf
import java.util.Date

/**
 * Maps between sync domain models and Firestore DTOs.
 *
 * These mappers are used by FirestoreRemoteDataSource to convert between
 * the clean domain models (used by the domain layer) and the Firestore-specific
 * DTOs (with Firestore annotations).
 */

// ==================== SyncBook ↔ BookFirestoreDto ====================

/**
 * Converts a SyncBook domain model to a BookFirestoreDto for Firestore upload.
 */
fun SyncBook.toFirestoreDto(): BookFirestoreDto {
    return BookFirestoreDto(
        id = id,
        title = title,
        authors = authors,
        imageUrl = imageUrl,
        description = description,
        languages = languages,
        firstPublishYear = firstPublishYear,
        averageRating = averageRating,
        ratingCount = ratingCount,
        numPages = numPages,
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
        lastModifiedAt = lastModifiedAt
    )
}

/**
 * Converts a BookFirestoreDto to a SyncBook domain model.
 */
fun BookFirestoreDto.toSyncBook(): SyncBook {
    return SyncBook(
        id = id,
        title = title,
        authors = authors,
        imageUrl = imageUrl,
        description = description,
        languages = languages,
        firstPublishYear = firstPublishYear,
        averageRating = averageRating,
        ratingCount = ratingCount,
        numPages = numPages,
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
        lastModifiedAt = lastModifiedAt
    )
}

// ==================== SyncBookshelf ↔ BookshelfFirestoreDto ====================

/**
 * Converts a SyncBookshelf domain model to a BookshelfFirestoreDto for Firestore upload.
 */
fun SyncBookshelf.toFirestoreDto(): BookshelfFirestoreDto {
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
 * Converts a BookshelfFirestoreDto to a SyncBookshelf domain model.
 */
fun BookshelfFirestoreDto.toSyncBookshelf(): SyncBookshelf {
    return SyncBookshelf(
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

// ==================== SharedShelf ↔ SharedShelfDto ====================

/**
 * Converts a SharedShelf domain model to a SharedShelfDto for Firestore upload.
 */
fun SharedShelf.toFirestoreDto(): SharedShelfDto {
    return SharedShelfDto(
        shareCode = shareCode,
        ownerId = ownerId,
        shelfId = shelfId,
        shelfName = shelfName,
        subscriberIds = subscriberIds,
        createdAt = createdAt?.let { Date(it) },
        bookCount = bookCount
    )
}

/**
 * Converts a SharedShelfDto to a SharedShelf domain model.
 */
fun SharedShelfDto.toSharedShelf(): SharedShelf {
    return SharedShelf(
        shareCode = shareCode,
        ownerId = ownerId,
        shelfId = shelfId,
        shelfName = shelfName,
        subscriberIds = subscriberIds,
        createdAt = createdAt?.time,
        bookCount = bookCount
    )
}

// ==================== Firestore Map to Domain ====================

/**
 * Converts a Firestore document snapshot map directly to SyncBook domain model.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toSyncBook(documentId: String): SyncBook {
    return SyncBook(
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
        lastModifiedAt = (this["last_modified_at"] as? Number)?.toLong() ?: 0L
    )
}

/**
 * Converts a Firestore document snapshot map directly to SyncBookshelf domain model.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toSyncBookshelf(documentId: String): SyncBookshelf {
    return SyncBookshelf(
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

/**
 * Converts a Firestore document snapshot map directly to SharedShelf domain model.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toSharedShelf(documentId: String): SharedShelf {
    return SharedShelf(
        shareCode = documentId,
        ownerId = this["owner_id"] as? String ?: "",
        shelfId = this["shelf_id"] as? String ?: "",
        shelfName = this["shelf_name"] as? String ?: "",
        subscriberIds = this["subscriber_ids"] as? List<String> ?: emptyList(),
        createdAt = (this["created_at"] as? Date)?.time,
        bookCount = (this["book_count"] as? Number)?.toInt() ?: 0
    )
}

// ==================== BookEntity ↔ SyncBook ====================

/**
 * Converts a BookEntity to a SyncBook domain model for sync operations.
 */
fun BookEntity.toSyncBook(): SyncBook {
    return SyncBook(
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
        lastModifiedAt = lastModifiedAt
    )
}

/**
 * Converts a SyncBook domain model to a BookEntity for local storage.
 *
 * @param ownerId The Firebase UID of the owner
 * @param cloudId The cloud document ID (same as id for user's own books)
 */
fun SyncBook.toEntity(
    ownerId: String,
    cloudId: String = id
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
        version = version
    )
}

// ==================== BookshelfEntity ↔ SyncBookshelf ====================

/**
 * Converts a BookshelfEntity to a SyncBookshelf domain model for sync operations.
 *
 * @param bookIds List of book IDs in this shelf (retrieved separately from cross-refs)
 */
fun BookshelfEntity.toSyncBookshelf(bookIds: List<String>): SyncBookshelf {
    return SyncBookshelf(
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
 * Converts a SyncBookshelf domain model to a BookshelfEntity for local storage.
 *
 * Note: bookIds from the SyncBookshelf need to be handled separately
 * to create BookshelfBookCrossRef entries.
 *
 * @param ownerId The Firebase UID of the owner
 * @param cloudId The cloud document ID (same as id for user's own shelves)
 */
fun SyncBookshelf.toEntity(
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
