package uk.co.zlurgg.mybookshelf.account.presentation

sealed interface AccountAction {
    data object ShowSignOutDialog : AccountAction
    data object DismissSignOutDialog : AccountAction
    data object ConfirmSignOut : AccountAction
    data object RequestDeleteAccount : AccountAction
    data object DismissDeleteConfirm : AccountAction
    data object ConfirmDeleteAccount : AccountAction
    data class OnReAuthCompleted(val idToken: String) : AccountAction
    data object OnReAuthFailed : AccountAction
    data object DismissError : AccountAction
    data object ResetNavigation : AccountAction
    data object ResetReAuth : AccountAction
}
