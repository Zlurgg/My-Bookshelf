package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookDetailsViewModel(): ViewModel(
//    private val bookRepository: BookRepository,
//    private val savedStateHandle: SavedStateHandle
) {

//    private val bookId = savedStateHandle.toRoute<Route.BookDetail>().id
    private val _state = MutableStateFlow(BookDetailsState())
    val state: StateFlow<BookDetailsState> = _state

    fun onAction(action: BookDetailsAction) {
        when (action) {
            is BookDetailsAction.OnAddToBookDetailsShelfClick -> {
                _state.update {
                    it.copy(
                        book = it.book,
                    )
                }
            }
            is BookDetailsAction.OnPurchaseClick -> {
                viewModelScope.launch {
                    if(state.value.isPurchased) {
//                        bookRepository.deleteFromPurchased(bookId)
                    } else {
                        state.value.book?.let { book ->
//                            bookRepository.markAsPurchased(book)
                        }
                    }
                }
            }
            is BookDetailsAction.OnRateBookDetailsClick -> {}
        }
    }
}