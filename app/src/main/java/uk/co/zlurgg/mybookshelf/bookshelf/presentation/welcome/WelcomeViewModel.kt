package uk.co.zlurgg.mybookshelf.bookshelf.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences
import uk.co.zlurgg.mybookshelf.sync.domain.repository.UserPreferencesRepository

class WelcomeViewModel(
    private val welcomePreferences: WelcomePreferences,
    private val currentUserProvider: CurrentUserProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    companion object {
        private const val TAG = "WelcomeVM"
    }

    private val _state = MutableStateFlow(WelcomeState())
    val state: StateFlow<WelcomeState> = _state.asStateFlow()

    fun onAction(action: WelcomeAction) {
        when (action) {
            is WelcomeAction.OnGetStartedClick -> {
                markWelcomeAsShown()
            }
        }
    }

    private fun markWelcomeAsShown() {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId()

            // Write to local DataStore (immediate)
            welcomePreferences.setWelcomeShown(userId)
            Timber.tag(TAG).d("Welcome marked as shown locally for user: %s", userId ?: "guest")

            // Write to Firestore (best-effort, don't block navigation)
            userId?.let { uid ->
                userPreferencesRepository.setWelcomeShown(uid, true)
                Timber.tag(TAG).d("Welcome marked as shown in cloud for user: %s", uid)
            }
        }
    }
}
