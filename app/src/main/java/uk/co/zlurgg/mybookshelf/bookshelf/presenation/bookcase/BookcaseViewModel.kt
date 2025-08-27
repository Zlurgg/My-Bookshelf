package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookcase.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial
import java.util.UUID

class BookcaseViewModel(
    private val repository: BookcaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state
    init {
        loadBookshelves()
    }

    fun onAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnAddBookshelfClick -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy()
                    }
                    try {
                        val newShelf = Bookshelf(
                            id = UUID.randomUUID().toString(),
                            name = action.name,
                            bookCount = 0,
                            shelfMaterial = ShelfMaterial.entries.toTypedArray().random()
                        )
                        repository.addShelf(newShelf)
                        _state.update {
                            it.copy(
                                bookshelves = it.bookshelves + newShelf,
                                isLoading = false,
                                operationSuccess = true,
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to add shelf: ${e.message}",
                            )
                        }
                    }
                }
            }
            is BookcaseAction.ResetOperationState -> {
                _state.update {
                    it.copy(
                        operationSuccess = false,
                        errorMessage = null
                    )
                }
            }

            is BookcaseAction.OnRemoveBookShelf -> {
                // Immediately remove from UI state
                _state.update {
                    it.copy(
                        bookshelves = it.bookshelves - action.bookshelf,
                        recentlyDeleted = action.bookshelf,
                        // For undo
                    )
                }
            }

            is BookcaseAction.OnUndoRemove -> {
                viewModelScope.launch {

                }
            }

            is BookcaseAction.OnBookshelfClick -> {
                // no-op: handled by the screen root for navigation
            }

            is BookcaseAction.OnChangeShelfMaterial -> {

            }
        }
    }

    private fun loadBookshelves() {
        viewModelScope.launch {
            _state.update { it.copy() }
            try {
                val shelves = repository.getAllShelves().first()
                _state.update {
                    it.copy(
                        bookshelves = shelves,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to add shelf: ${e.message}",
                    )
                }
            }
        }
    }
}

