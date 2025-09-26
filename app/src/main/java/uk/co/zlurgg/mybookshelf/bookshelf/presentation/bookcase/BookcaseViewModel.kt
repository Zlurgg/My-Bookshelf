package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

@OptIn(ExperimentalCoroutinesApi::class)
class BookcaseViewModel(
    private val bookcaseUseCases: BookcaseUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state.asStateFlow()

    init {
        loadBookshelves()
    }

    fun onAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnAddBookshelfClick -> {
                addBookshelf(action.name, action.style)
            }

            is BookcaseAction.ShowAddDialog -> {
                _state.update { it.copy(showAddDialog = action.showDialog) }
            }

            is BookcaseAction.ResetOperationState -> {
                _state.update {
                    it.copy(
                        operationSuccess = false,
                        errorMessage = null
                    )
                }
            }

            is BookcaseAction.ToggleReorderMode -> {
                _state.update { it.copy(isReorderMode = !it.isReorderMode) }
            }

            is BookcaseAction.OnReorderShelf -> {
                reorderShelf(action.bookshelf, action.newPosition)
            }

            is BookcaseAction.OnRemoveBookShelf -> {
                // Optimistic UI update
                _state.update {
                    it.copy(
                        bookshelves = it.bookshelves - action.bookshelf,
                        recentlyDeleted = action.bookshelf,
                    )
                }
                // Persist deletion
                viewModelScope.launch {
                    when (bookcaseUseCases.deleteShelf.execute(action.bookshelf.id)) {
                        is Result.Success -> {
                            // Success - optimistic update already applied
                        }
                        is Result.Error -> {
                            // Revert UI on failure
                            _state.update { current ->
                                current.copy(
                                    bookshelves = current.bookshelves + action.bookshelf,
                                    recentlyDeleted = null,
                                    errorMessage = ErrorFormatter.formatOperationError("remove shelf", Exception("Delete operation failed"))
                                )
                            }
                        }
                    }
                }
            }

            is BookcaseAction.OnUndoRemove -> {
                val toRestore = state.value.recentlyDeleted
                if (toRestore != null) {
                    viewModelScope.launch {
                        when (bookcaseUseCases.deleteShelf.restore(toRestore)) {
                            is Result.Success -> {
                                _state.update { current ->
                                    current.copy(
                                        bookshelves = current.bookshelves + toRestore,
                                        recentlyDeleted = null,
                                        operationSuccess = true
                                    )
                                }
                            }
                            is Result.Error -> {
                                _state.update { current ->
                                    current.copy(
                                        errorMessage = ErrorFormatter.formatOperationError("restore shelf", Exception("Restore operation failed"))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is BookcaseAction.OnBookshelfClick -> {
                // no-op: handled by the screen root for navigation
            }
        }
    }

    private fun addBookshelf(name: String, style: ShelfStyle) {
        viewModelScope.launch {
            when (val result = bookcaseUseCases.createShelf.execute(name, style, state.value.bookshelves)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            bookshelves = it.bookshelves + result.data,
                            isLoading = false,
                            operationSuccess = true,
                            showAddDialog = false
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatOperationError("add shelf", Exception("Add operation failed"))
                        )
                    }
                }
            }
        }
    }

    private fun loadBookshelves() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            bookcaseUseCases.getAllShelves.execute()
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatOperationError("load shelves", Exception("Load operation failed"))
                        )
                    }
                }
                .collect { bookcase ->
                    _state.update {
                        it.copy(
                            bookshelves = bookcase.bookshelves,
                            bookCounts = bookcase.bookCounts,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun reorderShelf(shelf: Bookshelf, newPosition: Int) {
        viewModelScope.launch {
            val currentShelves = state.value.bookshelves

            when (val result = bookcaseUseCases.reorderShelves.execute(shelf, newPosition, currentShelves)) {
                is Result.Success -> {
                    // Optimistic UI update with the reordered shelves
                    _state.update { it.copy(bookshelves = result.data) }
                }
                is Result.Error -> {
                    // Revert on error by reloading from database
                    _state.update {
                        it.copy(
                            errorMessage = ErrorFormatter.formatOperationError("reorder shelves", Exception("Reorder operation failed"))
                        )
                    }
                    loadBookshelves()
                }
            }
        }
    }

}

