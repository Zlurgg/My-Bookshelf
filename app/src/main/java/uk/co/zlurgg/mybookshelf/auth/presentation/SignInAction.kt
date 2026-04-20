package uk.co.zlurgg.mybookshelf.auth.presentation

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

sealed interface SignInAction {
    data class SignIn(
        val fetchCredential: suspend () -> Result<String, DataError.Local>
    ) : SignInAction
    data class DevSignIn(val userNumber: Int = 1) : SignInAction // Debug builds only - signs in with test user
    data object ContinueAsGuest : SignInAction
    data object ResetState : SignInAction
    data object ImportGuestData : SignInAction
    data object SkipGuestDataImport : SignInAction
}
