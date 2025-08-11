package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_list

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

data class BookListState(
    val searchQuery: String = "Programming Books",
    val searchResults: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTabIndex: Int = 0,
)
