package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class BookSearchViewModel: ViewModel() {

    private val _state = MutableStateFlow(BookSearchState())
    val state: StateFlow<BookSearchState> = _state

    fun onAction(action: BookSearchAction) {
        when (action) {
            is BookSearchAction.OnBookClick -> {

            }
            is BookSearchAction.OnSearchQueryChange -> {
                _state.update {
                    it.copy(searchQuery = action.query)
                }
            }

        }
    }
}