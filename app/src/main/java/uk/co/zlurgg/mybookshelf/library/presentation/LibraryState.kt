package uk.co.zlurgg.mybookshelf.library.presentation

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState

data class LibraryState(
    val allBooks: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortOption: LibrarySortOption = LibrarySortOption.RECENTLY_ADDED,
    val selectedReadingStatus: ReadingStatus? = null,
    val isTidyMode: Boolean = false,
    val isSearchDialogVisible: Boolean = false,
    val bookSearchState: BookSearchState = BookSearchState()
)
