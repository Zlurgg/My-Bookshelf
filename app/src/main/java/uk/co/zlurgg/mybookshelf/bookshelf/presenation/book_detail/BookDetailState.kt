package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

data class BookDetailState(
    val isLoading: Boolean = true,
    val rating: Int? = null,
    val isPurchased: Boolean = false,
    val book: Book? = null
)
