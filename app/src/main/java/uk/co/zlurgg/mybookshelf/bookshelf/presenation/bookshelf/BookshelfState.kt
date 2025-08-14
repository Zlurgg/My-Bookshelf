package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

data class BookshelfState(
    val shelfId: String = "",
    val shelfName: String = "",
    val books: List<Book> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Book> = emptyList(),
    val isSearchLoading: Boolean = false,
    val isSearchDialogVisible: Boolean = false,
    val recentlyDeleted: Book? = null,
)