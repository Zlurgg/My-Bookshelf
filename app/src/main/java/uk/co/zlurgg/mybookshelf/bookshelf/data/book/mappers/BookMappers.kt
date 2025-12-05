package uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers

import uk.co.zlurgg.mybookshelf.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchedBookDto
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
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
        internetArchiveId = internetArchiveIds?.firstOrNull()
    )
}

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
        internetArchiveId = internetArchiveId
    )
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
        readingStatus = ReadingStatus.valueOf(readingStatus),
        personalRating = personalRating,
        personalNotes = personalNotes,
        dateAdded = dateAdded,
        purchaseDate = purchaseDate,
        // Enhanced metadata
        isbn = isbn,
        publisher = publisher,
        publishDate = publishDate,
        internetArchiveId = internetArchiveId
    )
}
