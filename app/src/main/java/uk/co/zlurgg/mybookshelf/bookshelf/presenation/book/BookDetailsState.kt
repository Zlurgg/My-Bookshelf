package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

data class BookDetailsState(
    val rating: Int? = null,
    val isPurchased: Boolean = false,
    val book: Book? = null
)
