package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookDetailsViewModel(): ViewModel(
//    private val bookRepository: BookRepository,
//    private val savedStateHandle: SavedStateHandle
) {

//    private val bookId = savedStateHandle.toRoute<Route.BookDetail>().id
//    private val bookShelfId = savedStateHandle.toRoute<Route.BookDetail>().id
    private val _state = MutableStateFlow(BookDetailsState())
    val state = _state
        .onStart {
            fetchBookDescription()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )
    fun onAction(action: BookDetailsAction) {
        when (action) {
            is BookDetailsAction.OnAddBookToBookshelfClick -> {
                viewModelScope.launch {
                    // update book with bookshelf id
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
            is BookDetailsAction.OnRateBookDetailsClick -> {
                _state.update { it.copy(
                    book = it.book?.copy(
                        ratingCount = it.rating
                    )
                ) }
            }
        }
    }

    private fun fetchBookDescription() {
        viewModelScope.launch {
//            bookRepository
//                .getBookDescription(bookId)
//                .onSuccess { description ->
//                    _state.update { it.copy(
//                        book = it.book?.copy(
//                            description = description
//                        ),
//                        isLoading = false
//                    ) }
//                }
        }
    }
}