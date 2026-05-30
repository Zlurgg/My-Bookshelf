package uk.co.zlurgg.mybookshelf.bookshelf.presentation

import uk.co.zlurgg.mybookshelf.book.domain.model.Book

sealed interface BookshelfAction {
    data class OnBookClick(val book: Book) : BookshelfAction
    data object OnSearchClick : BookshelfAction
    data class OnSearchQueryChange(val query: String) : BookshelfAction
    data object OnToggleSearchByTitle : BookshelfAction
    data object OnToggleSearchByAuthor : BookshelfAction
    data object OnToggleSearchBySubject : BookshelfAction
    data object OnToggleSafeSearch : BookshelfAction
    data object OnToggleLibraryScope : BookshelfAction
    data object OnLoadMore : BookshelfAction
    data object OnDismissSearchDialog : BookshelfAction
    data class OnRemoveBook(val book: Book) : BookshelfAction
    data object OnUndoRemove : BookshelfAction
    data class OnAddBookClick(val book: Book) : BookshelfAction
    data object OnBackClick : BookshelfAction
    data object OnToggleTidyMode : BookshelfAction
    data object OnCreateBookClub : BookshelfAction
    data class OnSearchResultBookClick(val book: Book) : BookshelfAction
    data object OnNavigationHandled : BookshelfAction
}
