package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

data class DeepLinkState(
    val isLoading: Boolean = false,
    val importSuccessful: Boolean = false,
    val error: String? = null,
    val conflictExistingName: String? = null,
    val conflictJsonData: String? = null,
    val conflictError: String? = null,  // Inline error for conflict resolution dialog
    val pendingClubCode: String? = null  // Book club invite code from deep link
)