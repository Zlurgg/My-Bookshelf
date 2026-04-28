package uk.co.zlurgg.mybookshelf.sharing.presentation

sealed interface DeepLinkAction {
    data class ImportFromToken(val token: String) : DeepLinkAction
    data object OnDismissError : DeepLinkAction
    data object OnDismissSuccess : DeepLinkAction
    data object OnDismissNameConflict : DeepLinkAction
    data class ResolveNameConflictWithNewName(val jsonData: String, val newName: String) : DeepLinkAction

    // Book club invite handling
    data class ReceiveBookClubInvite(val code: String) : DeepLinkAction
    data object ClearBookClubInvite : DeepLinkAction
}
