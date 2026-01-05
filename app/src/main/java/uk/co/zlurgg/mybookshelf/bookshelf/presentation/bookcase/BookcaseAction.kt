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
    data object OnTutorialShelfClick : BookcaseAction
    data class OnShareShelfClick(val shelf: Bookshelf) : BookcaseAction
    data class OnDuplicateShelfClick(val shelf: Bookshelf) : BookcaseAction
    data object ResetSwitchToPersonalTab : BookcaseAction

    // Share Options Actions (Book Club)
    data object DismissShareOptions : BookcaseAction
    data object OnShareCopy : BookcaseAction
    data object OnCreateBookClub : BookcaseAction
    data object DismissInviteLink : BookcaseAction

    // Join Book Club Actions
    data object ShowJoinBookClubDialog : BookcaseAction
    data object DismissJoinBookClubDialog : BookcaseAction
    data class OnLookupBookClub(val codeOrUrl: String) : BookcaseAction
    data object DismissBookClubPreview : BookcaseAction
    data object OnConfirmJoinBookClub : BookcaseAction
    data object DismissJoinSuccess : BookcaseAction
    data class HandleInviteLink(val code: String) : BookcaseAction

    // Settings Menu Actions
    data object CheckForUpdates : BookcaseAction
    data object DownloadUpdate : BookcaseAction
    data object DismissUpdate : BookcaseAction
    data object DismissUpToDate : BookcaseAction

    // Auth Actions
    data object OnSignInClick : BookcaseAction
    data object ResetNavigateToSignIn : BookcaseAction
    data object ShowSignOutDialog : BookcaseAction
    data object DismissSignOutDialog : BookcaseAction
    data object ConfirmSignOut : BookcaseAction
}