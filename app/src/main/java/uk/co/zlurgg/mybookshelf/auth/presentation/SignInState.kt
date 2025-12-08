package uk.co.zlurgg.mybookshelf.auth.presentation

import uk.co.zlurgg.mybookshelf.sync.domain.model.GuestDataInfo

data class SignInState(
    val isLoading: Boolean = false,
    val isSignInSuccessful: Boolean = false,
    val isContinuingAsGuest: Boolean = false,
    val errorMessage: String? = null,
    val navigateToDestination: PostSignInDestination? = null,
    val showGuestDataImportDialog: Boolean = false,
    val guestDataInfo: GuestDataInfo? = null
)

/**
 * Represents the destination to navigate to after sign-in.
 */
sealed class PostSignInDestination {
    data object Welcome : PostSignInDestination()
    data object Bookcase : PostSignInDestination()
}
