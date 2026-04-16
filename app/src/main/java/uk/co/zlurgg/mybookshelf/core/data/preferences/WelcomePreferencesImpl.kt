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
 *
 * Welcome state is stored per-user using keys like "welcome_shown_<userId>" or "welcome_shown_guest".
 */
class WelcomePreferencesImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) : WelcomePreferences {

    private object PreferencesKeys {
        /**
         * Creates a per-user welcome shown key.
         * @param userId The user ID, or null for guest
         */
        fun welcomeShownKey(userId: String?): Preferences.Key<Boolean> {
            val suffix = userId ?: "guest"
            return booleanPreferencesKey("welcome_shown_$suffix")
        }
    }

    override suspend fun setWelcomeShown(userId: String?) {
        val key = PreferencesKeys.welcomeShownKey(userId)
        dataStore.edit { preferences ->
            preferences[key] = true
        }
    }

    override fun hasShownWelcome(userId: String?): Flow<Boolean> {
        val key = PreferencesKeys.welcomeShownKey(userId)
        return dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }
}
