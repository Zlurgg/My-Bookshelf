package uk.co.zlurgg.mybookshelf.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCases

class SignInViewModel(
    private val signInUseCases: SignInUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    init {
        checkSignInStatus()
    }

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.SignIn -> signIn(action.context)
            is SignInAction.ResetState -> resetState()
        }
    }

    private fun checkSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = signInUseCases.checkSignInStatus()
            if (isSignedIn) {
                Timber.tag(TAG).d("User already signed in, updating state")
                _state.update { it.copy(isSignInSuccessful = true) }
            }
        }
    }

    private fun signIn(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, signInError = null) }

            val result = signInUseCases.signIn(context)

            _state.update {
                it.copy(
                    isLoading = false,
                    isSignInSuccessful = result.data != null,
                    signInError = result.errorMessage
                )
            }
        }
    }

    private fun resetState() {
        _state.update { SignInState() }
    }

    companion object {
        private const val TAG = "SignInVM"
    }
}
