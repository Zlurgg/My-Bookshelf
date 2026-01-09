package uk.co.zlurgg.mybookshelf.auth.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

private val Context.authDataStore by preferencesDataStore(name = "auth_preferences")

class AuthStateRepositoryImpl(
    private val context: Context
) : AuthStateRepository {

    override suspend fun isSignedIn(): Result<Boolean, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            context.authDataStore.data
                .map { preferences -> preferences[SIGNED_IN_KEY] ?: false }
                .first()
        }
    }

    override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            context.authDataStore.edit { preferences ->
                preferences[SIGNED_IN_KEY] = isSignedIn
            }
        }
    }

    companion object {
        private const val TAG = "AuthStateRepository"
        private val SIGNED_IN_KEY = booleanPreferencesKey("signed_in_state")
    }
}
