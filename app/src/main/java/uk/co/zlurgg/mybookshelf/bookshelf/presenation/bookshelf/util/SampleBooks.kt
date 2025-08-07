package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book

val sampleBooks = List(50) {
    Book(
        id = it.toString(),
        title = "Test Book $it with a longer title making int a bit",
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
        numEditions = 1
    )
}
val sampleBook =
Book(
    id = "1",
    title = "One Book with a longer title making int a bit",
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
    numEditions = 1
)