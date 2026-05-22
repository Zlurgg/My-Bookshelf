package uk.co.zlurgg.mybookshelf.book.presentation.preview

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.BookColorGenerator

val sampleBooks = List(50) {
    Book(
        id = "sample-$it",
        title = "Test Book $it with a longer title making it a longer for each one $it",
        authors = listOf("Author"),
        imageUrl = "https://picsum.photos/200/300",
        description = null,
        purchased = false,
        spineColor = BookColorGenerator.generateSpineColor(),
        languages = listOf(""),
        firstPublishYear = null,
        numPages = 1000,
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
        spineColor = BookColorGenerator.generateSpineColor(),
        languages = listOf(""),
        firstPublishYear = null,
        numPages = 1000,
    )
