package uk.co.zlurgg.mybookshelf.core.presentation

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.core.util.randomReadableDarkColor

val sampleBooks = List(50) {
    Book(
        id = it.toString(),
        title = "Test Book $it with a longer title making it a longer for each one $it",
        authors = listOf("Author"),
        imageUrl = "https://picsum.photos/200/300",
        description = null,
        purchased = false,
        affiliateLink = "",
        spineColor = randomReadableDarkColor(),
        languages = listOf(""),
        firstPublishYear = null,
        averageRating = null,
        ratingCount = null,
        numPages = 1000,
        numEditions = 1,
        onShelf = false
    )
}
val sampleBook =
Book(
    id = "1",
    title = "One Book with a longer title making it a bit too crazy",
    authors = listOf("Author"),
    imageUrl = "https://picsum.photos/200/300",
    description = null,
    purchased = false,
    affiliateLink = "",
    spineColor = randomReadableDarkColor(),
    languages = listOf(""),
    firstPublishYear = null,
    averageRating = null,
    ratingCount = null,
    numPages = 1000,
    numEditions = 1,
    onShelf = false
)