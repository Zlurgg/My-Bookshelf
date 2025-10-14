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
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
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
                    when (val deleteResult = bookcaseUseCases.deleteShelf.execute(action.bookshelf.id)) {
                        is Result.Success -> {
                            // Success - optimistic update already applied
                        }
                        is Result.Error -> {
                            // Revert UI on failure
                            _state.update { current ->
                                current.copy(
                                    bookshelves = current.bookshelves + action.bookshelf,
                                    recentlyDeleted = null,
                                    errorMessage = ErrorFormatter.formatDataErrorMessage(deleteResult.error, "remove shelf")
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
                        when (val restoreResult = bookcaseUseCases.deleteShelf.restore(toRestore)) {
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
                                        errorMessage = ErrorFormatter.formatDataErrorMessage(restoreResult.error, "restore shelf")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is BookcaseAction.OnBookshelfClick -> {
                // Navigation is handled by the screen root
            }

            is BookcaseAction.ShowRenameDialog -> {
                _state.update {
                    it.copy(
                        showRenameDialog = true,
                        shelfToRename = action.bookshelf
                    )
                }
            }

            is BookcaseAction.DismissRenameDialog -> {
                _state.update {
                    it.copy(
                        showRenameDialog = false,
                        shelfToRename = null,
                        renameError = null
                    )
                }
            }

            is BookcaseAction.OnRenameShelf -> {
                renameShelf(action.shelfId, action.newName)
            }

            is BookcaseAction.ShowChangeStyleDialog -> {
                _state.update {
                    it.copy(
                        showChangeStyleDialog = true,
                        shelfToChangeStyle = action.bookshelf
                    )
                }
            }

            is BookcaseAction.DismissChangeStyleDialog -> {
                _state.update {
                    it.copy(
                        showChangeStyleDialog = false,
                        shelfToChangeStyle = null
                    )
                }
            }

            is BookcaseAction.OnChangeStyle -> {
                changeShelfStyle(action.shelfId, action.newStyle)
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
                            errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "add shelf")
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
                    val error = if (e is Exception) {
                        ErrorMapper.mapExceptionToDataError(e)
                    } else {
                        DataError.Local.UNKNOWN
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(error, "load shelves")
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
                            errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "reorder shelves")
                        )
                    }
                    loadBookshelves()
                }
            }
        }
    }

    private fun renameShelf(shelfId: String, newName: String) {
        viewModelScope.launch {
            when (val renameResult = bookcaseUseCases.renameShelf.execute(shelfId, newName)) {
                is Result.Success -> {
                    // Update the shelf name in the current state
                    _state.update { current ->
                        val updatedShelves = current.bookshelves.map { shelf ->
                            if (shelf.id == shelfId) {
                                shelf.copy(name = newName)
                            } else {
                                shelf
                            }
                        }
                        current.copy(
                            bookshelves = updatedShelves,
                            showRenameDialog = false,
                            shelfToRename = null,
                            operationSuccess = true,
                            errorMessage = null,
                            renameError = null
                        )
                    }
                }
                is Result.Error -> {
                    // Set inline error and keep dialog open so user can see it
                    _state.update {
                        it.copy(
                            renameError = ErrorFormatter.formatDataErrorMessage(renameResult.error, "rename shelf")
                        )
                    }
                }
            }
        }
    }

    private fun changeShelfStyle(shelfId: String, newStyle: ShelfStyle) {
        viewModelScope.launch {
            when (val styleResult = bookcaseUseCases.updateShelfStyle.execute(shelfId, newStyle)) {
                is Result.Success -> {
                    // Update the shelf style in the current state
                    _state.update { current ->
                        val updatedShelves = current.bookshelves.map { shelf ->
                            if (shelf.id == shelfId) {
                                shelf.copy(shelfStyle = newStyle)
                            } else {
                                shelf
                            }
                        }
                        current.copy(
                            bookshelves = updatedShelves,
                            showChangeStyleDialog = false,
                            shelfToChangeStyle = null,
                            operationSuccess = true,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    // Show error message
                    _state.update {
                        it.copy(
                            errorMessage = ErrorFormatter.formatDataErrorMessage(styleResult.error, "change shelf style")
                        )
                    }
                }
            }
        }
    }

}

