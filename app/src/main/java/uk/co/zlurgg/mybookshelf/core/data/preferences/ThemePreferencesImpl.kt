package uk.co.zlurgg.mybookshelf.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.core.domain.model.ThemeMode
import uk.co.zlurgg.mybookshelf.core.domain.preferences.ThemePreferences

class ThemePreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : ThemePreferences {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    override fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data.map { preferences ->
            ThemeMode.fromKey(preferences[themeModeKey] ?: ThemeMode.SYSTEM.key)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[themeModeKey] = mode.key
        }
    }
}
