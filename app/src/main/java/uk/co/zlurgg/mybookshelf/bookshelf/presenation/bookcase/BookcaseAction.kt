package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle


sealed interface BookcaseAction {
    data class OnBookshelfClick(val bookshelf: Bookshelf) : BookcaseAction
    data class OnAddBookshelfClick(val name: String, val style: ShelfStyle) : BookcaseAction
    data class OnRemoveBookShelf(val bookshelf: Bookshelf) : BookcaseAction
    data class OnUndoRemove(val bookshelf: Bookshelf) : BookcaseAction
    data class ShowAddDialog(val showDialog: Boolean) : BookcaseAction
    data object ResetOperationState : BookcaseAction
}