package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookSearchViewModel: ViewModel() {

    private val _state = MutableStateFlow(BookSearchState())
    val state: StateFlow<BookSearchState> = _state

}