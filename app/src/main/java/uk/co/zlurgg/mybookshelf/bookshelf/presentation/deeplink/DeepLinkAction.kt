package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

sealed interface DeepLinkAction {
    data class ImportFromToken(val token: String) : DeepLinkAction
    data object OnDismissError : DeepLinkAction
    data object OnDismissSuccess : DeepLinkAction
    data object OnDismissNameConflict : DeepLinkAction
    data class ResolveNameConflictWithNewName(val jsonData: String, val newName: String) : DeepLinkAction
}