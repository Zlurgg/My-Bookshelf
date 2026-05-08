package uk.co.zlurgg.mybookshelf.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

/**
 * DataStore-based implementation of WelcomePreferences.
 * Welcome state is per-device using a single key.
 */
class WelcomePreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : WelcomePreferences {

    override suspend fun setWelcomeShown() {
        dataStore.edit { preferences ->
            preferences[WELCOME_SHOWN_KEY] = true
        }
    }

    override fun hasShownWelcome(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[WELCOME_SHOWN_KEY] ?: false
        }
    }

    companion object {
        private val WELCOME_SHOWN_KEY = booleanPreferencesKey("welcome_shown")
    }
}
