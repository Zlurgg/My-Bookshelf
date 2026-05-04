package uk.co.zlurgg.mybookshelf.app.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.core.domain.preferences.ThemePreferences

class ThemeViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ThemeState())
    val state: StateFlow<ThemeState> = _state.asStateFlow()

    init {
        themePreferences.observeThemeMode()
            .onEach { mode -> _state.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: ThemeAction) {
        when (action) {
            is ThemeAction.SetThemeMode -> {
                viewModelScope.launch {
                    themePreferences.setThemeMode(action.mode)
                }
            }
        }
    }
}
