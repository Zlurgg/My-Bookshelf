package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial


sealed interface BookcaseAction {
    data class OnBookshelfClick(val bookshelf: Bookshelf) : BookcaseAction
    data class OnAddBookshelfClick(val name: String) : BookcaseAction
    data class OnRemoveBookShelf(val bookshelf: Bookshelf) : BookcaseAction
    data class OnUndoRemove(val bookshelf: Bookshelf) : BookcaseAction
    data class OnChangeShelfMaterial(val shelfMaterial: ShelfMaterial) : BookcaseAction
    data class ShowAddDialog(val showDialog: Boolean) : BookcaseAction
    data object ResetOperationState : BookcaseAction

}