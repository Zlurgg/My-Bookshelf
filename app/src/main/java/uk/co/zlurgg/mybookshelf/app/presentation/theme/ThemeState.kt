package uk.co.zlurgg.mybookshelf.app.presentation.theme

import uk.co.zlurgg.mybookshelf.core.domain.model.ThemeMode

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
