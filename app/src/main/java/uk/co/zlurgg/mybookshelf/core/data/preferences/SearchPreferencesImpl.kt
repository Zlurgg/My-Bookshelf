package uk.co.zlurgg.mybookshelf.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferenceState
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferences

class SearchPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : SearchPreferences {

    companion object {
        private val SEARCH_BY_TITLE_KEY = booleanPreferencesKey("search_by_title")
        private val SEARCH_BY_AUTHOR_KEY = booleanPreferencesKey("search_by_author")
        private val SEARCH_BY_SUBJECT_KEY = booleanPreferencesKey("search_by_subject")
        private val SAFE_SEARCH_ENABLED_KEY = booleanPreferencesKey("safe_search_enabled")
    }

    override fun observe(): Flow<SearchPreferenceState> =
        dataStore.data.map { prefs ->
            SearchPreferenceState(
                searchByTitle = prefs[SEARCH_BY_TITLE_KEY] ?: true,
                searchByAuthor = prefs[SEARCH_BY_AUTHOR_KEY] ?: false,
                searchBySubject = prefs[SEARCH_BY_SUBJECT_KEY] ?: false,
                safeSearchEnabled = prefs[SAFE_SEARCH_ENABLED_KEY] ?: true
            )
        }

    override suspend fun update(state: SearchPreferenceState) {
        dataStore.edit { prefs ->
            prefs[SEARCH_BY_TITLE_KEY] = state.searchByTitle
            prefs[SEARCH_BY_AUTHOR_KEY] = state.searchByAuthor
            prefs[SEARCH_BY_SUBJECT_KEY] = state.searchBySubject
            prefs[SAFE_SEARCH_ENABLED_KEY] = state.safeSearchEnabled
        }
    }
}
