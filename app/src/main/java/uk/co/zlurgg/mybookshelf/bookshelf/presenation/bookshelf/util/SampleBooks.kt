package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book

val sampleBooks = List(50) {
    Book(
        id = it.toString(),
        title = "Test Book $it with a longer title making int a bit",
        author = "Author",
        spineImageUrl = "https://picsum.photos/200/300",
        fullImageUrl = "",
        blurb = "",
        purchased = false,
        affiliateLink = "",
        spineColor = randomReadableDarkColor()
    )
}