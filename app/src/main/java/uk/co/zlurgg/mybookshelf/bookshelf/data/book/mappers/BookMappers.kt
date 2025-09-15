package uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchedBookDto
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookColorGenerator


fun SearchedBookDto.toBook(): Book {
    val generatedImageUrl = when {
        coverKey != null -> "https://covers.openlibrary.org/b/olid/${coverKey}-L.jpg"
        coverAlternativeKey != null -> "https://covers.openlibrary.org/b/id/${coverAlternativeKey}-L.jpg"
        else -> "" // Empty string fallback when no cover data is available
    }
    
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
        spineColor = BookColorGenerator.generateSpineColor()
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
        spineColor = spineColor
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
        spineColor = spineColor
    )
}
