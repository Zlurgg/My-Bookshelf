package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class BookViewModel(): ViewModel() {
    private val _state = MutableStateFlow(BookState(
        book = TODO() // this will need changing
    ))
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