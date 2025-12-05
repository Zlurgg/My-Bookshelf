package uk.co.zlurgg.mybookshelf.sync.data.mapper

import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto

/**
 * Maps between BookEntity and BookFirestoreDto for Firestore sync.
 */

/**
 * Converts a BookEntity to a BookFirestoreDto for uploading to Firestore.
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
        lastModifiedAt = lastModifiedAt
    )
}

/**
 * Converts a BookFirestoreDto from Firestore to a BookEntity for local storage.
 *
 * @param ownerId The Firebase UID of the owner
 * @param cloudId The Firestore document ID (same as id for user's own books)
 */
fun BookFirestoreDto.toEntity(
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

/**
 * Converts a Firestore document snapshot map to BookFirestoreDto.
 * Useful when working directly with DocumentSnapshot.data
 */
fun Map<String, Any?>.toBookFirestoreDto(documentId: String): BookFirestoreDto {
    @Suppress("UNCHECKED_CAST")
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
        lastModifiedAt = (this["last_modified_at"] as? Number)?.toLong() ?: 0L
    )
}
