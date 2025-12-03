package uk.co.zlurgg.mybookshelf.auth.presentation

sealed interface SignInAction {
    data object SignIn : SignInAction
    data object ResetState : SignInAction
}
