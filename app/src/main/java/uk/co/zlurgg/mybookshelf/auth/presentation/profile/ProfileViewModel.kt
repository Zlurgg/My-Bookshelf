package uk.co.zlurgg.mybookshelf.auth.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ProfileViewModel(
    private val authUseCases: AuthUseCases,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUser()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.ShowSignOutDialog -> _state.update { it.copy(showSignOutDialog = true) }
            ProfileAction.DismissSignOutDialog -> _state.update { it.copy(showSignOutDialog = false) }
            ProfileAction.ConfirmSignOut -> signOut()
            ProfileAction.RequestDeleteAccount -> _state.update { it.copy(showDeleteConfirmDialog = true) }
            ProfileAction.DismissDeleteConfirm -> _state.update { it.copy(showDeleteConfirmDialog = false) }
            ProfileAction.ConfirmDeleteAccount -> deleteAccount()
            is ProfileAction.OnReAuthCompleted -> retryAfterReAuth(action.idToken)
            ProfileAction.DismissReAuth -> _state.update { it.copy(showReAuthDialog = false) }
            ProfileAction.DismissError -> _state.update { it.copy(errorMessage = null) }
            ProfileAction.ResetNavigation -> _state.update { it.copy(navigateToSignIn = false) }
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
        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmDialog = false, isDeleting = true) }
            when (val result = authUseCases.deleteAccount()) {
                is Result.Success -> _state.update { it.copy(isDeleting = false, navigateToSignIn = true) }
                is Result.Error -> handleDeleteError(result.error)
            }
        }
    }

    private fun retryAfterReAuth(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(showReAuthDialog = false, isDeleting = true) }
            when (val result = authUseCases.deleteAccount.retryAfterReAuth(idToken)) {
                is Result.Success -> _state.update { it.copy(isDeleting = false, navigateToSignIn = true) }
                is Result.Error -> handleDeleteError(result.error)
            }
        }
    }

    private fun handleDeleteError(error: DataError) {
        if (error == DataError.Local.REQUIRES_RECENT_LOGIN) {
            _state.update { it.copy(isDeleting = false, showReAuthDialog = true) }
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
        private const val TAG = "ProfileVM"
    }
}
