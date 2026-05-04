package uk.co.zlurgg.mybookshelf.app.presentation.theme

import uk.co.zlurgg.mybookshelf.core.domain.model.ThemeMode

sealed interface ThemeAction {
    data class SetThemeMode(val mode: ThemeMode) : ThemeAction
}
