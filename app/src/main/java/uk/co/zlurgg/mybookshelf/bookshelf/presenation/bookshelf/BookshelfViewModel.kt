package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookshelfViewModel(): ViewModel() {
    private val _state = MutableStateFlow(BookshelfState())
    val state: StateFlow<BookshelfState> = _state
}