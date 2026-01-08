package uk.co.zlurgg.mybookshelf.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdatePreferencesRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "welcome_preferences")

/**
 * DataStore-based implementation of WelcomePreferences and UpdatePreferencesRepository.
 * Handles persistent storage of welcome screen state and update preferences using AndroidX DataStore.
 *
 * Welcome state is stored per-user using keys like "welcome_shown_<userId>" or "welcome_shown_guest".
 */
class WelcomePreferencesImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore,
) : WelcomePreferences, UpdatePreferencesRepository {
    private object PreferencesKeys {
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")

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

    override suspend fun getDismissedVersion(): String? {
        return dataStore.data.first()[PreferencesKeys.DISMISSED_UPDATE_VERSION]
    }

    override suspend fun setDismissedVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISMISSED_UPDATE_VERSION] = version
        }
    }
}
