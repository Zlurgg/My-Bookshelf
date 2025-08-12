package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class BookshelfViewModel : ViewModel() {

    private val _state = MutableStateFlow(BookshelfState())
    val state: StateFlow<BookshelfState> = _state

    fun onAction(action: BookshelfAction) {
        when (action) {
            is BookshelfAction.OnAddBook -> {
                _state.update { current ->
                    current.copy(
                        books = current.books + action.book
                    )
                }
            }

            is BookshelfAction.OnRemoveBook -> {
                _state.update { current ->
                    current.copy(
                        books = current.books.filterNot { it.id == action.book.id },
                        recentlyDeleted = action.book
                    )
                }
            }

            is BookshelfAction.OnUndoRemove -> {
                _state.update { current ->
                    current.recentlyDeleted?.let { deletedBook ->
                        current.copy(
                            books = current.books + deletedBook,
                            recentlyDeleted = null
                        )
                    } ?: current
                }
            }
            is BookshelfAction.OnBackClick -> {
                // navigation handled in compose root
            }
            is BookshelfAction.OnSearchClick -> {

            }
            is BookshelfAction.OnBookClick -> {

            }
        }
    }
}
