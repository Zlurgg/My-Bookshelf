package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookcaseViewModel : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state

    fun onAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnAddBookshelfClick -> {
                _state.update {
                    it.copy(
                        bookshelves = it.bookshelves,
                        showAddDialog = false
                    )
                }
            }

            is BookcaseAction.OnRemoveBookShelf -> {
                viewModelScope.launch {

                }
            }

            is BookcaseAction.OnUndoRemove -> {
                viewModelScope.launch {

                }
            }

            BookcaseAction.OnAddBookshelfDialogOpen -> {
                _state.update { it.copy(showAddDialog = true) }
            }

            BookcaseAction.OnAddBookshelfDialogDismiss -> {
                _state.update { it.copy(showAddDialog = false) }
            }

            is BookcaseAction.OnBookshelfClick -> {
                // no-op: handled by the screen root for navigation
            }
        }
    }
}