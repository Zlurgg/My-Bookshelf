package uk.co.zlurgg.mybookshelf.core.domain.preferences

import kotlinx.coroutines.flow.Flow

data class SearchPreferenceState(
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = false,
    val searchBySubject: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val libraryScopeEnabled: Boolean = false,
)

interface SearchPreferences {
    fun observe(): Flow<SearchPreferenceState>
    suspend fun update(state: SearchPreferenceState)
}
