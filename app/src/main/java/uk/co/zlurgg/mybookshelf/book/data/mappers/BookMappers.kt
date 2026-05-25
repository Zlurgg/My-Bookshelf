package uk.co.zlurgg.mybookshelf.book.data.mappers

import uk.co.zlurgg.mybookshelf.book.data.dto.SearchedBookDto
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.MaturityRating
import uk.co.zlurgg.mybookshelf.book.domain.model.PrintType
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
        numPages = numPagesMedian,
        purchased = false,
        spineColor = 0, // Placeholder - generated when added to shelf
        provider = BookProvider.OPEN_LIBRARY,
        // Enhanced metadata - take first item from arrays
        isbn = isbns?.firstOrNull(),
        publisher = publishers?.firstOrNull(),
        publishDate = publishDates?.firstOrNull(),
        subjects = subjects ?: emptyList()
    )
}

/**
 * Converts Book domain model to entity.
 */
fun Book.toBookEntity(): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        description = description,
        imageUrl = imageUrl,
        languages = languages,
        authors = authors,
        firstPublishYear = firstPublishYear,
        numPagesMedian = numPages,
        purchased = purchased,
        spineColor = spineColor,
        provider = provider.name,
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
        subjects = subjects,
        // Google Books metadata
        previewLink = previewLink,
        infoLink = infoLink,
        maturityRating = maturityRating.name,
        printType = printType.name,
        // Per-user search snippet (not synced to Firestore)
        searchSnippet = searchSnippet,
    )
}

fun BookEntity.toBook(): Book {
    return Book(
        id = id,
        title = title,
        subtitle = subtitle,
        description = description,
        imageUrl = imageUrl,
        languages = languages,
        authors = authors,
        firstPublishYear = firstPublishYear,
        numPages = numPagesMedian,
        purchased = purchased,
        spineColor = spineColor,
        provider = BookProvider.valueOf(provider),
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
        subjects = subjects,
        // Google Books metadata
        previewLink = previewLink,
        infoLink = infoLink,
        maturityRating = MaturityRating.valueOf(maturityRating),
        printType = PrintType.valueOf(printType),
        // Per-user search snippet (not synced to Firestore)
        searchSnippet = searchSnippet,
    )
}
