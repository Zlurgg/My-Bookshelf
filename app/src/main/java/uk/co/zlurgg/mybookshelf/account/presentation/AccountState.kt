package uk.co.zlurgg.mybookshelf.account.presentation

data class AccountState(
    val userEmail: String? = null,
    val userName: String? = null,
    val profilePictureUrl: String? = null,
    val isSignedIn: Boolean = false,
    val showSignOutDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val requestReAuth: Boolean = false,
    val navigateToSignIn: Boolean = false,
    val errorMessage: String? = null,
)
