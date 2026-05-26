package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferenceState
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferences

class StubSearchPreferences : SearchPreferences {
    private val _flow = MutableStateFlow(SearchPreferenceState())
    var lastUpdatedState: SearchPreferenceState? = null

    override fun observe(): Flow<SearchPreferenceState> = _flow

    override suspend fun update(state: SearchPreferenceState) {
        lastUpdatedState = state
        _flow.value = state
    }

    /**
     * Pre-set the observed prefs state for a test, without recording it as a
     * persisted update — distinguishes test setup from VM-driven persistence.
     */
    fun seed(state: SearchPreferenceState) {
        _flow.value = state
    }

    fun reset() {
        _flow.value = SearchPreferenceState()
        lastUpdatedState = null
    }
}
