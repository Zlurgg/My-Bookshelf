package uk.co.zlurgg.mybookshelf.core.domain.preferences

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.domain.model.ThemeMode

interface ThemePreferences {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
