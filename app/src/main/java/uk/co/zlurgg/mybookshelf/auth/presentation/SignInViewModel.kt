package uk.co.zlurgg.mybookshelf.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.ShouldShowWelcomeUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.HasGuestDataUseCase
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.MigrateLocalDataUseCase

class SignInViewModel(
    private val signInUseCases: SignInUseCases,
    private val shouldShowWelcome: ShouldShowWelcomeUseCase,
    private val hasGuestDataUseCase: HasGuestDataUseCase,
    private val migrateLocalDataUseCase: MigrateLocalDataUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "SignInVM"
    }

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    init {
        checkSignInStatus()
    }

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.SignIn -> signIn()
            is SignInAction.ContinueAsGuest -> continueAsGuest()
            is SignInAction.ResetState -> resetState()
            is SignInAction.ImportGuestData -> importGuestData()
            is SignInAction.SkipGuestDataImport -> skipGuestDataImport()
        }
    }

    private fun continueAsGuest() {
        viewModelScope.launch {
            val destination = determineDestination()
            _state.update {
                it.copy(
                    isContinuingAsGuest = true,
                    navigateToDestination = destination
                )
            }
        }
    }

    private fun checkSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = signInUseCases.checkSignInStatus.execute()
            if (isSignedIn) {
                val destination = determineDestination()
                _state.update {
                    it.copy(
                        isSignInSuccessful = true,
                        navigateToDestination = destination
                    )
                }
            }
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = signInUseCases.signIn.execute()) {
                is Result.Success -> {
                    // Check for guest data before navigating
                    val guestDataInfo = hasGuestDataUseCase.execute()
                    Timber.tag(TAG).d("Guest data check: %s", guestDataInfo)

                    if (guestDataInfo.hasData) {
                        // Show dialog to ask user about importing guest data
                        Timber.tag(TAG).d("Guest data found, showing import dialog")
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSignInSuccessful = true,
                                showGuestDataImportDialog = true,
                                guestDataInfo = guestDataInfo
                            )
                        }
                    } else {
                        // No guest data, proceed to navigation
                        Timber.tag(TAG).d("No guest data, proceeding to navigation")
                        val destination = determineDestination()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSignInSuccessful = true,
                                navigateToDestination = destination
                            )
                        }
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

    private suspend fun determineDestination(): PostSignInDestination {
        return if (shouldShowWelcome.execute()) {
            PostSignInDestination.Welcome
        } else {
            PostSignInDestination.Bookcase
        }
    }

    private fun importGuestData() {
        viewModelScope.launch {
            Timber.tag(TAG).d("User chose to import guest data")
            _state.update { it.copy(isLoading = true) }

            when (val result = migrateLocalDataUseCase.execute()) {
                is Result.Success -> {
                    Timber.tag(TAG).d("Guest data imported successfully")
                    val destination = determineDestination()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showGuestDataImportDialog = false,
                            guestDataInfo = null,
                            navigateToDestination = destination
                        )
                    }
                }
                is Result.Error -> {
                    Timber.tag(TAG).e("Failed to import guest data: %s", result.error)
                    // Still proceed, but show error briefly
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "import guest data")
                        )
                    }
                }
            }
        }
    }

    private fun skipGuestDataImport() {
        viewModelScope.launch {
            Timber.tag(TAG).d("User skipped guest data import")
            val destination = determineDestination()
            _state.update {
                it.copy(
                    showGuestDataImportDialog = false,
                    guestDataInfo = null,
                    navigateToDestination = destination
                )
            }
        }
    }

    private fun resetState() {
        _state.update {
            it.copy(
                isLoading = false,
                isSignInSuccessful = false,
                isContinuingAsGuest = false,
                errorMessage = null,
                navigateToDestination = null,
                showGuestDataImportDialog = false,
                guestDataInfo = null
            )
        }
    }
}
