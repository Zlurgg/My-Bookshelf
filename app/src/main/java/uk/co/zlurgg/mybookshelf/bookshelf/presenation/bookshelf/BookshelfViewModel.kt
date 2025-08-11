package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseState

class BookshelfViewModel(): ViewModel() {
    private val _state = MutableStateFlow(BookshelfState())
    val state: StateFlow<BookshelfState> = _state
}