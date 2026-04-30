package uk.co.zlurgg.mybookshelf.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.account.domain.usecase.DeleteAccountUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class AccountViewModel(
    private val authUseCases: AuthUseCases,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        loadUser()
    }

    fun onAction(action: AccountAction) {
        when (action) {
            AccountAction.ShowSignOutDialog -> _state.update { it.copy(showSignOutDialog = true) }
            AccountAction.DismissSignOutDialog -> _state.update { it.copy(showSignOutDialog = false) }
            AccountAction.ConfirmSignOut -> signOut()
            AccountAction.RequestDeleteAccount -> _state.update { it.copy(showDeleteConfirmDialog = true) }
            AccountAction.DismissDeleteConfirm -> _state.update { it.copy(showDeleteConfirmDialog = false) }
            AccountAction.ConfirmDeleteAccount -> deleteAccount()
            is AccountAction.OnReAuthCompleted -> retryAfterReAuth(action.idToken)
            AccountAction.OnReAuthFailed -> {
                _state.update {
                    it.copy(errorMessage = "Sign-in was cancelled or failed")
                }
            }
            AccountAction.DismissError -> _state.update { it.copy(errorMessage = null) }
            AccountAction.ResetNavigation -> _state.update { it.copy(navigateToSignIn = false) }
            AccountAction.ResetReAuth -> _state.update { it.copy(requestReAuth = false) }
        }
    }

    private fun loadUser() {
        val user = authUseCases.getSignedInUser()
        if (user != null) {
            _state.update {
                it.copy(
                    userName = user.username,
                    userEmail = user.email,
                    profilePictureUrl = user.profilePictureUrl,
                    isSignedIn = true,
                )
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(showSignOutDialog = false) }
            when (val result = authUseCases.signOut()) {
                is Result.Success -> _state.update { it.copy(navigateToSignIn = true) }
                is Result.Error -> {
                    Timber.tag(TAG).e("Sign-out failed: %s", result.error)
                    _state.update {
                        it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "sign out"))
                    }
                }
            }
        }
    }

    private fun deleteAccount() {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmDialog = false, isDeleting = true) }
            when (val result = deleteAccountUseCase()) {
                is Result.Success -> _state.update { it.copy(isDeleting = false, navigateToSignIn = true) }
                is Result.Error -> handleDeleteError(result.error)
            }
        }
    }

    private fun retryAfterReAuth(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (val result = deleteAccountUseCase.retryAfterReAuth(idToken)) {
                is Result.Success -> _state.update { it.copy(isDeleting = false, navigateToSignIn = true) }
                is Result.Error -> handleDeleteError(result.error)
            }
        }
    }

    private fun handleDeleteError(error: DataError) {
        if (error == DataError.Local.REQUIRES_RECENT_LOGIN) {
            _state.update { it.copy(isDeleting = false, requestReAuth = true) }
        } else {
            Timber.tag(TAG).e("Account deletion failed: %s", error)
            _state.update {
                it.copy(
                    isDeleting = false,
                    errorMessage = ErrorFormatter.formatDataErrorMessage(error, "delete account"),
                )
            }
        }
    }

    companion object {
        private const val TAG = "AccountVM"
    }
}
