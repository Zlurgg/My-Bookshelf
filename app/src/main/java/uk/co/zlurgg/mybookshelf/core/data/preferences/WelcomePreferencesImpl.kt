package uk.co.zlurgg.mybookshelf.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "welcome_preferences")

/**
 * DataStore-based implementation of WelcomePreferences.
 * Handles persistent storage of welcome screen state using AndroidX DataStore.
 */
class WelcomePreferencesImpl(private val context: Context) : WelcomePreferences {

    private object PreferencesKeys {
        val WELCOME_SHOWN = booleanPreferencesKey("welcome_shown")
    }

    override suspend fun setWelcomeShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WELCOME_SHOWN] = true
        }
    }

    override fun hasShownWelcome(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.WELCOME_SHOWN] ?: false
        }
    }
}
