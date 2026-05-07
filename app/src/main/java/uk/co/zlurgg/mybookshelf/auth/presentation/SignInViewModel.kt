package uk.co.zlurgg.mybookshelf.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.DevSignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.ResumeSessionUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.ShouldShowWelcomeUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SignInViewModel(
    private val authUseCases: AuthUseCases,
    private val shouldShowWelcome: ShouldShowWelcomeUseCase,
    private val resumeSession: ResumeSessionUseCase,
    private val devSignInUseCase: DevSignInUseCase? = null // Only injected in debug builds
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    init {
        checkSignInStatus()
    }

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.SignIn -> signIn(action.fetchCredential)
            is SignInAction.DevSignIn -> devSignIn(action.userNumber)
            is SignInAction.ContinueAsGuest -> continueAsGuest()
            is SignInAction.ResetState -> resetState()
        }
    }

    /**
     * Development-only sign-in using the Auth Emulator.
     * Creates/signs-in a test user with email/password.
     *
     * @param userNumber Which test user to sign in as (1=Alice, 2=Bob, 3=Charlie)
     */
    private fun devSignIn(userNumber: Int) {
        if (!BuildConfig.DEBUG || devSignInUseCase == null) {
            Timber.tag(TAG).w("Dev sign-in attempted in release build or use case not available")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = devSignInUseCase(userNumber)) {
                is Result.Success -> {
                    Timber.tag(TAG).d("Dev sign-in successful: %s", result.data.userId)

                    resumeSession()

                    val destination = determineDestination()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true,
                            navigateToDestination = destination
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                result.error,
                                "dev sign-in",
                            )
                        )
                    }
                }
            }
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
            val isSignedIn = authUseCases.checkSignInStatus()
            if (isSignedIn) {
                resumeSession()

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

    private fun signIn(
        fetchCredential: suspend () -> Result<String, DataError.Local>
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val credentialResult = fetchCredential()
            if (credentialResult is Result.Error) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorFormatter.formatDataErrorMessage(
                            credentialResult.error,
                            "sign in"
                        )
                    )
                }
                return@launch
            }

            val idToken = (credentialResult as Result.Success).data
            when (val result = authUseCases.signIn(idToken)) {
                is Result.Success -> {
                    resumeSession()

                    val destination = determineDestination()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true,
                            navigateToDestination = destination
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

    private suspend fun determineDestination(): PostSignInDestination {
        return if (shouldShowWelcome()) {
            PostSignInDestination.Welcome
        } else {
            PostSignInDestination.Bookcase
        }
    }

    private fun resetState() {
        _state.update {
            it.copy(
                isLoading = false,
                isSignInSuccessful = false,
                isContinuingAsGuest = false,
                errorMessage = null,
                navigateToDestination = null
            )
        }
    }

    companion object {
        private const val TAG = "SignInVM"
    }
}
