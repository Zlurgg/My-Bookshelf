package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf


sealed interface BookcaseAction {
    data class OnBookShelfClick(val bookshelf: Bookshelf) : BookcaseAction
    data class OnAddBookShelf(val name: String) : BookcaseAction
    data class OnRemoveBookShelf(val bookshelf: Bookshelf) : BookcaseAction
    data class OnUndoRemove(val bookshelf: Bookshelf) : BookcaseAction
    data object OnAddBookshelfDialogDismiss : BookcaseAction
    data object OnAddBookshelfDialogOpen : BookcaseAction
}