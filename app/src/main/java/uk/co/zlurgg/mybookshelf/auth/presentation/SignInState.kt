package uk.co.zlurgg.mybookshelf.auth.presentation

data class SignInState(
    val isLoading: Boolean = false,
    val isSignInSuccessful: Boolean = false,
    val isContinuingAsGuest: Boolean = false,
    val errorMessage: String? = null
)
