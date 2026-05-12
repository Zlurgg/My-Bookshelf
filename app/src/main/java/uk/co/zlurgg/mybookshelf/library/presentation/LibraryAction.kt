package uk.co.zlurgg.mybookshelf.library.presentation

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus

sealed interface LibraryAction {
    data class OnSearchQueryChange(val query: String) : LibraryAction
    data class OnSortOptionSelected(val option: LibrarySortOption) : LibraryAction
    data class OnReadingStatusSelected(val status: ReadingStatus?) : LibraryAction
    data class OnBookClick(val book: Book) : LibraryAction
    data object OnToggleTidyMode : LibraryAction
}
