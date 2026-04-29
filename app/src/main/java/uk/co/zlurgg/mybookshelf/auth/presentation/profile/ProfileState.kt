package uk.co.zlurgg.mybookshelf.auth.presentation.profile

data class ProfileState(
    val userEmail: String? = null,
    val userName: String? = null,
    val profilePictureUrl: String? = null,
    val isSignedIn: Boolean = false,
    // Sign out
    val showSignOutDialog: Boolean = false,
    val navigateToSignIn: Boolean = false,
    // Delete account
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val showReAuthDialog: Boolean = false,
    val errorMessage: String? = null,
)
