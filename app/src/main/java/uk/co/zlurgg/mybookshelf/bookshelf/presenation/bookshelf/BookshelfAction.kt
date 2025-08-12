package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

sealed interface BookshelfAction {
    data class OnBookClick(val book: Book) : BookshelfAction
    data class OnRemoveBook(val book: Book) : BookshelfAction
    data object OnUndoRemove : BookshelfAction
    data class OnAddBook(val book: Book) : BookshelfAction
    data object OnBackClick : BookshelfAction
}