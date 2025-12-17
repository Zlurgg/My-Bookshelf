package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

sealed interface BookshelfAction {
    data class OnBookClick(val book: Book) : BookshelfAction
    data object OnSearchClick : BookshelfAction
    data class OnSearchQueryChange(val query: String) : BookshelfAction
    data object OnToggleSearchByTitle : BookshelfAction
    data object OnToggleSearchByAuthor : BookshelfAction
    data object OnDismissSearchDialog : BookshelfAction
    data class OnRemoveBook(val book: Book) : BookshelfAction
    data object OnUndoRemove : BookshelfAction
    data class OnAddBookClick(val book: Book) : BookshelfAction
    data object OnBackClick : BookshelfAction
    data object OnToggleTidyMode : BookshelfAction
    data object OnShareShelf : BookshelfAction

    // Book Club Actions
    data object OnShowShareOptions : BookshelfAction
    data object OnDismissShareOptions : BookshelfAction
    data object OnShareCopy : BookshelfAction
    data object OnCreateBookClub : BookshelfAction
    data object OnDismissInviteLink : BookshelfAction
    data object OnCopyInviteLink : BookshelfAction
}