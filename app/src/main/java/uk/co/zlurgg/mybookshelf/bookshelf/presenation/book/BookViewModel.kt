package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseAction

class BookViewModel(): ViewModel() {
    private val _state = MutableStateFlow(BookState())
    val state: StateFlow<BookState> = _state

    fun onAction(action: BookAction) {
        when (action) {
            is BookAction.OnAddToBookShelfClick -> {
                _state.update {
                    it.copy(
                        book = it.book,
                    )
                }
            }
            is BookAction.OnPurchaseClick -> {}
            is BookAction.OnRateBookClick -> {}
        }
    }
}