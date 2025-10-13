package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

data class BookDetailState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val onShelf: Boolean = false,
    val errorMessage: String? = null
)
