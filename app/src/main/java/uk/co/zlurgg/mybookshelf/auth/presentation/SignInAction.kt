package uk.co.zlurgg.mybookshelf.auth.presentation

import android.content.Context

sealed interface SignInAction {
    data class SignIn(val context: Context) : SignInAction
    data object ResetState : SignInAction
}
