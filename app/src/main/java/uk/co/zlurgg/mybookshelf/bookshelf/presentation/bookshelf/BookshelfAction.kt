package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort

sealed interface BookshelfAction {
    data class OnBookClick(val book: Book) : BookshelfAction
    data object OnSearchClick : BookshelfAction
    data class OnSearchQueryChange(val query: String) : BookshelfAction
    data class OnSortChange(val sort: BookSearchSort) : BookshelfAction
    data object OnToggleAdvancedSearch : BookshelfAction
    data class OnAuthorFilterChange(val authorFilter: String) : BookshelfAction
    data class OnTitleFilterChange(val titleFilter: String) : BookshelfAction
    data object OnDismissSearchDialog : BookshelfAction
    data class OnRemoveBook(val book: Book) : BookshelfAction
    data object OnUndoRemove : BookshelfAction
    data class OnAddBookClick(val book: Book) : BookshelfAction
    data object OnBackClick : BookshelfAction
    data object OnToggleTidyMode : BookshelfAction
}