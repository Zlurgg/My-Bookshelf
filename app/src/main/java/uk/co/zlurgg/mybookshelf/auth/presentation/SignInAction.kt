package uk.co.zlurgg.mybookshelf.auth.presentation

sealed interface SignInAction {
    data object SignIn : SignInAction
    data class DevSignIn(val userNumber: Int = 1) : SignInAction // Debug builds only - signs in with test user
    data object ContinueAsGuest : SignInAction
    data object ResetState : SignInAction
    data object ImportGuestData : SignInAction
    data object SkipGuestDataImport : SignInAction
}
