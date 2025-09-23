package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

sealed class DeepLinkAction {
    data class ImportFromToken(val token: String) : DeepLinkAction()
    data object DismissError : DeepLinkAction()
    data object DismissSuccess : DeepLinkAction()
    data object DismissNameConflict : DeepLinkAction()
    data class ResolveNameConflictWithNewName(val jsonData: String, val newName: String) : DeepLinkAction()
}