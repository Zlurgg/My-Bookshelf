package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.library.domain.usecase.LibraryUseCases

class LibraryViewModel(
    private val libraryUseCases: LibraryUseCases,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private val TIDY_MODE_KEY = booleanPreferencesKey("library_tidy_mode")
    }

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadTidyMode()
        observeBooks()
        observeDebouncedQuery()
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = action.query) }
                queryFlow.value = action.query
            }
            is LibraryAction.OnSortOptionSelected -> {
                _state.update { it.copy(sortOption = action.option) }
                applyFilters()
            }
            is LibraryAction.OnReadingStatusSelected -> {
                _state.update { it.copy(selectedReadingStatus = action.status) }
                applyFilters()
            }
            is LibraryAction.OnBookClick -> { /* handled by navigation callback */ }
            is LibraryAction.OnToggleTidyMode -> {
                val newMode = !_state.value.isTidyMode
                _state.update { it.copy(isTidyMode = newMode) }
                viewModelScope.launch {
                    dataStore.edit { it[TIDY_MODE_KEY] = newMode }
                }
            }
        }
    }

    private fun loadTidyMode() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val tidyMode = prefs[TIDY_MODE_KEY] ?: false
            _state.update { it.copy(isTidyMode = tidyMode) }
        }
    }

    private fun observeBooks() {
        viewModelScope.launch {
            libraryUseCases.getAllLibraryBooks().collectLatest { books ->
                _state.update { it.copy(allBooks = books, isLoading = false) }
                applyFilters()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDebouncedQuery() {
        viewModelScope.launch {
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { applyFilters() }
        }
    }

    private fun applyFilters() {
        val currentState = _state.value
        val query = currentState.searchQuery.trim()
        val status = currentState.selectedReadingStatus

        var result = currentState.allBooks

        // Search filter
        if (query.isNotEmpty()) {
            result = result.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                    book.authors.any { it.contains(query, ignoreCase = true) }
            }
        }

        // Reading status filter
        if (status != null) {
            result = result.filter { it.readingStatus == status }
        }

        // Sort
        result = when (currentState.sortOption) {
            LibrarySortOption.RECENTLY_ADDED -> result.sortedByDescending { it.dateAdded ?: 0L }
            LibrarySortOption.TITLE_AZ -> result.sortedBy { it.title.lowercase() }
            LibrarySortOption.AUTHOR_AZ -> result.sortedBy {
                it.authors.firstOrNull()?.lowercase() ?: ""
            }
        }

        _state.update { it.copy(filteredBooks = result) }
    }
}
