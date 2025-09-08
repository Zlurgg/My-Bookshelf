package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book

data class BookDetailState(
    val isLoading: Boolean = true,
    val rating: Int? = null,
    val isPurchased: Boolean = false,
    val book: Book? = null,
    val onShelf: Boolean = false
)
