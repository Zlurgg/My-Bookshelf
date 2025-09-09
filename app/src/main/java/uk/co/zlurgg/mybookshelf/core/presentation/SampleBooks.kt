package uk.co.zlurgg.mybookshelf.core.presentation

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookColorGenerator

val sampleBooks = List(50) {
    Book(
        id = "sample-$it",
        title = "Test Book $it with a longer title making it a longer for each one $it",
        authors = listOf("Author"),
        imageUrl = "https://picsum.photos/200/300",
        description = null,
        purchased = false,
        affiliateLink = "",
        spineColor = BookColorGenerator.generateSpineColor(),
        languages = listOf(""),
        firstPublishYear = null,
        averageRating = null,
        ratingCount = null,
        numPages = 1000,
        numEditions = 1
    )
}
val sampleBook =
Book(
    id = "sample-book-single",
    title = "One Book with a longer title making it a bit too crazy",
    authors = listOf("Author"),
    imageUrl = "https://picsum.photos/200/300",
    description = null,
    purchased = false,
    affiliateLink = "",
    spineColor = BookColorGenerator.generateSpineColor(),
    languages = listOf(""),
    firstPublishYear = null,
    averageRating = null,
    ratingCount = null,
    numPages = 1000,
    numEditions = 1
)