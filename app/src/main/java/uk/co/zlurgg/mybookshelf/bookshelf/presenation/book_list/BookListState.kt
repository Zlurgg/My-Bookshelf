package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_list

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

data class BookListState(
    val searchQuery: String = "Kotlin",
    val searchResults: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTabIndex: Int = 0,
)
