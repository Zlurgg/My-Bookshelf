package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

data class BookSearchState(
    val searchQuery: String = "Programming Books",
    val searchResults: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTabIndex: Int = 0,
)
