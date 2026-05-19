package uk.co.zlurgg.mybookshelf.library.presentation

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus

sealed interface LibraryAction {
    data class OnSearchQueryChange(val query: String) : LibraryAction
    data class OnSortOptionSelected(val option: LibrarySortOption) : LibraryAction
    data class OnReadingStatusSelected(val status: ReadingStatus?) : LibraryAction
    data class OnBookClick(val book: Book) : LibraryAction
    data object OnToggleTidyMode : LibraryAction

    // Remote search dialog actions
    data object OnSearchClick : LibraryAction
    data object OnDismissSearchDialog : LibraryAction
    data class OnRemoteSearchQueryChange(val query: String) : LibraryAction
    data object OnToggleSearchByTitle : LibraryAction
    data object OnToggleSearchByAuthor : LibraryAction
    data class OnAddBookToLibrary(val book: Book) : LibraryAction
    data class OnSearchResultBookClick(val book: Book) : LibraryAction

    // Selection mode actions
    data object OnToggleSelectionMode : LibraryAction
    data class OnToggleBookSelection(val bookId: String) : LibraryAction
    data object OnSelectAll : LibraryAction
    data object OnDeselectAll : LibraryAction
    data object OnDeleteSelectedClick : LibraryAction
    data object OnConfirmDelete : LibraryAction
    data object OnDismissDeleteDialog : LibraryAction
}
