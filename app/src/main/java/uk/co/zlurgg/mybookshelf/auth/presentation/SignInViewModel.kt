package uk.co.zlurgg.mybookshelf.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SignInViewModel(
    private val signInUseCases: SignInUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    init {
        checkSignInStatus()
    }

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.SignIn -> signIn()
            is SignInAction.ResetState -> resetState()
        }
    }

    private fun checkSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = signInUseCases.checkSignInStatus.execute()
            if (isSignedIn) {
                _state.update { it.copy(isSignInSuccessful = true) }
            }
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = signInUseCases.signIn.execute()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "sign in")
                        )
                    }
                }
            }
        }
    }

    private fun resetState() {
        _state.update {
            it.copy(
                isLoading = false,
                isSignInSuccessful = false,
                errorMessage = null
            )
        }
    }
}
