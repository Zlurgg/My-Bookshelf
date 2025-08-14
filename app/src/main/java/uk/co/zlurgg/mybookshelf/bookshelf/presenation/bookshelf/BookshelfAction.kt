package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

sealed interface BookshelfAction {
    data class OnBookClick(val book: Book) : BookshelfAction
    data object OnSearchClick : BookshelfAction
    data class OnSearchQueryChange(val query: String) : BookshelfAction
    data object OnDismissSearchDialog : BookshelfAction
    data class OnRemoveBook(val book: Book) : BookshelfAction
    data object OnUndoRemove : BookshelfAction
    data class OnAddBookFromSearch(val book: Book) : BookshelfAction
    data object OnBackClick : BookshelfAction

}