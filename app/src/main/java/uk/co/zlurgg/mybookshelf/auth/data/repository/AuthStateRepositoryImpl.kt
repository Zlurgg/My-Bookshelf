package uk.co.zlurgg.mybookshelf.auth.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository

private val Context.authDataStore by preferencesDataStore(name = "auth_preferences")

class AuthStateRepositoryImpl(
    private val context: Context
) : AuthStateRepository {

    override suspend fun isSignedIn(): Boolean {
        return context.authDataStore.data
            .map { preferences -> preferences[SIGNED_IN_KEY] ?: false }
            .first()
    }

    override suspend fun setSignedInState(isSignedIn: Boolean) {
        context.authDataStore.edit { preferences ->
            preferences[SIGNED_IN_KEY] = isSignedIn
        }
    }

    companion object {
        private val SIGNED_IN_KEY = booleanPreferencesKey("signed_in_state")
    }
}
