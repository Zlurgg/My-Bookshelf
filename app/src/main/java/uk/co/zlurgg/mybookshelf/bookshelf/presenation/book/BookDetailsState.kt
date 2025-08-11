package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

data class BookDetailsState(
    val isLoading: Boolean = true,
    val rating: Int? = null,
    val isPurchased: Boolean = false,
    val book: Book? = null
)
