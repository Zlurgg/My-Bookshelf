package uk.co.zlurgg.mybookshelf.book.data.mappers

import uk.co.zlurgg.mybookshelf.book.data.dto.SearchedBookDto
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Converts search result DTO to Book domain model.
 * Note: Uses small image URLs (-S.jpg) for fast loading in search results.
 * Uses placeholder color (0) for search results - actual spine color
 * is generated when book is added to shelf for better performance.
 */
fun SearchedBookDto.toBook(): Book {
    // Determine which cover identifier to use (prioritize coverKey over coverAlternativeKey)
    val coverIdentifier = coverKey ?: coverAlternativeKey?.toString()
    val generatedImageUrl = ApiConfig.OpenLibrary.CoverUrls.buildCoverUrl(
        coverKey = coverIdentifier,
        size = ApiConfig.OpenLibrary.CoverUrls.CoverSize.SMALL
    )

    return Book(
        id = id.substringAfterLast("/"),
        title = title,
        imageUrl = generatedImageUrl,
        authors = authorNames ?: emptyList(),
        description = null,
        languages = languages ?: emptyList(),
        firstPublishYear = firstPublishYear.toString(),
        averageRating = ratingsAverage,
        ratingCount = ratingsCount,
        numPages = numPagesMedian,
        numEditions = numEditions ?: 0,
        purchased = false,
        spineColor = 0, // Placeholder - generated when added to shelf
        // Enhanced metadata - take first item from arrays
        isbn = isbns?.firstOrNull(),
        publisher = publishers?.firstOrNull(),
        publishDate = publishDates?.firstOrNull(),
        internetArchiveId = internetArchiveIds?.firstOrNull(),
        subjects = subjects ?: emptyList()
    )
}

/**
 * Converts Book domain model to entity with default ownerId (null).
 */
fun Book.toBookEntity(): BookEntity {
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
        // Personal metadata
        readingStatus = readingStatus.name,
        personalRating = personalRating,
        personalNotes = personalNotes,
        dateAdded = dateAdded,
        purchaseDate = purchaseDate,
        // Enhanced metadata
        isbn = isbn,
        publisher = publisher,
        publishDate = publishDate,
        internetArchiveId = internetArchiveId,
        subjects = subjects
    )
}

// TODO: remove old-name mappings after next destructive DB migration
private fun parseReadingStatus(value: String): ReadingStatus = when (value) {
    "NOT_READ", "WANT_TO_READ" -> ReadingStatus.NOT_READ
    "READING", "CURRENTLY_READING" -> ReadingStatus.READING
    "FINISHED", "READ" -> ReadingStatus.FINISHED
    else -> ReadingStatus.NOT_READ
}

fun BookEntity.toBook(): Book {
    return Book(
        id = id,
        title = title,
        description = description,
        imageUrl = imageUrl,
        languages = languages,
        authors = authors,
        firstPublishYear = firstPublishYear,
        averageRating = ratingsAverage,
        ratingCount = ratingsCount,
        numPages = numPagesMedian,
        numEditions = numEditions,
        purchased = purchased,
        spineColor = spineColor,
        // Personal metadata
        readingStatus = parseReadingStatus(readingStatus),
        personalRating = personalRating,
        personalNotes = personalNotes,
        dateAdded = dateAdded,
        purchaseDate = purchaseDate,
        // Enhanced metadata
        isbn = isbn,
        publisher = publisher,
        publishDate = publishDate,
        internetArchiveId = internetArchiveId,
        subjects = subjects
    )
}
