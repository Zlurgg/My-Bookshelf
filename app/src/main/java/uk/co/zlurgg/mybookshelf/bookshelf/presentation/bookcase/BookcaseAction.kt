package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle


sealed interface BookcaseAction {
    data class OnBookshelfClick(val bookshelf: Bookshelf) : BookcaseAction
    data class OnAddBookshelfClick(val name: String, val style: ShelfStyle) : BookcaseAction
    data class OnRemoveBookShelf(val bookshelf: Bookshelf) : BookcaseAction
    data class OnUndoRemove(val bookshelf: Bookshelf) : BookcaseAction
    data class ShowAddDialog(val showDialog: Boolean) : BookcaseAction
    data object ResetOperationState : BookcaseAction
    data object ToggleReorderMode : BookcaseAction
    data class OnReorderShelf(val bookshelf: Bookshelf, val newPosition: Int) : BookcaseAction
    data class ShowRenameDialog(val bookshelf: Bookshelf) : BookcaseAction
    data object DismissRenameDialog : BookcaseAction
    data class OnRenameShelf(val shelfId: String, val newName: String) : BookcaseAction
    data class ShowChangeStyleDialog(val bookshelf: Bookshelf) : BookcaseAction
    data object DismissChangeStyleDialog : BookcaseAction
    data class OnChangeStyle(val shelfId: String, val newStyle: ShelfStyle) : BookcaseAction
}