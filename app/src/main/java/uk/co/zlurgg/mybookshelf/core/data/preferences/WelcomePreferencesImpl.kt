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
 */
class WelcomePreferencesImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) : WelcomePreferences, UpdatePreferencesRepository {

    private object PreferencesKeys {
        val WELCOME_SHOWN = booleanPreferencesKey("welcome_shown")
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
    }

    override suspend fun setWelcomeShown() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WELCOME_SHOWN] = true
        }
    }

    override fun hasShownWelcome(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.WELCOME_SHOWN] ?: false
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
