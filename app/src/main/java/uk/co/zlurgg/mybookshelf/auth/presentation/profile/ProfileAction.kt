package uk.co.zlurgg.mybookshelf.auth.presentation.profile

sealed interface ProfileAction {
    data object ShowSignOutDialog : ProfileAction
    data object DismissSignOutDialog : ProfileAction
    data object ConfirmSignOut : ProfileAction
    data object RequestDeleteAccount : ProfileAction
    data object DismissDeleteConfirm : ProfileAction
    data object ConfirmDeleteAccount : ProfileAction
    data class OnReAuthCompleted(val idToken: String) : ProfileAction
    data object DismissReAuth : ProfileAction
    data object DismissError : ProfileAction
    data object ResetNavigation : ProfileAction
}
