package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.BookShelf

data class BookcaseState(
    val bookshelves: List<BookShelf> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val recentlyDeleted: BookShelf? = null
)