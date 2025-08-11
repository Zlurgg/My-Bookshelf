package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

data class BookshelfState(
    val books: List<Book> = emptyList(),
    val searchQuery: String = "Kotlin",
    val searchResults: List<Book> = emptyList(),
)