package uk.co.zlurgg.mybookshelf.bookshelf.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

class WelcomeViewModel(
    private val welcomePreferences: WelcomePreferences
) : ViewModel() {

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
            welcomePreferences.setWelcomeShown()
        }
    }
}
