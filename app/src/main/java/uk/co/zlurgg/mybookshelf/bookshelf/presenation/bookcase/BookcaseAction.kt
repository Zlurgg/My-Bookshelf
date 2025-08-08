package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.BookShelf

sealed class BookcaseAction {
    data class OnBookShelfClick(val bookshelf: BookShelf) : BookcaseAction()
    data class OnAddBookShelf(val name: String) : BookcaseAction()
    data class OnRemoveBookShelf(val bookshelf: BookShelf) : BookcaseAction()
    data class OnUndoRemove(val bookshelf: BookShelf) : BookcaseAction()
    object OnAddDialogDismiss : BookcaseAction()
    object OnAddDialogOpen : BookcaseAction()
}